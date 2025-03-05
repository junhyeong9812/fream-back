# 알림 시스템 (Notification System)

## 개요

알림 시스템은 사용자에게 중요한 이벤트와 정보를 실시간으로 전달하기 위한 모듈입니다. 이 시스템은 쇼핑 관련 알림(거래, 입찰, 보관, 혜택 등)과 스타일 관련 알림(좋아요, 댓글, 팔로우 등)을 카테고리별로 제공하며, WebSocket을 통한 실시간 알림 전송 기능을 지원합니다. 관리자는 전체 사용자에게 알림을 발송할 수 있으며, 사용자는 자신의 알림을 카테고리별로 조회하고 읽음 처리할 수 있습니다.

## 아키텍처

```
com.fream.back.domain.notification/
├── controller/
│   ├── command/            # 알림 생성, 읽음 처리, WebSocket 연결 관리
│   └── query/              # 알림 조회 및 필터링
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 알림 생성, 읽음 처리 서비스
    └── query/              # 알림 조회 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **NotificationCommandController**: 알림 생성, 읽음 처리, WebSocket Ping 처리를 위한 API를 제공합니다.
2. **NotificationQueryController**: 알림 조회 및 필터링을 위한 API를 제공합니다.

### 서비스

1. **NotificationCommandService**: 알림 생성, 읽음 처리, 삭제 로직을 담당합니다.
2. **NotificationQueryService**: 알림 조회 및 필터링 로직을 담당합니다.

### 엔티티

1. **Notification**: 알림 정보(카테고리, 유형, 메시지, 읽음 상태 등)를 저장합니다.
2. **NotificationCategory**: 알림 상위 카테고리 열거형(SHOPPING, STYLE).
3. **NotificationType**: 알림 세부 유형 열거형(TRADE, BID, STORAGE, FAVORITE, BENEFIT, ANNOUNCEMENT, LIKE, COMMENT, FOLLOW).

### 저장소

**NotificationRepository**: 알림 엔티티의 CRUD 및 다양한 조회 기능을 제공합니다.

## API 엔드포인트

### Command API

```
POST /notifications?userId={userId}
```
특정 사용자에게 알림을 생성합니다.

**요청 본문 예시:**
```json
{
  "category": "SHOPPING",
  "type": "BID",
  "message": "입찰이 성공적으로 완료되었습니다."
}
```

```
POST /notifications/broadcast
```
모든 사용자에게 알림을 전송합니다(관리자 전용).

**요청 본문 예시:**
```json
{
  "category": "SHOPPING",
  "type": "ANNOUNCEMENT",
  "message": "신규 이벤트가 시작되었습니다. 지금 확인해보세요!"
}
```

```
PATCH /notifications/{id}/read
```
특정 알림을 읽음 상태로 변경합니다.

### Query API

```
GET /notifications/filter/category?category=SHOPPING
```
특정 카테고리의 알림만 조회합니다.

```
GET /notifications/filter/type?type=BID
```
특정 유형의 알림만 조회합니다.

```
GET /notifications/filter/category/read-status?category=SHOPPING&isRead=false&page=0&size=10
```
카테고리와 읽음 상태를 기준으로 알림을 필터링하여 조회합니다.

```
GET /notifications/filter/type/read-status?type=BID&isRead=false&page=0&size=10
```
유형과 읽음 상태를 기준으로 알림을 필터링하여 조회합니다.

### WebSocket 엔드포인트

```
/app/ping
```
클라이언트가 주기적으로 Ping을 보내 WebSocket 연결을 유지하고 Redis TTL을 갱신합니다.

**WebSocket 구독 경로:**
```
/user/queue/notifications
```
사용자는 이 경로를 구독하여 실시간 알림을 수신합니다.

## 데이터베이스 스키마

### NOTIFICATION 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| category       | VARCHAR        | 알림 카테고리(ENUM 문자열)     |
| type           | VARCHAR        | 알림 유형(ENUM 문자열)        |
| message        | VARCHAR        | 알림 메시지                  |
| is_read        | BOOLEAN        | 읽음 상태                    |
| user_id        | BIGINT         | 사용자 외래 키                |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

## 알림 카테고리 및 유형

### 카테고리

- **SHOPPING**: 쇼핑 관련 알림 (거래, 입찰, 보관, 혜택 등)
- **STYLE**: 스타일 관련 알림 (좋아요, 댓글, 팔로우 등)

### 유형

#### SHOPPING 카테고리
- **TRADE**: 거래 관련 알림
- **BID**: 입찰 관련 알림
- **STORAGE**: 보관 관련 알림
- **FAVORITE**: 관심 상품 관련 알림
- **BENEFIT**: 혜택 관련 알림
- **ANNOUNCEMENT**: 공지사항 관련 알림

#### STYLE 카테고리
- **LIKE**: 좋아요 관련 알림
- **COMMENT**: 댓글 관련 알림
- **FOLLOW**: 팔로우 관련 알림

## 주요 기능 상세

### 1. 실시간 알림 전송

알림 시스템은 WebSocket을 통해 실시간 알림을 전송합니다:

1. **연결 관리**:
    - 사용자가 로그인하면 WebSocket 연결을 맺고 `/user/queue/notifications` 경로를 구독합니다.
    - 클라이언트는 주기적으로 `/app/ping`에 메시지를 보내 연결을 유지합니다.
    - Redis를 사용하여 연결된 사용자 정보를 관리합니다(`WebSocket:User:{email}` 키).

2. **알림 전송**:
    - 알림 이벤트가 발생하면 `NotificationCommandService`가 알림을 생성합니다.
    - Redis에 해당 사용자의 키가 있다면 WebSocket을 통해 알림을 실시간으로 전송합니다.
    - `SimpMessagingTemplate`을 사용하여 특정 사용자에게 알림을 전송합니다.

### 2. 전체 사용자 알림 발송

관리자는 모든 사용자에게 동시에 알림을 발송할 수 있습니다:

1. `createNotificationForAll` 메소드는 모든 사용자를 조회합니다.
2. 각 사용자마다 동일한 내용의 알림을 생성합니다.
3. 현재 연결된 사용자에게는 WebSocket을 통해 실시간으로 알림을 전송합니다.

### 3. 알림 필터링

사용자는 다양한 기준으로 알림을 필터링하여 조회할 수 있습니다:

1. **카테고리별 필터링**: SHOPPING 또는 STYLE 카테고리 기준 조회
2. **유형별 필터링**: 세부 유형(TRADE, BID, LIKE 등) 기준 조회
3. **읽음 상태 기준 필터링**: 읽지 않은 알림 또는 읽은 알림만 조회
4. **복합 필터링**: 카테고리 + 읽음 상태, 유형 + 읽음 상태 등 복합 조건 조회

### 4. 외부 시스템 연동

다른 도메인과 연동하여 다양한 상황에서 알림을 발생시킵니다:

1. **주문 시스템 연동**: 배송 완료 시 알림 발송
2. **공지사항 시스템 연동**: 새 공지사항 등록 시 전체 사용자에게 알림 발송
3. **향후 추가될 연동**: 추가 시스템과의 연동은 `NotificationCommandService`에 메소드를 추가하여 구현

## 보안

1. 모든 알림 API는 인증된 사용자만 접근할 수 있습니다.
2. 사용자는 자신의 알림만 조회하고 읽음 처리할 수 있습니다.
3. 전체 사용자 알림 발송 기능은 관리자 권한이 필요합니다(필요에 따라 구현).

## Redis 활용

알림 시스템은 Redis를 사용하여 WebSocket 연결 상태를 관리합니다:

1. **키 형식**: `WebSocket:User:{email}`
2. **TTL 관리**:
    - 연결 시 30분 TTL로 키를 설정합니다.
    - 클라이언트의 주기적인 Ping으로 TTL이 10분 이하로 남았을 때 갱신합니다.
3. **연결 확인**: 알림 발송 전 Redis 키 존재 여부로 연결 상태를 확인합니다.

## 구현 참고사항

### 1. WebSocket 연결 관리

클라이언트는 주기적으로 서버에 Ping을 보내 연결을 유지해야 합니다:

```javascript
// 클라이언트 예시 코드
function sendPing() {
  stompClient.send("/app/ping", {}, {});
}

