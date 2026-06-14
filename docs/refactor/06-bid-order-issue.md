# 06. bid/order 모델 이슈 분석 + 재설계 권고

> 사용자 지적: "bid와 order 관계가 잘못 묶여 있다." 분석 결과 **도메인 모델링(바운디드 컨텍스트) 문제**이며, 단순 수정이 아니라 재설계 결정이 필요하다.

## 현재 모델
```
OrderBid (order 도메인)               SaleBid (sale 도메인)
- user(구매자)                         - seller(판매자)
- productSize                          - productSize
- bidPrice, status(BidStatus)          - bidPrice, status(BidStatus)
- order  @OneToOne ───┐                - sale   @OneToOne
- sale   @OneToOne ───┼──교차참조──────  - order  @OneToOne ───┐
- isInstantPurchase   │                - isInstantSale        │
                      └────────────────────────────────────────┘
```

## 무엇이 잘못 묶였나
1. **입찰(bid)과 거래(order/sale)의 개념 혼재**: 입찰은 "매칭 전 가격 제시(standing offer)", 주문/판매는 "성사된 거래(transaction)". 서로 수명주기가 다른데 한 도메인에 묶임.
2. **구매입찰이 order에, 판매입찰이 sale에 분산**: 같은 개념(bid)이 두 도메인에 흩어져 매칭 로직이 양쪽에 걸침.
3. **OrderBid ↔ Order ↔ Sale ↔ SaleBid 교차 @OneToOne**: order↔sale 도메인 간 **엔티티 순환**의 핵심 원인. 거래코어 SCC를 못 풀게 만드는 매듭.
4. **BidStatus.MATCHED를 assignOrder/assignSale가 강제 세팅** — 매칭 부수효과가 연관관계 설정 메서드에 숨어 있어 추적 어려움.

## 재설계 옵션 (사용자 도메인 결정 필요)
| 옵션 | 개요 | 장점 | 단점 |
|------|------|------|------|
| A. 전용 `bid`(또는 trade-matching) 모듈 | OrderBid+SaleBid를 하나의 Bid 개념으로 통합한 매칭 컨텍스트 신설. Bid는 order/sale을 **ID로** 참조 | 개념 명확, order↔sale 순환 근본 해소, 매칭 로직 한 곳 | 신규 모듈·이관 비용 큼 |
| B. 현 위치 유지 + 교차참조를 ID로 | OrderBid/SaleBid는 그대로 두되 `order`/`sale` @OneToOne을 `orderId`/`saleId`로 | 점진적, 순환만 우선 제거 | 개념 혼재는 잔존 |
| C. 이벤트 기반 매칭 | 입찰 매칭을 이벤트(BidMatchedEvent)로 표현, order/sale은 이벤트 소비로 생성 | 결합 최소, 사가 명확 | 설계 복잡도↑ |

## 권고
- **순환 우선 제거가 목표면 B → 장기적으로 A**가 자연스럽다. 즉 1차로 bid의 order/sale @OneToOne을 ID 참조로 끊어 거래코어 SCC를 풀고(이번 inquiry/chatQuestion에서 검증한 FK→ID 패턴 재사용), 이후 Bid를 전용 모듈로 추출.
- **선결**: 입찰→주문/판매 매칭 플로우의 특성화 테스트(현재 무테스트). 매칭은 돈이 걸린 핵심 로직이라 안전망 필수.
- **확정 필요(사용자)**: 목표 모델(A/B/C), 입찰이 order/sale을 ID 참조할지/이벤트로 연결할지, instant purchase/sale 처리.

## 영향 범위(예상 touch point)
- `OrderBid`/`SaleBid` 엔티티, `OrderBidCommandService`/`SaleBidCommandService`, `OrderCommandService`(매칭 시 Order 생성), 양쪽 repository(QueryDSL 조인), 관련 DTO.
- 거래코어(order·payment·sale·shipment·warehouseStorage) 전체 SCC 해소와 함께 진행하는 게 효율적.

> 이 작업은 **돈이 걸린 매칭 로직 + 다도메인 + 무테스트**라 stakes 높음. 안전망 → 사용자 모델 확정 → 단계 커밋 순의 집중 세션 권장.
