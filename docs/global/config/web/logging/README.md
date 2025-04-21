# 로그 뷰어 설정

이 디렉토리는 Fream 백엔드 애플리케이션의 로그 조회 관련 설정을 포함합니다.

## LogViewerConfig

로그 뷰어 관련 설정을 담당하는 클래스입니다.

```java
@Configuration
@EnableMethodSecurity
public class LogViewerConfig {
    @Value("${logging.file.path:/home/ubuntu/springlog}")
    private String logDirectoryPath;

    @Bean
    public String logDirectoryPath() {
        return logDirectoryPath;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("https://www.pinjun.xyz"));
        configuration.setAllowedMethods(Arrays.asList("GET"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/logs/**", configuration);
        return source;
    }
}
```

### 주요 기능

- **로그 디렉토리 설정**: 로그 파일이 저장되는 경로를 설정합니다.
- **CORS 설정**: 로그 API에 대한 CORS 정책을 설정합니다.
- **메서드 보안 활성화**: `@PreAuthorize` 등의 애노테이션을 통한 메서드 수준 보안을 활성화합니다.

## LogController

로그 파일 목록 조회 및 내용 조회를 위한 REST API 컨트롤러입니다.

```java
@Slf4j
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
public class LogController {
    private final String logDirectoryPath;
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+"   // 타임스탬프 그룹
                    + "\\[(.*?)\\]\\s+"                                           // 스레드 이름 그룹
                    + "(ERROR|WARN|INFO|DEBUG|TRACE)\\s+"                         // 로그 레벨 그룹
                    + "(.*?)\\s+-\\s+"                                            // 로거 이름 그룹
                    + "(.*)");                                                    // 메시지 그룹
    
    public LogController(@Qualifier("logDirectoryPath") String logDirectoryPath) {
        this.logDirectoryPath = logDirectoryPath;
    }
    
    // 로그 파일 목록 조회
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAvailableLogFiles() {
        try {
            Path logDirectory = Paths.get(logDirectoryPath);
            if (!Files.exists(logDirectory) || !Files.isDirectory(logDirectory)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Collections.singletonMap("error", "로그 디렉토리를 찾을 수 없습니다."));
            }

            List<LogFileDTO> logFiles = Files.list(logDirectory)
                    .filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".log"))
                    .map(path -> {
                        try {
                            return new LogFileDTO(
                                    path.getFileName().toString(),
                                    Files.size(path),
                                    Files.getLastModifiedTime(path).toInstant().toString()
                            );
                        } catch (IOException e) {
                            log.error("파일 정보 읽기 실패: {}", path, e);
                            return new LogFileDTO(path.getFileName().toString(), 0L, "");
                        }
                    })
                    .sorted(Comparator.comparing(LogFileDTO::getLastModified).reversed())
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("logFiles", logFiles);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("로그 파일 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("error", "로그 파일 목록을 조회할 수 없습니다."));
        }
    }
    
    // 로그 파일 내용 조회
    @GetMapping("/{fileName}")
    public ResponseEntity<LogResponseDTO> getLogContent(
            @PathVariable String fileName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "true") boolean reverse,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {

        // 경로 조작 방지
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.badRequest()
                    .body(LogResponseDTO.builder().error("잘못된 파일 이름입니다.").build());
        }

        Path logFile = Paths.get(logDirectoryPath, fileName);

        if (!Files.exists(logFile) || !Files.isRegularFile(logFile)) {
            return ResponseEntity.notFound().build();
        }

        try {
            // 날짜 범위 파싱
            LocalDateTime startDateTime = null;
            LocalDateTime endDateTime = null;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            if (startTime != null && !startTime.isEmpty()) {
                startDateTime = LocalDateTime.parse(startTime, formatter);
            }

            if (endTime != null && !endTime.isEmpty()) {
                endDateTime = LocalDateTime.parse(endTime, formatter);
            }

            // 로그 내용 읽기
            LogResponseDTO response = readLogFileWithPaging(
                    logFile, page, size, search, level, reverse, startDateTime, endDateTime);

            response.setFileName(fileName);
            response.setPage(page);
            response.setSize(size);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("로그 파일 읽기 중 오류 발생: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LogResponseDTO.builder()
                            .error("로그 파일을 읽는 중 오류가 발생했습니다.")
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(LogResponseDTO.builder()
                            .error(e.getMessage())
                            .build());
        }
    }
    
    // 로그 파일 내용 페이징 처리하여 읽기
    private LogResponseDTO readLogFileWithPaging(
            Path logFile, int page, int size, String search, String level,
            boolean reverse, LocalDateTime startTime, LocalDateTime endTime) throws IOException {

        // 유효성 검사
        if (page < 0 || size <= 0 || size > 1000) {
            throw new IllegalArgumentException("잘못된 페이징 파라미터입니다. (0 ≤ page, 0 < size ≤ 1000)");
        }

        long fileSize = Files.size(logFile);
        if (fileSize == 0) {
            return LogResponseDTO.builder()
                    .totalLines(0)
                    .totalFilteredLines(0)
                    .content(new ArrayList<>())
                    .build();
        }

        // 역방향 읽기를 위한 설정
        List<LogLineDTO> resultLines = new ArrayList<>();
        int foundLines = 0;
        int totalFilteredLines = 0;

        try (RandomAccessFile raf = new RandomAccessFile(logFile.toFile(), "r")) {
            // 버퍼 크기 설정
            final int BUFFER_SIZE = 8192;
            byte[] buffer = new byte[BUFFER_SIZE];

            // 로그 라인 시작 위치 추적
            List<Long> linePositions = new ArrayList<>();
            long position = 0;

            // 로그 파일의 모든 라인 위치 인덱싱 (대용량 파일에서는 최적화 필요)
            while (position < fileSize) {
                linePositions.add(position);
                raf.seek(position);
                String line = raf.readLine();
                if (line == null) break;

                // UTF-8 처리를 위한 바이트 길이 계산
                position += line.getBytes("UTF-8").length + 1; // +1 for newline
            }

            // 총 라인 수
            int totalLines = linePositions.size();
            foundLines = totalLines;

            // 필터링 및 페이징을 위한 처리
            List<Integer> filteredLineIndices = new ArrayList<>();

            for (int i = 0; i < totalLines; i++) {
                int lineIndex = reverse ? totalLines - 1 - i : i;
                long linePosition = linePositions.get(lineIndex);

                raf.seek(linePosition);
                String line = raf.readLine();
                if (line == null) continue;

                // 로그 라인 파싱
                LogLineDTO logLine = parseLogLine(line);

                // 필터 적용
                if (matchesFilter(logLine, search, level, startTime, endTime)) {
                    filteredLineIndices.add(lineIndex);
                    totalFilteredLines++;
                }
            }

            // 페이징 적용
            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, filteredLineIndices.size());

            if (startIndex < filteredLineIndices.size()) {
                for (int i = startIndex; i < endIndex; i++) {
                    int lineIndex = filteredLineIndices.get(i);
                    long linePosition = linePositions.get(lineIndex);

                    raf.seek(linePosition);
                    String line = raf.readLine();
                    if (line == null) continue;

                    LogLineDTO logLine = parseLogLine(line);
                    logLine.setLineNumber(lineIndex + 1); // 1-based line number
                    resultLines.add(logLine);
                }
            }
        }

        return LogResponseDTO.builder()
                .totalLines(foundLines)
                .totalFilteredLines(totalFilteredLines)
                .content(resultLines)
                .build();
    }
    
    // 로그 라인 파싱
    private LogLineDTO parseLogLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String timestamp = matcher.group(1);
            String thread = matcher.group(2);
            String level = matcher.group(3);
            String logger = matcher.group(4);
            String message = matcher.group(5);

            return LogLineDTO.builder()
                    .timestamp(timestamp)
                    .thread(thread)
                    .level(level)
                    .logger(logger)
                    .message(message)
                    .rawLine(line)
                    .build();
        } else {
            // 패턴에 맞지 않는 라인은 원시 로그로 처리
            return LogLineDTO.builder()
                    .rawLine(line)
                    .build();
        }
    }
    
    // 필터 적용
    private boolean matchesFilter(
            LogLineDTO logLine, String search, String level,
            LocalDateTime startTime, LocalDateTime endTime) {

        // 검색어 필터
        if (search != null && !search.isEmpty() &&
                !logLine.getRawLine().toLowerCase().contains(search.toLowerCase())) {
            return false;
        }

        // 로그 레벨 필터
        if (level != null && !level.isEmpty() &&
                logLine.getLevel() != null && !logLine.getLevel().equalsIgnoreCase(level)) {
            return false;
        }

        // 시간 범위 필터
        if ((startTime != null || endTime != null) && logLine.getTimestamp() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                LocalDateTime logTime = LocalDateTime.parse(logLine.getTimestamp(), formatter);

                if (startTime != null && logTime.isBefore(startTime)) {
                    return false;
                }

                if (endTime != null && logTime.isAfter(endTime)) {
                    return false;
                }
            } catch (Exception e) {
                // 날짜 파싱 오류 시 무시하고 계속 진행
                log.debug("로그 날짜 파싱 실패: {}", logLine.getTimestamp());
            }
        }

        return true;
    }
}
```

