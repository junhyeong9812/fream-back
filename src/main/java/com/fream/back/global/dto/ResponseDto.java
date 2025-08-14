package com.fream.back.global.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseDto<T> {
    private boolean success;
    private String message;
    private T data;
    private String errorCode; // 에러 코드 추가
    private Long timestamp; // 타임스탬프 추가

    public static <T> ResponseDto<T> success(T data) {
        return ResponseDto.<T>builder()
                .success(true)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ResponseDto<T> success(T data, String message) {
        return ResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    public static <T> ResponseDto<T> fail(String message) {
        return ResponseDto.<T>builder()
                .success(false)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 에러 코드와 함께 실패 응답 생성
     */
    public static <T> ResponseDto<T> error(String errorCode, String message) {
        return ResponseDto.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }

    /**
     * 에러 코드, 메시지, 데이터와 함께 실패 응답 생성
     */
    public static <T> ResponseDto<T> error(String errorCode, String message, T data) {
        return ResponseDto.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
}