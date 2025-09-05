package com.fream.back.domain.faq.aop.aspect;

import com.fream.back.domain.faq.aop.annotation.FAQContentProcessing;
import com.fream.back.domain.faq.aop.annotation.FAQContentProcessing.FailureAction;
import com.fream.back.domain.faq.dto.FAQCreateRequestDto;
import com.fream.back.domain.faq.dto.FAQUpdateRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class FAQContentProcessingAspect {

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(<script[^>]*>.*?</script>)|" +
                    "(javascript:)|" +
                    "(on\\w+\\s*=)|" +
                    "(<iframe[^>]*>.*?</iframe>)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL
    );

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]",
            Pattern.CASE_INSENSITIVE
    );

    @Around("@annotation(contentProcessing)")
    public Object processContent(ProceedingJoinPoint joinPoint, FAQContentProcessing contentProcessing) throws Throwable {
        Object[] args = joinPoint.getArgs();

        try {
            // 요청 DTO 처리
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof FAQCreateRequestDto) {
                    args[i] = processFAQCreateRequest((FAQCreateRequestDto) args[i], contentProcessing);
                } else if (args[i] instanceof FAQUpdateRequestDto) {
                    args[i] = processFAQUpdateRequest((FAQUpdateRequestDto) args[i], contentProcessing);
                }
            }

            // 메서드 실행
            Object result = joinPoint.proceed(args);

            // 응답 처리
            if (contentProcessing.sanitizeOutput() && result != null) {
                result = processOutput(result, contentProcessing);
            }

            return result;

        } catch (Exception e) {
            return handleProcessingFailure(e, contentProcessing, joinPoint);
        }
    }

    private FAQCreateRequestDto processFAQCreateRequest(FAQCreateRequestDto dto,
                                                        FAQContentProcessing contentProcessing) {
        // HTML 컨텐츠 처리
        if (contentProcessing.sanitizeHtml()) {
            String processedAnswer = processHtmlContent(dto.getAnswer(), contentProcessing);
            dto.setAnswer(processedAnswer);
        }

        // 이미지 최적화
        if (contentProcessing.optimizeImages() && dto.getFiles() != null) {
            List<MultipartFile> optimizedFiles = optimizeImages(dto.getFiles(), contentProcessing);
            dto.setFiles(optimizedFiles);
        }

        // 컨텐츠 압축
        if (contentProcessing.compressContent() && dto.getAnswer().length() > 1000) {
            // 압축 로직은 저장 시 처리
            log.debug("Content marked for compression");
        }

        // 자동 요약 생성
        if (contentProcessing.generateSummary()) {
            String summary = generateSummary(dto.getAnswer(), contentProcessing.summaryMaxLength());
            // summary는 별도 필드나 메타데이터로 저장
            log.debug("Generated summary: {}", summary);
        }

        return dto;
    }

    private FAQUpdateRequestDto processFAQUpdateRequest(FAQUpdateRequestDto dto,
                                                        FAQContentProcessing contentProcessing) {
        // HTML 컨텐츠 처리
        if (contentProcessing.sanitizeHtml()) {
            String processedAnswer = processHtmlContent(dto.getAnswer(), contentProcessing);
            dto.setAnswer(processedAnswer);
        }

        // 새 이미지 최적화
        if (contentProcessing.optimizeImages() && dto.getNewFiles() != null) {
            List<MultipartFile> optimizedFiles = optimizeImages(dto.getNewFiles(), contentProcessing);
            dto.setNewFiles(optimizedFiles);
        }

        return dto;
    }

    private String processHtmlContent(String content, FAQContentProcessing contentProcessing) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        try {
            // XSS 방지
            if (contentProcessing.preventXSS()) {
                content = preventXSS(content);
            }

            // HTML 파싱
            Document doc = Jsoup.parse(content);

            // 화이트리스트 기반 정제
            if (contentProcessing.useWhitelist()) {
                content = sanitizeWithWhitelist(content, contentProcessing);
                doc = Jsoup.parse(content);
            }

            // 이미지 태그 최적화
            if (contentProcessing.optimizeImageTags()) {
                optimizeImageTags(doc, contentProcessing);
            }

            // 링크 처리
            if (contentProcessing.validateLinks()) {
                processLinks(doc, contentProcessing);
            }

            // 길이 제한 체크
            String processed = doc.body().html();
            if (processed.length() > contentProcessing.maxContentLength()) {
                processed = truncateContent(processed, contentProcessing.maxContentLength());
            }

            return processed;

        } catch (Exception e) {
            log.error("Error processing HTML content: {}", e.getMessage());
            return handleContentError(content, contentProcessing);
        }
    }

    private String preventXSS(String content) {
        // XSS 패턴 제거
        Matcher matcher = XSS_PATTERN.matcher(content);
        content = matcher.replaceAll("");

        // HTML 엔티티 이스케이프
        content = content.replace("<script", "&lt;script")
                .replace("javascript:", "")
                .replace("onerror=", "")
                .replace("onclick=", "")
                .replace("onload=", "");

        return content;
    }

    private String sanitizeWithWhitelist(String content, FAQContentProcessing contentProcessing) {
        // Jsoup Safelist 생성
        Safelist safelist = new Safelist();

        // 허용된 태그 추가
        for (String tag : contentProcessing.allowedTags()) {
            safelist.addTags(tag);
        }

        // 기본 속성 추가
        safelist.addAttributes("a", "href", "target", "rel")
                .addAttributes("img", "src", "alt", "width", "height", "loading")
                .addAttributes("div", "class", "id")
                .addAttributes("span", "class", "id")
                .addAttributes("p", "class")
                .addAttributes("table", "class", "border")
                .addAttributes("td", "colspan", "rowspan")
                .addAttributes("th", "colspan", "rowspan");

        // 금지된 속성 제거
        for (String attr : contentProcessing.prohibitedAttributes()) {
            safelist.removeAttributes("*", attr);
        }

        // URL 프로토콜 설정
        safelist.addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https");

        return Jsoup.clean(content, safelist);
    }

    private void optimizeImageTags(Document doc, FAQContentProcessing contentProcessing) {
        Elements images = doc.select("img");

        for (Element img : images) {
            // 이미지 크기 제한
            String width = img.attr("width");
            if (!width.isEmpty()) {
                try {
                    int w = Integer.parseInt(width);
                    if (w > contentProcessing.maxImageWidth()) {
                        img.attr("width", String.valueOf(contentProcessing.maxImageWidth()));
                        img.removeAttr("height"); // 비율 유지
                    }
                } catch (NumberFormatException e) {
                    // 무시
                }
            }

            // lazy loading 설정
            if (contentProcessing.enableLazyLoading()) {
                img.attr("loading", "lazy");
            }

            // alt 텍스트 확인
            if (!img.hasAttr("alt") || img.attr("alt").isEmpty()) {
                img.attr("alt", "FAQ 이미지");
            }

            // 반응형 스타일 추가
            String style = img.attr("style");
            if (!style.contains("max-width")) {
                img.attr("style", style + "; max-width: 100%; height: auto;");
            }
        }
    }

    private void processLinks(Document doc, FAQContentProcessing contentProcessing) {
        Elements links = doc.select("a[href]");
        Set<String> invalidLinks = new HashSet<>();

        for (Element link : links) {
            String href = link.attr("href");

            // URL 검증
            if (href.startsWith("http://") || href.startsWith("https://")) {
                // 외부 링크 처리
                if (contentProcessing.externalLinksInNewWindow()) {
                    link.attr("target", "_blank");
                    link.attr("rel", "noopener noreferrer");
                }

                // 링크 유효성 검증 (비동기)
                if (contentProcessing.validateLinks()) {
                    CompletableFuture.runAsync(() -> {
                        if (!isValidUrl(href)) {
                            invalidLinks.add(href);
                        }
                    });
                }
            } else if (href.startsWith("javascript:")) {
                // JavaScript 링크 제거
                link.remove();
            }
        }

        // 유효하지 않은 링크 처리
        if (!invalidLinks.isEmpty()) {
            log.warn("Found {} invalid links in content", invalidLinks.size());
        }
    }

    private boolean isValidUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            return false;
        }
    }

    private List<MultipartFile> optimizeImages(List<MultipartFile> files,
                                               FAQContentProcessing contentProcessing) {
        if (files == null || files.isEmpty()) {
            return files;
        }

        return files.stream()
                .map(file -> optimizeImage(file, contentProcessing))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private MultipartFile optimizeImage(MultipartFile file, FAQContentProcessing contentProcessing) {
        try {
            // 이미지 읽기
            BufferedImage originalImage = ImageIO.read(file.getInputStream());
            if (originalImage == null) {
                return file; // 이미지가 아닌 파일
            }

            // 크기 조정
            BufferedImage resizedImage = resizeImage(originalImage,
                    contentProcessing.maxImageWidth());

            // 압축
            byte[] compressedBytes = compressImage(resizedImage,
                    contentProcessing.imageQuality());

            // MultipartFile로 변환
            return new OptimizedMultipartFile(
                    file.getName(),
                    file.getOriginalFilename(),
                    file.getContentType(),
                    compressedBytes
            );

        } catch (IOException e) {
            log.error("Error optimizing image: {}", e.getMessage());
            return file; // 원본 반환
        }
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int maxWidth) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        if (originalWidth <= maxWidth) {
            return originalImage;
        }

        // 비율 계산
        double ratio = (double) maxWidth / originalWidth;
        int newHeight = (int) (originalHeight * ratio);

        // 리사이징
        BufferedImage resizedImage = new BufferedImage(maxWidth, newHeight,
                originalImage.getType());
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(originalImage, 0, 0, maxWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // JPEG 압축
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG writer found");
        }

        ImageWriter writer = writers.next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(ios);

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }

        writer.write(null, new IIOImage(image, null, null), param);

        ios.close();
        writer.dispose();

        return outputStream.toByteArray();
    }

    private String generateSummary(String content, int maxLength) {
        // HTML 태그 제거
        String plainText = Jsoup.parse(content).text();

        if (plainText.length() <= maxLength) {
            return plainText;
        }

        // 문장 단위로 자르기
        String[] sentences = plainText.split("\\. ");
        StringBuilder summary = new StringBuilder();

        for (String sentence : sentences) {
            if (summary.length() + sentence.length() > maxLength) {
                break;
            }
            summary.append(sentence).append(". ");
        }

        // 마지막 처리
        String result = summary.toString().trim();
        if (result.length() > maxLength) {
            result = result.substring(0, maxLength - 3) + "...";
        }

        return result;
    }

    private String truncateContent(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }

        // HTML 구조 유지하며 자르기
        Document doc = Jsoup.parse(content);
        String truncated = doc.text();

        if (truncated.length() > maxLength) {
            truncated = truncated.substring(0, maxLength - 3) + "...";
        }

        return truncated;
    }

    private Object processOutput(Object result, FAQContentProcessing contentProcessing) {
        // 출력 데이터 정제 로직
        // 실제 구현은 반환 타입에 따라 달라짐
        return result;
    }

    private Object handleProcessingFailure(Exception e, FAQContentProcessing contentProcessing,
                                           ProceedingJoinPoint joinPoint) throws Throwable {
        FailureAction action = contentProcessing.onFailure();

        switch (action) {
            case THROW_EXCEPTION:
                throw e;
            case LOG_AND_CONTINUE:
                log.error("Content processing failed, continuing: {}", e.getMessage());
                return joinPoint.proceed();
            case USE_ORIGINAL:
                return joinPoint.proceed();
            case RETURN_EMPTY:
                return null;
            default:
                return joinPoint.proceed();
        }
    }

    private String handleContentError(String content, FAQContentProcessing contentProcessing) {
        FailureAction action = contentProcessing.onFailure();

        switch (action) {
            case USE_ORIGINAL:
                return content;
            case RETURN_EMPTY:
                return "";
            default:
                return content;
        }
    }

    // 최적화된 MultipartFile 구현
    private static class OptimizedMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public OptimizedMultipartFile(String name, String originalFilename,
                                      String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return originalFilename; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() { return content; }

        @Override
        public java.io.InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}