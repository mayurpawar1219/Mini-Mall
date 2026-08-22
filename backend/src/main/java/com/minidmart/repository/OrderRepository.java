package com.minidmart.repository;

import com.minidmart.entity.Order;
import com.minidmart.entity.OrderStatus;
import com.minidmart.entity.OrderType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Optional<Order> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);

    // Customer ownership-scoped queries
    Page<Order> findByUserId(UUID userId, Pageable pageable);
    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    // Admin filtered queries
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByType(OrderType type, Pageable pageable);
    Page<Order> findByStatusAndType(OrderStatus status, OrderType type, Pageable pageable);

    // Dashboard & Analytics
    long countByUserId(UUID userId);
    long countByUserIdAndStatus(UUID userId, OrderStatus status);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.status NOT IN :statuses")
    long countByUserIdAndStatusNotIn(@Param("userId") UUID userId, @Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN :statuses")
    long countByStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    long countByStatus(OrderStatus status);
    long countByType(OrderType type);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.type = :type AND o.status IN :statuses")
    long countByTypeAndStatusIn(@Param("type") OrderType type, @Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countByStatusAndCreatedAtBetween(@Param("status") OrderStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status IN :statuses")
    BigDecimal sumTotalAmountByStatusIn(@Param("statuses") Collection<OrderStatus> statuses);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status IN :statuses AND o.completedAt >= :startDate AND o.completedAt <= :endDate")
    BigDecimal sumTotalAmountByStatusInAndCompletedAtBetween(@Param("statuses") Collection<OrderStatus> statuses, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.type IN :types AND o.status NOT IN :statuses AND ((o.pickupSlot.date IS NOT NULL AND o.pickupSlot.date = :today) OR (o.pickupSlot IS NULL AND o.createdAt >= :startOfDay AND o.createdAt <= :endOfDay))")
    long countPickupOrdersToday(@Param("types") Collection<OrderType> types, @Param("statuses") Collection<OrderStatus> statuses, @Param("today") java.time.LocalDate today, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.type = :type AND o.status NOT IN :statuses AND o.createdAt >= :startOfDay AND o.createdAt <= :endOfDay")
    long countDeliveryOrdersToday(@Param("type") OrderType type, @Param("statuses") Collection<OrderStatus> statuses, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);
}

