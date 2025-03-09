# 스타일 시스템 (Style System)

## 개요

스타일 시스템은 사용자가 구매한 제품을 기반으로 자신의 스타일을 공유하고 소통할 수 있는 소셜 기능을 제공하는 모듈입니다. 사용자는 자신의 구매 상품을 활용한 스타일 게시물을 생성하고, 다른 사용자의 스타일을 조회, 좋아요, 관심 등록, 댓글 작성 등의 상호작용을 할 수 있습니다. 이를 통해 커뮤니티 기반의 패션 경험을 제공합니다.

## 아키텍처

```
com.fream.back.domain.style/
├── controller/
│   ├── command/            # 스타일 생성, 수정, 삭제 및 상호작용(좋아요, 관심, 댓글)을 위한 컨트롤러
│   └── query/              # 스타일 조회 및 검색을 위한 컨트롤러
├── dto/                    # 데이터 전송 객체
├── entity/                 # 데이터베이스 엔티티
├── repository/             # 데이터 접근 계층
└── service/
    ├── command/            # 스타일 생성, 수정, 삭제 및 상호작용을 위한 서비스
    └── query/              # 스타일 조회 및 검색을 위한 서비스
```

## 주요 구성 요소

### 컨트롤러

1. **StyleCommandController**: 스타일 생성, 수정, 삭제 API를 제공합니다.
2. **StyleQueryController**: 스타일 조회, 검색, 미디어 파일 다운로드 API를 제공합니다.
3. **StyleLikeCommandController**: 스타일 좋아요 토글 API를 제공합니다.
4. **StyleInterestCommandController**: 스타일 관심 등록 토글 API를 제공합니다.
5. **StyleCommentCommandController**: 댓글 생성, 수정, 삭제 API를 제공합니다.
6. **StyleCommentQueryController**: 댓글 조회 API를 제공합니다.
7. **StyleCommentLikeCommandController**: 댓글 좋아요 토글 API를 제공합니다.

### 서비스

1. **StyleCommandService**: 스타일 생성, 수정, 삭제 및 뷰 카운트 증가 로직을 담당합니다.
2. **StyleQueryService**: 스타일 조회 및 필터링된 스타일 목록 조회를 담당합니다.
3. **StyleLikeCommandService**: 스타일 좋아요 토글 로직을 담당합니다.
4. **StyleLikeQueryService**: 사용자의 스타일 좋아요 상태 조회를 담당합니다.
5. **StyleInterestCommandService**: 스타일 관심 등록 토글 로직을 담당합니다.
6. **StyleInterestQueryService**: 사용자의 스타일 관심 등록 상태 조회를 담당합니다.
7. **StyleOrderItemCommandService**: 스타일과 주문 상품 연결 로직을 담당합니다.
8. **MediaUrlCommandService**: 스타일 미디어 파일 저장 및 삭제 로직을 담당합니다.
9. **MediaUrlQueryService**: 스타일 미디어 URL 조회를 담당합니다.
10. **StyleCommentCommandService**: 댓글 생성, 수정, 삭제 로직을 담당합니다.
11. **StyleCommentQueryService**: 댓글 조회 로직을 담당합니다.
12. **StyleCommentLikeCommandService**: 댓글 좋아요 토글 로직을 담당합니다.
13. **StyleCommentLikeQueryService**: 사용자의 댓글 좋아요 상태 조회를 담당합니다.

### 엔티티

1. **Style**: 스타일 기본 정보(작성자, 콘텐츠, 조회수)를 저장합니다.
2. **MediaUrl**: 스타일에 연결된 미디어 URL 정보를 저장합니다.
3. **StyleOrderItem**: 스타일과 주문 상품 간의 관계를 저장합니다.
4. **StyleLike**: 스타일 좋아요 정보를 저장합니다.
5. **StyleInterest**: 스타일 관심 등록 정보를 저장합니다.
6. **StyleComment**: 스타일 댓글 정보(대댓글 포함)를 저장합니다.
7. **StyleCommentLike**: 댓글 좋아요 정보를 저장합니다.

### 저장소

