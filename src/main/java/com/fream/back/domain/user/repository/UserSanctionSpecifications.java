package com.fream.back.domain.user.repository;

import com.fream.back.domain.user.dto.SanctionDto;
import com.fream.back.domain.user.entity.SanctionStatus;
import com.fream.back.domain.user.entity.SanctionType;
import com.fream.back.domain.user.entity.User;
import com.fream.back.domain.user.entity.UserSanction;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserSanctionSpecifications {

    public static Specification<UserSanction> withSearchCriteria(SanctionDto.SanctionSearchRequestDto searchDto) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // User ID 조건
            if (searchDto.getUserId() != null) {
                Join<UserSanction, User> userJoin = root.join("user");
                predicates.add(criteriaBuilder.equal(userJoin.get("id"), searchDto.getUserId()));
            }

            // Email 조건
            if (searchDto.getEmail() != null && !searchDto.getEmail().isEmpty()) {
                Join<UserSanction, User> userJoin = root.join("user");
                predicates.add(criteriaBuilder.like(userJoin.get("email"), "%" + searchDto.getEmail() + "%"));
            }

            // Status 조건
            if (searchDto.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), searchDto.getStatus()));
            }

            // Type 조건
            if (searchDto.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), searchDto.getType()));
            }

            // Start date range 조건
            if (searchDto.getStartDateStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), searchDto.getStartDateStart()));
            }
            if (searchDto.getStartDateEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("startDate"), searchDto.getStartDateEnd()));
            }

            // End date range 조건
            if (searchDto.getEndDateStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("endDate"), searchDto.getEndDateStart()));
            }
            if (searchDto.getEndDateEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), searchDto.getEndDateEnd()));
            }

            // Created date range 조건
            if (searchDto.getCreatedDateStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdDate"), searchDto.getCreatedDateStart()));
            }
            if (searchDto.getCreatedDateEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdDate"), searchDto.getCreatedDateEnd()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<UserSanction> hasUserId(Long userId) {
        return (root, query, criteriaBuilder) -> {
            Join<UserSanction, User> userJoin = root.join("user");
            return criteriaBuilder.equal(userJoin.get("id"), userId);
        };
    }

    public static Specification<UserSanction> hasStatus(SanctionStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<UserSanction> hasType(SanctionType type) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("type"), type);
    }

    public static Specification<UserSanction> createdBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("createdDate"), start, end);
    }

    public static Specification<UserSanction> startDateBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("startDate"), start, end);
    }

    public static Specification<UserSanction> endDateBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("endDate"), start, end);
    }

    public static Specification<UserSanction> userEmailLike(String email) {
        return (root, query, criteriaBuilder) -> {
            Join<UserSanction, User> userJoin = root.join("user");
            return criteriaBuilder.like(userJoin.get("email"), "%" + email + "%");
        };
    }
}