package com.fream.back.domain.faq.aop;

import com.fream.back.domain.faq.aop.annotation.FAQContentProcessing;
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
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * FAQ 컨텐츠 처리 AOP
 * HTML 컨텐츠 정제, 이미지 최적화, XSS 방지 등
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2)
public class FAQContentProcessingAspect {

    // 위험한 태그/속성 패턴
    private static final Pattern DANGEROUS_TAGS = Pattern.compile(
            "<\\s*(script|iframe|object|embed|applet)[^>]*>.*?</\\s*\\1\\s*>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
            "javascript:|on\\w+\\s*=",
            Pattern.CASE_INSENSITIVE
    );

    // 이미지 처리 통계
    private final Map<String, ImageProcessingStats> imageStats = new ConcurrentHashMap<>();

    private static class ImageProcessingStats {
        private int totalProcessed = 0;
        private long totalSizeBefore = 0;
        private long totalSizeAfter = 0;
        private int optimizationCount = 0;
        private int failureCount = 0;

        public double getCompressionRatio() {
            return totalSizeBefore > 0 ?
                    (double)(totalSizeBefore - totalSizeAfter) / totalSizeBefore * 100 : 0;
        }
    }

    @Around("@annotation(contentProcessing)")
    public Object processContent(ProceedingJoinPoint joinPoint, FAQContentProcessing contentProcessing) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String methodName = joinPoint.getSignature().getName();

        log.debug("FAQ_CONTENT_PROCESSING_START - Method: {}", methodName);

