package com.fream.back.domain.user.service.point;

import com.fream.back.domain.user.dto.PointDto;
import com.fream.back.domain.user.entity.Point;
import com.fream.back.domain.user.entity.PointStatus;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.InsufficientPointsException;
import com.fream.back.domain.user.exception.InvalidPointAmountException;
import com.fream.back.domain.user.exception.UserNotFoundException;
import com.fream.back.domain.user.repository.PointRepository;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointCommandService {

    private final PointRepository pointRepository;
    private final UserQueryService userQueryService;

    /**
     * 포인트 적립
     */
    @Transactional
    public PointDto.PointResponse addPoint(String email, PointDto.AddPointRequest request) {
        log.info("포인트 적립 시작 - 사용자: {}, 적립액: {}", email, request.getAmount());

        try {
            // 입력값 검증
            validateAddPointRequest(request);

            User user = userQueryService.findByEmail(email);
            if (user == null) {
                throw new UserNotFoundException(email);
            }

            Point point = Point.builder()
                    .user(user)
                    .amount(request.getAmount())
                    .remainingAmount(request.getAmount())
                    .reason(request.getReason())
                    .expirationDate(LocalDate.now().plusDays(90)) // 90일 후 만료
                    .status(PointStatus.AVAILABLE)
                    .build();

            Point savedPoint = pointRepository.save(point);

            log.info("포인트 적립 완료 - 사용자: {}, 적립액: {}, 포인트 ID: {}",
                    email, request.getAmount(), savedPoint.getId());

            return PointDto.PointResponse.from(savedPoint);
        } catch (UserNotFoundException | InvalidPointAmountException e) {
            throw e;
        } catch (Exception e) {
            log.error("포인트 적립 중 예상치 못한 오류 발생 - 사용자: {}, 적립액: {}",
                    email, request.getAmount(), e);
            throw new InvalidPointAmountException("포인트 적립 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 포인트 사용
     */
    @Transactional
    public PointDto.UsePointResponse usePoint(String email, PointDto.UsePointRequest request) {
        log.info("포인트 사용 시작 - 사용자: {}, 사용액: {}", email, request.getAmount());

        try {
            // 입력값 검증
            validateUsePointRequest(request);

            User user = userQueryService.findByEmail(email);
            if (user == null) {
                throw new UserNotFoundException(email);
            }

            int pointsToUse = request.getAmount();

            // 가용 포인트 총합 확인
            Integer totalAvailable = pointRepository.getTotalAvailablePoints(user);
            if (totalAvailable == null || totalAvailable < pointsToUse) {
                log.warn("포인트 부족 - 사용자: {}, 필요: {}, 보유: {}",
                        email, pointsToUse, totalAvailable != null ? totalAvailable : 0);
                throw new InsufficientPointsException(pointsToUse, totalAvailable != null ? totalAvailable : 0);
            }

            // 만료 임박 포인트부터 사용
            List<Point> availablePoints = pointRepository.findByUserAndStatusOrderByExpirationDateAsc(user, PointStatus.AVAILABLE);
            int remainingToUse = pointsToUse;
            int totalUsed = 0;

            for (Point point : availablePoints) {
                if (remainingToUse <= 0) break;

                Integer usedAmount = point.use(remainingToUse);
                int used = usedAmount != null ? usedAmount : 0;
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

            log.info("포인트 사용 완료 - 사용자: {}, 사용액: {}, 남은 포인트: {}",
                    email, totalUsed, remainingTotal);

            return PointDto.UsePointResponse.builder()
                    .usedPoints(totalUsed)
                    .remainingTotalPoints(remainingTotal)
                    .message("포인트가 성공적으로 사용되었습니다.")
                    .build();
        } catch (UserNotFoundException | InvalidPointAmountException | InsufficientPointsException e) {
            throw e;
        } catch (Exception e) {
            log.error("포인트 사용 중 예상치 못한 오류 발생 - 사용자: {}, 사용액: {}",
                    email, request.getAmount(), e);
            throw new InsufficientPointsException("포인트 사용 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 만료된 포인트 처리 (스케줄러에서 호출)
     */
    @Transactional
    public int expirePoints() {
        log.info("만료 포인트 처리 시작");

        try {
            List<Point> expiredPoints = pointRepository.findByStatusAndExpirationDateBefore(
                    PointStatus.AVAILABLE, LocalDate.now());

            for (Point point : expiredPoints) {
                point.expire();
            }

            log.info("만료 포인트 처리 완료 - 만료된 포인트 수: {}", expiredPoints.size());
            return expiredPoints.size();
        } catch (Exception e) {
            log.error("만료 포인트 처리 중 오류 발생", e);
            return 0;
        }
    }

    /**
     * 포인트 적립 요청 검증
     */
    private void validateAddPointRequest(PointDto.AddPointRequest request) {
        if (request == null) {
            throw new InvalidPointAmountException("포인트 적립 요청 데이터가 없습니다.");
        }

        if (request.getAmount() <= 0) {
            throw new InvalidPointAmountException(request.getAmount());
        }

        if (request.getAmount() > 1000000) { // 최대 100만 포인트 적립 제한
            throw new InvalidPointAmountException("한 번에 적립할 수 있는 포인트는 최대 1,000,000점입니다.");
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new InvalidPointAmountException("포인트 적립 사유를 입력해주세요.");
        }
    }

    /**
     * 포인트 사용 요청 검증
     */
    private void validateUsePointRequest(PointDto.UsePointRequest request) {
        if (request == null) {
            throw new InvalidPointAmountException("포인트 사용 요청 데이터가 없습니다.");
        }

        if (request.getAmount() <= 0) {
            throw new InvalidPointAmountException(request.getAmount());
        }

        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new InvalidPointAmountException("포인트 사용 사유를 입력해주세요.");
        }
    }
}