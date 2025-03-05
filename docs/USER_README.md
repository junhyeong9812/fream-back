# 사용자 시스템 (User System)

## 개요

사용자 시스템은 애플리케이션의 인증, 사용자 관리, 프로필 관리 등을 담당하는 종합적인 모듈입니다. JWT(JSON Web Token)를 활용한 보안 인증 메커니즘과 Redis를 이용한 토큰 관리를 통해 안전하고 효율적인 사용자 인증 시스템을 제공합니다. 사용자 계정 생성부터 프로필 관리, 비밀번호 재설정, 계정 삭제까지 사용자 관련 전체 라이프사이클을 지원합니다.

## 아키텍처

이 시스템은 CQRS(Command Query Responsibility Segregation) 패턴을 따르며 다음과 같이 구성되어 있습니다:

```
com.fream.back.domain.user/
├── controller/
│   ├── command/          # 사용자 생성, 업데이트, 인증 등 명령 컨트롤러
│   └── query/            # 사용자 정보 조회 컨트롤러
├── dto/                  # 데이터 전송 객체
├── entity/               # 데이터베이스 엔티티 및 열거형
├── redis/                # Redis 관련 서비스
├── repository/           # 데이터 접근 계층
├── security/             # 보안 관련 컴포넌트
├── service/              
│   ├── command/          # 사용자 관련 명령 서비스
│   └── query/            # 사용자 관련 조회 서비스
└── utils/                # 유틸리티 클래스
```

## 주요 기능

### 인증 (Authentication)

1. **로그인**: 이메일/비밀번호 기반 인증 및 JWT 토큰 발급
2. **토큰 관리**: Access Token 및 Refresh Token 발급 및 관리
3. **Redis 기반 토큰 저장소**: 화이트리스트 방식의 토큰 관리
4. **로그아웃**: 토큰 무효화 및 쿠키 제거

### 사용자 관리

1. **회원가입**: 새 사용자 등록 및 기본 프로필 생성
2. **계정 정보 업데이트**: 이메일, 비밀번호, 전화번호 등 사용자 정보 변경
3. **계정 삭제**: 회원 탈퇴 및 관련 데이터 정리
4. **비밀번호 찾기/재설정**: 이메일을 통한 임시 비밀번호 발급 및 비밀번호 변경

### 프로필 관리

1. **프로필 정보 조회**: 사용자 프로필 정보 조회
2. **프로필 업데이트**: 프로필 사진, 소개, 닉네임 등 변경
3. **계정 설정**: 알림 설정, 개인정보 제공 동의 등 관리

## 데이터 모델

### 사용자 (User) 엔티티

사용자 엔티티는 다음과 같은 주요 필드를 포함합니다:

- **id**: 사용자 ID (PK)
- **email**: 이메일 주소 (유니크)
- **password**: 암호화된 비밀번호
- **phoneNumber**: 전화번호 (유니크)
- **referralCode**: 추천인 코드 (유니크)
- **shoeSize**: 신발 사이즈 (Enum)
- **age**: 나이
- **gender**: 성별 (Enum)
- **role**: 사용자 역할 (USER, ADMIN 등)
- **sellerGrade**: 판매자 등급
- **termsAgreement**: 이용약관 동의 여부
- **phoneNotificationConsent**: 전화 알림 동의 여부
- **emailNotificationConsent**: 이메일 알림 동의 여부
- **optionalPrivacyAgreement**: 선택적 개인정보 동의 여부

### 관계 엔티티

- **Profile**: 사용자 프로필 정보 (1:1)
- **Address**: 주소록 (1:N)
- **PaymentInfo**: 결제 정보 (1:N)
- **Interest**: 관심 상품 (1:N)
- **BankAccount**: 은행 계좌 정보 (1:1)
- **Point**: 포인트 내역 (1:N)

## API 엔드포인트

### 인증 관련 API

```
POST /auth/login
```
이메일과 비밀번호로 로그인하고 JWT 토큰을 쿠키로 발급합니다.

**요청 본문 예시:**
```json
{
  "email": "user@example.com",
  "password": "securePassword123"
}
```

```
POST /auth/refresh
```
Refresh Token을 사용하여 새로운 Access Token을 발급합니다.

```
POST /auth/logout
```
현재 세션을 로그아웃 처리하고 토큰을 무효화합니다.

```
GET /auth/email
```
현재 로그인한 사용자의 이메일을 반환합니다.

### 사용자 관리 API

```
POST /users/register
```
새 사용자를 등록합니다.

**요청 본문 예시:**
```json
{
  "email": "newuser@example.com",
  "password": "securePassword123",
  "phoneNumber": "01012345678",
  "shoeSize": "SIZE_270",
  "isOver14": true,
  "termsAgreement": true,
  "privacyAgreement": true,
  "optionalPrivacyAgreement": false,
  "adConsent": false
}
```

```
PUT /users/update
```
현재 로그인한 사용자의 정보를 업데이트합니다.

