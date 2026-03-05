package com.servicehub.repository;

import com.servicehub.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByIsActiveTrue();
    List<Category> findByDepartmentId(Long departmentId);
    List<Category> findByDepartmentIdAndIsActiveTrue(Long departmentId);
    Optional<Category> findByKey(String key);
    boolean existsByName(String name);
    boolean existsByKey(String key);
}
