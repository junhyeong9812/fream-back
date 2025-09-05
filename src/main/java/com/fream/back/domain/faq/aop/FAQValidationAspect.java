package com.fream.back.domain.faq.aop;

import com.fream.back.domain.faq.aop.annotation.FAQValidation;
import com.fream.back.domain.faq.aop.annotation.FAQValidation.*;
import com.fream.back.domain.faq.dto.FAQCreateRequestDto;
import com.fream.back.domain.faq.dto.FAQUpdateRequestDto;
import com.fream.back.domain.faq.entity.FAQCategory;
import com.fream.back.domain.faq.exception.FAQErrorCode;
import com.fream.back.domain.faq.exception.FAQException;
import com.fream.back.domain.faq.repository.FAQRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * FAQ 검증 AOP Aspect
 * FAQ 생성/수정 시 입력 데이터의 유효성을 검증합니다.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@Order(1) // 다른 Aspect보다 먼저 실행
public class FAQValidationAspect {

    private final FAQRepository faqRepository;

    // 검증 결과 캐시 (동일 요청 중복 검증 방지)
    private final Map<Integer, ValidationResult> validationCache = new ConcurrentHashMap<>();

    // 금지어 목록
    private final Set<String> prohibitedWords = ConcurrentHashMap.newKeySet();

