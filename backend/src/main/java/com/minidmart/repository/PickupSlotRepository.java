package com.minidmart.repository;

import com.minidmart.entity.PickupSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PickupSlotRepository extends JpaRepository<PickupSlot, Long> {

    List<PickupSlot> findByDateAndEnabledTrueAndStartTimeAfterOrderByStartTimeAsc(LocalDate date, LocalDateTime time);

    List<PickupSlot> findByDateOrderByStartTimeAsc(LocalDate date);
    
    boolean existsByDateAndStartTime(LocalDate date, LocalDateTime startTime);

    @Query("SELECT COUNT(p) > 0 FROM PickupSlot p WHERE p.date = :date AND p.startTime < :endTime AND p.endTime > :startTime AND (:id IS NULL OR p.id != :id)")
    boolean existsOverlappingSlot(@Param("date") LocalDate date, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("id") Long id);

    @Modifying
    @Query("UPDATE PickupSlot p SET p.currentBookings = p.currentBookings + 1 WHERE p.id = :id AND p.currentBookings < p.capacity AND p.enabled = true AND p.startTime > CURRENT_TIMESTAMP")
    int incrementBookingIfCapacityAllows(@Param("id") Long id);

    @Modifying
    @Query("UPDATE PickupSlot p SET p.currentBookings = p.currentBookings - 1 WHERE p.id = :id AND p.currentBookings > 0")
    int decrementBookingSafely(@Param("id") Long id);
}
