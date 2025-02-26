package com.fream.back.domain.user.service.command;

import com.fream.back.domain.user.dto.PointDto;
import com.fream.back.domain.user.entity.Point;
import com.fream.back.domain.user.entity.PointStatus;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.PointRepository;
import com.fream.back.domain.user.service.query.PointQueryService;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointCommandService {

    private final PointRepository pointRepository;
    private final UserQueryService userQueryService;
    private final PointQueryService pointQueryService;

    /**
     * 포인트 적립
     */
    @Transactional
    public PointDto.PointResponse addPoint(String email, PointDto.AddPointRequest request) {
        User user = userQueryService.findByEmail(email);

        Point point = Point.builder()
                .user(user)
                .amount(request.getAmount())
                .remainingAmount(request.getAmount())
                .reason(request.getReason())
                .expirationDate(LocalDate.now().plusDays(90)) // 90일 후 만료
                .status(PointStatus.AVAILABLE)
                .build();

        Point savedPoint = pointRepository.save(point);
        return PointDto.PointResponse.from(savedPoint);
    }

    /**
     * 포인트 사용
     */
    @Transactional
    public PointDto.UsePointResponse usePoint(String email, PointDto.UsePointRequest request) {
        User user = userQueryService.findByEmail(email);
        int pointsToUse = request.getAmount();

        // 가용 포인트 총합 확인
        Integer totalAvailable = pointRepository.getTotalAvailablePoints(user);
        if (totalAvailable == null || totalAvailable < pointsToUse) {
            throw new IllegalArgumentException("사용 가능한 포인트가 부족합니다. 보유: " +
                    (totalAvailable == null ? 0 : totalAvailable) + ", 요청: " + pointsToUse);
        }

        // 만료 임박 포인트부터 사용
        List<Point> availablePoints = pointRepository.findByUserAndStatusOrderByExpirationDateAsc(user, PointStatus.AVAILABLE);
        int remainingToUse = pointsToUse;
        int totalUsed = 0;

        for (Point point : availablePoints) {
            if (remainingToUse <= 0) break;

            int used = point.use(remainingToUse);
            remainingToUse -= used;
            totalUsed += used;

            // 포인트 사용 이유 추가
            if (used > 0) {
                point.setReason(request.getReason());
                pointRepository.save(point);
            }
        }

        // 사용 후 남은 총 포인트 계산
        Integer remainingTotal = pointRepository.getTotalAvailablePoints(user);
        if (remainingTotal == null) remainingTotal = 0;

        return PointDto.UsePointResponse.builder()
                .usedPoints(totalUsed)
                .remainingTotalPoints(remainingTotal)
                .message("포인트가 성공적으로 사용되었습니다.")
                .build();
    }

    /**
     * 만료된 포인트 처리 (스케줄러에서 호출)
     */
    @Transactional
    public int expirePoints() {
        List<Point> expiredPoints = pointRepository.findByStatusAndExpirationDateBefore(
                PointStatus.AVAILABLE, LocalDate.now());

        for (Point point : expiredPoints) {
            point.expire();
        }

        return expiredPoints.size();
    }
}