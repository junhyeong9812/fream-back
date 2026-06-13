/**
 * user 모듈의 공개 읽기 API.
 *
 * <p>{@code UserQueryService} 등 타 모듈이 사용하는 조회 서비스를 named interface로 노출한다.
 * 타 모듈은 user의 entity/repository(내부 타입)가 아니라 이 조회 API를 통해 사용자 정보를 얻어야 한다.
 */
@NamedInterface("query")
package com.fream.back.domain.user.service.query;

import org.springframework.modulith.NamedInterface;
