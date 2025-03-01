# 사용자(User) 도메인

이 문서는 Fream 프로젝트의 사용자(User) 도메인에 대한 상세 정보를 제공합니다.

## 목차
- [엔티티 구조](#엔티티-구조)
- [API 엔드포인트](#api-엔드포인트)
    - [인증(Auth) API](#인증auth-api)
    - [사용자(User) API](#사용자user-api)
    - [프로필(Profile) API](#프로필profile-api)
    - [팔로우(Follow) API](#팔로우follow-api)
    - [포인트(Point) API](#포인트point-api)
    - [계좌정보(Bank Account) API](#계좌정보bank-account-api)
    - [차단(Block) API](#차단block-api)

## 엔티티 구조

### User

사용자의 기본 정보와 계정 정보를 관리합니다.

| 필드                       | 설명                       | 타입               |
|--------------------------|--------------------------|-------------------|
| id                       | 사용자 ID (PK)              | Long              |
| email                    | 이메일 주소 (Unique)          | String            |
| password                 | 암호화된 비밀번호               | String            |
| referralCode             | 고유 추천인 코드 (Unique)       | String            |
| phoneNumber              | 전화번호 (Unique)            | String            |
| shoeSize                 | 신발 사이즈 (Enum)            | ShoeSize          |
| termsAgreement           | 이용약관 동의 여부               | boolean           |
| phoneNotificationConsent | 전화 알림 수신 동의 여부           | boolean           |
| emailNotificationConsent | 이메일 수신 동의 여부             | boolean           |
| optionalPrivacyAgreement | 선택적 개인정보 동의 여부           | boolean           |
| role                     | 사용자 역할 (USER, ADMIN)     | Role              |
| sellerGrade              | 판매자 등급 (1~5)             | Integer           |
| age                      | 나이                       | Integer           |
| gender                   | 성별 (MALE, FEMALE, OTHER) | Gender            |
| profile                  | 사용자 프로필 (1:1)            | Profile           |
| interests                | 관심 상품 (1:N)              | List\<Interest\>   |
| addresses                | 주소록 (1:N)                | List\<Address\>    |
| paymentInfos             | 결제 정보 (1:N)              | List\<PaymentInfo\> |
| bankAccount              | 판매 정산 계좌 (1:1)           | BankAccount       |
| points                   | 포인트 내역 (1:N)             | List\<Point\>      |
| referredUsers            | 내가 추천한 사용자 (1:N)         | List\<User\>       |
| referrer                 | 나를 추천한 사용자 (N:1)         | User              |

### Profile

사용자의 프로필 정보를 관리합니다.

| 필드              | 설명                | 타입                     |
|-----------------|-------------------|------------------------|
| id              | 프로필 ID (PK)       | Long                    |
| user            | 프로필 소유 사용자 (1:1) | User                    |
| profileName     | 프로필 이름 (Unique)   | String                  |
| Name            | 실명                | String                  |
| bio             | 소개글               | String                  |
| isPublic        | 프로필 공개 여부         | boolean                 |
| profileImageUrl | 프로필 이미지 URL       | String                  |
| followings      | 내가 팔로우한 프로필 (1:N) | List\<Follow\>          |
| followers       | 나를 팔로우한 프로필 (1:N) | List\<Follow\>          |
| blockedByProfiles | 나를 차단한 프로필 (1:N) | List\<BlockedProfile\> |
| styles          | 스타일 (1:N)         | List\<Style\>           |

### Follow

사용자 간의 팔로우 관계를 관리합니다.

| 필드               | 설명                | 타입     |
|------------------|-------------------|--------|
| id               | 팔로우 ID (PK)       | Long   |
| follower         | 팔로우를 한 프로필 (N:1) | Profile |
| following        | 팔로우된 프로필 (N:1)   | Profile |

### Point

사용자의 포인트 적립 및 사용 내역을 관리합니다.

| 필드              | 설명                                 | 타입           |
|-----------------|------------------------------------|--------------|
| id              | 포인트 ID (PK)                        | Long         |
| user            | 포인트 소유 사용자 (N:1)                   | User         |
| amount          | 초기 포인트 금액                          | int          |
| remainingAmount | 남은 포인트 금액                          | int          |
| reason          | 포인트 적립/사용 이유                       | String       |
| expirationDate  | 포인트 유효기간                           | LocalDate    |
| status          | 포인트 상태 (AVAILABLE, USED, EXPIRED) | PointStatus  |

### BankAccount

사용자의 판매 정산 계좌 정보를 관리합니다.

| 필드            | 설명                | 타입     |
|---------------|-------------------|--------|
| id            | 계좌 ID (PK)        | Long   |
| user          | 계좌 소유 사용자 (1:1)   | User   |
| bankName      | 은행명               | String |
| accountNumber | 계좌 번호             | String |
| accountHolder | 예금주 이름            | String |

### BlockedProfile

사용자 간의 차단 관계를 관리합니다.

| 필드            | 설명                | 타입     |
|---------------|-------------------|--------|
| id            | 차단 ID (PK)        | Long   |
| profile       | 차단을 설정한 프로필 (N:1) | Profile |
| blockedProfile | 차단된 프로필 (N:1)    | Profile |

## API 엔드포인트

### 인증(Auth) API

#### 로그인

**POST /auth/login**
- 요청: `LoginRequestDto`
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- 응답: JWT 토큰이 쿠키로 설정됨 (ACCESS_TOKEN, REFRESH_TOKEN)

#### 토큰 새로 고침

**POST /auth/refresh**
- 요청: REFRESH_TOKEN 쿠키가 포함된 요청
- 응답: 새 ACCESS_TOKEN이 쿠키로 설정됨

#### 로그아웃

**POST /auth/logout**
- 요청: ACCESS_TOKEN 및 REFRESH_TOKEN 쿠키가 포함된 요청
- 응답: 쿠키 만료 및 토큰 무효화

#### 이메일 확인

**GET /auth/email**
- 요청: ACCESS_TOKEN 쿠키가 포함된 요청
- 응답: 현재 로그인한 사용자의 이메일

### 사용자(User) API

#### 회원가입

**POST /users/register**
- 요청: `UserRegistrationDto`
  ```json
  {
    "email": "user@example.com",
    "password": "password123",
    "phoneNumber": "01012345678",
    "referralCode": "ABC123",
    "shoeSize": "SIZE_270",
    "isOver14": true,
    "termsAgreement": true,
    "privacyAgreement": true,
    "optionalPrivacyAgreement": false,
    "adConsent": false
  }
  ```
- 응답: 회원가입 성공 메시지

#### 사용자 정보 업데이트

**PUT /users/update**
- 요청: `LoginInfoUpdateDto`
  ```json
  {
    "newEmail": "newuser@example.com",
    "Password": "currentPassword",
    "newPassword": "newPassword123",
    "newPhoneNumber": "01098765432",
    "newShoeSize": "SIZE_275",
    "adConsent": true,
    "privacyConsent": true,
    "smsConsent": true,
    "emailConsent": true
  }
  ```
- 응답: 업데이트된 `LoginInfoDto`

#### 로그인 정보 조회

**GET /users/login-info**
- 응답: `LoginInfoDto`
  ```json
  {
    "email": "user@example.com",
    "phoneNumber": "01012345678",
    "shoeSize": "SIZE_270",
    "optionalPrivacyAgreement": true,
    "smsConsent": true,
    "emailConsent": true
  }
  ```

#### 이메일 찾기

**POST /users/find-email**
- 요청: `EmailFindRequestDto`
  ```json
  {
    "phoneNumber": "01012345678"
  }
  ```
- 응답: 해당 전화번호로 등록된 이메일

#### 비밀번호 재설정 자격 확인

**POST /users/reset-password**
- 요청: `PasswordResetRequestDto`
  ```json
  {
    "email": "user@example.com",
    "phoneNumber": "01012345678"
  }
  ```
- 응답: 확인 상태

#### 임시 비밀번호 이메일 발송

**POST /users/reset-password-sandEmail**
- 요청: `PasswordResetRequestDto`
  ```json
  {
    "email": "user@example.com",
    "phoneNumber": "01012345678"
  }
  ```
- 응답: 이메일 발송 상태

#### 비밀번호 재설정

**POST /users/reset**
- 요청: `PasswordResetRequestDto`
  ```json
  {
    "email": "user@example.com",
    "phoneNumber": "01012345678",
    "newPassword": "newPassword123",
    "confirmPassword": "newPassword123"
  }
  ```
- 응답: 비밀번호 변경 성공 메시지

#### 회원 탈퇴

**DELETE /users/delete-account**
- 응답: 회원 탈퇴 완료 메시지

### 프로필(Profile) API

#### 프로필 조회

**GET /profiles**
- 응답: `ProfileInfoDto`
  ```json
  {
    "profileId": 1,
    "profileImage": "profile_1_abc123.jpg",
    "profileName": "username",
    "realName": "User Name",
    "email": "user@example.com",
    "bio": "자기소개",
    "isPublic": true,
    "blockedProfiles": []
  }
  ```

#### 프로필 업데이트

**PUT /profiles**
- 요청: Multipart 요청
    - `profileImage`: 프로필 이미지 파일 (선택)
    - `dto`: `ProfileUpdateDto`
      ```json
      {
        "profileName": "newUsername",
        "Name": "New Name",
        "bio": "새로운 자기소개",
        "isPublic": true
      }
      ```
- 응답: 프로필 업데이트 성공 메시지

#### 프로필 이미지 조회

**GET /profiles/{profileId}/image**
- 응답: 프로필 이미지 파일

### 팔로우(Follow) API

#### 팔로우 생성

**POST /follows/{profileId}**
- 응답: 팔로우 성공 메시지

#### 팔로우 삭제

**DELETE /follows/{profileId}**
- 응답: 팔로우 삭제 성공 메시지

#### 팔로워 목록 조회

**GET /follows/followers**
- 응답: 페이지네이션된 `FollowDto` 목록

#### 팔로잉 목록 조회

**GET /follows/followings**
- 응답: 페이지네이션된 `FollowDto` 목록

### 포인트(Point) API

#### 포인트 적립

**POST /points/commands**
- 요청: `PointDto.AddPointRequest`
  ```json
  {
    "amount": 1000,
    "reason": "회원가입 보너스"
  }
  ```
- 응답: `PointDto.PointResponse`

#### 포인트 사용

**POST /points/commands/use**
- 요청: `PointDto.UsePointRequest`
  ```json
  {
    "amount": 500,
    "reason": "상품 구매"
  }
  ```
- 응답: `PointDto.UsePointResponse`

#### 포인트 내역 조회

**GET /points/queries**
- 응답: `PointDto.PointResponse` 목록

#### 사용 가능 포인트 조회

**GET /points/queries/available**
- 응답: 사용 가능한 `PointDto.PointResponse` 목록

#### 포인트 요약 정보 조회

**GET /points/queries/summary**
- 응답: `PointDto.PointSummaryResponse`

#### 포인트 상세 조회

**GET /points/queries/{pointId}**
- 응답: `PointDto.PointResponse`

### 계좌정보(Bank Account) API

#### 계좌 등록/수정

**POST /bank-account**
- 요청: `BankAccountDto`
  ```json
  {
    "bankName": "신한은행",
    "accountNumber": "110-123-456789",
    "accountHolder": "홍길동"
  }
  ```
- 응답: 계좌 등록/수정 성공 메시지

#### 계좌 정보 조회

**GET /bank-account**
- 응답: `BankAccountInfoDto`
  ```json
  {
    "bankName": "신한은행",
    "accountNumber": "110-123-456789",
    "accountHolder": "홍길동"
  }
  ```

#### 계좌 정보 삭제

**DELETE /bank-account**
- 응답: 계좌 삭제 성공 메시지

### 차단(Block) API

#### 프로필 차단

**POST /profiles/blocked**
- 요청:
  ```json
  {
    "blockedProfileId": 2
  }
  ```
- 응답: 차단 성공 메시지

#### 차단 해제

**DELETE /profiles/blocked?blockedProfileId={profileId}**
- 응답: 차단 해제 성공 메시지

#### 차단 목록 조회

**GET /profiles/blocked**
- 응답: `BlockedProfileDto` 목록
  ```json
  [
    {
      "profileId": 2,
      "profileName": "blockedUser",
      "profileImageUrl": "profile_2_xyz987.jpg"
    }
  ]
  ```