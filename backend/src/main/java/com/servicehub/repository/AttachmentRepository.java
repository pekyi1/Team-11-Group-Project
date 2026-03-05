package com.servicehub.repository;

import com.servicehub.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByRequestIdAndIsDeletedFalse(Long requestId);
    long countByRequestIdAndIsDeletedFalse(Long requestId);
}
