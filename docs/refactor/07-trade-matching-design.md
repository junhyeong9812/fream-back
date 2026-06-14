# 07. trade(매칭) 컨텍스트 재설계 (목표 모델 + 마이그레이션)

> 사용자 도메인 결정(2026-06): **① Bid = BUY/SELL 통합 단일 개념 / ② Order·Sale은 별개 유지(직접 참조 X, 거래 ID 연결) / ③ 전용 trade(matching) 모듈 신설**.
> 이 문서는 그 결정에 따른 타깃 설계와 단계적 이행 계획. 구현은 안전망 선행 필요(매칭=돈 걸린 핵심 로직, 현재 무테스트).

## 1. 개념 정리 (Ubiquitous Language)
- **Bid(입찰)**: 매칭 전 가격 제시. `direction(BUY|SELL)`을 가진 **단일 개념**. 거래 성사 전까지 독립적으로 존재.
- **Matching(매칭)**: BUY/SELL Bid를 가격·상품사이즈로 맺거나, 즉시구매/즉시판매로 반대편 Bid에 즉시 체결.
- **Order(구매 거래)** / **Sale(판매 거래)**: 매칭으로 **성사된** 거래의 구매측/판매측. 입찰 책임 없음. 서로 직접 참조하지 않음.
- **거래 연결**: 한 번의 매칭이 (구매측)Order + (판매측)Sale을 만든다. 이들은 공통 식별자(예: `tradeId` 또는 `matchId`)로 연결되거나, 매칭 이벤트로 생성된다.

## 2. 목표 모델
```
[trade 모듈 — 신설, 매칭 컨텍스트]
  Bid
    - id, direction(BUY|SELL)
    - productSizeId   (catalog ID 참조)
    - bidderId        (identity userId)
    - price, status(PENDING|MATCHED|CANCELLED|EXPIRED), instant(boolean)
    - matchId         (매칭 성사 시 부여; Order/Sale을 ID로 잇는 키)
  MatchingService
    - 입찰 등록 / 매칭(입찰 vs 입찰, 즉시 체결)
    - 성사 시 BidMatchedEvent(matchId, buyerBidId, sellerBidId, productSizeId, price, buyerId, sellerId) 발행

[order 모듈]  Order  ← BidMatchedEvent 수신 또는 trade가 OrderApi 호출로 생성. matchId 보유. OrderBid 제거.
[sale  모듈]  Sale   ← 동일. SaleBid 제거. Sale↔Order 직접 @OneToOne 제거.
```
- **order ↔ sale 엔티티 순환 제거**: bid가 더 이상 Order·Sale을 교차 @OneToOne 하지 않음(핵심 SCC 매듭 해소).
- trade 의존: `catalog`(productSize 조회 API), `identity`(user 조회 API)만. order/sale로의 생성은 **이벤트**(trade→order/sale 동기 결합 회피).

## 3. 통신 방식
- **매칭 성사 → 거래 생성**: `BidMatchedEvent`(ApplicationEvent) → order/sale이 `@ApplicationModuleListener`로 수신해 각자 Order/Sale 생성. trade는 order/sale을 모름(단방향).
- **거래 상태 역통지**(결제완료/취소로 bid 상태 갱신 필요 시): order/sale → trade로 이벤트(OrderSettledEvent 등) 또는 trade가 조회 API. (양방향 동기 결합 금지)
- bid가 표시용 상품/사용자 정보 필요 시: catalog/identity 요약 API enrich(기 확립 패턴).

## 4. 단계적 이행 (strangler, 순차 커밋)
1. **안전망**: 현재 입찰→매칭→Order/Sale 플로우 특성화 테스트(OrderBidCommandService·SaleBidCommandService·OrderCommandService 매칭 경로). 즉시구매/즉시판매 포함.
2. **trade 모듈 + Bid 골격 신설**: `@ApplicationModule(trade)`, Bid 엔티티(통합), MatchingService(빈 골격). DB: 기존 order_bid/sale_bid → bid 테이블 설계(기존 데이터 있으면 마이그레이션 스크립트, record-level 검증).
3. **매칭 로직 이관**: OrderBid/SaleBid 매칭 로직 → trade.MatchingService. BidMatchedEvent 발행.
4. **Order/Sale 생성 이벤트화**: order/sale이 BidMatchedEvent 수신 생성. Order/Sale의 OrderBid/SaleBid @OneToOne·교차참조 제거, matchId로 대체.
5. **레거시 제거**: OrderBid/SaleBid 엔티티·서비스 제거(또는 trade로 흡수).
6. **검증**: `ModularityTests`에서 order↔sale 순환 소멸 확인 + 특성화/통합 테스트 + (가능 시) 매칭 시나리오.

## 5. 리스크 / 선결
- **돈이 걸린 매칭 로직 + 현재 무테스트** → 1단계 안전망이 절대 선행.
- **DB 마이그레이션**(order_bid/sale_bid → bid): 운영 데이터 존재 시 record-level 검증·롤백 계획 필요(stakes 높음).
- **즉시구매/즉시판매·부분매칭·동시성**(같은 입찰 중복 매칭): 동시성 방어(상태 기반/락/멱등) 설계 필요.
- 거래코어 SCC(payment·shipment·warehouseStorage) 해소와 맞물림 — bid 분리가 order↔sale을 풀면 나머지 SCC도 순차 해소 용이.

## 6. 다음 확정 필요(구현 진입 전)
- 매칭 단위 식별자 이름/형태(`matchId` UUID vs 시퀀스), Order/Sale이 이를 보유하는 방식.
- 운영 DB에 기존 입찰 데이터 유무(마이그레이션 범위 결정).
- 부분 매칭/수량 개념 여부, 즉시 체결의 정확한 규칙.
