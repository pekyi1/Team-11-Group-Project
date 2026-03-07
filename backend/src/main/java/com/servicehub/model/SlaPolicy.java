package com.servicehub.model;

import com.servicehub.model.enums.Priority;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * SLA Policy entity representing time-based targets for service requests.
 * Each policy is uniquely defined by a combination of Category and Priority.
 */
@Entity
@Table(
    name = "sla_policies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_sla_policy_category_priority",
            columnNames = {"category_id", "priority"}
        )
    }
)
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(name = "response_time_hours", nullable = false)
    private Integer responseTimeHours;

    @Column(name = "resolution_time_hours", nullable = false)
    private Integer resolutionTimeHours;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
