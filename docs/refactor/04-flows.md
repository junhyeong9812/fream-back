# 04. 핵심 플로우

## 거래(입찰 매칭 → 주문/결제) — 개념
```
구매자 ── 구매입찰(OrderBid, bidPrice) ──┐
                                          ├─ 매칭 ─▶ Order 생성 + Payment + Shipment + (창고보관)
판매자 ── 판매입찰(SaleBid, bidPrice) ──┘            └─ 상태머신: PENDING_PAYMENT → PAYMENT_COMPLETED
                                                          → PREPARING → IN_WAREHOUSE → SHIPMENT_STARTED
즉시구매/즉시판매(isInstantPurchase/Sale)는 반대편 입찰에 바로 매칭.          → IN_TRANSIT → COMPLETED
```
> 현재 OrderBid·SaleBid가 Order·Sale을 서로 @OneToOne 교차참조 → order↔sale 엔티티 순환. 재설계 대상([06](06-bid-order-issue.md)).

- 결제완료(PaymentCompletedEvent, Kafka)·주문처리(OrderProcessingEvent)는 이미 비동기 일부 존재.
- 상태 전이는 `Order.updateStatus`(허용 전이만), `Payment.updateStatus`(PaymentStatus 머신).

## 문의(inquiry) — FK→ID + enrich 적용 후
```
[작성] Controller(email→userQueryService.findUserIdByEmail) → CommandService(toEntity(userId) 저장)
       → 응답: findUserSummary(userId)로 작성자 email/profile enrich
[검색] QueryService.getInquiries → Repository(QUser 조인 없이 userId만 투영)
       → enrichAuthors(page): userId 수집 → findUserSummaries(ids) 배치 1회 → 작성자 정보 채움
[권한] 비공개 문의는 작성자(inquiry.userId == 현재 userId) 또는 isAdmin(email)만
```

## 챗봇(chatQuestion)
```
ChatController(email) → ChatService.processQuestion
  → userId = findUserIdByEmail(email)
  → FAQQueryService(faq::query)로 FAQ 참고 → GPT 호출 → 답변
  → ChatQuestion 저장(userId) + GPTUsageService.logGPTUsage(userId)
사용량 로그 조회: getUsageLogs → userId 배치 enrich로 작성자 email 표시
권한: ChatSecurityAspect → userQueryService.getRoleName(email)
```

## 인증
```
LoginAuthenticationFilter → AuthService.login(dto, ip) → TokenDto(JWT)
JwtAuthenticationFilter → SecurityContext. SecurityUtils.extractEmailFromSecurityContext()로 현재 사용자 email.
IpBlockingFilter(레디스 기반 IP 차단) + TokenRefreshFilter.
```

## 알림(notification) — 현재 vs 목표
- 현재: sale·shipment·payment·notice·user가 NotificationCommandService를 **직접 주입**해 호출(결합).
- 목표(Phase 3): 발행 도메인이 도메인 이벤트 발행 → notification이 `@ApplicationModuleListener`로 수신(결합 제거).
