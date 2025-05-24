package com.fream.back.domain.user.service.point;

import com.fream.back.domain.user.dto.PointDto;
import com.fream.back.domain.user.entity.Point;
import com.fream.back.domain.user.entity.PointStatus;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.exception.PointHistoryNotFoundException;
import com.fream.back.domain.user.exception.UserNotFoundException;
import com.fream.back.domain.user.repository.PointRepository;
import com.fream.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
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
        log.info("포인트 내역 전체 조회 시작 - 사용자: {}", email);

        try {
            User user = userQueryService.findByEmail(email);
            if (user == null) {
                throw new UserNotFoundException(email);
            }

            List<Point> points = pointRepository.findByUserOrderByCreatedDateDesc(user);

            List<PointDto.PointResponse> result = points.stream()
                    .map(PointDto.PointResponse::from)
                    .collect(Collectors.toList());

            log.info("포인트 내역 조회 완료 - 사용자: {}, 내역 수: {}", email, result.size());
            return result;
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("포인트 내역 조회 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new PointHistoryNotFoundException("포인트 내역을 조회할 수 없습니다.");
        }
    }

    /**
     * 사용자의 사용 가능한 포인트만 조회 (유효기간 임박순)
     */
    @Transactional(readOnly = true)
    public List<PointDto.PointResponse> getAvailablePoints(String email) {
        log.info("사용 가능한 포인트 조회 시작 - 사용자: {}", email);

        try {
            User user = userQueryService.findByEmail(email);
            if (user == null) {
                throw new UserNotFoundException(email);
            }

            List<Point> availablePoints = pointRepository.findByUserAndStatusOrderByExpirationDateAsc(
                    user, PointStatus.AVAILABLE);

            List<PointDto.PointResponse> result = availablePoints.stream()
                    .map(PointDto.PointResponse::from)
                    .collect(Collectors.toList());

            log.info("사용 가능한 포인트 조회 완료 - 사용자: {}, 가용 포인트 항목 수: {}",
                    email, result.size());
            return result;
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용 가능한 포인트 조회 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new PointHistoryNotFoundException("사용 가능한 포인트를 조회할 수 없습니다.");
        }
    }

    /**
     * 포인트 종합 정보 조회 (사용 가능 포인트 합계, 포인트 세부내역, 만료 예정)
     */
    @Transactional(readOnly = true)
    public PointDto.PointSummaryResponse getPointSummary(String email) {
        log.info("포인트 종합 정보 조회 시작 - 사용자: {}", email);

        try {
            User user = userQueryService.findByEmail(email);
            if (user == null) {
                throw new UserNotFoundException(email);
            }

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

            PointDto.PointSummaryResponse result = PointDto.PointSummaryResponse.builder()
                    .totalAvailablePoints(totalAvailable)
                    .pointDetails(allPoints)
                    .expiringPoints(expiringPointsDto)
                    .build();

            log.info("포인트 종합 정보 조회 완료 - 사용자: {}, 총 포인트: {}, 만료 예정: {}",
                    email, totalAvailable, expiringPointsDto.size());
            return result;
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("포인트 종합 정보 조회 중 예상치 못한 오류 발생 - 사용자: {}", email, e);
            throw new PointHistoryNotFoundException("포인트 종합 정보를 조회할 수 없습니다.");
        }
    }

    /**
     * 포인트 상세 조회
     */
    @Transactional(readOnly = true)
    public PointDto.PointResponse getPointDetail(String email, Long pointId) {
        log.info("포인트 상세 조회 시작 - 사용자: {}, 포인트 ID: {}", email, pointId);

        try {
            User user = userQueryService.findByEmail(email);
            if (user == null) {
                throw new UserNotFoundException(email);
            }

            Point point = pointRepository.findById(pointId)
                    .orElseThrow(() -> new PointHistoryNotFoundException(pointId));

            // 해당 포인트가 현재 사용자의 것인지 확인
            if (!point.getUser().getId().equals(user.getId())) {
                log.warn("포인트 접근 권한 없음 - 사용자: {}, 포인트 ID: {}, 포인트 소유자: {}",
                        email, pointId, point.getUser().getEmail());
                throw new PointHistoryNotFoundException("해당 포인트에 접근할 권한이 없습니다.");
            }

            PointDto.PointResponse result = PointDto.PointResponse.from(point);
            log.info("포인트 상세 조회 완료 - 사용자: {}, 포인트 ID: {}", email, pointId);
            return result;
        } catch (UserNotFoundException | PointHistoryNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("포인트 상세 조회 중 예상치 못한 오류 발생 - 사용자: {}, 포인트 ID: {}",
                    email, pointId, e);
            throw new PointHistoryNotFoundException("포인트 상세 정보를 조회할 수 없습니다.");
        }
    }
}