        try {
            // 요청 데이터 전처리
            preprocessContent(args, contentProcessing);

            // 이미지 최적화
            if (contentProcessing.optimizeImages()) {
                optimizeImages(args, contentProcessing);
            }

            // 메서드 실행
            Object result = joinPoint.proceed(args);

            // 응답 데이터 후처리
            if (contentProcessing.sanitizeOutput()) {
                result = postprocessContent(result, contentProcessing);
            }

            log.debug("FAQ_CONTENT_PROCESSING_SUCCESS - Method: {}", methodName);
            return result;

        } catch (Exception e) {
            log.error("FAQ_CONTENT_PROCESSING_ERROR - Method: {}, Error: {}",
                    methodName, e.getMessage());

            // 실패 처리
            switch (contentProcessing.onFailure()) {
                case THROW_EXCEPTION:
                    throw e;
                case LOG_AND_CONTINUE:
                    log.warn("Content processing failed, continuing with original content");
                    return joinPoint.proceed();
                case USE_ORIGINAL:
                    return joinPoint.proceed();
                case RETURN_EMPTY:
                    return null;
                default:
                    return joinPoint.proceed();
            }
        }
    }

    /**
     * 컨텐츠 전처리
     */
    private void preprocessContent(Object[] args, FAQContentProcessing annotation) {
        for (Object arg : args) {
            if (arg instanceof FAQCreateRequestDto) {
                FAQCreateRequestDto dto = (FAQCreateRequestDto) arg;

                // HTML 정제
                if (annotation.sanitizeHtml()) {
                    dto.setAnswer(sanitizeHtml(dto.getAnswer(), annotation));
                }

                // 컨텐츠 길이 제한
                if (dto.getAnswer().length() > annotation.maxContentLength()) {
                    dto.setAnswer(dto.getAnswer().substring(0, annotation.maxContentLength()));
                    log.warn("Content truncated to {} characters", annotation.maxContentLength());
                }

            } else if (arg instanceof FAQUpdateRequestDto) {
                FAQUpdateRequestDto dto = (FAQUpdateRequestDto) arg;

                // HTML 정제
                if (annotation.sanitizeHtml()) {
                    dto.setAnswer(sanitizeHtml(dto.getAnswer(), annotation));
                }

                // 컨텐츠 길이 제한
                if (dto.getAnswer().length() > annotation.maxContentLength()) {
                    dto.setAnswer(dto.getAnswer().substring(0, annotation.maxContentLength()));
                    log.warn("Content truncated to {} characters", annotation.maxContentLength());
                }
            }
        }
    }

    /**
     * HTML 정제
     */
    private String sanitizeHtml(String html, FAQContentProcessing annotation) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        // XSS 방지
        if (annotation.preventXSS()) {
            html = removeScriptTags(html);
            html = removeJavaScript(html);
        }

        // Safelist 설정
        Safelist safelist = createSafelist(annotation);

        // HTML 정제
        String cleanHtml = Jsoup.clean(html, safelist);

        // Jsoup으로 파싱
        Document doc = Jsoup.parse(cleanHtml);

        // 추가 처리
        if (annotation.validateLinks()) {
            validateAndFixLinks(doc, annotation);
        }

        if (annotation.optimizeImageTags()) {
            optimizeImageTags(doc, annotation);
        }

        // 금지된 속성 제거
        removeProhibitedAttributes(doc, annotation.prohibitedAttributes());

        log.debug("HTML_SANITIZED - Original length: {}, Cleaned length: {}",
                html.length(), doc.body().html().length());

        return doc.body().html();
    }

    /**
     * Safelist 생성
     */
    private Safelist createSafelist(FAQContentProcessing annotation) {
        Safelist safelist = Safelist.relaxed();

        // 허용된 태그 추가
        for (String tag : annotation.allowedTags()) {
            safelist.addTags(tag);
        }

        // 이미지 속성
        safelist.addAttributes("img", "src", "alt", "width", "height", "loading", "class");

        // 링크 속성
        safelist.addAttributes("a", "href", "target", "rel");

        // 프로토콜
        safelist.addProtocols("img", "src", "http", "https", "data");
        safelist.addProtocols("a", "href", "http", "https", "mailto");

        return safelist;
    }

    /**
     * 스크립트 태그 제거
     */
    private String removeScriptTags(String html) {
        return DANGEROUS_TAGS.matcher(html).replaceAll("");
    }

    /**
     * JavaScript 제거
     */
    private String removeJavaScript(String html) {
        return JAVASCRIPT_PATTERN.matcher(html).replaceAll("");
    }

    /**
     * 금지된 속성 제거
     */
    private void removeProhibitedAttributes(Document doc, String[] prohibitedAttributes) {
        for (String attr : prohibitedAttributes) {
            Elements elements = doc.select("[" + attr + "]");
            elements.removeAttr(attr);
        }
    }

    /**
     * 링크 검증 및 수정
     */
    private void validateAndFixLinks(Document doc, FAQContentProcessing annotation) {
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            String href = link.attr("href");

            // 외부 링크 처리
            if ((href.startsWith("http://") || href.startsWith("https://")) &&
                    annotation.externalLinksInNewWindow()) {

                if (!href.contains("fream.com")) {
                    link.attr("target", "_blank");
                    link.attr("rel", "noopener noreferrer");
                }
            }

            // 위험한 프로토콜 제거
            if (href.startsWith("javascript:") || href.startsWith("data:")) {
                link.removeAttr("href");
                log.warn("Removed dangerous link: {}", href);
            }
        }
    }

    /**
     * 이미지 태그 최적화
     */
    private void optimizeImageTags(Document doc, FAQContentProcessing annotation) {
        Elements images = doc.select("img");

        for (Element img : images) {
            // lazy loading 추가
            if (annotation.enableLazyLoading()) {
                img.attr("loading", "lazy");
            }

            // 최대 크기 제한
            String width = img.attr("width");
            if (width.isEmpty() || Integer.parseInt(width) > annotation.maxImageWidth()) {
                img.attr("width", String.valueOf(annotation.maxImageWidth()));
                img.removeAttr("height"); // 비율 유지를 위해 height 제거
            }

            // alt 텍스트 확인
            if (!img.hasAttr("alt")) {
                img.attr("alt", "FAQ 이미지");
            }

            // 반응형 클래스 추가
            String classes = img.attr("class");
            if (!classes.contains("img-fluid")) {
                img.addClass("img-fluid");
            }

            // 위험한 src 제거
            String src = img.attr("src");
            if (src.startsWith("javascript:") ||
                    (src.startsWith("data:") && !src.startsWith("data:image/"))) {
                img.remove();
                log.warn("Removed dangerous image: {}", src);
            }
        }
    }

    /**
     * 이미지 파일 최적화
     */
    private void optimizeImages(Object[] args, FAQContentProcessing annotation) {
        for (Object arg : args) {
            List<MultipartFile> files = null;

            if (arg instanceof FAQCreateRequestDto) {
                files = ((FAQCreateRequestDto) arg).getFiles();
            } else if (arg instanceof FAQUpdateRequestDto) {
                files = ((FAQUpdateRequestDto) arg).getNewFiles();
            }

            if (files != null) {
                optimizeImageFiles(files, annotation);
            }
        }
    }

    /**
     * 이미지 파일 리스트 최적화
     */
    private void optimizeImageFiles(List<MultipartFile> files, FAQContentProcessing annotation) {
        if (files == null || files.isEmpty()) {
            return;
        }

        String statKey = "FAQ_IMAGES";
        ImageProcessingStats stats = imageStats.computeIfAbsent(statKey, k -> new ImageProcessingStats());

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            try {
                if (!isImageFile(file)) continue;

                long originalSize = file.getSize();
                stats.totalSizeBefore += originalSize;

                // 이미지 리사이징 및 압축
                byte[] optimizedBytes = resizeAndCompressImage(
                        file.getBytes(),
                        annotation.maxImageWidth(),
                        annotation.imageQuality()
                );

                stats.totalSizeAfter += optimizedBytes.length;
                stats.totalProcessed++;

                if (optimizedBytes.length < originalSize) {
                    stats.optimizationCount++;
                    log.debug("IMAGE_OPTIMIZED - Original: {}KB, Optimized: {}KB, Saved: {}%",
                            originalSize / 1024,
                            optimizedBytes.length / 1024,
                            (originalSize - optimizedBytes.length) * 100 / originalSize);
                }

                // 실제 환경에서는 최적화된 바이트를 새 MultipartFile로 교체하는 로직 필요

            } catch (IOException e) {
                stats.failureCount++;
                log.error("IMAGE_OPTIMIZATION_ERROR - File: {}, Error: {}",
                        file.getOriginalFilename(), e.getMessage());
            }
        }

        // 통계 로깅
        if (stats.totalProcessed > 0 && stats.totalProcessed % 10 == 0) {
            log.info("IMAGE_PROCESSING_STATS - Total: {}, Optimized: {}, Failed: {}, CompressionRatio: {:.1f}%",
                    stats.totalProcessed, stats.optimizationCount, stats.failureCount, stats.getCompressionRatio());
        }
    }

    /**
     * 이미지 파일 여부 확인
     */
    private boolean isImageFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * 이미지 리사이징 및 압축
     */
    private byte[] resizeAndCompressImage(byte[] imageBytes, int maxWidth, float quality) throws IOException {
        BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(imageBytes));

        if (originalImage == null) {
            return imageBytes;
        }

        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        // 리사이징 필요 여부 확인
        if (originalWidth <= maxWidth) {
            return compressImage(originalImage, quality);
        }

        // 비율 계산
        double ratio = (double) maxWidth / originalWidth;
        int newWidth = maxWidth;
        int newHeight = (int) (originalHeight * ratio);

        // 리사이징
        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();

        log.debug("IMAGE_RESIZED - {}x{} -> {}x{}",
                originalWidth, originalHeight, newWidth, newHeight);

        return compressImage(resizedImage, quality);
    }

    /**
     * 이미지 압축
     */
    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // JPEG 압축 (실제 환경에서는 ImageIO 플러그인 사용 권장)
        ImageIO.write(image, "jpg", baos);

        return baos.toByteArray();
    }

    /**
     * 응답 데이터 후처리
     */
    private Object postprocessContent(Object result, FAQContentProcessing annotation) {
        // 필요시 응답 데이터 추가 처리
        // 예: HTML 엔티티 인코딩, 추가 정제 등
        return result;
    }

    /**
     * 요약 생성
     */
    private String generateSummary(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }

        // HTML 태그 제거
        String plainText = Jsoup.parse(content).text();

        // 요약 생성
        if (plainText.length() > maxLength) {
            return plainText.substring(0, maxLength - 3) + "...";
        }

        return plainText;
    }
}