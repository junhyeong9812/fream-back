# 포인트 시스템 (Point System)

## 개요

포인트 시스템은 사용자의 포인트 적립, 사용, 조회 및 관리를 위한 종합적인 모듈입니다. 이 시스템을 통해 사용자는 다양한 활동에 대한 보상으로 포인트를 적립하고, 적립된 포인트를 서비스 내에서 사용할 수 있습니다. 포인트는 유효기간이 있으며, 만료 임박한 포인트부터 우선적으로 사용되는 선입선출(FIFO) 방식으로 관리됩니다. 사용자는 포인트의 적립/사용 내역, 현재 사용 가능한 포인트, 만료 예정 포인트 등을 확인할 수 있습니다.

## 아키텍처

이 시스템은 CQRS(Command Query Responsibility Segregation) 패턴을 따르며 다음과 같이 구성되어 있습니다:

```
com.fream.back.domain.user/
├── controller/
│   └── point/
│       ├── PointCommandController.java  # 포인트 적립/사용 API 엔드포인트
│       └── PointQueryController.java    # 포인트 조회 API 엔드포인트
├── dto/
│   └── PointDto.java                    # 포인트 관련 DTO 클래스들
├── entity/
│   ├── Point.java                       # 포인트 엔티티
│   └── PointStatus.java                 # 포인트 상태 열거형
├── repository/
│   └── PointRepository.java             # 포인트 데이터 접근 계층
└── service/
    └── point/
        ├── PointCommandService.java     # 포인트 적립/사용 서비스
        └── PointQueryService.java       # 포인트 조회 서비스
```

## 주요 구성 요소

### 포인트 관리

1. **PointCommandController**: 포인트 적립 및 사용을 처리하는 API 컨트롤러
2. **PointQueryController**: 포인트 내역 조회를 처리하는 API 컨트롤러
3. **PointCommandService**: 포인트 적립, 사용, 만료 처리 비즈니스 로직을 담당하는 서비스
4. **PointQueryService**: 포인트 조회 관련 비즈니스 로직을 담당하는 서비스
5. **PointRepository**: 포인트 데이터를 데이터베이스에서 접근하는 레포지토리

### 데이터 객체

1. **PointDto**: 포인트 관련 DTO 클래스들의 집합
    - **AddPointRequest**: 포인트 적립 요청 DTO
    - **UsePointRequest**: 포인트 사용 요청 DTO
    - **PointResponse**: 포인트 내역 응답 DTO
    - **PointSummaryResponse**: 포인트 종합 정보 응답 DTO
    - **UsePointResponse**: 포인트 사용 결과 응답 DTO
2. **Point**: 포인트 정보를 저장하는 엔티티
3. **PointStatus**: 포인트 상태를 정의하는 열거형 (AVAILABLE, USED, EXPIRED)

## 데이터 모델

### 포인트 (Point) 엔티티

포인트 엔티티는 다음과 같은 필드를 포함합니다:

- **id**: 포인트 ID (PK)
- **user**: 포인트 소유 사용자 (User 엔티티 참조)
- **amount**: 초기 적립된 포인트 금액
- **remainingAmount**: 현재 남아있는 포인트 금액
- **reason**: 포인트 적립/사용 이유
- **expirationDate**: 포인트 유효기간
- **status**: 포인트 상태 (AVAILABLE, USED, EXPIRED)
- **createdDate**: 포인트가 생성된 시간 (BaseTimeEntity에서 상속)
- **modifiedDate**: 포인트가 수정된 시간 (BaseTimeEntity에서 상속)

## API 엔드포인트

### 포인트 명령 API

```
POST /points/commands
```
포인트를 적립합니다.

**요청 본문 예시:**
```json
{
  "amount": 1000,
  "reason": "회원가입 보너스"
}
```

**응답 예시:**
```json
{
  "id": 1,
  "amount": 1000,
  "remainingAmount": 1000,
  "reason": "회원가입 보너스",
  "expirationDate": "2023-12-01",
  "status": "AVAILABLE",
  "createdDate": "2023-09-01T10:30:00"
}
```

```
POST /points/commands/use
```
포인트를 사용합니다.

