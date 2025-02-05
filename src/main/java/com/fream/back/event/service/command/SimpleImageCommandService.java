package com.fream.back.event.service.command;

import com.fream.back.event.entity.Event;
import com.fream.back.event.entity.SimpleImage;
import com.fream.back.event.repository.SimpleImageRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SimpleImageCommandService {
    private final SimpleImageRepository simpleImageRepository;
    private final FileUtils fileUtils;

    public void createSimpleImages(Event event, List<MultipartFile> imageFiles) {
        // 예: /home/xxx/프로젝트경로/event/{eventId}/
        String directory = System.getProperty("user.dir") + "/event/" + event.getId() + "/";

        for (int i = 0; i < imageFiles.size(); i++) {
            MultipartFile file = imageFiles.get(i);
            // "simple_eventId_순번" 형태의 프리픽스
            String prefix = "simple_" + event.getId() + "_" + (i + 1);
            // fileUtils.saveFile() 에서 고유 파일명(확장자 포함)이 생성됨
            String savedFileName = fileUtils.saveFile(directory, prefix, file);

            // DB에는 "savedFileName"만 보관
            SimpleImage simpleImage = SimpleImage.builder()
                    .savedFileName(savedFileName)
                    .event(event)  // 빌더로 직접 연결해도 되고, event.addSimpleImage() 써도 됨
                    .build();

            event.addSimpleImage(simpleImage);
            simpleImageRepository.save(simpleImage);
        }
    }

    // 기존 이미지 전체 삭제 (DB에서)
    public void deleteAllByEvent(Long eventId) {
        // 여기서도 "조회"가 필요하지만, 커맨드 서비스 자체에서
        // "전체 삭제"만 수행하기 위해 단순히 repository.deleteByEventId(eventId) 같은 방식을 사용 가능
        simpleImageRepository.deleteByEventId(eventId);
    }

    // 심플이미지 수정/삭제 등 추가 로직
}

