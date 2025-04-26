# 채팅 질문 시스템 (ChatQuestion System) - 개선 버전

## 개요

채팅 질문 시스템은 사용자가 질문을 입력하면 FAQ 데이터를 기반으로 AI가 답변을 제공하는 기능을 구현한 모듈입니다. 이 시스템은 OpenAI의 GPT 모델을 활용하여 질문에 대한 응답을 생성하고, 사용자별 채팅 기록을 관리하며, GPT API 사용량을 추적합니다. 로그인한 사용자만 서비스를 이용할 수 있으며, 관리자는 API 사용 통계를 조회할 수 있습니다.

이번 업데이트에서는 코드 품질 향상, 성능 최적화, 그리고 추가 기능 구현을 통해 시스템을 개선했습니다.

## 아키텍처 개선

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
├── exception/                # 도메인별 예외 처리
│   ├── ChatPermissionException
│   ├── ChatQueryException
│   ├── ChatQuestionErrorCode
│   ├── ChatQuestionException
│   ├── GPTApiException
│   ├── GPTUsageException
│   └── InvalidQuestionException
├── repository/
│   ├── ChatQuestionRepository                # 기본 채팅 데이터 접근
│   ├── ChatQuestionRepositoryCustom          # 커스텀 쿼리 인터페이스 
│   ├── ChatQuestionRepositoryImpl            # QueryDSL 구현체
│   ├── GPTUsageLogRepository                 # 기본 사용량 로그 데이터 접근
│   ├── GPTUsageLogRepositoryCustom           # 커스텀 쿼리 인터페이스
│   └── GPTUsageLogRepositoryImpl             # QueryDSL 구현체
└── service/
    ├── ChatService           # 채팅 관련 비즈니스 로직
    ├── GPTService            # GPT API 호출 처리
    └── GPTUsageService       # GPT 사용량 로깅 및 통계
