package com.fream.back.domain.characterization;

import com.fream.back.domain.inquiry.dto.InquirySearchCondition;
import com.fream.back.domain.inquiry.dto.InquirySearchResultDto;
import com.fream.back.domain.inquiry.entity.Inquiry;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.inquiry.repository.InquiryRepository;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.entity.User;
import com.fream.back.global.config.QueryDslConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 특성화 테스트 — inquiry 검색 쿼리의 크로스 도메인(QUser/Profile) 조인 투영.
 *
 * <p>{@code InquiryRepositoryImpl.searchInquiries}는 현재 user·profile 테이블을 조인해
 * 작성자의 email·profileName·name을 {@link InquirySearchResultDto}로 투영한다.
 * Inquiry→User FK를 Long userId로 끊는 리팩토링 시 이 조인을 user 모듈 API 조회로
 * 대체해야 하므로, 그 전에 현재 투영 동작(어떤 user 필드가 어떻게 채워지는지)을 고정한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class InquirySearchCharacterizationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Test
    void searchInquiries_projectsAuthorUserAndProfileInfo() {
        User author = User.builder()
                .email("inquirer@test.com")
                .password("pw")
                .referralCode("REF-INQ")
                .phoneNumber("010-0000-0000")
                .build();
        em.persist(author);

        Profile profile = Profile.builder()
                .user(author)
                .profileName("inquirer-profile")
                .Name("문의자")
                .build();
        em.persist(profile);

        Inquiry inquiry = Inquiry.builder()
                .title("배송 문의")
                .content("배송이 언제 오나요")
                .category(InquiryCategory.DELIVERY)
                .status(InquiryStatus.REQUESTED)
                .user(author)
                .isPrivate(false)
                .build();
        em.persist(inquiry);

        em.flush();
        em.clear();

        Page<InquirySearchResultDto> page = inquiryRepository.searchInquiries(
                InquirySearchCondition.forAdmin(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        InquirySearchResultDto dto = page.getContent().get(0);
        // 문의 정보
        assertThat(dto.getTitle()).isEqualTo("배송 문의");
        assertThat(dto.getCategory()).isEqualTo(InquiryCategory.DELIVERY);
        // 크로스 도메인 조인으로 채워지는 작성자 정보 (리팩토링이 보존해야 할 핵심)
        assertThat(dto.getUserId()).isEqualTo(author.getId());
        assertThat(dto.getEmail()).isEqualTo("inquirer@test.com");
        assertThat(dto.getProfileName()).isEqualTo("inquirer-profile");
        assertThat(dto.getName()).isEqualTo("문의자");
    }
}
