package com.fream.back.domain.user.service.admin;

import com.fream.back.domain.user.dto.UserGradeDto.*;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.entity.UserGrade;
import com.fream.back.domain.user.repository.UserGradeRepository;
import com.fream.back.domain.user.repository.UserRepository;
import com.fream.back.global.exception.EntityNotFoundException;
import com.fream.back.global.exception.InvalidRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminGradeService {

    private final UserGradeRepository userGradeRepository;
    private final UserRepository userRepository;

    /**
     * 모든 등급 조회
     */
    @Transactional(readOnly = true)
    public List<GradeResponseDto> getAllGrades() {
        List<UserGrade> grades = userGradeRepository.findAllByOrderByLevelAsc();

        return grades.stream()
                .map(this::convertToGradeResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 등급 상세 조회
     */
    @Transactional(readOnly = true)
    public GradeResponseDto getGradeById(Long gradeId) {
        UserGrade grade = userGradeRepository.findById(gradeId)
                .orElseThrow(() -> new EntityNotFoundException("Grade not found with id: " + gradeId));

        return convertToGradeResponseDto(grade);
    }

    /**
     * 등급 생성
     */
    @Transactional
    public GradeResponseDto createGrade(GradeRequestDto requestDto) {
        // 중복 체크 (레벨, 이름)
        if (userGradeRepository.findByLevel(requestDto.getLevel()).isPresent()) {
            throw new InvalidRequestException("Grade already exists with level: " + requestDto.getLevel());
        }

        if (userGradeRepository.findByName(requestDto.getName()).isPresent()) {
            throw new InvalidRequestException("Grade already exists with name: " + requestDto.getName());
        }

        // 새 등급 생성
        UserGrade grade = UserGrade.builder()
                .level(requestDto.getLevel())
                .name(requestDto.getName())
                .description(requestDto.getDescription())
                .minPurchaseAmount(requestDto.getMinPurchaseAmount())
                .pointRate(requestDto.getPointRate())
                .benefits(requestDto.getBenefits())
                .build();

        userGradeRepository.save(grade);

        return convertToGradeResponseDto(grade);
    }

    /**
     * 등급 수정
     */
    @Transactional
    public GradeResponseDto updateGrade(Long gradeId, GradeUpdateRequestDto requestDto) {
        UserGrade grade = userGradeRepository.findById(gradeId)
                .orElseThrow(() -> new EntityNotFoundException("Grade not found with id: " + gradeId));

        // 중복 체크 (이름)
        if (requestDto.getName() != null &&
                !requestDto.getName().equals(grade.getName()) &&
                userGradeRepository.findByName(requestDto.getName()).isPresent()) {
            throw new InvalidRequestException("Grade already exists with name: " + requestDto.getName());
        }

        // 등급 업데이트
        grade.updateGrade(
                requestDto.getName(),
                requestDto.getDescription(),
                requestDto.getMinPurchaseAmount(),
                requestDto.getPointRate(),
                requestDto.getBenefits()
        );

        return convertToGradeResponseDto(grade);
    }

    /**
     * 등급 삭제
     */
    @Transactional
    public void deleteGrade(Long gradeId) {
        UserGrade grade = userGradeRepository.findById(gradeId)
                .orElseThrow(() -> new EntityNotFoundException("Grade not found with id: " + gradeId));

        // 사용자 수 확인
        Long userCount = userGradeRepository.countUsersByGradeId(gradeId);
        if (userCount > 0) {
            throw new InvalidRequestException("Cannot delete grade with active users. Current users: " + userCount);
        }

        userGradeRepository.delete(grade);
    }

    /**
     * 등급별 사용자 수 조회
     */
    @Transactional(readOnly = true)
    public Map<Integer, Long> getGradeUserCounts() {
        List<Object[]> counts = userGradeRepository.countUsersGroupByGrade();
        Map<Integer, Long> result = new HashMap<>();

        for (Object[] row : counts) {
            if (row[0] != null) {
                Long gradeId = (Long) row[0];
                Long userCount = (Long) row[1];

                UserGrade grade = userGradeRepository.findById(gradeId).orElse(null);
                if (grade != null) {
                    result.put(grade.getLevel(), userCount);
                }
            }
        }

        return result;
    }

    /**
     * 등급 통계 조회
     */
    @Transactional(readOnly = true)
    public List<GradeStatisticsDto> getGradeStatistics() {
        List<UserGrade> grades = userGradeRepository.findAllByOrderByLevelAsc();
        List<GradeStatisticsDto> result = new ArrayList<>();

        for (UserGrade grade : grades) {
            Long userCount = userGradeRepository.countUsersByGradeId(grade.getId());

            result.add(GradeStatisticsDto.builder()
                    .id(grade.getId())
                    .level(grade.getLevel())
                    .name(grade.getName())
                    .userCount(userCount)
                    .build());
        }

        return result;
    }

    /**
     * 등급 자동 설정 실행
     */
    @Transactional
    public AutoAssignResultDto runGradeAutoAssignment() {
        List<User> users = userRepository.findAll();
        int processed = 0;
        int updated = 0;

        for (User user : users) {
            processed++;

            // 사용자 총 구매액 조회 (가정: 주문 테이블에서 가져와야 함)
            // 실제 구현에서는 OrderRepository 등에서 가져와야 함
            Integer totalPurchaseAmount = 0; // 임시 값

            // 구매액에 맞는 등급 찾기
            List<UserGrade> matchingGrades = userGradeRepository.findGradeByPurchaseAmount(totalPurchaseAmount);

            if (!matchingGrades.isEmpty()) {
                // 가장 높은 조건의 등급 선택 (이미 DB 쿼리에서 DESC로 정렬됨)
                UserGrade newGrade = matchingGrades.get(0);

                // 기존 등급과 다르면 업데이트
                if (user.getGrade() == null || !user.getGrade().getId().equals(newGrade.getId())) {
                    user.addGrade(newGrade);
                    updated++;
                }
            }
        }

        return AutoAssignResultDto.builder()
                .processed(processed)
                .updated(updated)
                .build();
    }

    /**
     * UserGrade 엔티티를 GradeResponseDto로 변환
     */
    private GradeResponseDto convertToGradeResponseDto(UserGrade grade) {
        Long userCount = userGradeRepository.countUsersByGradeId(grade.getId());

        return GradeResponseDto.builder()
                .id(grade.getId())
                .level(grade.getLevel())
                .name(grade.getName())
                .description(grade.getDescription())
                .minPurchaseAmount(grade.getMinPurchaseAmount())
                .pointRate(grade.getPointRate())
                .benefits(grade.getBenefits())
                .userCount(userCount)
                .createdDate(grade.getCreatedDate())
                .updatedDate(grade.getModifiedDate())
                .build();
    }
}