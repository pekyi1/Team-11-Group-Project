package com.servicehub.repository;

import com.servicehub.model.StatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StatusHistoryRepository extends JpaRepository<StatusHistory, Long> {
    /**
     * Finds all status history records for a given request, ordered by changed_at descending (most recent first).
     *
     * @param requestId the ID of the service request
     * @return list of status history records, most recent first
     */
    List<StatusHistory> findByRequestIdOrderByChangedAtDesc(Long requestId);
    
    /**
     * Finds all status history records for a given request, ordered by changed_at ascending (oldest first).
     * Kept for backward compatibility.
     *
     * @param requestId the ID of the service request
     * @return list of status history records, oldest first
     */
    List<StatusHistory> findByRequestIdOrderByChangedAtAsc(Long requestId);
}
