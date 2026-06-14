# 03. 도메인 설명 (19개)

> 각 도메인 = Spring Modulith 모듈 1개. "결합 상태"는 verify() 기준.

## identity 그룹
### user
계정·프로필·인증 주체. 핵심 엔티티: `User`(email·password·referralCode·phoneNumber 필수, profile/points/addresses/paymentInfos 등 집계), `Profile`, `UserGrade`, `Point`, `Follow`, `BankAccount`, `OAuthConnection`. 다수 모듈이 의존하는 중심.
**공개 API(@NamedInterface "query")**: `UserQueryService`(findByEmail, getLoginInfo, **findUserSummary/findUserSummaries/findUserIdByEmail/isAdmin/getRoleName**), `UserSummary`. → 타 모듈은 user 엔티티 대신 이 API 사용.
결합: 🔴 SCC(많은 모듈이 entity 직접 참조 — 점진 제거 중).

### address
배송 주소록. `Address`(user FK). user와 양방향. 결합: 🔴 SCC.

## catalog 그룹
### product
상품/브랜드/카테고리/컬렉션/사이즈/관심(Interest) + Elasticsearch 검색. 핵심: `Product`, `ProductColor`, `ProductSize`, `Brand`, `Interest`(user FK), 조회수 ViewEvent(Kafka). 결합: 🔴 SCC(order·sale·event·style·user와 얽힘).

## trade 그룹 (거래 코어 — 🔴 SCC, 양방향 엔티티 FK 덩어리)
### order
구매 주문. `Order`(user, payment/orderShipment/warehouseStorage/orderBid 1:1, orderItems 1:N, status 상태머신), `OrderBid`(구매 입찰 — Order·Sale 교차참조 ⚠️ 06 문서), `OrderItem`. query 측은 일부 미구현이었음(Phase 0에서 죽은 import 제거).
### payment
결제. `Payment`(추상, SINGLE_TABLE — General/Account/Card), order·sale·user 참조, PaymentInfo(user). Kafka 결제완료 이벤트.
### sale
판매. `Sale`(seller, productSize, status), `SaleBid`(판매 입찰 — Sale·Order 교차참조 ⚠️). 
### shipment
배송. `OrderShipment`(order 1:1), `SellerShipment`(sale 1:1). 배송사 추적(Playwright/CJ).
### warehouseStorage
창고 보관. `WarehouseStorage`(order 1:1).

## feed 그룹
### style
SNS 스타일 피드. `Style`(profile FK), 좋아요/댓글/관심, 조회수 StyleViewEvent(Kafka). 결합: 🔴 SCC(user·product·order 의존).

## support 그룹 (CS)
### faq
자주 묻는 질문. `FAQ`, 이미지. **clean** ✅. 공개 API: `faq :: query`(FAQQueryService), `faq :: dto`(FAQResponseDto).
### inquiry
1:1 문의. `Inquiry`(**userId** — FK→ID 전환 완료), 검색은 user 요약 API enrich. **clean** ✅.
### notice
공지. notification·user 의존. 🟡(notification 이벤트화/named interface 필요).
### chatQuestion
GPT 챗봇 Q&A + 사용량 로그. `ChatQuestion`/`GPTUsageLog`(**userId** — FK→ID 전환 완료), faq·user 공개 API 사용. **clean** ✅.
### inspection
검수 기준. user 조회만 사용. **clean** ✅(user :: query).

## notification
알림 sink. `Notification`(user). 여러 도메인이 직접 주입 → **이벤트 수신 전용 전환 예정**(Phase 3). 🔴 SCC.

## platform 그룹
### accessLog
접근 로그(GeoIP, Kafka). 타 도메인 의존 없음. **clean** ✅(`{}`).
### monitoring
관리자 모니터링. 의존 없음. **clean** ✅.
### weather
날씨 데이터(스케줄러, ApplicationEvent 사용처). 의존 없음. **clean** ✅.
### event
프로모션 이벤트. product·user 의존(내부 타입 접근 잔존). 🟡.
