package com.fream.back.domain.event.service.command;

import com.fream.back.domain.event.entity.Event;
import com.fream.back.domain.event.entity.SimpleImage;
import com.fream.back.domain.event.exception.ImageNotFoundException;
import com.fream.back.domain.event.repository.SimpleImageRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SimpleImageCommandService {
    private final SimpleImageRepository simpleImageRepository;
    private final FileUtils fileUtils;

    /**
     * 이벤트에 연결된 심플이미지 생성
     */
    public void createSimpleImages(Event event, List<MultipartFile> imageFiles) {
        log.debug("심플이미지 생성 시작: eventId={}, 이미지 개수={}", event.getId(), imageFiles.size());

        // 이벤트 ID에 해당하는 디렉토리 경로
        String directory = "event/" + event.getId();

        for (int i = 0; i < imageFiles.size(); i++) {
            MultipartFile file = imageFiles.get(i);

            // 파일이 비어있는지 체크
            if (file.isEmpty()) {
                log.warn("빈 이미지 파일이 전달됨: index={}, eventId={}", i, event.getId());
                continue;
            }

            try {
                // "simple_eventId_순번" 형태의 프리픽스
                String prefix = "simple_" + event.getId() + "_" + (i + 1) + "_";

                // fileUtils.saveFile()에서 고유 파일명(확장자 포함)이 생성됨
                // FileUtils에서 BASE_DIR(/home/ubuntu/fream)에 directory를 추가해서 저장
                String savedFileName = fileUtils.saveFile(directory, prefix, file);
                log.debug("이미지 파일 저장 완료: fileName={}, eventId={}", savedFileName, event.getId());

                // DB에는 "savedFileName"만 보관
                SimpleImage simpleImage = SimpleImage.builder()
                        .savedFileName(savedFileName)
                        .event(event)  // 빌더로 직접 연결해도 되고, event.addSimpleImage() 써도 됨
                        .build();

                event.addSimpleImage(simpleImage);
                simpleImageRepository.save(simpleImage);
                log.debug("심플이미지 엔티티 저장 완료: imageId={}, eventId={}", simpleImage.getId(), event.getId());
            } catch (Exception e) {
                log.error("심플이미지 저장 중 오류 발생: index={}, eventId={}", i, event.getId(), e);
                throw new RuntimeException("이미지 저장 중 오류 발생: " + e.getMessage(), e);
            }
        }
        log.debug("심플이미지 생성 완료: eventId={}, 이미지 개수={}", event.getId(), imageFiles.size());
    }

    /**
     * 기존 이미지 전체 삭제 (DB에서)
     */
    public void deleteAllByEvent(Long eventId) {
        log.debug("이벤트 관련 심플이미지 전체 삭제: eventId={}", eventId);

        try {
            // 여기서도 "조회"가 필요하지만, 커맨드 서비스 자체에서
            // "전체 삭제"만 수행하기 위해 단순히 repository.deleteByEventId(eventId) 같은 방식을 사용 가능
            simpleImageRepository.deleteByEventId(eventId);
            log.debug("이벤트 관련 심플이미지 전체 삭제 완료: eventId={}", eventId);
        } catch (Exception e) {
            log.error("이벤트 관련 심플이미지 전체 삭제 실패: eventId={}", eventId, e);
            throw new RuntimeException("이벤트 이미지 삭제 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 심플이미지 삭제
     */
    public void deleteSimpleImage(Long imageId) {
        log.debug("심플이미지 삭제: imageId={}", imageId);

        SimpleImage image = simpleImageRepository.findById(imageId)
                .orElseThrow(() -> {
                    log.error("삭제할 심플이미지를 찾을 수 없음: imageId={}", imageId);
                    return new ImageNotFoundException("이미지를 찾을 수 없습니다. ID: " + imageId);
                });

        try {
            // 파일 시스템에서 삭제
            String directory = "event/" + image.getEvent().getId();
            boolean deleted = fileUtils.deleteFile(directory, image.getSavedFileName());
            log.debug("심플이미지 파일 삭제 {}: fileName={}, imageId={}",
                    deleted ? "성공" : "실패", image.getSavedFileName(), imageId);

            // DB에서 삭제
            simpleImageRepository.delete(image);
            log.debug("심플이미지 엔티티 삭제 완료: imageId={}", imageId);
        } catch (Exception e) {
            log.error("심플이미지 삭제 중 오류 발생: imageId={}", imageId, e);
            throw new RuntimeException("이미지 삭제 중 오류 발생: " + e.getMessage(), e);
        }
    }
}