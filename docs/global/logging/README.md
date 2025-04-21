# 로깅 모듈

이 디렉토리는 Fream 백엔드 애플리케이션의 로그 관리 및 모니터링을 위한 컴포넌트들을 포함합니다.

## LogController

로그 파일 조회 및 분석을 위한 API를 제공하는 컨트롤러입니다.

```java
@Slf4j
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
public class LogController {
    @Value("${logging.file.path:/path/to/springlog}")
    private String logDirectoryPath;
    
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+"   // 타임스탬프 그룹
                    + "\\[(.*?)\\]\\s+"                                           // 스레드 이름 그룹
                    + "(ERROR|WARN|INFO|DEBUG|TRACE)\\s+"                         // 로그 레벨 그룹
                    + "(.*?)\\s+-\\s+"                                            // 로거 이름 그룹
                    + "(.*)");                                                    // 메시지 그룹
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAvailableLogFiles();
    
    @GetMapping("/{fileName}")
    public ResponseEntity<LogResponseDTO> getLogContent(
            @PathVariable String fileName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "true") boolean reverse,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime);
            
    private LogResponseDTO readLogFileWithPaging(
            Path logFile, int page, int size, String search, String level,
            boolean reverse, LocalDateTime startTime, LocalDateTime endTime) throws IOException;
            
    private LogLineDTO parseLogLine(String line);
    
    private boolean matchesFilter(
            LogLineDTO logLine, String search, String level,
            LocalDateTime startTime, LocalDateTime endTime);
}
```

### 주요 기능

- **로그 파일 목록 조회**: 애플리케이션의 로그 파일 목록과 메타데이터를 조회합니다.
- **로그 내용 조회**: 특정 로그 파일의 내용을 페이징하여 조회합니다.
- **로그 필터링**: 로그 레벨, 검색어, 시간 범위 등으로 로그를 필터링합니다.
- **로그 라인 파싱**: 로그 패턴에 맞게 로그 라인을 파싱하여 구조화된 정보로 변환합니다.

### 로그 조회 API

1. **로그 파일 목록 조회**
    - 엔드포인트: `GET /api/logs`
    - 응답: 로그 파일 목록, 크기, 마지막 수정 시간

2. **로그 내용 조회**
    - 엔드포인트: `GET /api/logs/{fileName}`
    - 파라미터:
        - `page`: 페이지 번호 (기본값: 0)
        - `size`: 페이지 크기 (기본값: 100)
        - `search`: 검색어 (선택)
        - `level`: 로그 레벨 (선택, ERROR/WARN/INFO/DEBUG/TRACE)
        - `reverse`: 역순 조회 여부 (기본값: true, 최신 로그부터)
        - `startTime`: 시작 시간 (선택, yyyy-MM-dd HH:mm:ss 형식)
        - `endTime`: 종료 시간 (선택, yyyy-MM-dd HH:mm:ss 형식)
    - 응답: 로그 라인 목록, 페이징 정보, 총 라인 수

### 로그 파싱 패턴

애플리케이션의 로그 형식에 맞게 정규식 패턴을 정의하여 다음 정보를 추출합니다:

1. 타임스탬프: `2023-04-21 13:45:28.123`
2. 스레드 이름: `[http-nio-8080-exec-1]`
3. 로그 레벨: `INFO`, `ERROR`, `WARN`, `DEBUG`, `TRACE`
4. 로거 이름: `com.fream.back.domain.user.controller.UserController`
5. 메시지: `사용자 로그인 성공: user@example.com`

### 관리자 접근 제한

로그 조회 API는 `@PreAuthorize("hasRole('ADMIN')")` 애노테이션을 통해 관리자 권한을 가진 사용자만 접근할 수 있도록 제한합니다.

### 효율적인 로그 파일 읽기

대용량 로그 파일을 효율적으로 처리하기 위해 다음과 같은 기술을 사용합니다:

- **RandomAccessFile**: 파일의 특정 위치로 직접 이동하여 읽기 가능
- **버퍼링**: 한 번에 일정량의 데이터만 메모리에 로드
- **라인 인덱싱**: 파일 내 각 라인의 시작 위치를 인덱싱하여 빠른 접근 제공
- **역방향 읽기**: 최신 로그부터 표시하기 위한 역방향 읽기 지원

### 사용 예시 (프론트엔드)

```javascript
// 로그 파일 목록 조회
async function getLogFiles() {
  const response = await fetch('/api/logs');
  const data = await response.json();
  return data.logFiles;
}

// 로그 내용 조회
async function getLogContent(fileName, page = 0, size = 100, options = {}) {
  const params = new URLSearchParams({
    page,
    size,
    ...options
  });
  
  const response = await fetch(`/api/logs/${fileName}?${params}`);
  return await response.json();
}

// 사용 예시
const logFiles = await getLogFiles();
const logContent = await getLogContent('spring-app.log', 0, 100, {
  level: 'ERROR',
  search: 'database connection',
  reverse: true
});
```

## 로깅 설정 (application.yml)

애플리케이션의 로깅 설정 예시입니다:

```yaml
logging:
  level:
    org.hibernate.SQL: debug
    org.springframework.security: DEBUG
    com.fream: INFO
  file:
    name: /logs/spring-app.log
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  logback:
    rollingpolicy:
      max-file-size: 10MB
      max-history: 30
      total-size-cap: 3GB
```

### 주요 설정

- **로그 레벨**: 각 패키지별 로그 출력 레벨을 설정합니다.
- **로그 파일**: 로그 파일 경로와 이름을 설정합니다.
- **로그 패턴**: 콘솔과 파일에 출력되는 로그 형식을 설정합니다.
- **로그 롤링 정책**: 로그 파일 크기 제한, 보관 기간, 전체 용량 제한을 설정합니다.