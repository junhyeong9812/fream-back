package com.fream.back.domain.product.service.productSize;

import com.fream.back.domain.product.entity.Category;
import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.entity.ProductSize;
import com.fream.back.domain.product.entity.enumType.SizeType;
import com.fream.back.domain.product.exception.CategoryNotFoundException;
import com.fream.back.domain.product.exception.InvalidSizeTypeException;
import com.fream.back.domain.product.exception.ProductColorNotFoundException;
import com.fream.back.domain.product.repository.ProductColorRepository;
import com.fream.back.domain.product.repository.ProductSizeRepository;
import com.fream.back.domain.product.service.category.CategoryQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 기존 ProductSizeCommandService.createProductSizes(...) 로직을
 * Spring Batch(Chunk + Skip)로 재구성한 예시.
 *
 * JobParameter:
 *  - productColorId   (Long)
 *  - categoryId       (Long)
 *  - requestedSizes   (String, CSV 예: "S,M,L")
 *  - releasePrice     (Integer)
 *
 * 처리 흐름:
 *   1. Reader: 중복 사이즈 제외, SizeCreationItem 리스트 생성
 *   2. Processor: 유효성 검사 -> 예외 발생 시 Skip
 *   3. Writer: DB 저장
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CreateSizesJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final ProductColorRepository productColorRepository;
    private final ProductSizeRepository productSizeRepository;
    private final CategoryQueryService categoryQueryService;

    /**
     * 배치 Job 정의
     */
    @Bean
    public Job createSizesJob() {
        log.info("[상품 사이즈 배치] 상품 사이즈 생성 Job 구성");
        return new JobBuilder("createSizesJob", jobRepository)
                .start(createSizesStep())
                .build();
    }

    /**
     * Chunk 기반 스텝:
     * - Reader: SizeCreationItem
     * - Processor: ProductSize
     * - Writer: 저장
     * - 각종 예외 발생 시 Skip
     */
    @Bean
    public Step createSizesStep() {
        log.info("[상품 사이즈 배치] 상품 사이즈 생성 Step 구성");
        return new StepBuilder("createSizesStep", jobRepository)
                .<SizeCreationItem, ProductSize>chunk(10, transactionManager)
                .reader(sizeItemReader(null, null, null, null))
                .processor(sizeItemProcessor())
                .writer(sizeItemWriter())
                .faultTolerant()
                .skip(InvalidSizeTypeException.class)
                .skip(ProductColorNotFoundException.class)
                .skip(CategoryNotFoundException.class)
                .skip(IllegalArgumentException.class)
                .skipLimit(100)
                .listener(skipListener())
                .build();
    }

    /**
     * Reader:
     *  - jobParameters에서 productColorId, categoryId, requestedSizes, releasePrice 받아옴
     *  - 카테고리 → SizeType 결정
     *  - 이미 존재하는 사이즈 제외
     *  - 결과: SizeCreationItem 리스트
     */
    @Bean
    @StepScope
    public ItemReader<SizeCreationItem> sizeItemReader(
            @Value("#{jobParameters['productColorId']}") Long productColorId,
            @Value("#{jobParameters['categoryId']}") Long categoryId,
            @Value("#{jobParameters['requestedSizes']}") String requestedSizesCsv,
            @Value("#{jobParameters['releasePrice']}") Integer releasePrice
    ) {
        log.info("[상품 사이즈 배치] Reader 시작 - productColorId: {}, categoryId: {}, requestedSizes: {}, releasePrice: {}",
                productColorId, categoryId, requestedSizesCsv, releasePrice);

        try {
            // 1) 카테고리 조회 -> SizeType 결정
            Category rootCategory = categoryQueryService.findRootCategoryById(categoryId);
            if (rootCategory == null) {
                log.error("[상품 사이즈 배치] 카테고리를 찾을 수 없습니다. ID: {}", categoryId);
                throw new CategoryNotFoundException(categoryId);
            }

            SizeType sizeType = determineSizeType(rootCategory.getName());
            log.debug("[상품 사이즈 배치] 사이즈 타입 결정: {}", sizeType);

            // 2) DB에서 이미 등록된 사이즈 목록 조회
            List<String> existingSizes = productSizeRepository.findAllByProductColorId(productColorId)
                    .stream()
                    .map(ProductSize::getSize)
                    .collect(Collectors.toList());
            log.debug("[상품 사이즈 배치] 이미 존재하는 사이즈: {}", existingSizes);

            // 3) CSV -> List<String>
            List<String> requestedSizes = Arrays.asList(requestedSizesCsv.split(","));
            log.debug("[상품 사이즈 배치] 요청된 사이즈: {}", requestedSizes);

            // 4) 중복 제거
            List<String> newSizes = requestedSizes.stream()
                    .filter(size -> !existingSizes.contains(size))
                    .collect(Collectors.toList());
            log.debug("[상품 사이즈 배치] 신규 추가할 사이즈: {}", newSizes);

            // 5) SizeCreationItem 변환 (null -> 0)
            int finalReleasePrice = (releasePrice != null) ? releasePrice : 0;
            List<SizeCreationItem> itemList = newSizes.stream()
                    .map(size -> new SizeCreationItem(
                            productColorId,
                            sizeType,
                            size,
                            finalReleasePrice
                    ))
                    .collect(Collectors.toList());
            log.info("[상품 사이즈 배치] 처리할 아이템 개수: {}", itemList.size());

            // 6) ListItemReader
            return new ListItemReader<>(itemList);
        } catch (Exception e) {
            log.error("[상품 사이즈 배치] Reader 실행 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * Processor:
     *  - 사이즈 유효성 검사 (isValidSize)
     *  - 잘못된 사이즈면 예외 -> Skip
     *  - 정상이면 ProductSize 엔티티 생성
     */
    @Bean
    @StepScope
    public ItemProcessor<SizeCreationItem, ProductSize> sizeItemProcessor() {
        return item -> {
            try {
                log.debug("[상품 사이즈 배치] 프로세싱 - 사이즈: {}, 타입: {}", item.getSize(), item.getSizeType());

                // 1) 사이즈 유효성 체크
                if (!isValidSize(item.getSize(), item.getSizeType())) {
                    log.warn("[상품 사이즈 배치] 유효하지 않은 사이즈: {}, 타입: {}", item.getSize(), item.getSizeType());
                    throw new InvalidSizeTypeException(item.getSizeType().name(), item.getSize());
                }

                // 2) ProductColor 조회
                ProductColor productColor = productColorRepository.findById(item.getProductColorId())
                        .orElseThrow(() -> new ProductColorNotFoundException(item.getProductColorId()));

                // 3) ProductSize 빌드
                ProductSize productSize = ProductSize.builder()
                        .productColor(productColor)
                        .size(item.getSize())
                        .sizeType(item.getSizeType())
                        .purchasePrice(item.getReleasePrice())
                        .salePrice(item.getReleasePrice())
                        .quantity(0)
                        .build();

                log.debug("[상품 사이즈 배치] 사이즈 생성 완료: {}", item.getSize());
                return productSize;
            } catch (Exception e) {
                log.error("[상품 사이즈 배치] 프로세서 처리 중 오류 발생 - 사이즈: {}", item.getSize(), e);
                throw e;
            }
        };
    }

    /**
     * Writer: ProductSize를 DB에 저장
     */
    @Bean
    @StepScope
    public ItemWriter<ProductSize> sizeItemWriter() {
        return items -> {
            try {
                log.info("[상품 사이즈 배치] 라이터 시작 - {} 개의 사이즈 저장", items.size());

                for (ProductSize productSize : items) {
                    ProductSize saved = productSizeRepository.save(productSize);
                    log.debug("[상품 사이즈 배치] 사이즈 저장 완료 - ID: {}, 사이즈: {}",
                            saved.getId(), saved.getSize());
                }

                log.info("[상품 사이즈 배치] 라이터 완료 - 모든 사이즈 저장됨");
            } catch (Exception e) {
                log.error("[상품 사이즈 배치] 라이터 실행 중 오류 발생", e);
                throw e;
            }
        };
    }

    /**
     * SkipListener: 잘못된 사이즈 스킵 로그
     */
    @Bean
    public SkipListener<SizeCreationItem, ProductSize> skipListener() {
        return new SkipListener<>() {
            @Override
            public void onSkipInProcess(SizeCreationItem item, Throwable t) {
                log.warn("[상품 사이즈 배치] 프로세스 중 스킵 - 사이즈: {}, 원인: {}",
                        item.getSize(), t.getMessage());
            }

            @Override
            public void onSkipInRead(Throwable t) {
                log.warn("[상품 사이즈 배치] 리드 중 스킵 - 원인: {}", t.getMessage());
            }

            @Override
            public void onSkipInWrite(ProductSize item, Throwable t) {
                log.warn("[상품 사이즈 배치] 라이트 중 스킵 - 사이즈: {}, 원인: {}",
                        item.getSize(), t.getMessage());
            }
        };
    }

    /**
     * 카테고리 이름 -> SizeType 결정
     */
    private SizeType determineSizeType(String rootCategoryName) {
        try {
            log.debug("[상품 사이즈 배치] 카테고리에 따른 사이즈 타입 결정 중: {}", rootCategoryName);
            switch (rootCategoryName.toUpperCase()) {
                case "CLOTHING":
                    return SizeType.CLOTHING;
                case "SHOES":
                    return SizeType.SHOES;
                case "ACCESSORIES":
                    return SizeType.ACCESSORIES;
                default:
                    log.error("[상품 사이즈 배치] 잘못된 카테고리 이름: {}", rootCategoryName);
                    throw new InvalidSizeTypeException("해당 카테고리에 맞는 SizeType이 존재하지 않습니다: " + rootCategoryName);
            }
        } catch (Exception e) {
            log.error("[상품 사이즈 배치] 사이즈 타입 결정 중 오류 발생", e);
            throw e;
        }
    }

    /**
     * 사이즈 유효성 검사
     */
    private boolean isValidSize(String size, SizeType sizeType) {
        boolean isValid = Arrays.asList(sizeType.getSizes()).contains(size);
        log.debug("[상품 사이즈 배치] 사이즈 유효성 검사 - 사이즈: {}, 타입: {}, 유효: {}",
                size, sizeType, isValid);
        return isValid;
    }

    /**
     * 배치에서 사용할 DTO
     */
    public static class SizeCreationItem {
        private final Long productColorId;
        private final SizeType sizeType;
        private final String size;
        private final int releasePrice;

        public SizeCreationItem(Long productColorId, SizeType sizeType, String size, int releasePrice) {
            this.productColorId = productColorId;
            this.sizeType = sizeType;
            this.size = size;
            this.releasePrice = releasePrice;
        }

        public Long getProductColorId() { return productColorId; }
        public SizeType getSizeType() { return sizeType; }
        public String getSize() { return size; }
        public int getReleasePrice() { return releasePrice; }

        @Override
        public String toString() {
            return "SizeCreationItem{" +
                    "productColorId=" + productColorId +
                    ", sizeType=" + sizeType +
                    ", size='" + size + '\'' +
                    ", releasePrice=" + releasePrice +
                    '}';
        }
    }
}