package com.minidmart.repository;

import com.minidmart.entity.ExchangeRequest;
import com.minidmart.entity.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, Long> {
    Page<ExchangeRequest> findByOrderUserId(UUID userId, Pageable pageable);
    Optional<ExchangeRequest> findByIdAndOrderUserId(Long id, UUID userId);
    boolean existsByOriginalItemIdAndStatusNot(Long orderItemId, RequestStatus status);

    // Analytics
    long countByStatus(RequestStatus status);
}