### 주요 기능

- **로그 파일 목록 조회**: 애플리케이션의 로그 파일 목록과 메타데이터를 조회합니다.
- **로그 내용 조회**: 특정 로그 파일의 내용을 페이징하여 조회합니다.
- **로그 필터링**: 로그 레벨, 검색어, 시간 범위 등으로 로그를 필터링합니다.
- **로그 라인 파싱**: 로그 패턴에 맞게 로그 라인을 파싱하여 구조화된 정보로 변환합니다.

## 로그 관련 DTO

로그 조회에 사용되는 데이터 전송 객체들입니다.

```java
// 로그 파일 정보 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogFileDTO {
    private String fileName;
    private Long fileSize;
    private String lastModified;
}

// 로그 한 줄의 파싱된 정보 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogLineDTO {
    private int lineNumber;
    private String timestamp;
    private String thread;
    private String level;
    private String logger;
    private String message;
    private String rawLine;
}

// 로그 조회 응답 DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogResponseDTO {
    private String fileName;
    private int page;
    private int size;
    private int totalLines;
    private int totalFilteredLines;
    private List<LogLineDTO> content;
    private String error;
}
```

## 로그 뷰어 사용 예시 (프론트엔드)

로그 뷰어를 사용하는 프론트엔드 코드 예시입니다.

