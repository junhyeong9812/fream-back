package com.fream.back.domain.product.repository;

import com.fream.back.domain.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByParentCategoryId(Long parentId);

    // 메인 카테고리 조회
    Optional<Category> findByNameAndParentCategoryIsNull(String name);

    // 서브 카테고리 조회
    Optional<Category> findByNameAndParentCategory(String name, Category parentCategory);

    // 메인 카테고리 목록 조회 (이름 내림차순 정렬)
    List<Category> findByParentCategoryIsNullOrderByNameDesc();

    // 특정 메인 카테고리에 대한 서브 카테고리 목록 조회 (이름 내림차순 정렬)
    List<Category> findByParentCategoryOrderByNameDesc(Category parentCategory);

    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.parentCategory WHERE c.id = :id")
    Optional<Category> findWithParentById(@Param("id") Long id);


    Optional<Category> findByNameAndParentCategoryIsNotNull(String name);

    boolean existsByNameAndParentCategoryIsNull(String name);

    boolean existsByNameAndParentCategory(String name, Category parentCategory);
}
