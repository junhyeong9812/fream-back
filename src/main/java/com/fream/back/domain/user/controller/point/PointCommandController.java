package com.fream.back.domain.user.controller.point;

import com.fream.back.domain.user.dto.PointDto;
import com.fream.back.domain.user.service.point.PointCommandService;
import com.fream.back.global.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/points/commands")
public class PointCommandController {

    private final PointCommandService pointCommandService;

    /**
     * 포인트 적립
     */
    @PostMapping
    public ResponseEntity<PointDto.PointResponse> addPoint(@RequestBody PointDto.AddPointRequest request) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        PointDto.PointResponse response = pointCommandService.addPoint(email, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 포인트 사용
     */
    @PostMapping("/use")
    public ResponseEntity<PointDto.UsePointResponse> usePoint(@RequestBody PointDto.UsePointRequest request) {
        String email = SecurityUtils.extractEmailFromSecurityContext();
        PointDto.UsePointResponse response = pointCommandService.usePoint(email, request);
        return ResponseEntity.ok(response);
    }
}