```javascript
// 로그 파일 목록 조회
async function getLogFiles() {
  const response = await fetch('/api/logs', {
    headers: {
      'Authorization': 'Bearer ' + accessToken
    }
  });
  const data = await response.json();
  return data.logFiles;
}

// 로그 내용 조회
async function getLogContent(fileName, options = {}) {
  const {
    page = 0,
    size = 100,
    search = '',
    level = '',
    reverse = true,
    startTime = '',
    endTime = ''
  } = options;
  
  const params = new URLSearchParams({
    page,
    size,
    ...(search && { search }),
    ...(level && { level }),
    reverse,
    ...(startTime && { startTime }),
    ...(endTime && { endTime })
  });
  
  const response = await fetch(`/api/logs/${fileName}?${params}`, {
    headers: {
      'Authorization': 'Bearer ' + accessToken
    }
  });
  
  return await response.json();
}

// 사용 예시
async function displayLogs() {
  // 로그 파일 목록 조회
  const logFiles = await getLogFiles();
  
  // 첫 번째 로그 파일의 내용 조회
  if (logFiles.length > 0) {
    const logContent = await getLogContent(logFiles[0].fileName, {
      level: 'ERROR',
      search: 'database',
      startTime: '2023-01-01 00:00:00'
    });
    
    // 로그 내용 표시
    const logLines = logContent.content;
    for (const line of logLines) {
      console.log(`[${line.timestamp}] [${line.level}] - ${line.message}`);
    }
  }
}

// 로그 레벨별 색상 매핑
const logLevelColors = {
  'ERROR': '#FF5252',
  'WARN': '#FFC107',
  'INFO': '#2196F3',
  'DEBUG': '#4CAF50',
  'TRACE': '#9E9E9E'
};
```

## 로깅 설정 (application.yml)

애플리케이션의 로깅 설정 예시입니다:

```yaml
logging:
  level:
    org.hibernate.SQL: debug
    org.springframework.security: DEBUG
    com.fream: INFO  # 프로젝트 패키지에 맞게 수정
  file:
    path: /logs  # 로그 파일 저장 경로
    name: /logs/spring-app.log  # 로그 파일 이름
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  logback:
    rollingpolicy:
      max-file-size: 10MB  # 개별 로그 파일 최대 크기
      max-history: 30      # 보관할 로그 파일 수
      total-size-cap: 3GB  # 전체 로그 파일 최대 크기
```

## 로그 뷰어 화면 구성 (예시)

로그 뷰어 UI 구성에 대한 권장 사항입니다:

1. **로그 파일 선택**: 드롭다운으로 로그 파일 목록 제공
2. **필터링 옵션**:
    - 로그 레벨 필터 (ERROR, WARN, INFO, DEBUG, TRACE)
    - 검색어 필터
    - 시간 범위 필터
3. **페이징 컨트롤**:
    - 페이지 번호 선택
    - 페이지 크기 선택
    - 이전/다음 페이지 버튼
4. **로그 표시 영역**:
    - 타임스탬프
    - 로그 레벨 (색상으로 구분)
    - 메시지
    - 스레드 정보 (접었다 펼칠 수 있는 형태)
    - 로거 정보 (접었다 펼칠 수 있는 형태)
5. **추가 기능**:
    - 새로고침 버튼
    - 정순/역순 정렬 옵션
    - CSV/JSON 내보내기 옵션

## 보안 고려 사항

로그 조회 API는 민감한 정보에 접근할 수 있으므로 다음과 같은 보안 조치가 필요합니다:

1. **인증 및 권한 검사**: `@PreAuthorize("hasRole('ADMIN')")` 애노테이션으로 관리자만 접근 가능하도록 제한
2. **경로 조작 방지**: 파일 이름에 `../`, `/` 등의 문자가 포함되지 않도록 검증
3. **CORS 설정**: 허용된 도메인에서만 API 접근 가능하도록 설정
4. **로깅**: 로그 조회 요청 자체를 로깅하여 감사 추적 제공
5. **속도 제한**: 과도한 API 호출을 방지하기 위한 속도 제한 설정 고려