package com.fream.back.global.exception;

import com.fream.back.domain.accessLog.exception.AccessLogException;
import com.fream.back.domain.chatQuestion.exception.ChatQuestionException;
import com.fream.back.domain.weather.exception.WeatherException;
import com.fream.back.global.exception.file.FileException;
import com.fream.back.global.exception.security.SecurityException;
import com.fream.back.global.exception.websocket.WebSocketException;
import com.fream.back.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기
 * 애플리케이션 전체에서 발생하는 예외를 처리하는 클래스
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * GlobalException 및 하위 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            HttpServletRequest request, GlobalException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("GlobalException: {} - {}", errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        errorCode.getStatus(),
                        request.getRequestURI()
                ));
    }

    /**
     * SecurityException 및 하위 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            HttpServletRequest request, SecurityException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("SecurityException: {} - {}", errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        errorCode.getStatus(),
                        request.getRequestURI()
                ));
    }

    /**
     * FileException 및 하위 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(FileException.class)
    public ResponseEntity<ErrorResponse> handleFileException(
            HttpServletRequest request, FileException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("FileException: {} - {}", errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        errorCode.getStatus(),
                        request.getRequestURI()
                ));
    }

    /**
     * WebSocketException 및 하위 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(WebSocketException.class)
    public ResponseEntity<ErrorResponse> handleWebSocketException(
            HttpServletRequest request, WebSocketException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("WebSocketException: {} - {}", errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        errorCode.getStatus(),
                        request.getRequestURI()
                ));
    }

    /**
     * WeatherException 및 하위 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(WeatherException.class)
    public ResponseEntity<ErrorResponse> handleWeatherException(
            HttpServletRequest request, WeatherException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("WeatherException: {} - {}", errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        errorCode.getStatus(),
                        request.getRequestURI()
                ));
    }

    /**
     * AccessLogException 및 하위 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(AccessLogException.class)
    public ResponseEntity<ErrorResponse> handleAccessLogException(
            HttpServletRequest request, AccessLogException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("AccessLogException: {} - {}", errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        errorCode.getStatus(),
                        request.getRequestURI()
                ));
    }

    /**
     * ChatQuestionException 및 하위 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(ChatQuestionException.class)
    public ResponseEntity<ErrorResponse> handleChatQuestionException(
            HttpServletRequest request, ChatQuestionException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("ChatQuestionException: {} - {}", errorCode.getCode(), e.getMessage(), e);

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ErrorResponse.of(
                        errorCode.getCode(),
                        errorCode.getMessage(),
                        errorCode.getStatus(),
                        request.getRequestURI()
                ));
    }

    /**
     * 입력값 검증 예외 처리 (@Valid 어노테이션 관련)
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            HttpServletRequest request, MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage());

        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        GlobalErrorCode.INVALID_INPUT_VALUE.getCode(),
                        message != null ? message : GlobalErrorCode.INVALID_INPUT_VALUE.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI()
                ));
    }

    /**
     * 바인딩 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResponse> handleBindException(
            HttpServletRequest request, BindException e) {
        log.error("BindException: {}", e.getMessage());

        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        GlobalErrorCode.INVALID_INPUT_VALUE.getCode(),
                        message != null ? message : GlobalErrorCode.INVALID_INPUT_VALUE.getMessage(),
                        HttpStatus.BAD_REQUEST.value(),
                        request.getRequestURI()
                ));
    }

    /**
     * 기타 모든 예외 처리
     *
     * @param request 현재 HTTP 요청
     * @param e 발생한 예외
     * @return 에러 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            HttpServletRequest request, Exception e) {
        log.error("Unhandled Exception: {}", e.getMessage(), e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage(),
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        request.getRequestURI()
                ));
    }
}