1. **StyleRepository**: 스타일 엔티티의 기본 CRUD 및 조회 기능을 제공합니다.
2. **StyleRepositoryCustom**: 스타일 필터링 및 상세 조회 기능을 위한 인터페이스입니다.
3. **StyleRepositoryCustomImpl**: QueryDSL을 사용한 스타일 필터링 및 상세 조회 구현체입니다.
4. **MediaUrlRepository**: 미디어 URL 엔티티의 CRUD 및 조회 기능을 제공합니다.
5. **StyleOrderItemRepository**: 스타일 주문 상품 연결 엔티티의 CRUD 및 조회 기능을 제공합니다.
6. **StyleLikeRepository**: 스타일 좋아요 엔티티의 CRUD 및 조회 기능을 제공합니다.
7. **StyleInterestRepository**: 스타일 관심 등록 엔티티의 CRUD 및 조회 기능을 제공합니다.
8. **StyleCommentRepository**: 스타일 댓글 엔티티의 CRUD 및 조회 기능을 제공합니다.
9. **StyleCommentLikeRepository**: 댓글 좋아요 엔티티의 CRUD 및 조회 기능을 제공합니다.

## API 엔드포인트

### 스타일 관리 API

```
POST /styles/commands
```
새로운 스타일을 생성합니다. 주문 상품, 텍스트 콘텐츠, 미디어 파일을 첨부할 수 있습니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "orderItemIds": [1, 2],
  "content": "새로 구매한 신발과 함께한 데일리룩",
  "mediaFiles": [이미지 파일]
}
```

```
PUT /styles/commands/{styleId}
```
기존 스타일을 수정합니다. 텍스트 콘텐츠, 미디어 파일을 변경할 수 있습니다.

**요청 예시:**
```
Content-Type: multipart/form-data

