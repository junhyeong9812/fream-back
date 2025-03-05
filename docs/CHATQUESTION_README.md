# 채팅 질문 시스템 (ChatQuestion System)

## 개요

채팅 질문 시스템은 사용자가 질문을 입력하면 FAQ 데이터를 기반으로 AI가 답변을 제공하는 기능을 구현한 모듈입니다. 이 시스템은 OpenAI의 GPT 모델을 활용하여 질문에 대한 응답을 생성하고, 사용자별 채팅 기록을 관리하며, GPT API 사용량을 추적합니다. 로그인한 사용자만 서비스를 이용할 수 있으며, 관리자는 API 사용 통계를 조회할 수 있습니다.

## 아키텍처

```
com.fream.back.domain.chatQuestion/
├── controller/
│   ├── ChatController        # 사용자 질문 처리 및 채팅 기록 조회
│   └── GPTUsageController    # GPT 사용량 통계 조회 (관리자용)
├── dto/
│   ├── chat/                 # 채팅 관련 DTO
│   ├── gpt/                  # GPT API 관련 DTO
│   └── log/                  # 사용량 로그 관련 DTO
├── entity/
│   ├── ChatQuestion          # 채팅 질문 및 답변 정보
│   └── GPTUsageLog           # GPT API 사용량 정보
├── repository/
│   ├── ChatQuestionRepository   # 채팅 데이터 접근
│   └── GPTUsageLogRepository    # 사용량 로그 데이터 접근
└── service/
    ├── ChatService           # 채팅 관련 비즈니스 로직
    ├── GPTService            # GPT API 호출 처리
    └── GPTUsageService       # GPT 사용량 로깅 및 통계
```

## 주요 구성 요소

### 컨트롤러

1. **ChatController**: 사용자 질문 처리 및 채팅 기록 조회 API를 제공합니다.
2. **GPTUsageController**: 관리자를 위한 GPT API 사용량 통계 조회 API를 제공합니다.

### 서비스

1. **ChatService**: 채팅 질문을 처리하고 답변을 생성하며, 채팅 기록을 관리합니다.
2. **GPTService**: OpenAI GPT API와의 통신을 담당하며, FAQ 데이터를 기반으로 질문에 대한 답변을 생성합니다.
3. **GPTUsageService**: GPT API 사용량을 로깅하고 통계를 제공합니다.

### 엔티티

1. **ChatQuestion**: 사용자 질문, AI 답변, 생성 시간 등 채팅 정보를 저장합니다.
2. **GPTUsageLog**: GPT API 사용량(토큰 수), 모델 이름, 요청 유형 등을 기록합니다.

### 저장소

1. **ChatQuestionRepository**: 채팅 데이터에 대한 CRUD 및 조회 기능을 제공합니다.
2. **GPTUsageLogRepository**: API 사용량 로그에 대한 CRUD 및 통계 쿼리 기능을 제공합니다.

## API 엔드포인트

### 채팅 API (사용자용)

```
POST /chat/question
```
질문을 전송하고 AI 응답을 받습니다. 로그인한 사용자만 접근 가능합니다.

**요청 본문 예시:**
```json
{
  "question": "환불 정책이 어떻게 되나요?"
}
```

**응답 본문 예시:**
```json
{
  "success": true,
  "data": {
    "question": "환불 정책이 어떻게 되나요?",
    "answer": "구매 후 7일 이내에는 제품에 이상이 없더라도 환불이 가능합니다. 단, 제품의 포장을 개봉하였거나 사용한 경우에는 반품 및 환불이 제한될 수 있습니다. 자세한 내용은 고객센터로 문의해주세요.",
    "createdAt": "2023-06-15T14:30:45"
  },
  "error": null
}
```

```
GET /chat/history?page=0&size=2
```
사용자의 채팅 기록을 페이지네이션하여 조회합니다. 로그인한 사용자만 접근 가능합니다.

