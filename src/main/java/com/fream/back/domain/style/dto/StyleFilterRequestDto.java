package com.fream.back.domain.style.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StyleFilterRequestDto {

    @Size(max = 100, message = "브랜드명은 100자 이하여야 합니다.")
    private String brandName;

    @Size(max = 100, message = "컬렉션명은 100자 이하여야 합니다.")
    private String collectionName;

    @Positive(message = "카테고리 ID는 양수여야 합니다.")
    private Long categoryId;

    private Boolean isMainCategory;

    @Size(max = 50, message = "프로필명은 50자 이하여야 합니다.")
    private String profileName;

    @Pattern(regexp = "popular|latest", message = "정렬 옵션은 'popular' 또는 'latest'여야 합니다.")
    private String sortBy = "latest"; // 기본값 설정
}