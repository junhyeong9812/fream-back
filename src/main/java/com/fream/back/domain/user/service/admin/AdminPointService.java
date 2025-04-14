package com.fream.back.domain.user.service.admin;

import com.fream.back.domain.user.dto.PointDto;
import com.fream.back.domain.user.entity.Point;
import com.fream.back.domain.user.entity.PointStatus;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.repository.PointRepository;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.exception.EntityNotFoundException;
import com.fream.back.global.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPointService {

    private final UserRepository userRepository;
    private final PointRepository pointRepository;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 특정 사용자의 모든 포인트 내역 조회
     */
    public List<PointDto.PointResponse> getUserPointHistory(Long userId) {
        User user = getUserById(userId);

        List<Point> points = pointRepository.findByUserOrderByCreatedDateDesc(user);
        return points.stream()
                .map(PointDto.PointResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 사용 가능한 포인트만 조회
     */
    public List<PointDto.PointResponse> getUserAvailablePoints(Long userId) {
        User user = getUserById(userId);

        List<Point> availablePoints = pointRepository.findByUserAndStatusOrderByExpirationDateAsc(
                user, PointStatus.AVAILABLE);

        return availablePoints.stream()
                .map(PointDto.PointResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 포인트 종합 정보 조회
     */
    public PointDto.PointSummaryResponse getUserPointSummary(Long userId) {
        User user = getUserById(userId);

        // 사용 가능한 포인트 총합
        int totalAvailablePoints = user.calculateTotalPoints();

        // 사용 가능한 포인트 목록
        List<Point> availablePoints = pointRepository.findByUserAndStatusOrderByExpirationDateAsc(
                user, PointStatus.AVAILABLE);

        List<PointDto.PointResponse> pointResponses = availablePoints.stream()
                .map(PointDto.PointResponse::from)
                .collect(Collectors.toList());

        // 만료 예정 포인트 (30일 이내)
        LocalDate thirtyDaysLater = LocalDate.now().plusDays(30);
        List<PointDto.PointResponse> expiringPoints = availablePoints.stream()
                .filter(point -> point.getExpirationDate() != null &&
                        point.getExpirationDate().isBefore(thirtyDaysLater))
                .map(PointDto.PointResponse::from)
                .collect(Collectors.toList());

        return PointDto.PointSummaryResponse.builder()
                .totalAvailablePoints(totalAvailablePoints)
                .pointDetails(pointResponses)
                .expiringPoints(expiringPoints)
                .build();
    }

    /**
     * 특정 포인트 상세 조회
     */
    public PointDto.PointResponse getPointDetail(Long pointId) {
        Point point = pointRepository.findById(pointId)
                .orElseThrow(() -> new EntityNotFoundException("Point not found with id: " + pointId));

        return PointDto.PointResponse.from(point);
    }

    /**
     * 포인트 지급 (어드민)
     */
    @Transactional
    public PointDto.PointResponse addPointByAdmin(Long userId, PointDto.AdminPointRequest request) {
        User user = getUserById(userId);

        Point point = Point.builder()
                .user(user)
                .amount(request.getAmount())
                .remainingAmount(request.getAmount())
                .reason(request.getReason())
                .status(PointStatus.AVAILABLE)
                .build();

        // 만료일 설정
        if (request.getExpirationDate() != null) {
            point.setExpirationDate(request.getExpirationDate());
        } else {
            // 기본 만료일: 1년 후
            point.setExpirationDate(LocalDate.now().plusYears(1));
        }

        Point savedPoint = pointRepository.save(point);
        return PointDto.PointResponse.from(savedPoint);
    }

    /**
     * 포인트 차감 (어드민)
     */
    @Transactional
    public PointDto.UsePointResponse deductPointByAdmin(Long userId, PointDto.AdminPointRequest request) {
        User user = getUserById(userId);

        int deductAmount = request.getAmount();
        if (deductAmount <= 0) {
            throw new InvalidRequestException("차감할 포인트는 0보다 커야 합니다.");
        }

        int availablePoints = user.calculateTotalPoints();
        if (availablePoints < deductAmount) {
            throw new InvalidRequestException(
                    "사용 가능한 포인트가 부족합니다. 가용 포인트: " + availablePoints + ", 차감 요청: " + deductAmount);
        }

        // 포인트 차감 로직 (만료일 가까운 순으로 차감)
        List<Point> availablePointsList = pointRepository.findByUserAndStatusOrderByExpirationDateAsc(
                user, PointStatus.AVAILABLE);

        int remainingToDeduct = deductAmount;
        for (Point point : availablePointsList) {
            if (remainingToDeduct <= 0) break;

            int deducted = point.use(remainingToDeduct);
            remainingToDeduct -= deducted;
            pointRepository.save(point);
        }

        // 차감 내역 기록
        Point deductionRecord = Point.builder()
                .user(user)
                .amount(-deductAmount)
                .remainingAmount(0)
                .reason(request.getReason())
                .status(PointStatus.USED)
                .build();

        pointRepository.save(deductionRecord);

        int updatedAvailablePoints = user.calculateTotalPoints();

        return PointDto.UsePointResponse.builder()
                .usedPoints(deductAmount)
                .remainingTotalPoints(updatedAvailablePoints)
                .message("포인트가 성공적으로 차감되었습니다.")
                .build();
    }

    /**
     * 포인트 통계 조회
     */
    public PointDto.PointStatisticsResponse getPointStatistics(String startDateStr, String endDateStr) {
        LocalDate startDate = startDateStr != null ?
                LocalDate.parse(startDateStr, dateFormatter) : LocalDate.now().minusMonths(1);
        LocalDate endDate = endDateStr != null ?
                LocalDate.parse(endDateStr, dateFormatter) : LocalDate.now();

        // 구현: 포인트 통계 정보 조회 로직
        // 예시: 지정된 기간 동안의 총 지급 포인트, 사용 포인트, 만료 포인트 등

        return PointDto.PointStatisticsResponse.builder()
                .totalIssuedPoints(0) // 실제 구현 필요
                .totalUsedPoints(0)   // 실제 구현 필요
                .totalExpiredPoints(0) // 실제 구현 필요
                .period(startDate + " ~ " + endDate)
                .build();
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }
}