package com.minidmart.service;

import com.minidmart.dto.InventoryAdjustmentRequest;
import com.minidmart.dto.InventoryResponse;
import com.minidmart.entity.Inventory;
import com.minidmart.entity.Product;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final AuditService auditService;

    @Transactional
    public void initializeInventory(Product product, int initialStock) {
        Inventory inventory = Inventory.builder()
                .product(product)
                .availableQuantity(initialStock)
                .reservedQuantity(0)
                .build();
        inventoryRepository.save(inventory);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProductId(Long productId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));
        return toResponse(inventory);
    }

    @Transactional
    public InventoryResponse adjustInventory(Long productId, InventoryAdjustmentRequest request, String staffId) {
        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for product ID: " + productId));

        int newAvailable = inventory.getAvailableQuantity() + request.getQuantityChange();

        if (newAvailable < 0) {
            throw new InvalidOperationException("Cannot reduce available quantity below 0. Current available: " + inventory.getAvailableQuantity());
        }

        inventory.setAvailableQuantity(newAvailable);
        inventory = inventoryRepository.save(inventory);

        if (staffId != null) {
            auditService.log(null, "INVENTORY_ADJUSTED", "Inventory", inventory.getId().toString(),
                    "Product ID: " + productId + " adjusted by " + request.getQuantityChange() + " by: " + staffId);
        }

        return toResponse(inventory);
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .productId(inventory.getProduct().getId())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .updatedAt(inventory.getUpdatedAt())
                .build();
    }
}