{
  "content": "새로 구매한 신발과 함께한 데일리룩(수정)",
  "existingUrlsFromFrontend": ["/styles/queries/1/media/image1.jpg"],
  "newMediaFiles": [새 이미지 파일]
}
```

```
DELETE /styles/commands/{styleId}
```
스타일을 삭제합니다. 연결된 미디어 파일, 좋아요, 관심 등록, 댓글도 함께 삭제됩니다.

```
POST /styles/commands/{styleId}/view
```
스타일 조회수를 증가시킵니다.

### 스타일 조회 API

```
GET /styles/queries/{styleId}
```
단일 스타일 상세 정보를 조회합니다. 로그인한 사용자의 경우 좋아요 및 관심 등록 상태도 함께 반환합니다.

**응답 예시:**
```json
{
  "id": 1,
  "profileId": 1,
  "profileName": "user1",
  "profileImageUrl": "/profiles/1/image.jpg",
  "content": "새로 구매한 신발과 함께한 데일리룩",
  "mediaUrls": ["/styles/queries/1/media/image1.jpg"],
  "likeCount": 42,
  "commentCount": 5,
  "liked": true,
  "interested": false,
  "productInfos": [
    {
      "productId": 1,
      "productName": "클래식 스니커즈",
      "productEnglishName": "Classic Sneakers",
      "colorName": "블랙",
      "thumbnailImageUrl": "/products/1/thumbnail.jpg",
      "minSalePrice": 89000
    }
  ],
  "createdDate": "2023-08-15T14:30:00"
}
```

```
GET /styles/queries?brandName=Nike&categoryId=2&sortBy=popular&page=0&size=10
```
필터링된 스타일 목록을 조회합니다. 브랜드, 컬렉션, 카테고리, 프로필명으로 필터링하고 인기순 또는 최신순으로 정렬할 수 있습니다.

**응답 예시:**
```json
{
  "content": [
    {
      "id": 1,
      "profileId": 1,
      "profileName": "user1",
      "profileImageUrl": "/profiles/1/image.jpg",
      "content": "새로 구매한 신발과 함께한 데일리룩",
      "mediaUrl": "/styles/queries/1/media/image1.jpg",
      "viewCount": 150,
      "likeCount": 42,
      "liked": true
    },
    // ... 추가 스타일 항목
  ],
  "pageable": { ... },
  "totalPages": 5,
  "totalElements": 48,
  "last": false,
  "size": 10,
  "number": 0,
  "sort": { ... },
  "numberOfElements": 10,
  "first": true,
  "empty": false
}
```

```
GET /styles/queries/profile/{profileId}?page=0&size=10
```
특정 프로필의 스타일 목록을 조회합니다.

```
GET /styles/queries/{styleId}/media/{fileName}
```
스타일 미디어 파일을 다운로드합니다.

### 스타일 상호작용 API

```
POST /styles/likes/commands/{styleId}/toggle
```
스타일 좋아요를 토글합니다(좋아요 상태이면 취소, 아니면 좋아요 추가).

```
POST /styles/interests/commands/{styleId}/toggle
```
스타일 관심 등록을 토글합니다(관심 등록 상태이면 취소, 아니면 관심 등록 추가).

### 댓글 관리 API

```
POST /styles/comments/commands
```
새 댓글 또는 대댓글을 작성합니다.

**요청 예시:**
```json
{
  "styleId": 1,
  "content": "정말 멋진 스타일이네요!",
  "parentCommentId": null  // 대댓글의 경우 부모 댓글 ID
}
```

```
PUT /styles/comments/commands/{commentId}
```
댓글을 수정합니다.

**요청 예시:**
```json
{
  "updatedContent": "정말 멋진 스타일이네요! 어디서 구매하셨나요?"
}
```

```
DELETE /styles/comments/commands/{commentId}
```
댓글을 삭제합니다. 대댓글도 함께 삭제됩니다.

```
GET /styles/comments/queries/{styleId}?page=0&size=10
```
특정 스타일의 댓글 목록을 조회합니다. 대댓글도 함께 조회됩니다.

**응답 예시:**
```json
{
  "comments": [
    {
      "id": 1,
      "profileId": 2,
      "profileName": "user2",
      "profileImageUrl": "/profiles/2/image.jpg",
      "content": "정말 멋진 스타일이네요!",
      "likeCount": 3,
      "liked": false,
      "createdDate": "2023-08-15T15:30:00",
      "replies": [
        {
          "id": 2,
          "profileId": 1,
          "profileName": "user1",
          "profileImageUrl": "/profiles/1/image.jpg",
          "content": "감사합니다!",
          "likeCount": 1,
          "liked": true,
          "createdDate": "2023-08-15T15:45:00",
          "replies": []
        }
      ]
    }
    // ... 추가 댓글 항목
  ],
  "totalComments": 15,
  "userProfileImageUrl": "/profiles/3/image.jpg"
}
```

```
POST /styles/comments/likes/commands/{commentId}/toggle
```
댓글 좋아요를 토글합니다(좋아요 상태이면 취소, 아니면 좋아요 추가).

## 데이터베이스 스키마

### 1. Style 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| profile_id     | BIGINT         | 프로필 외래 키               |
| content        | TEXT           | 텍스트 콘텐츠                |
| view_count     | BIGINT         | 조회수                      |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 2. MediaUrl 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| style_id       | BIGINT         | 스타일 외래 키               |
| url            | VARCHAR        | 미디어 URL                  |

### 3. StyleOrderItem 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| style_id       | BIGINT         | 스타일 외래 키               |
| order_item_id  | BIGINT         | 주문 상품 외래 키            |

### 4. StyleLike 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| style_id       | BIGINT         | 스타일 외래 키               |
| profile_id     | BIGINT         | 프로필 외래 키               |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 5. StyleInterest 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| style_id       | BIGINT         | 스타일 외래 키               |
| profile_id     | BIGINT         | 프로필 외래 키               |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 6. StyleComment 테이블

| 필드               | 타입           | 설명                        |
|-------------------|----------------|---------------------------|
| id                | BIGINT         | 기본 키                     |
| style_id          | BIGINT         | 스타일 외래 키               |
| profile_id        | BIGINT         | 프로필 외래 키               |
| content           | TEXT           | 댓글 내용                   |
| parent_comment_id | BIGINT         | 부모 댓글 외래 키(자기참조)     |
| created_date      | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date     | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

### 7. StyleCommentLike 테이블

| 필드            | 타입           | 설명                        |
|----------------|----------------|---------------------------|
| id             | BIGINT         | 기본 키                     |
| comment_id     | BIGINT         | 댓글 외래 키                 |
| profile_id     | BIGINT         | 프로필 외래 키               |
| created_date   | TIMESTAMP      | 생성 시간(BaseTimeEntity)    |
| modified_date  | TIMESTAMP      | 수정 시간(BaseTimeEntity)    |

## 파일 저장 구조

### 경로 구조

- 기본 경로: `/home/ubuntu/fream`
- 스타일별 하위 폴더: `styles/{styleId}/`
- 파일 이름: `media_` 접두사와 UUID 기반 생성

### 예시

- 물리적 파일 경로: `/home/ubuntu/fream/styles/1/media_550e8400-e29b-41d4-a716-446655440000.jpg`
- 데이터베이스 저장 URL: `/styles/queries/1/media/media_550e8400-e29b-41d4-a716-446655440000.jpg`
- 웹 접근 URL: `https://www.domain.com/api/styles/queries/1/media/media_550e8400-e29b-41d4-a716-446655440000.jpg`