    // 검증 패턴들
    private static final Pattern HTML_INJECTION_PATTERN = Pattern.compile(
            "(?i)<(?!/?(?:p|div|span|br|hr|h[1-6]|strong|em|u|s|mark|ul|ol|li|dl|dt|dd|" +
                    "table|thead|tbody|tr|th|td|img|a|blockquote|pre|code)(?:\\s|>|/>))[^>]*>"
    );

    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "(?i)\\b(SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|" +
                    "UNION|DECLARE|CAST|CONVERT|SCRIPT|JAVASCRIPT|EVAL|WAITFOR|DELAY)\\b"
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(?i)(javascript:|vbscript:|on\\w+\\s*=|<script|<iframe|<object|<embed|<applet)"
    );

    // 이미지 파일의 Magic Numbers
    private static final Map<String, byte[]> IMAGE_MAGIC_NUMBERS = new HashMap<>();

    static {
        IMAGE_MAGIC_NUMBERS.put("jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        IMAGE_MAGIC_NUMBERS.put("jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
        IMAGE_MAGIC_NUMBERS.put("png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});
        IMAGE_MAGIC_NUMBERS.put("gif", new byte[]{0x47, 0x49, 0x46});
        IMAGE_MAGIC_NUMBERS.put("webp", new byte[]{0x52, 0x49, 0x46, 0x46});
    }

    @PostConstruct
    public void init() {
        initializeProhibitedWords();
        log.info("FAQ Validation Aspect initialized with {} prohibited words", prohibitedWords.size());
    }

    /**
     * FAQ 검증 포인트컷
     */
    @Pointcut("@annotation(com.fream.back.domain.faq.aop.annotation.FAQValidation)")
    public void validationPointcut() {}

    /**
     * FAQ 검증 수행
     */
    @Around("validationPointcut() && @annotation(validation)")
    public Object validateFAQ(ProceedingJoinPoint joinPoint, FAQValidation validation) throws Throwable {

        if (!validation.enabled()) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();

        try {
            // 우선순위에 따른 검증 실행
            ValidationContext context = createValidationContext(joinPoint, validation);

            // 캐시 확인
            int requestHash = Arrays.hashCode(joinPoint.getArgs());
            ValidationResult cachedResult = validationCache.get(requestHash);

            if (cachedResult != null && !cachedResult.isExpired()) {
                log.debug("Using cached validation result for request hash: {}", requestHash);
                if (!cachedResult.isValid()) {
                    return handleValidationFailure(cachedResult, validation, joinPoint);
                }
            } else {
                // 검증 수행
                ValidationResult result = performValidation(context);
                validationCache.put(requestHash, result);

                if (!result.isValid()) {
                    return handleValidationFailure(result, validation, joinPoint);
                }
            }

            // 비동기 검증
            if (validation.async()) {
                performAsyncValidation(context);
            }

            // 메서드 실행
            Object methodResult = joinPoint.proceed();

            long duration = System.currentTimeMillis() - startTime;
            log.debug("FAQ validation completed in {}ms", duration);

            return methodResult;

        } catch (Exception e) {
            log.error("Validation error: {}", e.getMessage(), e);
            throw e;
        } finally {
            // 캐시 정리 (1분 이상 된 항목 제거)
            cleanValidationCache();
        }
    }

    /**
     * 검증 컨텍스트 생성
     */
    private ValidationContext createValidationContext(ProceedingJoinPoint joinPoint,
                                                      FAQValidation validation) {
        ValidationContext context = new ValidationContext();
        context.setValidation(validation);
        context.setArgs(joinPoint.getArgs());
        context.setMethodName(joinPoint.getSignature().getName());

        // DTO 추출
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof FAQCreateRequestDto) {
                context.setCreateDto((FAQCreateRequestDto) arg);
            } else if (arg instanceof FAQUpdateRequestDto) {
                context.setUpdateDto((FAQUpdateRequestDto) arg);
            }
        }

        return context;
    }

    /**
     * 검증 수행
     */
    private ValidationResult performValidation(ValidationContext context) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();

        FAQValidation validation = context.getValidation();

        // 검증 타입별 처리
        for (ValidationType type : validation.validations()) {
            List<String> typeErrors = validateByType(type, context);
            errors.addAll(typeErrors);
        }

        // 권한 검증
        if (validation.requireAdmin()) {
            String permissionError = validateAdminPermission();
            if (permissionError != null) {
                errors.add(permissionError);
            }
        }

        result.setErrors(errors);
        result.setValid(errors.isEmpty());
        result.setTimestamp(System.currentTimeMillis());

        return result;
    }

    /**
     * 타입별 검증 수행
     */
    private List<String> validateByType(ValidationType type, ValidationContext context) {
        List<String> errors = new ArrayList<>();
        FAQValidation validation = context.getValidation();

        String question = extractQuestion(context);
        String answer = extractAnswer(context);
        List<MultipartFile> files = extractFiles(context);
        FAQCategory category = extractCategory(context);

        switch (type) {
            case CONTENT:
                validateContent(question, answer, errors);
                break;

            case LENGTH:
                validateLength(question, answer, validation, errors);
                break;

            case FILE:
                validateFiles(files, validation, errors);
                break;

            case CATEGORY:
                validateCategory(category, errors);
                break;

            case PROHIBITED_WORDS:
                if (validation.checkProhibitedWords()) {
                    checkProhibitedWords(question, answer, errors);
                }
                break;

            case INJECTION:
                if (validation.checkHtmlInjection()) {
                    checkHtmlInjection(question, answer, errors);
                }
                if (validation.checkSqlInjection()) {
                    checkSqlInjection(question, answer, errors);
                }
                break;

            case DUPLICATE:
                if (validation.checkDuplicate()) {
                    checkDuplicate(question, context, errors);
                }
                break;

            case PERMISSION:
                // 별도 처리
                break;
        }

        return errors;
    }

    /**
     * 컨텐츠 기본 검증
     */
    private void validateContent(String question, String answer, List<String> errors) {
        if (!StringUtils.hasText(question)) {
            errors.add("질문은 필수 입력 항목입니다.");
        }

        if (!StringUtils.hasText(answer)) {
            errors.add("답변은 필수 입력 항목입니다.");
        }
    }

    /**
     * 길이 검증
     */
    private void validateLength(String question, String answer, FAQValidation validation,
                                List<String> errors) {
        if (question != null) {
            int length = question.trim().length();
            if (length < validation.minQuestionLength()) {
                errors.add(String.format("질문은 최소 %d자 이상이어야 합니다. (현재: %d자)",
                        validation.minQuestionLength(), length));
            }
            if (length > validation.maxQuestionLength()) {
                errors.add(String.format("질문은 최대 %d자까지 가능합니다. (현재: %d자)",
                        validation.maxQuestionLength(), length));
            }
        }

        if (answer != null) {
            int length = answer.length();
            if (length < validation.minAnswerLength()) {
                errors.add(String.format("답변은 최소 %d자 이상이어야 합니다. (현재: %d자)",
                        validation.minAnswerLength(), length));
            }
            if (length > validation.maxAnswerLength()) {
                errors.add(String.format("답변은 최대 %d자까지 가능합니다. (현재: %d자)",
                        validation.maxAnswerLength(), length));
            }
        }
    }

    /**
     * 파일 검증
     */
    private void validateFiles(List<MultipartFile> files, FAQValidation validation,
                               List<String> errors) {
        if (files == null || files.isEmpty()) {
            return;
        }

        // 빈 파일 제거
        files = files.stream()
                .filter(file -> !file.isEmpty())
                .collect(Collectors.toList());

        // 파일 개수 검증
        if (files.size() > validation.maxFileCount()) {
            errors.add(String.format("파일은 최대 %d개까지 업로드 가능합니다. (현재: %d개)",
                    validation.maxFileCount(), files.size()));
            return; // 개수 초과시 추가 검증 불필요
        }

        Set<String> allowedExtensions = new HashSet<>(Arrays.asList(validation.allowedExtensions()));
        long totalSize = 0;

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                errors.add("파일명이 없는 파일이 있습니다.");
                continue;
            }

            // 파일 크기 검증
            long fileSize = file.getSize();
            if (fileSize > validation.maxFileSize()) {
                errors.add(String.format("파일 '%s'의 크기(%.2fMB)가 최대 허용 크기(%.2fMB)를 초과합니다.",
                        filename,
                        fileSize / (1024.0 * 1024.0),
                        validation.maxFileSize() / (1024.0 * 1024.0)));
            }
            totalSize += fileSize;

            // 확장자 검증
            String extension = getFileExtension(filename).toLowerCase();
            if (!allowedExtensions.contains(extension)) {
                errors.add(String.format("파일 '%s'의 확장자 '%s'는 허용되지 않습니다. (허용: %s)",
                        filename, extension, String.join(", ", allowedExtensions)));
            }

            // 파일 내용 검증 (Magic Number)
            if (!validateFileSignature(file, extension)) {
                errors.add(String.format("파일 '%s'의 내용이 확장자와 일치하지 않거나 손상되었을 수 있습니다.",
                        filename));
            }
        }

        // 전체 파일 크기 검증
        if (totalSize > validation.maxTotalFileSize()) {
            errors.add(String.format("전체 파일 크기(%.2fMB)가 최대 허용 크기(%.2fMB)를 초과합니다.",
                    totalSize / (1024.0 * 1024.0),
                    validation.maxTotalFileSize() / (1024.0 * 1024.0)));
        }
    }

    /**
     * 카테고리 검증
     */
    private void validateCategory(FAQCategory category, List<String> errors) {
        if (category == null) {
            errors.add("카테고리는 필수 선택 항목입니다.");
        }
    }

    /**
     * 금지어 검사
     */
    private void checkProhibitedWords(String question, String answer, List<String> errors) {
        Set<String> foundWords = new HashSet<>();
        String lowerQuestion = question != null ? question.toLowerCase() : "";
        String lowerAnswer = answer != null ? answer.toLowerCase() : "";

        for (String word : prohibitedWords) {
            if (lowerQuestion.contains(word.toLowerCase()) ||
                    lowerAnswer.contains(word.toLowerCase())) {
                foundWords.add(word);
            }
        }

        if (!foundWords.isEmpty()) {
            errors.add("금지된 단어가 포함되어 있습니다: " + String.join(", ", foundWords));
        }
    }

    /**
     * HTML 인젝션 검사
     */
    private void checkHtmlInjection(String question, String answer, List<String> errors) {
        if (question != null && HTML_INJECTION_PATTERN.matcher(question).find()) {
            errors.add("질문에 허용되지 않은 HTML 태그가 포함되어 있습니다.");
        }

        // 답변은 HTML 에디터를 사용하므로 더 관대하게 처리
        if (answer != null && XSS_PATTERN.matcher(answer).find()) {
            errors.add("답변에 보안 위험이 있는 스크립트가 포함되어 있습니다.");
        }
    }

    /**
     * SQL 인젝션 검사
     */
    private void checkSqlInjection(String question, String answer, List<String> errors) {
        if (question != null && SQL_INJECTION_PATTERN.matcher(question).find()) {
            errors.add("질문에 SQL 명령어로 의심되는 패턴이 포함되어 있습니다.");
        }

        if (answer != null && SQL_INJECTION_PATTERN.matcher(answer).find()) {
            // 답변은 코드 예제를 포함할 수 있으므로 경고만
            log.warn("Answer contains SQL-like patterns, please review: {}",
                    answer.substring(0, Math.min(answer.length(), 100)));
        }
    }

    /**
     * 중복 검사
     */
    private void checkDuplicate(String question, ValidationContext context, List<String> errors) {
        if (question == null) {
            return;
        }

        try {
            // 수정 시에는 자기 자신 제외
            Long excludeId = null;
            if (context.getUpdateDto() != null) {
                // ID 추출 로직 (실제 구현시 DTO에 ID 필드 필요)
                excludeId = extractIdFromContext(context);
            }

            boolean exists = checkQuestionExists(question, excludeId);

            if (exists) {
                errors.add("동일한 질문이 이미 등록되어 있습니다.");
            }
        } catch (Exception e) {
            log.error("Error checking duplicate question: {}", e.getMessage());
        }
    }

    /**
     * 관리자 권한 검증
     */
    private String validateAdminPermission() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return "로그인이 필요합니다.";
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.contains("ADMIN"));

        if (!isAdmin) {
            return "관리자 권한이 필요합니다.";
        }

        return null;
    }

    /**
     * 비동기 검증 수행
     */
    private void performAsyncValidation(ValidationContext context) {
        CompletableFuture.runAsync(() -> {
            try {
                // 외부 API 호출이나 복잡한 검증
                log.debug("Performing async validation for method: {}", context.getMethodName());

                // 예: 이미지 내용 상세 분석
                // 예: 외부 스팸 필터 API 호출
                // 예: 번역 API를 통한 다국어 금지어 검사

            } catch (Exception e) {
                log.error("Async validation error: {}", e.getMessage());
            }
        });
    }

    /**
     * 검증 실패 처리
     */
    private Object handleValidationFailure(ValidationResult result, FAQValidation validation,
                                           ProceedingJoinPoint joinPoint) throws Throwable {

        String errorMessage = validation.customErrorMessage().isEmpty() ?
                String.join("; ", result.getErrors()) :
                validation.customErrorMessage();

        ValidationFailureAction action = validation.onFailure();

        log.warn("Validation failed: {} - Action: {}", errorMessage, action);

        switch (action) {
            case THROW_EXCEPTION:
                throw new FAQException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, errorMessage);

            case LOG_AND_CONTINUE:
                log.warn("Validation failed but continuing: {}", errorMessage);
                return joinPoint.proceed();

            case RETURN_DEFAULT:
                return null;

            case SANITIZE:
                return sanitizeAndProceed(joinPoint, result);

            case REQUEST_CORRECTION:
                Map<String, Object> correctionResponse = new HashMap<>();
                correctionResponse.put("success", false);
                correctionResponse.put("errors", result.getErrors());
                correctionResponse.put("message", "입력 데이터 수정이 필요합니다.");
                return correctionResponse;

            default:
                throw new FAQException(FAQErrorCode.FAQ_INVALID_REQUEST_DATA, errorMessage);
        }
    }

    /**
     * 데이터 정제 후 진행
     */
    private Object sanitizeAndProceed(ProceedingJoinPoint joinPoint, ValidationResult result)
            throws Throwable {

        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof FAQCreateRequestDto) {
                FAQCreateRequestDto dto = (FAQCreateRequestDto) args[i];
                dto.setQuestion(sanitizeText(dto.getQuestion()));
                dto.setAnswer(sanitizeHtml(dto.getAnswer()));
            } else if (args[i] instanceof FAQUpdateRequestDto) {
                FAQUpdateRequestDto dto = (FAQUpdateRequestDto) args[i];
                dto.setQuestion(sanitizeText(dto.getQuestion()));
                dto.setAnswer(sanitizeHtml(dto.getAnswer()));
            }
        }

        return joinPoint.proceed(args);
    }

    // ===== 유틸리티 메서드 =====

    private String extractQuestion(ValidationContext context) {
        if (context.getCreateDto() != null) {
            return context.getCreateDto().getQuestion();
        }
        if (context.getUpdateDto() != null) {
            return context.getUpdateDto().getQuestion();
        }
        return null;
    }

    private String extractAnswer(ValidationContext context) {
        if (context.getCreateDto() != null) {
            return context.getCreateDto().getAnswer();
        }
        if (context.getUpdateDto() != null) {
            return context.getUpdateDto().getAnswer();
        }
        return null;
    }

    private List<MultipartFile> extractFiles(ValidationContext context) {
        if (context.getCreateDto() != null) {
            return context.getCreateDto().getFiles();
        }
        if (context.getUpdateDto() != null) {
            return context.getUpdateDto().getNewFiles();
        }
        return Collections.emptyList();
    }

    private FAQCategory extractCategory(ValidationContext context) {
        if (context.getCreateDto() != null) {
            return context.getCreateDto().getCategory();
        }
        if (context.getUpdateDto() != null) {
            return context.getUpdateDto().getCategory();
        }
        return null;
    }

    private Long extractIdFromContext(ValidationContext context) {
        // 실제 구현시 UpdateDto에서 ID 추출
        return null;
    }

    private boolean checkQuestionExists(String question, Long excludeId) {
        return faqRepository.findAll().stream()
                .filter(faq -> excludeId == null || !faq.getId().equals(excludeId))
                .anyMatch(faq -> faq.getQuestion().equalsIgnoreCase(question));
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private boolean validateFileSignature(MultipartFile file, String extension) {
        byte[] magicNumbers = IMAGE_MAGIC_NUMBERS.get(extension.toLowerCase());
        if (magicNumbers == null) {
            return true; // 검증할 수 없는 형식은 통과
        }

        try {
            byte[] fileHeader = new byte[magicNumbers.length];
            int bytesRead = file.getInputStream().read(fileHeader);

            if (bytesRead < magicNumbers.length) {
                return false;
            }

            return Arrays.equals(fileHeader, magicNumbers);
        } catch (IOException e) {
            log.error("Error reading file signature: {}", e.getMessage());
            return false;
        }
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return "";
        }

        // 기본 HTML 태그 제거
        text = text.replaceAll("<[^>]*>", "");

        // SQL 키워드 제거
        text = SQL_INJECTION_PATTERN.matcher(text).replaceAll("***");

        // 특수문자 이스케이프
        text = text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");

        return text.trim();
    }

    private String sanitizeHtml(String html) {
        if (html == null) {
            return "";
        }

        // XSS 패턴 제거
        html = XSS_PATTERN.matcher(html).replaceAll("");

        // 위험한 속성 제거
        html = html.replaceAll("\\son\\w+\\s*=", "");

        return html.trim();
    }

    private void initializeProhibitedWords() {
        // 기본 금지어 목록
        prohibitedWords.addAll(Arrays.asList(
                "spam", "스팸", "광고", "도박", "사기",
                "불법", "마약", "욕설1", "욕설2", "욕설3"
        ));

        // 실제로는 데이터베이스나 파일에서 로드
        loadProhibitedWordsFromDatabase();
    }

    private void loadProhibitedWordsFromDatabase() {
        // 데이터베이스에서 금지어 목록 로드
        try {
            // jdbcTemplate.query("SELECT word FROM prohibited_words", ...)
            log.debug("Loaded prohibited words from database");
        } catch (Exception e) {
            log.error("Failed to load prohibited words: {}", e.getMessage());
        }
    }

    private void cleanValidationCache() {
        long now = System.currentTimeMillis();
        validationCache.entrySet().removeIf(entry ->
                now - entry.getValue().getTimestamp() > 60000 // 1분 이상 된 캐시 제거
        );
    }

    // ===== 내부 클래스 =====

    /**
     * 검증 컨텍스트
     */
    private static class ValidationContext {
        private FAQValidation validation;
        private Object[] args;
        private String methodName;
        private FAQCreateRequestDto createDto;
        private FAQUpdateRequestDto updateDto;

        // Getters and Setters
        public FAQValidation getValidation() { return validation; }
        public void setValidation(FAQValidation validation) { this.validation = validation; }

        public Object[] getArgs() { return args; }
        public void setArgs(Object[] args) { this.args = args; }

        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }

        public FAQCreateRequestDto getCreateDto() { return createDto; }
        public void setCreateDto(FAQCreateRequestDto createDto) { this.createDto = createDto; }

        public FAQUpdateRequestDto getUpdateDto() { return updateDto; }
        public void setUpdateDto(FAQUpdateRequestDto updateDto) { this.updateDto = updateDto; }
    }

    /**
     * 검증 결과
     */
    private static class ValidationResult {
        private boolean valid;
        private List<String> errors = new ArrayList<>();
        private long timestamp;

        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000; // 1분 후 만료
        }
    }
}