**요청 본문 예시:**
```json
{
  "amount": 500,
  "reason": "상품 구매"
}
```

**응답 예시:**
```json
{
  "usedPoints": 500,
  "remainingTotalPoints": 500,
  "message": "포인트가 성공적으로 사용되었습니다."
}
```

### 포인트 조회 API

```
GET /points/queries
```
사용자의 모든 포인트 내역을 조회합니다.

**응답 예시:**
```json
[
  {
    "id": 1,
    "amount": 1000,
    "remainingAmount": 500,
    "reason": "회원가입 보너스",
    "expirationDate": "2023-12-01",
    "status": "AVAILABLE",
    "createdDate": "2023-09-01T10:30:00"
  },
  {
    "id": 2,
    "amount": 500,
    "remainingAmount": 0,
    "reason": "상품 구매",
    "expirationDate": "2023-12-15",
    "status": "USED",
    "createdDate": "2023-09-05T15:45:00"
  }
]
```

```
GET /points/queries/available
```
사용자의 사용 가능한 포인트만 조회합니다.

**응답 예시:**
```json
[
  {
    "id": 1,
    "amount": 1000,
    "remainingAmount": 500,
    "reason": "회원가입 보너스",
    "expirationDate": "2023-12-01",
    "status": "AVAILABLE",
    "createdDate": "2023-09-01T10:30:00"
  }
]
```

```
GET /points/queries/summary
```
사용자의 포인트 종합 정보를 조회합니다.

**응답 예시:**
```json
{
  "totalAvailablePoints": 500,
  "pointDetails": [
    {
      "id": 1,
      "amount": 1000,
      "remainingAmount": 500,
      "reason": "회원가입 보너스",
      "expirationDate": "2023-12-01",
      "status": "AVAILABLE",
      "createdDate": "2023-09-01T10:30:00"
    },
    {
      "id": 2,
      "amount": 500,
      "remainingAmount": 0,
      "reason": "상품 구매",
      "expirationDate": "2023-12-15",
      "status": "USED",
      "createdDate": "2023-09-05T15:45:00"
    }
  ],
  "expiringPoints": [
    {
      "id": 1,
      "amount": 1000,
      "remainingAmount": 500,
      "reason": "회원가입 보너스",
      "expirationDate": "2023-12-01",
      "status": "AVAILABLE",
      "createdDate": "2023-09-01T10:30:00"
    }
  ]
}
```

```
GET /points/queries/{pointId}
```
특정 포인트의 상세 정보를 조회합니다.

**응답 예시:**
```json
{
  "id": 1,
  "amount": 1000,
  "remainingAmount": 500,
  "reason": "회원가입 보너스",
  "expirationDate": "2023-12-01",
  "status": "AVAILABLE",
  "createdDate": "2023-09-01T10:30:00"
}
```

## 인증 및 권한

- 모든 API 요청은 사용자 인증이 필요합니다.
- 인증은 SecurityUtils에서 현재 인증된 사용자의 이메일을 추출하여 처리합니다.
- 각 사용자는 자신의 포인트만 접근할 수 있습니다.

## 비즈니스 로직

### 포인트 적립

1. 인증된 사용자의 이메일로 사용자 정보를 조회합니다.
2. 요청된 금액과 이유로 새로운 Point 엔티티를 생성합니다.
    - 기본 유효기간은 90일로 설정됩니다.
    - 초기 상태는 'AVAILABLE'로 설정됩니다.
3. 생성된 포인트를 데이터베이스에 저장합니다.
4. 저장된 포인트 정보를 DTO로 변환하여 반환합니다.

### 포인트 사용

1. 인증된 사용자의 이메일로 사용자 정보를 조회합니다.
2. 사용 가능한 총 포인트를 조회하여 요청 금액과 비교합니다.
    - 사용 가능한 포인트가 부족하면 예외를 발생시킵니다.
3. 만료일이 가장 빠른 포인트부터 순차적으로 사용합니다.
    - 각 포인트 내역의 remainingAmount를 감소시킵니다.
    - 포인트가 모두 사용되면 상태를 'USED'로 변경합니다.
