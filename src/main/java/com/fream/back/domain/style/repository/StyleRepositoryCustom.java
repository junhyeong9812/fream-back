package com.fream.back.domain.style.repository;

import com.fream.back.domain.style.dto.ProfileStyleResponseDto;
import com.fream.back.domain.style.dto.StyleDetailResponseDto;
import com.fream.back.domain.style.dto.StyleFilterRequestDto;
import com.fream.back.domain.style.dto.StyleResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface StyleRepositoryCustom {
    Page<StyleResponseDto> filterStyles(StyleFilterRequestDto filterRequestDto, Pageable pageable);
    StyleDetailResponseDto getStyleDetail(Long styleId);
    Page<ProfileStyleResponseDto> getStylesByProfile(Long profileId, Pageable pageable);
    Map<Long, Long> styleCountByColorIds(List<Long> colorIds);
}
