package com.fream.back.domain.characterization;

import com.fream.back.domain.chatQuestion.entity.ChatQuestion;
import com.fream.back.domain.chatQuestion.repository.ChatQuestionRepository;
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
 * 특성화 테스트 — chatQuestion이 작성자를 userId(FK→ID 전환)로 참조한 뒤에도
 * 사용자별 질문 조회가 동작하는지 고정.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(QueryDslConfig.class)
class ChatQuestionRepositoryCharacterizationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ChatQuestionRepository chatQuestionRepository;

    @Test
    void findByUserId_returnsOnlyThatUsersQuestions() {
        Long userId = 100L;
        em.persist(ChatQuestion.builder().question("q1").answer("a1").isAnswered(true).userId(userId).build());
        em.persist(ChatQuestion.builder().question("q2").answer("a2").isAnswered(true).userId(userId).build());
        em.persist(ChatQuestion.builder().question("other").answer("a").isAnswered(true).userId(999L).build());
        em.flush();
        em.clear();

        Page<ChatQuestion> page = chatQuestionRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).allMatch(c -> c.getUserId().equals(userId));
        assertThat(chatQuestionRepository.countByUserId(userId)).isEqualTo(2);
    }
}
