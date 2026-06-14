package com.fream.back.domain.characterization;

import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.service.query.UserQueryService;
import com.fream.back.domain.user.service.query.UserSummary;
import com.fream.back.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * user 모듈 배치 조회 API({@link UserQueryService#findUserSummaries}) 검증.
 * 타 모듈이 user 엔티티 대신 사용할 요약 정보(id·email·profileName·name)를 fetch join으로 제공한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class UserSummaryQueryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findUserSummaries_returnsSummariesWithProfileInfo() {
        UserQueryService userQueryService = new UserQueryService(userRepository);

        User withProfile = User.builder()
                .email("a@test.com").password("pw").referralCode("REF-A").phoneNumber("010-1").build();
        em.persist(withProfile);
        em.persist(Profile.builder().user(withProfile).profileName("a-profile").Name("에이").build());

        User noProfile = User.builder()
                .email("b@test.com").password("pw").referralCode("REF-B").phoneNumber("010-2").build();
        em.persist(noProfile);

        em.flush();
        em.clear();

        Map<Long, UserSummary> result = userQueryService.findUserSummaries(
                List.of(withProfile.getId(), noProfile.getId()));

        assertThat(result).hasSize(2);
        UserSummary a = result.get(withProfile.getId());
        assertThat(a.email()).isEqualTo("a@test.com");
        assertThat(a.profileName()).isEqualTo("a-profile");
        assertThat(a.name()).isEqualTo("에이");
        // 프로필 없는 사용자는 profile 필드가 null
        UserSummary b = result.get(noProfile.getId());
        assertThat(b.email()).isEqualTo("b@test.com");
        assertThat(b.profileName()).isNull();
    }

    @Test
    void findUserSummaries_emptyInput_returnsEmptyMap() {
        UserQueryService userQueryService = new UserQueryService(userRepository);
        assertThat(userQueryService.findUserSummaries(List.of())).isEmpty();
    }
}
