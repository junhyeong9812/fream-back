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
├── exception/              # 알림 도메인 예외 처리
├── repository/
│   ├── base/               # JpaRepository 인터페이스
│   ├── custom/             # QueryDSL용 커스텀 인터페이스
│   └── impl/               # QueryDSL 구현체
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

1. **NotificationRepository**: 기본적인 CRUD 연산을 제공하는 JpaRepository 인터페이스입니다.
2. **NotificationRepositoryCustom**: QueryDSL을 활용한 복잡한 쿼리 메서드를 정의하는 커스텀 인터페이스입니다.
3. **NotificationRepositoryCustomImpl**: QueryDSL 활용 커스텀 인터페이스의 구현체입니다.

## 주요 개선 사항

### 1. QueryDSL 도입
- 복잡한 JPQL 쿼리를 QueryDSL로 대체하여 타입 안전성 및 가독성 향상
- 동적 쿼리 처리 개선으로 복잡한 필터링 조건을 유연하게 처리
- 커스텀 레포지토리 인터페이스와 구현체로 분리하여 관심사 분리

### 2. 리팩토링 및 코드 품질 개선
- 메서드 및 클래스 분리를 통한 관심사 분리
- 헬퍼 메서드 분리로 코드 중복 제거
- 상수값 추출로 매직 넘버/스트링 제거
- 코드 문서화 개선 (JavaDoc)
- 일관된 로깅 패턴 적용

### 3. 기능 개선 및 추가
- 모든 알림 읽음 처리 API 추가
- 읽지 않은 알림 개수 조회 API 추가
- 정렬 기능 추가 (동적 정렬)
- 페이지네이션 파라미터 검증 로직 개선
- 유효성 검사 강화

### 4. 구조 개선
- CQRS 패턴에 맞게 Command와 Query 서비스 분리 유지
- 컨트롤러에서 비즈니스 로직 제거 및 서비스로 이관
- 예외 처리 계층 개선

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

```
PATCH /notifications/read-all
```
사용자의 모든 알림을 읽음 상태로 변경합니다 (신규 API).

```
DELETE /notifications/{id}
```
특정 알림을 삭제합니다.

```
DELETE /notifications/user
```
사용자의 모든 알림을 삭제합니다.

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
GET /notifications/filter/category/read-status?category=SHOPPING&isRead=false&page=0&size=10&sort=createdDate,desc
```
카테고리와 읽음 상태를 기준으로 알림을 필터링하여 조회합니다.
- 정렬 옵션을 추가로 지정할 수 있습니다 (신규 기능).

```
GET /notifications/filter/type/read-status?type=BID&isRead=false&page=0&size=10&sort=createdDate,desc
```
유형과 읽음 상태를 기준으로 알림을 필터링하여 조회합니다.
- 정렬 옵션을 추가로 지정할 수 있습니다 (신규 기능).

```
GET /notifications/count/unread
```
읽지 않은 알림 개수를 조회합니다 (신규 API).

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
4. 개선된 메서드 구조로 유지보수성이 향상되었습니다.

### 3. 알림 필터링

사용자는 다양한 기준으로 알림을 필터링하여 조회할 수 있습니다:

1. **카테고리별 필터링**: SHOPPING 또는 STYLE 카테고리 기준 조회
2. **유형별 필터링**: 세부 유형(TRADE, BID, LIKE 등) 기준 조회
3. **읽음 상태 기준 필터링**: 읽지 않은 알림 또는 읽은 알림만 조회
4. **복합 필터링**: 카테고리 + 읽음 상태, 유형 + 읽음 상태 등 복합 조건 조회
5. **동적 정렬**: 원하는 필드와 정렬 방향으로 조회 결과 정렬 (신규 기능)

### 4. 읽음 처리 기능 강화

1. **단일 알림 읽음 처리**: 특정 알림을 읽음 상태로 변경합니다.
2. **전체 알림 읽음 처리**: 사용자의 모든 알림을 한 번에 읽음 상태로 변경합니다 (신규 기능).
3. **읽지 않은 알림 개수 조회**: 읽지 않은 알림의 개수를 빠르게 조회할 수 있습니다 (신규 기능).

### 5. 외부 시스템 연동

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

## 성능 최적화

### 1. QueryDSL 활용
- 복잡한 동적 쿼리를 QueryDSL로 구현하여 가독성과 유지보수성 향상
- 불필요한 JOIN 회피 및 필요한 경우에만 fetchJoin 사용
- 카운트 쿼리 최적화를 위한 PageableExecutionUtils 활용

### 2. 조회 성능 최적화
- 엔티티 조회 시 Fetch Join을 통한 N+1 문제 방지
- 필요한 데이터만 효율적으로 로딩하기 위한 동적 쿼리 활용
- 페이지네이션과 정렬에 대한 최적화 적용

### 3. 실시간 알림 최적화
- Redis를 활용한 연결 상태 관리로 불필요한 알림 전송 회피
- 벌크 알림 발송 시 연결된 사용자에게만 선택적 전송
- 조회가 많은 서비스이므로 `@Transactional(readOnly = true)` 적극 활용

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

### 3. QueryDSL 활용 예시

```java
// 동적 조건을 활용한 QueryDSL 쿼리 예시
public Page<Notification> findByUserEmailAndCategoryAndIsRead(
        String email, NotificationCategory category, boolean isRead, Pageable pageable) {
    
    List<Notification> content = queryFactory
            .selectFrom(notification)
            .join(notification.user, user).fetchJoin()
            .where(
                    userEmailEq(email),
                    categoryEq(category),
                    isReadEq(isRead)
            )
            .orderBy(getOrderSpecifiers(pageable).toArray(OrderSpecifier[]::new))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();
            
    JPAQuery<Long> countQuery = queryFactory
            .select(notification.count())
            .from(notification)
            .join(notification.user, user)
            .where(
                    userEmailEq(email),
                    categoryEq(category),
                    isReadEq(isRead)
            );
            
    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
}

// 동적 조건 메서드
private BooleanExpression categoryEq(NotificationCategory category) {
    return category != null ? notification.category.eq(category) : null;
}
```

## 확장 가능성

1. **알림 설정**: 사용자별로 알림 수신 여부를 설정할 수 있는 기능 추가
2. **알림 그룹화**: 동일한 유형의 알림을 그룹화하여 표시하는 기능
3. **알림 만료**: 일정 기간이 지난 알림을 자동으로 삭제하거나 아카이브하는 기능
4. **모바일 푸시 알림**: Firebase Cloud Messaging(FCM) 등을 사용한 모바일 푸시 알림 연동
5. **이메일/SMS 알림**: 중요 알림은 이메일이나 SMS로도 발송하는 기능
6. **알림 템플릿**: 다양한 알림 유형별로 템플릿을 관리하고 적용하는 기능
7. **알림 리마인더**: 중요 알림에 대해 일정 시간 후 다시 알림을 발송하는 기능
8. **분석 및 통계**: 알림 수신, 읽음 비율 등에 대한 분석 기능