## 주요 기능 상세

### 1. 스타일 생성 및 미디어 관리

스타일 시스템은 텍스트 콘텐츠와 함께 이미지 또는 동영상과 같은 미디어 파일을 첨부할 수 있으며, 다음과 같은 미디어 처리 로직을 제공합니다:

1. **미디어 파일 저장**:
    - 새 스타일 생성 또는 업데이트 시 미디어 파일을 서버에 저장합니다.
    - 미디어 파일은 `styles/{styleId}/` 하위 폴더에 UUID 기반 파일명으로 저장됩니다.

2. **미디어 URL 관리**:
    - 저장된 미디어 파일의 URL을 DB에 저장하고 클라이언트에서 접근 가능하도록 제공합니다.
    - URL 형식: `/styles/queries/{styleId}/media/{fileName}`

3. **미디어 제거 처리**:
    - 스타일 수정 시 제거된 미디어 파일은 자동으로 삭제됩니다.
    - 스타일 삭제 시 연결된 모든 미디어 파일을 삭제합니다.

### 2. 상품 정보 연동

스타일은 사용자가 구매한 상품과 연결될 수 있으며, 다음과 같은 기능을 제공합니다:

1. **주문 상품 연결**:
    - 스타일 생성 시 사용자가 구매한 상품 ID 목록을 통해 스타일과 상품을 연결합니다.
    - `StyleOrderItem` 엔티티를 통해 스타일과 주문 상품 간의 다대다 관계를 관리합니다.

2. **상품 정보 표시**:
    - 스타일 상세 조회 시 연결된 상품의 이름, 브랜드, 가격 등의 정보를 함께 제공합니다.
    - 사용자는 스타일에서 상품 정보를 확인하고 해당 상품 페이지로 이동할 수 있습니다.

### 3. 스타일 필터링 및 정렬

다양한 조건으로 스타일을 필터링하고 정렬할 수 있으며, 다음과 같은 기능을 제공합니다:

1. **필터링 조건**:
    - 브랜드명(`brandName`): 특정 브랜드의 상품이 포함된 스타일
    - 컬렉션명(`collectionName`): 특정 컬렉션의 상품이 포함된 스타일
    - 카테고리ID(`categoryId`): 특정 카테고리의 상품이 포함된 스타일
    - 프로필명(`profileName`): 특정 프로필이 작성한 스타일

2. **정렬 방식**:
    - 인기순(`popular`): 조회수가 많은 순서로 정렬
    - 최신순(기본값): 최근에 작성된 순서로 정렬

3. **성능 최적화**:
    - 복잡한 조인과 필터링 연산을 최적화하기 위해 두 단계 쿼리 접근 방식을 사용합니다:
        1. 필터링 조건에 맞는 스타일 ID를 먼저 찾습니다.
        2. 찾은 ID로 실제 필요한 데이터를 조회합니다.

### 4. 사용자 상호작용 기능

사용자 간의 소통과 상호작용을 위한 다양한 기능을 제공합니다:

1. **좋아요 기능**:
    - 사용자는 스타일에 좋아요를 표시하거나 취소할 수 있습니다.
    - 각 스타일의 좋아요 수를 집계하여 인기도를 표시합니다.

2. **관심 등록 기능**:
    - 사용자는 마음에 드는 스타일을 관심 목록에 추가하거나 제거할 수 있습니다.
    - 추후 사용자의 관심 스타일 목록을 조회하는 기능으로 확장 가능합니다.