**응답 본문 예시:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 15,
        "question": "환불 정책이 어떻게 되나요?",
        "answer": "구매 후 7일 이내에는 제품에 이상이 없더라도 환불이 가능합니다...",
        "createdAt": "2023-06-15T14:30:45"
      },
      {
        "id": 12,
        "question": "배송은 얼마나 걸리나요?",
        "answer": "일반 배송의 경우 주문 후 1-3일 내에 배송이 시작되며...",
        "createdAt": "2023-06-14T11:20:30"
      }
    ],
    "pageable": { ... },
    "totalPages": 5,
    "totalElements": 10,
    "last": false,
    "size": 2,
    "number": 0,
    "sort": { ... },
    "numberOfElements": 2,
    "first": true,
    "empty": false
  },
  "error": null
}
```

```
GET /chat/history/count?size=2
```
채팅 기록의 총 페이지 수를 조회합니다. 로그인한 사용자만 접근 가능합니다.

**응답 본문 예시:**
```json
{
  "success": true,
  "data": 5,
  "error": null
}
```

### GPT 사용량 API (관리자용)

```
GET /admin/gpt/stats?startDate=2023-06-01&endDate=2023-06-15
```
특정 기간의 GPT API 사용량 통계를 조회합니다. 관리자 권한이 필요합니다.

**응답 본문 예시:**
```json
{
  "success": true,
  "data": {
    "totalTokensUsed": 45280,
    "estimatedCost": 90,
    "dailyUsage": [
      { "date": "2023-06-01", "tokenCount": 3500 },
      { "date": "2023-06-02", "tokenCount": 4200 },
      ...
    ],
    "usageByModel": {
      "gpt-3.5-turbo": 40000,
      "gpt-4": 5280
    },
    "usageByRequestType": {
      "FAQ_CHAT": 45280
    }
  },
  "error": null
}
```

```
GET /admin/gpt/logs?page=0&size=20
```
GPT API 사용량 로그를 페이지네이션하여 조회합니다. 관리자 권한이 필요합니다.

**응답 본문 예시:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 156,
        "userName": "user1@example.com",
        "requestType": "FAQ_CHAT",
        "promptTokens": 350,
        "completionTokens": 120,
        "totalTokens": 470,
        "modelName": "gpt-3.5-turbo",
        "requestDate": "2023-06-15 14:30:45",
        "questionContent": "환불 정책이 어떻게 되나요?"
      },
      ...
    ],
    "pageable": { ... },
    "totalPages": 8,
    "totalElements": 156,
    "last": false,
    "size": 20,
    "number": 0,
    "sort": { ... },
    "numberOfElements": 20,
    "first": true,
    "empty": false
  },
  "error": null
}
```

```
GET /admin/gpt/total-tokens
```
총 누적 토큰 사용량을 조회합니다. 관리자 권한이 필요합니다.

**응답 본문 예시:**
```json
{
  "success": true,
  "data": 245680,
  "error": null
}
```

## 데이터베이스 스키마

### chat_question 테이블

| 필드         | 타입          | 설명                          |
|-------------|--------------|-------------------------------|
| id          | BIGINT       | 기본 키                       |
| question    | VARCHAR      | 사용자 질문                    |
| answer      | TEXT         | AI 응답                       |
| user_id     | BIGINT       | 사용자 외래 키                 |
| is_answered | BOOLEAN      | 답변 완료 여부                 |
| created_at  | TIMESTAMP    | 생성 시간                     |
| client_ip   | VARCHAR      | 클라이언트 IP (비회원용, 미사용) |

### gpt_usage_log 테이블

| 필드              | 타입          | 설명                       |
|------------------|--------------|----------------------------|
| id               | BIGINT       | 기본 키                     |
| user_id          | BIGINT       | 사용자 외래 키 (NULL 가능)   |
| request_type     | VARCHAR      | 요청 유형                   |
| prompt_tokens    | INT          | 입력 토큰 수                |
| completion_tokens| INT          | 출력 토큰 수                |
| total_tokens     | INT          | 총 토큰 수                  |
| model_name       | VARCHAR      | 사용된 모델명               |
| request_id       | VARCHAR      | GPT API 요청 ID             |
| chat_question_id | BIGINT       | 채팅 질문 외래 키 (NULL 가능)|
| created_date     | TIMESTAMP    | 생성 시간 (BaseTimeEntity)   |
| modified_date    | TIMESTAMP    | 수정 시간 (BaseTimeEntity)   |

