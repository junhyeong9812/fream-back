package com.fream.back.global.controller;

import com.fream.back.global.dto.LogFileDTO;
import com.fream.back.global.dto.LogResponseDTO;
import com.fream.back.global.dto.LogLineDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasRole('ADMIN')") // 관리자만 접근 가능
public class LogController {

    @Value("${logging.file.path:/path/to/springlog}")
    private String logDirectoryPath;

    // 로그 레벨을 파악하기 위한 패턴 (로그 형식에 맞게 조정 필요)
    private static final Pattern LOG_PATTERN = Pattern.compile(
            "(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+"   // 타임스탬프 그룹
                    + "\\[(.*?)\\]\\s+"                                           // 스레드 이름 그룹
                    + "(ERROR|WARN|INFO|DEBUG|TRACE)\\s+"                         // 로그 레벨 그룹
                    + "(.*?)\\s+-\\s+"                                            // 로거 이름 그룹
                    + "(.*)");                                                    // 메시지 그룹

    /**
     * 사용 가능한 로그 파일 목록을 반환합니다.
     */
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

    /**
     * 로그 파일의 내용을 페이징하여 반환합니다.
     * 역방향 읽기를 지원하여 최신 로그부터 볼 수 있습니다.
     */
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

    /**
     * 로그 파일을 효율적으로 읽고 페이징 처리하는 메서드
     */
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

    /**
     * 로그 라인을 파싱하여 구조화된 데이터로 변환
     */
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

    /**
     * 필터 조건에 맞는지 확인
     */
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