package com.fream.back.domain.user.service.query;

import com.fream.back.domain.user.dto.PointDto;
import com.fream.back.domain.user.entity.Point;
import com.fream.back.domain.user.entity.PointStatus;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointQueryService {

    private final PointRepository pointRepository;
    private final UserQueryService userQueryService;

    /**
     * 사용자의 포인트 내역 전체 조회 (최근 포인트 적립순)
     */
    @Transactional(readOnly = true)
    public List<PointDto.PointResponse> getAllPointHistory(String email) {
        User user = userQueryService.findByEmail(email);
        List<Point> points = pointRepository.findByUserOrderByCreatedDateDesc(user);

        return points.stream()
                .map(PointDto.PointResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 사용 가능한 포인트만 조회 (유효기간 임박순)
     */
    @Transactional(readOnly = true)
    public List<PointDto.PointResponse> getAvailablePoints(String email) {
        User user = userQueryService.findByEmail(email);
        List<Point> availablePoints = pointRepository.findByUserAndStatusOrderByExpirationDateAsc(
                user, PointStatus.AVAILABLE);

        return availablePoints.stream()
                .map(PointDto.PointResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 포인트 종합 정보 조회 (사용 가능 포인트 합계, 포인트 세부내역, 만료 예정)
     */
    @Transactional(readOnly = true)
    public PointDto.PointSummaryResponse getPointSummary(String email) {
        User user = userQueryService.findByEmail(email);

        // 총 사용 가능 포인트
        Integer totalAvailable = pointRepository.getTotalAvailablePoints(user);
        if (totalAvailable == null) totalAvailable = 0;

        // 모든 포인트 내역
        List<PointDto.PointResponse> allPoints = getAllPointHistory(email);

        // 30일 이내 만료 예정 포인트
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        List<Point> expiringPoints = pointRepository.findExpiringPoints(user, today, thirtyDaysLater);
        List<PointDto.PointResponse> expiringPointsDto = expiringPoints.stream()
                .map(PointDto.PointResponse::from)
                .collect(Collectors.toList());

        return PointDto.PointSummaryResponse.builder()
                .totalAvailablePoints(totalAvailable)
                .pointDetails(allPoints)
                .expiringPoints(expiringPointsDto)
                .build();
    }

    /**
     * 포인트 상세 조회
     */
    @Transactional(readOnly = true)
    public PointDto.PointResponse getPointDetail(String email, Long pointId) {
        User user = userQueryService.findByEmail(email);

        Point point = pointRepository.findById(pointId)
                .orElseThrow(() -> new IllegalArgumentException("해당 포인트 내역이 존재하지 않습니다. ID: " + pointId));

        // 해당 포인트가 현재 사용자의 것인지 확인
        if (!point.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 포인트에 접근할 권한이 없습니다.");
        }

        return PointDto.PointResponse.from(point);
    }
}