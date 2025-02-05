package com.fream.back.domain.product.service.kafka;

import com.fream.back.domain.product.dto.kafka.ViewEvent;
import com.fream.back.domain.product.entity.ProductColor;
import com.fream.back.domain.product.entity.ProductColorViewLog;
import com.fream.back.domain.product.repository.ProductColorRepository;
import com.fream.back.domain.product.repository.ProductColorViewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ViewEventConsumer {

    private final ProductColorRepository productColorRepository;
    private final ProductColorViewLogRepository viewLogRepository;

    // KafkaListener: "view-log-topic" 구독
    @KafkaListener(topics = "view-log-topic",
            groupId = "my-group",
            containerFactory = "viewEventKafkaListenerContainerFactory"
    )
    public void listen(ViewEvent event) {
        // 1) productColorId로 ProductColor 조회
        ProductColor productColor = productColorRepository.findById(event.getProductColorId())
                .orElseThrow(() -> new IllegalArgumentException("해당 ProductColor가 존재하지 않습니다."));

        // 2) 로그 엔티티 생성
        ProductColorViewLog viewLog = ProductColorViewLog.create(
                productColor,
                event.getEmail(),  // 이메일
                event.getAge() == null ? 0 : event.getAge(),  // age가 null이면 0
                event.getGender()  // gender가 null이면 다른 값으로 처리해도 됨
        );

        // 3) viewedAt 덮어쓰기(카프카 메시지의 시점으로 사용)
        viewLog.addViewedAt(event.getViewedAt());

        // 4) DB 저장
        viewLogRepository.save(viewLog);
    }
    // 트래픽일 많은 경우 해당 데이터를 버퍼에 담아서 한번에 전송
    // private final List<ProductColorViewLog> buffer = Collections.synchronizedList(new ArrayList<>());
    // private static final int FLUSH_SIZE = 30;
    //
    // @KafkaListener(topics = "view-log-topic", groupId = "my-group")
    // public void listenWithBuffer(ViewEvent event) {
    //     ProductColor productColor = ...;
    //     ProductColorViewLog viewLog = ...;
    //
    //     buffer.add(viewLog);
    //
    //     if (buffer.size() >= FLUSH_SIZE) {
    //         // 한꺼번에 insert
    //         List<ProductColorViewLog> toSave;
    //         synchronized (buffer) {
    //             toSave = new ArrayList<>(buffer);
    //             buffer.clear();
    //         }
    //         viewLogRepository.saveAll(toSave);
    //     }
    // }
}
