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

    public MediaUrl saveMediaFile(Style style, MultipartFile mediaFile) {
        // 1) 하위 디렉토리 = "styles/{styleId}"
        String directory = STYLE_FOLDER_PREFIX + "/" + style.getId();
        // 2) prefix = "media_"
        String uniqueFileName = fileUtils.saveFile(directory, "media_", mediaFile);

        // DB에 "media_abc123.jpg" 만 저장할 건지,
        // 혹은 "styles/{styleId}/media_abc123.jpg"로 저장할 건지는 선택사항
        // 여기서는 DB에는 uniqueFileName만 저장 (ex: "media_abc123.jpg")
        MediaUrl savedMediaUrl = MediaUrl.builder()
                .style(style)
                .url(uniqueFileName)  // ex: "media_abc123.jpg"
                .build();
        return mediaUrlRepository.save(savedMediaUrl);
    }
    public void deleteMediaUrl(MediaUrl mediaUrl) {
        // 파일 삭제
        String directory = STYLE_FOLDER_PREFIX + "/" + mediaUrl.getStyle().getId();
        String fileName = mediaUrl.getUrl(); // "media_abc.jpg"
        fileUtils.deleteFile(directory, fileName);

        // 엔티티 삭제
        mediaUrl.getStyle().removeMediaUrl(mediaUrl);
        mediaUrlRepository.delete(mediaUrl);
    }
}
