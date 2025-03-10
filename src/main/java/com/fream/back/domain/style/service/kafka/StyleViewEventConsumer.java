package com.fream.back.domain.style.service.kafka;

import com.fream.back.domain.style.dto.kafka.StyleViewEvent;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.entity.StyleViewLog;
import com.fream.back.domain.style.repository.StyleRepository;
import com.fream.back.domain.style.repository.StyleViewLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Service
@RequiredArgsConstructor
public class StyleViewEventConsumer {

    private final StyleRepository styleRepository;
    private final StyleViewLogRepository styleViewLogRepository;

    // 버퍼링을 위한 큐 설정 (고부하 시스템을 위한 최적화)
    private final ConcurrentLinkedQueue<StyleViewLog> buffer = new ConcurrentLinkedQueue<>();
    private static final int FLUSH_SIZE = 30;

    // KafkaListener: "style-view-log-topic" 구독
    @KafkaListener(topics = "style-view-log-topic",
            groupId = "style-view-group",
            containerFactory = "styleViewEventKafkaListenerContainerFactory"
    )
    @Transactional
    public void listen(StyleViewEvent event) {
        try {
            log.info("스타일 뷰 이벤트 수신: styleId={}, email={}", event.getStyleId(), event.getEmail());

            // 1) styleId로 Style 조회
            Style style = styleRepository.findById(event.getStyleId())
                    .orElseThrow(() -> new IllegalArgumentException("해당 Style이 존재하지 않습니다. styleId=" + event.getStyleId()));

            // 2) Style 엔티티의 viewCount 증가
            style.incrementViewCount();
            styleRepository.save(style);

            // 3) 로그 엔티티 생성
            StyleViewLog viewLog = StyleViewLog.create(
                    style,
                    event.getEmail(),
                    event.getAge() == null ? 0 : event.getAge(),
                    event.getGender()
            );

            // 4) viewedAt 덮어쓰기(카프카 메시지의 시점으로 사용)
            viewLog.addViewedAt(event.getViewedAt());

            // 5) 버퍼에 추가 (메모리 버퍼링 사용)
            buffer.add(viewLog);

            // 6) 버퍼 크기가 임계값 이상이면 일괄 저장
            if (buffer.size() >= FLUSH_SIZE) {
                flushBuffer();
            }
        } catch (Exception e) {
            log.error("스타일 뷰 이벤트 처리 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 버퍼에 있는 로그를 일괄적으로 DB에 저장
     */
    @Transactional
    public void flushBuffer() {
        List<StyleViewLog> toSave = new ArrayList<>();
        StyleViewLog viewLogEntry;

        // 변수명을 log에서 viewLogEntry로 변경하여 @Slf4j의 log와 충돌 방지
        while ((viewLogEntry = buffer.poll()) != null) {
            toSave.add(viewLogEntry);
        }

        if (!toSave.isEmpty()) {
            log.info("스타일 뷰 로그 일괄 저장: {} 건", toSave.size());
            styleViewLogRepository.saveAll(toSave);
        }
    }
}