```

## 주요 개선 사항

### 1. QueryDSL 도입

복잡한 쿼리를 더 유연하고 타입 안전하게 처리하기 위해 QueryDSL을 도입했습니다:

- **커스텀 리포지토리 인터페이스 분리**: 기본 JPA 리포지토리와 커스텀 쿼리 인터페이스 분리
- **타입 안전 쿼리**: 컴파일 시점에 SQL 문법 오류 감지
- **통계 쿼리 최적화**: 복잡한 그룹핑, 집계 쿼리의 성능 및 유지보수성 향상
- **동적 쿼리 생성 용이**: 다양한 검색 조건에 따른 동적 쿼리 구현 개선

### 2. 코드 구조 및 품질 개선

- **단일 책임 원칙(SRP) 적용**: 클래스와 메서드가 하나의 책임만 가지도록 리팩토링
- **메서드 분리**: 긴 메서드를 더 작고 재사용 가능한 메서드로 분리
- **일관된 예외 처리**: 도메인별 예외 클래스를 활용한 통일된 예외 처리 전략
- **보안 강화**: 컨트롤러 단에서의 추가 권한 검증 로직 구현
- **코드 가독성 향상**: 명확한 변수명과 함수명, 상세한 주석 추가

### 3. 성능 최적화

- **데이터베이스 쿼리 최적화**: QueryDSL을 활용한 효율적인 쿼리 구현
- **트랜잭션 범위 최적화**: 읽기 전용 트랜잭션 활용(`@Transactional(readOnly = true)`)
- **재시도 메커니즘**: 네트워크 오류나 일시적 서버 오류 시 자동 재시도 기능 추가
- **로깅 전략 개선**: 적절한 로그 레벨 사용으로 불필요한 로그 출력 감소

### 4. 새로운 기능 추가

- **최근 질문 조회 API**: 사용자의 최근 질문 목록을 빠르게 조회할 수 있는 API 추가
- **키워드 검색 기능**: 질문 내용을 기반으로 과거 채팅을 검색할 수 있는 기능 구현
- **사용량 통계 기간 제한**: 과도한 데이터 조회를 방지하기 위한 기간 제한 로직 추가

## API 엔드포인트 (신규 및 개선)

### 채팅 API (사용자용)

기존 API 외에 추가된 새로운 엔드포인트:

```
GET /chat/recent?limit=5
```
사용자의 최근 질문 목록을 조회합니다. 로그인한 사용자만 접근 가능합니다.

**응답 본문 예시:**
```json
{
  "success": true,
  "data": [
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
  "error": null
}
```

### GPT 사용량 API (관리자용)

모든 API에 다음과 같은 개선 사항이 적용되었습니다:
- 더 상세하고 정확한 응답 데이터
- 권한 검증 강화
- 오류 처리 개선

## 주요 기능 상세 (개선된 사항)

### 1. 질문 처리 및 응답 생성 (개선)

1. 사용자가 질문을 입력하면 `ChatController`가 요청을 받고 입력값 검증을 수행합니다.
2. `ChatService`는 사용자 정보를 확인하고 FAQ 데이터를 조회합니다.
3. `GPTService`는 질문과 FAQ 데이터를 OpenAI API로 전송하여 응답을 받습니다. 네트워크 오류 시 자동 재시도합니다.
4. 질문과 응답은 `ChatQuestion` 엔티티에 저장됩니다.
5. API 사용량은 `GPTUsageService`를 통해 `GPTUsageLog` 엔티티에 기록됩니다. 이 과정에서 발생하는 오류는 주 서비스에 영향을 주지 않습니다.
6. 응답이 사용자에게 반환됩니다.

### 2. 채팅 기록 관리 (개선)

1. 사용자는 `ChatController`를 통해 자신의 채팅 기록을 페이지네이션하여 조회할 수 있습니다.
2. 새로운 API를 통해 최근 질문 목록을 빠르게 조회할 수 있습니다.
3. 모든 조회는 QueryDSL을 통해 최적화된 쿼리로 처리됩니다.
4. 페이지 크기는 기본값이 설정되어 있지만 요청 시 변경 가능합니다. 유효한 범위 내의 값인지 검증을 수행합니다.

### 3. GPT API 사용량 추적 (개선)

1. 모든 GPT API 요청은 `GPTUsageService`를 통해 로깅됩니다. 독립적인 트랜잭션으로 처리하여 로깅 실패가 주 서비스에 영향을 주지 않습니다.
2. 로그는 사용자 정보, 요청 유형, 토큰 사용량, 모델명 등을 포함합니다.
3. 관리자는 기간별, 모델별, 요청 유형별 사용량 통계를 조회할 수 있으며, 과도한 데이터 조회를 방지하기 위한 기간 제한이 적용됩니다.
4. 예상 비용은 모델별 가격 정보를 기반으로 계산되며, 더 정확한 계산 로직이 적용되었습니다.

## 오류 처리 개선

1. 도메인별로 세분화된 예외 클래스를 통해 더 명확한 오류 처리가 가능해졌습니다:
    - `ChatPermissionException`: 권한 관련 오류
    - `ChatQueryException`: 데이터 조회 관련 오류
    - `GPTApiException`: GPT API 호출 관련 오류
    - `GPTUsageException`: 사용량 로깅 및 통계 관련 오류
    - `InvalidQuestionException`: 질문 데이터 유효성 관련 오류

2. 모든 예외는 적절한 HTTP 상태 코드와 상세한 오류 메시지를 포함합니다.

3. 로깅 전략이 개선되어 오류 발생 시 더 많은 컨텍스트 정보가 로그에 기록됩니다.

## 성능 고려사항

### 데이터베이스 최적화

1. **효율적인 인덱싱**: 자주 조회하는 필드에 인덱스를 추가하여 조회 성능 향상
2. **페이징 최적화**: 효율적인 페이징 처리를 통한 대용량 데이터 처리 성능 개선
3. **Fetch Join 활용**: N+1 문제 방지를 위한 적절한 Fetch 전략 적용

### API 호출 최적화

1. **재시도 메커니즘**: 일시적인 네트워크 오류에 대한 자동 재시도 로직 적용
2. **비동기 로깅**: 사용량 로깅을 독립적인 트랜잭션으로 처리하여 응답 시간 개선
3. **타임아웃 설정**: API 호출 시 적절한 타임아웃 설정을 통한 서비스 안정성 향상

## 보안 강화

1. **입력값 검증 강화**: 모든 사용자 입력에 대한 철저한 유효성 검사 추가
2. **권한 검증 다중화**: 스프링 시큐리티와 서비스 레이어에서의 중복 검증을 통한 보안 강화
3. **민감 정보 보호**: 로그에 민감한 정보가 노출되지 않도록 로깅 전략 개선

## 구현 참고사항

### 개선된 GPT 프롬프트

GPT에 전달되는 프롬프트가 개선되어 더 효과적인 응답을 생성할 수 있게 되었습니다:

- **역할 정의 강화**: "온라인 쇼핑몰 상담 도우미"로서의 역할을 더 명확히 정의
- **유연한 응답 전략**: FAQ에 없는 질문에도 일반적인 지식을 활용하여 도움이 될 만한 정보 제공
- **답변 형식 개선**: 확실한 정보와 불확실한 정보를 구분하여 제공하는 형식 도입

### QueryDSL 활용 예시

실제 프로젝트에서 QueryDSL을 활용한 코드 예시:

```java
// 일별 토큰 사용량 집계 쿼리
@Override
public List<GPTDailyUsageDto> getDailyTokenUsage(LocalDateTime startDate, LocalDateTime endDate) {
    String dateFunction = "DATE_FORMAT({0}, '%Y-%m-%d')";
    
    return queryFactory
            .select(Projections.constructor(GPTDailyUsageDto.class,
                    Expressions.stringTemplate(dateFunction, gptUsageLog.createdDate),
                    gptUsageLog.totalTokens.sum()))
            .from(gptUsageLog)
            .where(gptUsageLog.createdDate.between(startDate, endDate))
            .groupBy(Expressions.stringTemplate(dateFunction, gptUsageLog.createdDate))
            .orderBy(Expressions.stringTemplate(dateFunction, gptUsageLog.createdDate).asc())
            .fetch();
}
```

### 재시도 메커니즘 구현

GPT API 호출 시 네트워크 오류나 서버 오류에 대응하기 위한 재시도 기능:

```java
@Retryable(
    value = {ResourceAccessException.class, HttpServerErrorException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public GPTResponseDto getGPTResponseWithUsage(String question, List<FAQResponseDto> faqList) {
    // API 호출 로직
}
```

## 향후 개선 계획

1. **AI 응답 캐싱**: 자주 묻는 질문에 대한 응답을 캐싱하여 API 호출 비용 절감
2. **사용자별 토큰 사용량 제한**: 사용자별 일일/월간 토큰 사용량 제한 설정
3. **대화 맥락 유지**: 이전 질문과 연결된 대화 맥락을 유지하는 기능
4. **답변 품질 평가 시스템**: 사용자가 AI 답변의 품질을 평가할 수 있는 피드백 메커니즘
5. **다국어 지원**: 다양한 언어로 질문을 처리하고 응답할 수 있는 기능

## 결론

이번 업데이트를 통해 ChatQuestion 시스템은 코드 품질, 성능, 보안 측면에서 크게 개선되었습니다. QueryDSL 도입으로 데이터베이스 쿼리가 최적화되었고, 코드 구조 개선을 통해 유지보수성이 향상되었습니다. 예외 처리 및 로깅 전략 개선으로 시스템의 안정성과 디버깅 용이성도 증가했습니다.

새롭게 추가된 기능들은 사용자 경험을 향상시키고, 관리자에게 더 정확한 사용량 분석 도구를 제공합니다. 지속적인 개선을 통해 더 효율적이고 안정적인 채팅 시스템으로 발전시켜 나갈 계획입니다.