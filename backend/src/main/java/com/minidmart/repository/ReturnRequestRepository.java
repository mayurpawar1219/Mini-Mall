package com.minidmart.repository;

import com.minidmart.entity.ReturnRequest;
import com.minidmart.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Page<ReturnRequest> findByOrderUserId(UUID userId, Pageable pageable);
    Optional<ReturnRequest> findByIdAndOrderUserId(Long id, UUID userId);
    boolean existsByOrderItemIdAndStatusNot(Long orderItemId, RequestStatus status);

    // Analytics
    long countByStatus(RequestStatus status);
}
