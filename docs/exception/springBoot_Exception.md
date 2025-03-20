# 스프링부트 예외 처리 구조 가이드

## 목차
1. [개요](#1-개요)
2. [예외 처리 구조](#2-예외-처리-구조)
3. [글로벌 예외 처리 구성](#3-글로벌-예외-처리-구성)
4. [도메인별 예외 처리 구성](#4-도메인별-예외-처리-구성)
5. [예외 처리기(핸들러) 정의](#5-예외-처리기-핸들러-정의)
6. [응답 형식 정의](#6-응답-형식-정의)
7. [구현 예제](#7-구현-예제)

## 1. 개요
이 문서는 스프링부트 프로젝트에서 도메인 중심의 효율적인 예외 처리 구조를 설계하고 구현하는 방법에 대한 가이드를 제공합니다. 예외 코드 관리, 예외 클래스 구현, 예외 핸들러 구성 등의 내용을 다룹니다.

## 2. 예외 처리 구조
도메인 중심 예외 처리 구조는 다음과 같이 구성됩니다:

```
com.example.project
├── global
│   ├── exception
│   │   ├── ErrorCode.java (인터페이스)
│   │   ├── GlobalErrorCode.java (글로벌 에러코드 열거형)
│   │   ├── GlobalException.java (기본 예외 클래스)
│   │   ├── GlobalExceptionHandler.java (글로벌 예외 핸들러)
│   │   └── handler
│   │       ├── ActivityExceptionHandler.java (활동 도메인 예외 핸들러)
│   │       ├── UserExceptionHandler.java (사용자 도메인 예외 핸들러)
│   │       └── ...
│   └── response
│       └── ErrorResponse.java (에러 응답 DTO)
├── domain
│   ├── activity
│   │   ├── controller
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   └── exception
│   │       ├── ActivityErrorCode.java (활동 도메인 에러코드)
│   │       ├── ActivityException.java (활동 도메인 기본 예외)
│   │       └── 구체적인 예외 클래스들... (AttendanceAlreadyCheckedException 등)
│   ├── user
│   │   ├── ...
│   │   └── exception
│   │       ├── UserErrorCode.java
│   │       ├── UserException.java
│   │       └── 구체적인 예외 클래스들...
│   └── ...
```

## 3. 글로벌 예외 처리 구성

### 3.1 ErrorCode 인터페이스 정의
모든 에러 코드 열거형이 구현해야 할 인터페이스를 정의합니다.

```java
package com.example.project.global.exception;

public interface ErrorCode {
    String getCode();    // 에러 코드 문자열
    String getMessage(); // 에러 메시지
}
```

### 3.2 글로벌 에러 코드 정의
애플리케이션 전반에 걸쳐 공통으로 사용되는 에러 코드를 정의합니다.

```java
package com.example.project.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {
    // 서버 에러
    INTERNAL_SERVER_ERROR("G001", "서버 내부 오류가 발생했습니다."),
    
    // 입력값 검증 에러
    INVALID_INPUT_VALUE("G002", "유효하지 않은 입력값입니다."),
    INVALID_TYPE_VALUE("G003", "유효하지 않은 타입입니다."),
    
    // 리소스 접근 에러
    RESOURCE_NOT_FOUND("G004", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED("G005", "지원하지 않는 HTTP 메소드입니다."),
    
    // 인증/인가 에러
    UNAUTHORIZED("G006", "인증이 필요합니다."),
    ACCESS_DENIED("G007", "접근 권한이 없습니다."),
    
    // 비즈니스 로직 에러
    BUSINESS_EXCEPTION("G008", "비즈니스 로직 처리 중 오류가 발생했습니다.");

    private final String code;
    private final String message;
}
```

### 3.3 기본 예외 클래스 정의
모든 커스텀 예외의 부모 클래스로 사용될 기본 예외 클래스를 정의합니다.

```java
package com.example.project.global.exception;

import lombok.Getter;

@Getter
public abstract class GlobalException extends RuntimeException {
    private final ErrorCode errorCode;

    public GlobalException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GlobalException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public GlobalException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
```

## 4. 도메인별 예외 처리 구성

### 4.1 도메인별 에러 코드 정의
각 도메인(기능 영역)별로 특화된 에러 코드를 정의합니다. 코드 접두사는 도메인마다 다르게 설정합니다.

```java
package com.example.project.domain.activity.exception;

import com.example.project.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivityErrorCode implements ErrorCode {
    // 활동 관련 에러 코드
    ACTIVITY_LOG_NOT_FOUND("A001", "활동 기록을 찾을 수 없습니다."),
    
    // 출석체크 관련 에러 코드
    ATTENDANCE_ALREADY_CHECKED("A101", "이미 출석체크를 완료했습니다."),
    
    // 광고 시청 관련 에러 코드
    AD_VIEW_LIMIT_EXCEEDED("A201", "일일 광고 시청 횟수(3회)를 초과했습니다."),
    BASIC_REWARD_ALREADY_COMPLETED("A202", "이미 기본 적립을 완료했습니다."),
    ADDITIONAL_REWARD_NOT_AVAILABLE("A203", "추가 적립은 기본 적립 후 10분이 지나야 가능합니다.");

    private final String code;
    private final String message;
}
```

### 4.2 도메인별 기본 예외 클래스 정의
각 도메인별로 기본이 되는 예외 클래스를 정의합니다.

```java
package com.example.project.domain.activity.exception;

import com.example.project.global.exception.GlobalException;
import lombok.Getter;

@Getter
public abstract class ActivityException extends GlobalException {
    public ActivityException(ActivityErrorCode errorCode) {
        super(errorCode);
    }

    public ActivityException(ActivityErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public ActivityException(ActivityErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
```

### 4.3 구체적인 예외 클래스 정의
특정 상황에 대한 구체적인 예외 클래스를 정의합니다.

```java
package com.example.project.domain.activity.exception;

public class ActivityLogNotFoundException extends ActivityException {
    public ActivityLogNotFoundException() {
        super(ActivityErrorCode.ACTIVITY_LOG_NOT_FOUND);
    }
}

public class AttendanceAlreadyCheckedException extends ActivityException {
    public AttendanceAlreadyCheckedException() {
        super(ActivityErrorCode.ATTENDANCE_ALREADY_CHECKED);
    }
}

public class AdViewLimitExceededException extends ActivityException {
    public AdViewLimitExceededException() {
        super(ActivityErrorCode.AD_VIEW_LIMIT_EXCEEDED);
    }
}
```

### 4.4 에러 코드 명명 규칙
- 글로벌 에러: G로 시작 (Global)
- 사용자 관련: U로 시작 (User)
- 활동 관련: A로 시작 (Activity)
- 상품 관련: P로 시작 (Product)
- 주문 관련: O로 시작 (Order)
- 결제 관련: F로 시작 (Finance)

각 도메인 내에서는 세부 기능별로 두 번째 자리를 다르게 지정하여 구분합니다.

## 5. 예외 처리기(핸들러) 정의

### 5.1 글로벌 예외 처리기 정의
모든 예외를 일관된 형식으로 처리하는 글로벌 예외 처리기를 정의합니다.

```java
package com.example.project.global.exception;

import com.example.project.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // GlobalException 처리
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(GlobalException e) {
        log.error("GlobalException: {}", e.getMessage());
        return ResponseEntity
                .status(getHttpStatus(e.getErrorCode()))
                .body(ErrorResponse.of(
                        e.getErrorCode().getCode(),
                        e.getErrorCode().getMessage()
                ));
    }

    // 입력값 검증 에러 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        GlobalErrorCode.INVALID_INPUT_VALUE.getCode(),
                        e.getBindingResult().getAllErrors().get(0).getDefaultMessage()
                ));
    }

    // 기타 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        GlobalErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        GlobalErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }

    // ErrorCode에 따른 HttpStatus 결정하는 메소드
    private HttpStatus getHttpStatus(ErrorCode errorCode) {
        if (errorCode.getCode().startsWith("G004") || 
            errorCode.getCode().equals("A001")) {
            return HttpStatus.NOT_FOUND;
        } else if (errorCode.getCode().startsWith("G006")) {
            return HttpStatus.UNAUTHORIZED;
        } else if (errorCode.getCode().startsWith("G007")) {
            return HttpStatus.FORBIDDEN;
        } else if (errorCode.getCode().startsWith("G005")) {
            return HttpStatus.METHOD_NOT_ALLOWED;
        } else {
            return HttpStatus.BAD_REQUEST;
        }
    }
}
```

### 5.2 도메인별 예외 처리기 정의
도메인별로 특화된 예외를 처리하는 예외 처리기를 정의합니다. 이 핸들러는 글로벌 패키지에 위치합니다.

```java
package com.example.project.global.exception.handler;

import com.example.project.domain.activity.exception.*;
import com.example.project.global.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ActivityExceptionHandler {

    @ExceptionHandler(ActivityLogNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleActivityLogNotFoundException(ActivityLogNotFoundException e) {
        log.error("ActivityLogNotFoundException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        e.getErrorCode().getCode(),
                        e.getErrorCode().getMessage()
                ));
    }

    @ExceptionHandler(AttendanceAlreadyCheckedException.class)
    public ResponseEntity<ErrorResponse> handleAttendanceAlreadyCheckedException(AttendanceAlreadyCheckedException e) {
        log.error("AttendanceAlreadyCheckedException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        e.getErrorCode().getCode(),
                        e.getErrorCode().getMessage()
                ));
    }

    // 기타 활동 관련 예외 처리...
}
```

## 6. 응답 형식 정의
일관된 에러 응답 형식을 위한 DTO 클래스를 정의합니다.

```java
package com.example.project.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ErrorResponse {
    private final String code;          // 에러 코드
    private final String message;       // 에러 메시지
    private final LocalDateTime timestamp; // 발생 시간

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, LocalDateTime.now());
    }
}
```

## 7. 구현 예제

### 7.1 컨트롤러에서 예외 발생 예제

```java
package com.example.project.domain.activity.controller;

import com.example.project.domain.activity.dto.AttendanceRequest;
import com.example.project.domain.activity.dto.AttendanceResponse;
import com.example.project.domain.activity.exception.AttendanceAlreadyCheckedException;
import com.example.project.domain.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping("/attendance")
    public ResponseEntity<AttendanceResponse> checkAttendance(@RequestBody AttendanceRequest request) {
        AttendanceResponse response = activityService.checkAttendance(request);
        return ResponseEntity.ok(response);
    }
}
```

### 7.2 서비스에서 예외 발생 예제

```java
package com.example.project.domain.activity.service;

import com.example.project.domain.activity.dto.AttendanceRequest;
import com.example.project.domain.activity.dto.AttendanceResponse;
import com.example.project.domain.activity.entity.Activity;
import com.example.project.domain.activity.entity.ActivityType;
import com.example.project.domain.activity.exception.AttendanceAlreadyCheckedException;
import com.example.project.domain.activity.repository.ActivityRepository;
import com.example.project.domain.user.entity.User;
import com.example.project.domain.user.exception.UserNotFoundException;
import com.example.project.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AttendanceResponse checkAttendance(AttendanceRequest request) {
        // 사용자 존재 여부 확인
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(UserNotFoundException::new);

        // 오늘 출석체크 여부 확인
        if (activityRepository.existsByUserIdAndTypeAndCreatedAtAfter(
                user.getId(), ActivityType.ATTENDANCE, LocalDate.now().atStartOfDay())) {
            throw new AttendanceAlreadyCheckedException();
        }

        // 출석체크 로직 수행
        Activity activity = Activity.builder()
                .user(user)
                .type(ActivityType.ATTENDANCE)
                .createdAt(LocalDateTime.now())
                .build();
        
        activityRepository.save(activity);

        return AttendanceResponse.builder()
                .userId(user.getId())
                .attendanceDate(LocalDate.now())
                .success(true)
                .build();
    }
}
```

### 7.3 도메인별 예외 사용 예제

사용자 도메인의 경우:

```java
package com.example.project.domain.user.exception;

import com.example.project.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL("U002", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD("U003", "비밀번호가 일치하지 않습니다.");

    private final String code;
    private final String message;
}
```

```java
package com.example.project.domain.user.exception;

import com.example.project.global.exception.GlobalException;

public abstract class UserException extends GlobalException {
    public UserException(UserErrorCode errorCode) {
        super(errorCode);
    }
}
```

```java
package com.example.project.domain.user.exception;

public class UserNotFoundException extends UserException {
    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }
}
```