4. 사용된 포인트의 이유를 업데이트합니다.
5. 사용 결과와 남은 총 포인트를 DTO로 변환하여 반환합니다.

### 포인트 만료 처리

1. 현재 날짜를 기준으로 유효기간이 지난 'AVAILABLE' 상태의 포인트를 조회합니다.
2. 조회된 포인트들의 상태를 'EXPIRED'로 변경하고 remainingAmount를 0으로 설정합니다.
3. 만료 처리된 포인트 수를 반환합니다.

### 포인트 조회

1. 인증된 사용자의 이메일로 사용자 정보를 조회합니다.
2. 요청에 따라 다음 정보를 조회합니다:
    - 모든 포인트 내역: 최근 적립 순으로 정렬
    - 사용 가능한 포인트: 유효기간 임박 순으로 정렬
    - 포인트 종합 정보: 총 사용 가능 포인트, 모든 내역, 30일 이내 만료 예정 포인트
    - 특정 포인트 상세 정보: 포인트 ID로 조회
3. 조회된 정보를 DTO로 변환하여 반환합니다.

## 구현 참고사항

### 개발자를 위한 안내

1. **포인트 유효기간 설정**:
    - 현재 포인트의 기본 유효기간은 90일로 설정되어 있습니다.
    - 필요에 따라 PointCommandService의 addPoint 메서드에서 유효기간을 조정할 수 있습니다.

2. **포인트 만료 처리 자동화**:
    - 포인트 만료 처리를 위해 스케줄러를 구현하는 것이 좋습니다.
    - 예: Spring의 @Scheduled 어노테이션을 사용하여 매일 자정에 expirePoints 메서드를 호출

3. **포인트 적립/사용 이벤트 알림**:
    - 포인트 적립 또는 사용 시 사용자에게 알림을 보내는 기능을 추가할 수 있습니다.
    - 알림 시스템과 연동하여 구현하는 것이 좋습니다.

4. **포인트 내역 페이지네이션**:
    - 포인트 내역이 많은 경우 페이지네이션을 구현하여 성능을 개선할 수 있습니다.

### 보안 고려사항

1. **권한 검증**:
    - 포인트 조회 및 사용 시 사용자 소유권 검증을 철저히 해야 합니다.
    - 현재 getPointDetail 메서드에서는 포인트 소유자 확인 로직이 구현되어 있습니다.

2. **포인트 사용 검증**:
    - 포인트 사용 시 중복 사용 방지 및 트랜잭션 처리에 주의해야 합니다.
    - 동시에 여러 요청이 들어올 경우 데이터 일관성을 유지해야 합니다.

## 비즈니스 활용 방안

포인트 시스템은 다음과 같은 비즈니스 기능에 활용될 수 있습니다:

1. **회원 가입 보너스**: 신규 회원에게 포인트를 지급하여 사용자 유입을 촉진합니다.
2. **활동 보상**: 상품 리뷰 작성, 설문 응답, 출석 체크 등 사용자 활동에 대한 보상으로 포인트를 지급합니다.
3. **구매 할인**: 상품 구매 시 포인트를 사용하여 할인 혜택을 제공합니다.
4. **마케팅 캠페인**: 특정 기간 동안 포인트를 추가로 지급하는 프로모션을 진행합니다.
5. **충성도 프로그램**: VIP 등급에 따라 포인트 적립률을 차등 적용하여 충성 고객을 유지합니다.

## 문제 해결

1. **포인트 적립 실패**:
    - 사용자 정보가 올바른지 확인하세요.
    - 적립 금액이 양수인지 확인하세요.

2. **포인트 사용 실패**:
    - 사용 가능한 포인트가 충분한지 확인하세요.
    - 사용 금액이 양수인지 확인하세요.

3. **포인트 조회 오류**:
    - 인증된 사용자인지 확인하세요.
    - 특정 포인트 조회 시 해당 ID가 존재하는지 확인하세요.

4. **포인트 만료 처리 문제**:
    - 스케줄러가 올바르게 설정되어 있는지 확인하세요.
    - expirePoints 메서드가 오류 없이 실행되는지 확인하세요.