## 주요 기능 상세

### 1. 질문 처리 및 응답 생성

1. 사용자가 질문을 입력하면 `ChatController`가 요청을 받습니다.
2. `ChatService`는 사용자 정보를 확인하고 FAQ 데이터를 조회합니다.
3. `GPTService`는 질문과 FAQ 데이터를 OpenAI API로 전송하여 응답을 받습니다.
4. 질문과 응답은 `ChatQuestion` 엔티티에 저장됩니다.
5. API 사용량은 `GPTUsageService`를 통해 `GPTUsageLog` 엔티티에 기록됩니다.
6. 응답이 사용자에게 반환됩니다.

### 2. 채팅 기록 관리

1. 사용자는 `ChatController`를 통해 자신의 채팅 기록을 페이지네이션하여 조회할 수 있습니다.
2. 각 채팅은 질문, 답변, 생성 시간을 포함합니다.
3. 페이지 크기는 기본값이 설정되어 있지만 요청 시 변경 가능합니다.
4. 채팅 기록은 최신순(생성 시간 내림차순)으로 정렬됩니다.

### 3. GPT API 사용량 추적

1. 모든 GPT API 요청은 `GPTUsageService`를 통해 로깅됩니다.
2. 로그는 사용자 정보, 요청 유형, 토큰 사용량, 모델명 등을 포함합니다.
3. 관리자는 기간별, 모델별, 요청 유형별 사용량 통계를 조회할 수 있습니다.
4. 예상 비용은 모델별 가격 정보를 기반으로 계산됩니다.

## 보안

1. 모든 API는 인증된 사용자만 접근 가능합니다(`@PreAuthorize("isAuthenticated()")`).
2. 관리자 API는 사용자가 관리자 권한을 가진 경우에만 접근할 수 있습니다(`userQueryService.checkAdminRole(email)`).
3. 사용자는 자신의 채팅 기록만 조회할 수 있습니다.

## 오류 처리

1. GPT API 호출 중 오류가 발생하면 로그가 기록되고 사용자에게 오류 메시지가 반환됩니다.
2. 로그 기록에 실패해도 주요 서비스는 계속 작동합니다.
3. 권한이 없는 사용자가 관리자 API에 접근하면 `AccessDeniedException`이 발생합니다.

## 구현 참고사항

### GPT 프롬프트 최적화

FAQ 데이터는 다음과 같은 형식으로 GPT에 전달됩니다:

```
카테고리: [카테고리명]
질문: [질문내용]
답변: [답변내용]
```

시스템 메시지에서 역할을 "온라인 쇼핑몰 상담 도우미"로 정의하고, FAQ에 없는 질문에는 "죄송합니다만, 해당 질문에 대한 정보가 없습니다. 고객센터로 문의해주세요."라고 응답하도록 지시합니다.

### 토큰 사용량 계산

OpenAI API 응답에서 반환되는 토큰 사용량 정보를 저장합니다:
- `prompt_tokens`: 입력 토큰 수
- `completion_tokens`: 출력 토큰 수
- `total_tokens`: 총 토큰 수

모델별 가격 정보를 기반으로 예상 비용을 계산합니다:
- GPT-3.5 Turbo: 약 $0.0018 / 1K 토큰
- GPT-4: 약 $0.03 / 1K 토큰

## 확장 가능성

1. **사용자별 토큰 사용량 제한**: 사용자별로 일일/월간 토큰 사용량 제한을 설정하여 비용을 관리할 수 있습니다.
2. **다중 모델 선택**: 사용자나 관리자가 다양한 GPT 모델 중에서 선택할 수 있도록 할 수 있습니다.
3. **비회원 채팅 지원**: 현재 구현은 되어 있지만 사용되지 않는 `clientIp` 필드를 활용하여 비회원 채팅 기능을 확장할 수 있습니다.
4. **답변 평가 시스템**: 사용자가 AI 답변의 품질을 평가할 수 있는 기능을 추가할 수 있습니다.
5. **AI 응답 캐싱**: 자주 묻는 질문에 대한 응답을 캐싱하여 API 호출 비용을 절감할 수 있습니다.