package com.fream.back.domain.characterization;

import com.fream.back.domain.inquiry.dto.InquirySearchCondition;
import com.fream.back.domain.inquiry.dto.InquirySearchResultDto;
import com.fream.back.domain.inquiry.entity.Inquiry;
import com.fream.back.domain.inquiry.entity.InquiryCategory;
import com.fream.back.domain.inquiry.entity.InquiryStatus;
import com.fream.back.domain.inquiry.repository.InquiryImageRepository;
import com.fream.back.domain.inquiry.repository.InquiryRepository;
import com.fream.back.domain.inquiry.service.query.InquiryQueryServiceImpl;
import com.fream.back.domain.user.entity.Profile;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.domain.user.service.query.UserQueryService;
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
 * 특성화 테스트 — inquiry 검색 결과의 작성자 정보 enrich.
 *
 * <p>FK→ID 전환 후: 리포지토리는 userId만 조회하고, 서비스가 user 모듈 요약 API로 작성자
 * 상세(email·profileName·name)를 enrich한다. 전환 전(조인 투영)과 동일한 관찰 가능 동작
 * (검색 결과에 작성자 정보가 채워짐)을 보존하는지 검증한다.
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

    @Autowired
    private InquiryImageRepository inquiryImageRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void searchInquiries_throughService_enrichesAuthorUserAndProfileInfo() {
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
                .userId(author.getId())
                .isPrivate(false)
                .build();
        em.persist(inquiry);

        em.flush();
        em.clear();

        InquiryQueryServiceImpl service = new InquiryQueryServiceImpl(
                inquiryRepository, inquiryImageRepository, new UserQueryService(userRepository));

        Page<InquirySearchResultDto> page = service.getInquiries(
                InquirySearchCondition.forAdmin(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        InquirySearchResultDto dto = page.getContent().get(0);
        // 문의 정보
        assertThat(dto.getTitle()).isEqualTo("배송 문의");
        assertThat(dto.getCategory()).isEqualTo(InquiryCategory.DELIVERY);
        // 서비스 enrich로 채워지는 작성자 정보 (조인 투영과 동일 결과 보존)
        assertThat(dto.getUserId()).isEqualTo(author.getId());
        assertThat(dto.getEmail()).isEqualTo("inquirer@test.com");
        assertThat(dto.getProfileName()).isEqualTo("inquirer-profile");
        assertThat(dto.getName()).isEqualTo("문의자");
    }
}
