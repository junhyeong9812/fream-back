package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.entity.UserGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserGradeRepository extends JpaRepository<UserGrade, Long> {

    Optional<UserGrade> findByLevel(Integer level);

    Optional<UserGrade> findByName(String name);

    List<UserGrade> findAllByOrderByLevelAsc();

    @Query("SELECT g FROM UserGrade g WHERE g.minPurchaseAmount <= :purchaseAmount ORDER BY g.minPurchaseAmount DESC")
    List<UserGrade> findGradeByPurchaseAmount(Integer purchaseAmount);

    @Query("SELECT COUNT(u) FROM User u WHERE u.grade.id = :gradeId")
    Long countUsersByGradeId(Long gradeId);

    @Query("SELECT u.grade.id as gradeId, COUNT(u) as userCount FROM User u GROUP BY u.grade.id")
    List<Object[]> countUsersGroupByGrade();
}