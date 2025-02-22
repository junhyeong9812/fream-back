package com.fream.back.domain.style.service.command;

import com.fream.back.domain.style.entity.MediaUrl;
import com.fream.back.domain.style.entity.Style;
import com.fream.back.domain.style.repository.MediaUrlRepository;
import com.fream.back.global.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class MediaUrlCommandService {

    private final MediaUrlRepository mediaUrlRepository;
    private final FileUtils fileUtils; // 파일 저장 유틸리티

    // style/{styleId} 폴더
    private static final String STYLE_FOLDER_PREFIX = "styles";
    // => 최종 "/home/ubuntu/fream/styles/{styleId}"
    private static final String API_URL_PREFIX = "/styles/queries";

    public MediaUrl saveMediaFile(Style style, MultipartFile mediaFile) {
        // 1. 파일 시스템에 저장할 경로 설정
        String directory = STYLE_FOLDER_PREFIX + "/" + style.getId();

        // 2. 파일 저장 및 유니크 파일명 생성
        String savedFileName = fileUtils.saveFile(directory, "media_", mediaFile);

        // 3. API 접근을 위한 URL 생성
        String url = String.format("%s/%d/media/%s", API_URL_PREFIX, style.getId(), savedFileName);

        // 4. MediaUrl 엔티티 생성 및 저장
        MediaUrl savedMediaUrl = MediaUrl.builder()
                .style(style)
                .url(url)  // 전체 URL 경로 저장
                .build();

        return mediaUrlRepository.save(savedMediaUrl);
    }

    public void deleteMediaFile(MediaUrl mediaUrl) {
        // 1. URL에서 파일명 추출
        String url = mediaUrl.getUrl();
        String fileName = url.substring(url.lastIndexOf('/') + 1);

        // 2. 파일 시스템 경로 설정
        String directory = STYLE_FOLDER_PREFIX + "/" + mediaUrl.getStyle().getId();

        // 3. 파일 삭제
        fileUtils.deleteFile(directory, fileName);

        // 4. 엔티티 삭제
        mediaUrl.getStyle().removeMediaUrl(mediaUrl);
        mediaUrlRepository.delete(mediaUrl);
    }
}