**요청 본문 예시:**
```json
{
  "Password": "currentPassword",
  "newEmail": "newemail@example.com",
  "newPassword": "newSecurePassword123",
  "newPhoneNumber": "01087654321",
  "newShoeSize": "SIZE_275",
  "smsConsent": true,
  "emailConsent": false
}
```

```
GET /users/login-info
```
현재 로그인한 사용자의 기본 정보를 조회합니다.

```
POST /users/find-email
```
전화번호를 이용해 이메일을 찾습니다.

**요청 본문 예시:**
```json
{
  "phoneNumber": "01012345678"
}
```

```
POST /users/reset-password
```
비밀번호 재설정 자격을 확인합니다.

**요청 본문 예시:**
```json
{
  "email": "user@example.com",
  "phoneNumber": "01012345678"
}
```

```
POST /users/reset-password-sandEmail
```
이메일로 임시 비밀번호를 발송합니다.

```
DELETE /users/delete-account
```
현재 로그인한 사용자의 계정을 삭제합니다.

## 인증 메커니즘

### JWT 토큰 관리

1. **토큰 발급**: 로그인 시 Access Token과 Refresh Token이 발급됩니다.
  - Access Token: 짧은 만료 시간 (기본 30분)
  - Refresh Token: 긴 만료 시간 (기본 1일)

2. **토큰 저장**: 모든 토큰은 클라이언트의 쿠키에 저장됩니다.
  - HttpOnly: 자바스크립트로 접근 불가
  - Secure: HTTPS 전용
  - SameSite: None (크로스 사이트 요청 허용)

3. **토큰 갱신**: Access Token이 만료되면 Refresh Token을 사용하여 새로운 토큰을 발급받을 수 있습니다.

4. **Redis 기반 화이트리스트**: 유효한 토큰만 Redis에 저장되어 있으며, 로그아웃 시 Redis에서 제거됩니다.

### 보안 고려사항

1. **비밀번호 암호화**: BCrypt를 사용하여 모든 비밀번호가 해싱되어 저장됩니다.
2. **CSRF 보호**: API 요청에 대한 CSRF 토큰 검증이 구현되어 있습니다.
3. **토큰 무효화**: 로그아웃 또는 비밀번호 변경 시 모든 기존 토큰이 무효화됩니다.
4. **IP 기반 추적**: 로그인 시 IP 주소가 기록되어 의심스러운 활동을 감지합니다.

## 이메일 변경 프로세스

사용자가 이메일을 변경할 경우 다음과 같은 프로세스가 진행됩니다:

1. 현재 비밀번호 확인을 통한 본인 인증
2. 새 이메일 주소 유효성 검증
3. 기존 토큰 제거 및 Redis에서 세션 삭제
4. 새 이메일 주소로 새 토큰 발급
5. 쿠키 업데이트 및 세션 갱신

## 비밀번호 재설정 프로세스

비밀번호를 잊어버린 사용자를 위한 프로세스:

1. 이메일과 전화번호 검증
2. 임시 비밀번호 생성 및 사용자 이메일로 전송
3. 임시 비밀번호로 로그인 후 비밀번호 변경 권장

## 구현 참고사항

### 개발자를 위한 안내

1. **새 사용자 속성 추가**:
  - 엔티티에 필드 추가
  - DTO 업데이트
  - 관련 서비스 메서드 추가

2. **토큰 유효 기간 변경**:
  - `application.properties` 또는 환경 변수에서 다음 값 수정:
    - `jwt.expiration`: Access Token 만료 시간 (밀리초)
    - `jwt.refreshExpiration`: Refresh Token 만료 시간 (밀리초)

3. **비밀번호 정책 변경**:
  - `UserControllerValidator` 클래스의 비밀번호 검증 로직 수정

### 설정 요구사항

1. **Redis 설정**:
  - 토큰 저장을 위한 Redis 서버 필요
  - 다음 속성 설정 필요:
    - `spring.redis.host`: Redis 호스트
    - `spring.redis.port`: Redis 포트

2. **SMTP 설정**:
  - 비밀번호 찾기 이메일 전송을 위한 SMTP 서버 설정
  - 다음 속성 설정 필요:
    - `spring.mail.host`: SMTP 서버 호스트
    - `spring.mail.port`: SMTP 서버 포트
    - `spring.mail.username`: SMTP 계정
    - `spring.mail.password`: SMTP 비밀번호
    - `spring.mail.properties.mail.smtp.auth`: SMTP 인증 사용 여부
    - `spring.mail.properties.mail.smtp.starttls.enable`: STARTTLS 사용 여부

## 문제 해결

1. **토큰 인증 실패**: 쿠키가 올바르게 설정되었는지, Redis 서버가 실행 중인지 확인하세요.

2. **비밀번호 재설정 이메일이 도착하지 않음**: SMTP 설정을 확인하고, 이메일 서비스 로그를 검토하세요.

3. **중복된 이메일/전화번호 오류**: 해당 필드가 이미 데이터베이스에 존재하는지 확인하세요.

4. **세션이 빠르게 만료됨**: 토큰 유효 기간 설정을 검토하고, 클라이언트의 시간이 서버와 동기화되어 있는지 확인하세요.