// 5분마다 ping 전송
setInterval(sendPing, 300000);
```

### 2. 알림 구독 방법

클라이언트는 다음과 같이 알림을 구독합니다:

```javascript
// 클라이언트 예시 코드
stompClient.subscribe('/user/queue/notifications', function(notification) {
  const payload = JSON.parse(notification.body);
  // 알림 처리 로직
});
```

### 3. DTO와 엔티티 변환

서비스 레이어에서는 엔티티와 DTO 간의 변환을 담당합니다:

```java
private NotificationDTO toDTO(Notification notification) {
    return NotificationDTO.builder()
            .id(notification.getId())
            .category(notification.getCategory())
            .type(notification.getType())
            .message(notification.getMessage())
            .isRead(notification.isRead())
            .createdAt(notification.getCreatedDate().toString())
            .build();
}
```

## 확장 가능성

1. **알림 설정**: 사용자별로 알림 수신 여부를 설정할 수 있는 기능 추가
2. **알림 그룹화**: 동일한 유형의 알림을 그룹화하여 표시하는 기능
3. **알림 만료**: 일정 기간이 지난 알림을 자동으로 삭제하거나 아카이브하는 기능
4. **모바일 푸시 알림**: Firebase Cloud Messaging(FCM) 등을 사용한 모바일 푸시 알림 연동
5. **이메일/SMS 알림**: 중요 알림은 이메일이나 SMS로도 발송하는 기능