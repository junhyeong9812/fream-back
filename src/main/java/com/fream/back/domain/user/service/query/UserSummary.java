package com.fream.back.domain.user.service.query;

/**
 * 타 모듈에 제공하는 사용자 요약 정보(공개 읽기 API).
 *
 * <p>모듈 간 user 엔티티 직접 참조를 대체하기 위한 값 객체. user의 조회 named interface("query")에 속한다.
 */
public record UserSummary(Long id, String email, String profileName, String name) {
}
