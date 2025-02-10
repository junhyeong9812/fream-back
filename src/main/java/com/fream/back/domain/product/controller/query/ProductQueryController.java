package com.fream.back.domain.product.controller.query;

import com.fream.back.domain.product.dto.ProductDetailResponseDto;
import com.fream.back.domain.product.dto.ProductSearchDto;
import com.fream.back.domain.product.dto.ProductSearchResponseDto;
import com.fream.back.domain.product.service.kafka.ViewEventProducer;
import com.fream.back.domain.product.service.product.ProductQueryService;
import com.fream.back.domain.user.entity.Gender;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.global.config.security.JwtAuthenticationFilter;
import com.fream.back.global.dto.commonDto;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

@RestController
@RequestMapping("/products/query")
@RequiredArgsConstructor
public class ProductQueryController {

    private final ProductQueryService productQueryService;
    private final UserQueryService userQueryService; // 이메일 -> User 엔티티 조회
    private final ViewEventProducer viewEventProducer;

    @GetMapping
    public ResponseEntity<commonDto.PageDto<ProductSearchResponseDto>> searchProducts(
            @ModelAttribute ProductSearchDto searchRequest,
            Pageable pageable) {
        // 유효성 검증
        searchRequest.validate();

        Page<ProductSearchResponseDto> pageResult = productQueryService.searchProducts(
                searchRequest.getKeyword(),
                searchRequest.getCategoryIds(),
                searchRequest.getGenders(),
                searchRequest.getBrandIds(),
                searchRequest.getCollectionIds(),
                searchRequest.getColors(),
                searchRequest.getSizes(),
                searchRequest.getMinPrice(),
                searchRequest.getMaxPrice(),
                searchRequest.getSortOption(),
                pageable);
        commonDto.PageDto<ProductSearchResponseDto> response = toPageDto(pageResult);
        return ResponseEntity.ok(response);
    }
    private <T> commonDto.PageDto<T> toPageDto(Page<T> pageResult) {
        return new commonDto.PageDto<>(
                pageResult.getContent(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getNumber(),  // 현재 페이지 index(0-based)
                pageResult.getSize()
        );
    }
    //뷰포인트가 없는 방식
    @GetMapping("/{productId}/detail")
    public ResponseEntity<ProductDetailResponseDto> getProductDetail(
            @PathVariable("productId") Long productId,
            @RequestParam("colorName") String colorName) {
        // 1) 상품 상세 (DB 조회)
        ProductDetailResponseDto detailDto = productQueryService.getProductDetail(productId, colorName);

        // 2) 이메일 추출 (익명 시 “anonymous”)
        String email = SecurityUtils.extractEmailOrAnonymous();
        System.out.println("email = " + email);



        // 3) 로그인 사용자라면 나이, 성별 조회
        Integer age = 0;
        Gender gender = Gender.OTHER;
        if (!"anonymousUser".equals(email)) {
            try {
                // 3) SecurityContext에 저장된 userInfo 꺼내기
                JwtAuthenticationFilter.UserInfo userInfo = SecurityUtils.extractUserInfo();
//                User user = userQueryService.findByEmail(email);
//                age = (user.getAge() == null) ? 0 : user.getAge();
//                gender = (user.getGender() == null) ? Gender.OTHER : user.getGender();
                age = (userInfo.getAge() == null) ? 0 : userInfo.getAge();
                gender = (userInfo.getGender() == null) ? Gender.OTHER : userInfo.getGender();
            } catch (IllegalArgumentException e) {
                // 만약 이메일이 있지만 User가 없다면 anonymous로 처리
                email = "anonymous";
            }
        }
        System.out.println("age = " + age);
        System.out.println("gender = " + gender);
        System.out.println("email = " + email);
        // 4) Producer에게 카프카 이벤트 발행
        viewEventProducer.sendViewEvent(detailDto.getColorId(), email, age, gender);

        // 5) 결과 반환
        return ResponseEntity.ok(detailDto);
    }


    @GetMapping("/{productId}/images")
    public ResponseEntity<byte[]> getProductImage(
            @PathVariable("productId") Long productId,
            @RequestParam("imageName") String imageName
    ) throws Exception {
        // 실제 경로: /home/ubuntu/fream/product/{productId}/{imageName}
        String baseDir = "/home/ubuntu/fream";
        String directory = "product/" + productId; // "product/10"

        String fullPath = baseDir + File.separator + directory + File.separator + imageName;
        File imageFile = new File(fullPath);

        if (!imageFile.exists()) {
            throw new IllegalArgumentException("이미지 파일이 존재하지 않습니다. fullPath=" + fullPath);
        }

        byte[] imageBytes = Files.readAllBytes(imageFile.toPath());

        String mimeType = Files.probeContentType(imageFile.toPath());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .header("Content-Type", mimeType)
                .body(imageBytes);
    }
}