3. **댓글 시스템**:
    - 사용자는 스타일에 댓글을 작성하고 다른 사용자의 댓글에 답글을 달 수 있습니다.
    - 댓글에 좋아요를 표시하여 유용한 댓글을 강조할 수 있습니다.
    - 계층형 구조(대댓글)를 지원하여 더 풍부한 소통이 가능합니다.

4. **조회수 추적**:
    - 각 스타일의 조회수를 추적하여 인기 있는 스타일을 파악할 수 있습니다.
    - 인기순 정렬 시 조회수를 기준으로 스타일을 정렬합니다.

## 보안

모든 스타일 생성, 수정, 삭제 API는 로그인한 사용자만 접근할 수 있습니다. 사용자 인증 및 권한 검사는 `SecurityUtils.extractEmailFromSecurityContext()` 메소드를 통해 수행됩니다.

## 캐시 관리

스타일 데이터의 성능 최적화를 위해 Nginx 캐시를 활용하며, 스타일 변경 시 캐시를 갱신합니다:

```java
@PostMapping
public ResponseEntity<Long> createStyle(/* ... */) {
    // ...
    nginxCachePurgeUtil.purgeStyleCache();
    return ResponseEntity.ok(createdStyle.getId());
}
```

## 구현 참고사항

### 1. 미디어 파일 처리 로직

스타일 미디어 파일 처리는 `MediaUrlCommandService` 클래스에서 담당합니다. 주요 메소드는 다음과 같습니다:

- **saveMediaFile()**: 미디어 파일을 저장하고 MediaUrl 엔티티를 생성합니다.
- **deleteMediaUrl()**: 미디어 파일을 삭제하고 관련 엔티티를 제거합니다.

### 2. 댓글 및 대댓글 처리

댓글과 대댓글은 `StyleComment` 엔티티의 자기 참조 관계를 통해 처리됩니다:

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_comment_id", nullable = true)
private StyleComment parentComment; // 부모 댓글

@OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
private List<StyleComment> childComments = new ArrayList<>(); // 대댓글 목록
```

### 3. 양방향 관계 관리

엔티티 간의 양방향 관계는 연관관계 메서드를 통해 일관성 있게 관리됩니다:

```java
// Style 엔티티
public void addComment(StyleComment comment) {
    this.comments.add(comment);
    comment.assignStyle(this);
}

// StyleComment 엔티티
public void assignStyle(Style style) {
    this.style = style;
}
```

### 4. QueryDSL을 활용한 복잡한 조회 처리

복잡한 필터링 및 조회 로직은 QueryDSL을 활용하여 효율적으로 구현하였습니다:

```java
@Override
public Page<StyleResponseDto> filterStyles(StyleFilterRequestDto filterRequestDto, Pageable pageable) {
    // 1단계: 필터링 조건에 맞는 스타일 ID 찾기
    List<Long> filteredStyleIds = queryFactory
            .select(style.id)
            .from(style)
            .leftJoin(/* ... */)
            .where(filterBuilder)
            .distinct()
            .fetch();

    // 2단계: 찾은 ID로 실제 필요한 데이터 조회
    var query = queryFactory.select(Projections.constructor(/* ... */))
            .from(style)
            .leftJoin(style.profile, profile)
            .where(style.id.in(filteredStyleIds))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize());
    
    // ... 정렬 및 결과 반환
}
```

## 확장 가능성

1. **해시태그 시스템**: 스타일에 해시태그를 추가하여 더 효율적인 분류와 검색이 가능하도록 할 수 있습니다.
2. **스타일 추천**: 사용자의 관심사, 구매 이력, 좋아요 패턴 등을 분석하여 개인화된 스타일 추천 기능을 추가할 수 있습니다.
3. **트렌드 분석**: 인기 있는 스타일과 상품을 분석하여 트렌드 보고서를 생성하는 기능을 추가할 수 있습니다.
4. **소셜 공유**: 외부 소셜 미디어 플랫폼(Instagram, Pinterest 등)과 연동하여 스타일을 공유하는 기능을 추가할 수 있습니다.
5. **AI 기반 스타일 분석**: 이미지 인식 기술을 활용하여 스타일 이미지에서 상품을 자동으로 인식하고 태그하는 기능을 추가할 수 있습니다.