package com.minidmart.repository;

import com.minidmart.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    Optional<Inventory> findByProductId(Long productId);
    @Modifying
    @Query("UPDATE Inventory i SET i.availableQuantity = i.availableQuantity - :quantity, i.reservedQuantity = i.reservedQuantity + :quantity, i.version = i.version + 1 WHERE i.id = :id AND i.availableQuantity >= :quantity")
    int reserveInventorySafely(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.availableQuantity = i.availableQuantity + :quantity, i.reservedQuantity = i.reservedQuantity - :quantity, i.version = i.version + 1 WHERE i.id = :id AND i.reservedQuantity >= :quantity")
    int releaseReservedInventorySafely(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.reservedQuantity = i.reservedQuantity - :quantity, i.version = i.version + 1 WHERE i.id = :id AND i.reservedQuantity >= :quantity")
    int consumeReservedInventorySafely(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE Inventory i SET i.availableQuantity = i.availableQuantity + :quantity, i.version = i.version + 1 WHERE i.id = :id")
    int addAvailableQuantitySafely(@Param("id") Long id, @Param("quantity") int quantity);

    // Analytics queries
    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableQuantity <= :threshold AND i.availableQuantity > 0")
    long countLowStockProducts(@Param("threshold") int threshold);

    @Query("SELECT COUNT(i) FROM Inventory i WHERE i.availableQuantity = 0")
    long countOutOfStockProducts();
}
