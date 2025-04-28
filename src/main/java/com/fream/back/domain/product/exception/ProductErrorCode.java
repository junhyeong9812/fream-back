package com.fream.back.domain.product.exception;

import com.fream.back.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 상품 도메인 관련 에러 코드 열거형
 * 체계적인 에러 코드 관리를 위해 서브 도메인별로 접두어를 사용
 */
@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {

    // 상품 관련 에러 (PRODUCT: P)
    PRODUCT_NOT_FOUND("P001", "상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    PRODUCT_CREATION_FAILED("P002", "상품 생성에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    PRODUCT_UPDATE_FAILED("P003", "상품 업데이트에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    PRODUCT_DELETION_FAILED("P004", "상품 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    PRODUCT_IN_USE("P005", "사용 중인 상품은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),

    // 색상(ProductColor) 관련 에러 (COLOR: PC)
    PRODUCT_COLOR_NOT_FOUND("PC001", "상품 색상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    PRODUCT_COLOR_CREATION_FAILED("PC002", "상품 색상 생성에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    PRODUCT_COLOR_UPDATE_FAILED("PC003", "상품 색상 업데이트에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    PRODUCT_COLOR_DELETION_FAILED("PC004", "상품 색상 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_COLOR_TYPE("PC005", "유효하지 않은 색상 타입입니다.", HttpStatus.BAD_REQUEST.value()),

    // 사이즈(ProductSize) 관련 에러 (SIZE: PS)
    PRODUCT_SIZE_NOT_FOUND("PS001", "상품 사이즈를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    PRODUCT_SIZE_CREATION_FAILED("PS002", "상품 사이즈 생성에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    PRODUCT_SIZE_UPDATE_FAILED("PS003", "상품 사이즈 업데이트에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    PRODUCT_SIZE_DELETION_FAILED("PS004", "상품 사이즈 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    INVALID_SIZE_TYPE("PS005", "유효하지 않은 사이즈 타입입니다.", HttpStatus.BAD_REQUEST.value()),

    // 카테고리 관련 에러 (CATEGORY: CT)
    CATEGORY_NOT_FOUND("CT001", "카테고리를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    CATEGORY_CREATION_FAILED("CT002", "카테고리 생성에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    CATEGORY_UPDATE_FAILED("CT003", "카테고리 업데이트에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    CATEGORY_DELETION_FAILED("CT004", "카테고리 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    CATEGORY_IN_USE("CT005", "사용 중인 카테고리는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    CATEGORY_ALREADY_EXISTS("CT006", "이미 존재하는 카테고리입니다.", HttpStatus.CONFLICT.value()),

    // 브랜드 관련 에러 (BRAND: BD)
    BRAND_NOT_FOUND("BD001", "브랜드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    BRAND_CREATION_FAILED("BD002", "브랜드 생성에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    BRAND_UPDATE_FAILED("BD003", "브랜드 업데이트에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    BRAND_DELETION_FAILED("BD004", "브랜드 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    BRAND_IN_USE("BD005", "사용 중인 브랜드는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    BRAND_ALREADY_EXISTS("BD006", "이미 존재하는 브랜드입니다.", HttpStatus.CONFLICT.value()),

    // 컬렉션 관련 에러 (COLLECTION: CL)
    COLLECTION_NOT_FOUND("CL001", "컬렉션을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    COLLECTION_CREATION_FAILED("CL002", "컬렉션 생성에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    COLLECTION_UPDATE_FAILED("CL003", "컬렉션 업데이트에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    COLLECTION_DELETION_FAILED("CL004", "컬렉션 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    COLLECTION_IN_USE("CL005", "사용 중인 컬렉션은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST.value()),
    COLLECTION_ALREADY_EXISTS("CL006", "이미 존재하는 컬렉션입니다.", HttpStatus.CONFLICT.value()),

    // 관심 상품 관련 에러 (INTEREST: IN)
    INTEREST_NOT_FOUND("IN001", "관심 상품을 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    INTEREST_TOGGLE_FAILED("IN002", "관심 상품 토글에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),

    // 이미지 관련 에러 (IMAGE: IMG)
    IMAGE_NOT_FOUND("IMG001", "이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND.value()),
    IMAGE_UPLOAD_FAILED("IMG002", "이미지 업로드에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),
    IMAGE_DELETION_FAILED("IMG003", "이미지 삭제에 실패했습니다.", HttpStatus.BAD_REQUEST.value()),

    // 필터 관련 에러 (FILTER: FT)
    FILTER_INVALID_PARAMS("FT001", "유효하지 않은 필터 파라미터입니다.", HttpStatus.BAD_REQUEST.value()),

    // Kafka 관련 에러 (KAFKA: KF)
    KAFKA_EVENT_SENDING_FAILED("KF001", "Kafka 이벤트 전송에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value()),
    KAFKA_EVENT_PROCESSING_FAILED("KF002", "Kafka 이벤트 처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR.value());

    private final String code;
    private final String message;
    private final int status;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getStatus() {
        return status;
    }
}