//package com.fream.back.domain.product.elasticsearch.index;
//
//import lombok.*;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.elasticsearch.annotations.DateFormat;
//import org.springframework.data.elasticsearch.annotations.Document;
//import org.springframework.data.elasticsearch.annotations.Field;
//import org.springframework.data.elasticsearch.annotations.FieldType;
//
//import java.util.List;
//
//@Document(indexName = "product-colors") // 인덱스명
//@Getter
//@Setter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class ProductColorIndex {
//
//    @Id
//    private Long colorId;                // 색상 ID (문서 식별자)
//    private Long productId;              // 상품 ID
//
//    // 검색/필터 대상 필드
//    @Field(type = FieldType.Text)
//    private String productName;          // 상품명 (한글)
//
//    @Field(type = FieldType.Text)
//    private String productEnglishName;   // 상품명 (영문)
//
//    @Field(type = FieldType.Text)
//    private String brandName;
//
//    @Field(type = FieldType.Text)
//    private String categoryName;
//
//    @Field(type = FieldType.Text)
//    private String collectionName;
//
//    @Field(type = FieldType.Text)
//    private String colorName;
//
//    @Field(type = FieldType.Keyword)
//    private String gender; // MALE, FEMALE, KIDS, UNISEX (또는 한글(남자/여자/어린이/공용)까지 넣을 수도 있음)
//
//    @Field(type = FieldType.Text) // 혹은 Keyword
//    private String thumbnailUrl;
//
//    // 필터링/정렬용
//    private Long brandId;
//    private Long categoryId;
//    private Long collectionId;
//    @Field(type = FieldType.Date, format = DateFormat.date, pattern = "yyyy-MM-dd")
//    private String releaseDate;
//
//    private int releasePrice; // 발매가
//    private int minPrice;     // size 중 최저 purchasePrice
//    private int maxPrice;     // size 중 최대 purchasePrice
//    private Long interestCount; // 관심 수
//
//    // 사이즈 배열 (ex: ["250","260","270"] 또는 ["M","L"] 등)
//    @Field(type = FieldType.Keyword)
//    private List<String> sizes;
//}
//
