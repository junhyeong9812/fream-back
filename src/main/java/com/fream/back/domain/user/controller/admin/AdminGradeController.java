package com.fream.back.domain.user.controller.admin;

import com.fream.back.domain.user.dto.UserGradeDto.*;
import com.fream.back.domain.user.service.admin.AdminGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/users/grades")
@RequiredArgsConstructor
public class AdminGradeController {

    private final AdminGradeService adminGradeService;

    /**
     * 모든 등급 조회
     */
    @GetMapping
    public ResponseEntity<List<GradeResponseDto>> getAllGrades() {
        List<GradeResponseDto> grades = adminGradeService.getAllGrades();
        return ResponseEntity.ok(grades);
    }

    /**
     * 등급 상세 조회
     */
    @GetMapping("/{gradeId}")
    public ResponseEntity<GradeResponseDto> getGradeById(@PathVariable Long gradeId) {
        GradeResponseDto grade = adminGradeService.getGradeById(gradeId);
        return ResponseEntity.ok(grade);
    }

    /**
     * 등급 생성
     */
    @PostMapping
    public ResponseEntity<GradeResponseDto> createGrade(@RequestBody GradeRequestDto requestDto) {
        GradeResponseDto createdGrade = adminGradeService.createGrade(requestDto);
        return ResponseEntity.ok(createdGrade);
    }

    /**
     * 등급 수정
     */
    @PutMapping("/{gradeId}")
    public ResponseEntity<GradeResponseDto> updateGrade(
            @PathVariable Long gradeId,
            @RequestBody GradeUpdateRequestDto requestDto) {

        GradeResponseDto updatedGrade = adminGradeService.updateGrade(gradeId, requestDto);
        return ResponseEntity.ok(updatedGrade);
    }

    /**
     * 등급 삭제
     */
    @DeleteMapping("/{gradeId}")
    public ResponseEntity<Void> deleteGrade(@PathVariable Long gradeId) {
        adminGradeService.deleteGrade(gradeId);
        return ResponseEntity.ok().build();
    }

    /**
     * 등급별 사용자 수 조회
     */
    @GetMapping("/counts")
    public ResponseEntity<Map<Integer, Long>> getGradeUserCounts() {
        Map<Integer, Long> gradeCounts = adminGradeService.getGradeUserCounts();
        return ResponseEntity.ok(gradeCounts);
    }

    /**
     * 등급 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<GradeStatisticsDto>> getGradeStatistics() {
        List<GradeStatisticsDto> statistics = adminGradeService.getGradeStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * 등급 자동 설정 실행
     * (구매액 기준으로 등급 자동 부여 배치 작업 수동 실행)
     */
    @PostMapping("/auto-assign")
    public ResponseEntity<AutoAssignResultDto> runGradeAutoAssignment() {
        AutoAssignResultDto result = adminGradeService.runGradeAutoAssignment();
        return ResponseEntity.ok(result);
    }
}