package com.servicehub.repository;

import com.servicehub.model.Category;
import com.servicehub.model.SlaPolicy;
import com.servicehub.model.enums.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Integer> {
    /**
     * Finds an SLA policy by category and priority.
     *
     * @param category the category entity
     * @param priority the priority enum value
     * @return Optional containing the policy if found
     */
    Optional<SlaPolicy> findByCategoryAndPriority(Category category, Priority priority);
    
    /**
     * Finds an SLA policy by category ID and priority (for backward compatibility).
     *
     * @param categoryId the category ID
     * @param priority the priority enum value
     * @return Optional containing the policy if found
     */
    Optional<SlaPolicy> findByCategoryIdAndPriority(Long categoryId, Priority priority);
}
