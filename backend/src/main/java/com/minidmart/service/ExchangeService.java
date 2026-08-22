package com.minidmart.service;

import com.minidmart.dto.EligibleItemResponse;
import com.minidmart.dto.ExchangeCreateRequest;
import com.minidmart.dto.ExchangeResponse;
import com.minidmart.dto.RequestStatusUpdateDto;
import com.minidmart.entity.*;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.ExchangeRequestRepository;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.OrderItemRepository;
import com.minidmart.repository.OrderRepository;
import com.minidmart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeRequestRepository exchangeRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditService auditService;

    // ---- Customer ----

    @Transactional
    public ExchangeResponse createExchangeRequest(ExchangeCreateRequest request, UUID userId) {
        OrderItem item = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", request.getOrderItemId()));

        Order order = item.getOrder();

        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("OrderItem", "id", request.getOrderItemId());
        }

        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.PICKED_UP) {
            throw new InvalidOperationException("Exchange not allowed for order status: " + order.getStatus());
        }

        if (order.getCompletedAt() == null || order.getCompletedAt().isBefore(LocalDateTime.now().minusDays(14))) {
            throw new InvalidOperationException("Exchange window of 14 days has expired");
        }

        if (exchangeRequestRepository.existsByOriginalItemIdAndStatusNot(item.getId(), RequestStatus.REJECTED)) {
            throw new InvalidOperationException("Item already has an active exchange request");
        }

        Product replacement = productRepository.findById(request.getReplacementProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getReplacementProductId()));

        if (!replacement.isActive()) {
            throw new InvalidOperationException("Replacement product is not active");
        }

        ExchangeRequest exchangeRequest = ExchangeRequest.builder()
                .order(order)
                .originalItem(item)
                .replacementProduct(replacement)
                .reason(request.getReason())
                .status(RequestStatus.PENDING)
                .build();

        exchangeRequest = exchangeRequestRepository.save(exchangeRequest);

        auditService.log(userId, "EXCHANGE_REQUESTED", "ExchangeRequest", exchangeRequest.getId().toString(),
                "Requested exchange for order item " + item.getId() + " with replacement product " + replacement.getId());

        return toResponse(exchangeRequest);
    }

    @Transactional(readOnly = true)
    public Page<ExchangeResponse> getCustomerExchanges(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return exchangeRequestRepository.findByOrderUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public void cancelCustomerExchange(Long id, UUID userId) {
        ExchangeRequest request = exchangeRequestRepository.findByIdAndOrderUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ExchangeRequest", "id", id));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Can only cancel pending requests");
        }

        exchangeRequestRepository.delete(request);

        auditService.log(userId, "EXCHANGE_CANCELLED", "ExchangeRequest", id.toString(),
                "Customer cancelled pending exchange request");
    }

    // ---- Staff / Admin ----

    @Transactional(readOnly = true)
    public Page<ExchangeResponse> getAllExchanges(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return exchangeRequestRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public ExchangeResponse updateStatus(Long id, RequestStatusUpdateDto dto, UUID staffId) {
        ExchangeRequest request = exchangeRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ExchangeRequest", "id", id));

        RequestStatus oldStatus = request.getStatus();
        RequestStatus newStatus = dto.getStatus();

        if (oldStatus == newStatus) {
            return toResponse(request);
        }

        if (oldStatus == RequestStatus.COMPLETED || oldStatus == RequestStatus.REJECTED) {
            throw new InvalidOperationException("Cannot change status from terminal state: " + oldStatus);
        }

        if (oldStatus == RequestStatus.PENDING && newStatus == RequestStatus.COMPLETED) {
            throw new InvalidOperationException("Must approve exchange before completing it");
        }
        
        // Allowed transitions: PENDING -> APPROVED, PENDING -> REJECTED, APPROVED -> COMPLETED
        if (oldStatus == RequestStatus.APPROVED && newStatus == RequestStatus.REJECTED) {
             throw new InvalidOperationException("Cannot reject an already approved exchange. Complete it or perform administrative cancellation.");
        }

        int quantity = request.getOriginalItem().getQuantity();
        Long replacementProductId = request.getReplacementProduct().getId();

        if (newStatus == RequestStatus.APPROVED) {
            // Atomically reserve inventory
            Inventory inventory = inventoryRepository.findByProductId(replacementProductId)
                    .orElseThrow(() -> new InvalidOperationException("Inventory missing for replacement product"));
            
            int updated = inventoryRepository.reserveInventorySafely(inventory.getId(), quantity);
            if (updated == 0) {
                throw new InvalidOperationException("Insufficient stock for replacement product to approve exchange");
            }
        }

        if (newStatus == RequestStatus.COMPLETED) {
            // Consume reserved replacement inventory
            Inventory repInv = inventoryRepository.findByProductId(replacementProductId)
                    .orElseThrow(() -> new InvalidOperationException("Inventory missing for replacement product"));
            
            int consumed = inventoryRepository.consumeReservedInventorySafely(repInv.getId(), quantity);
            if (consumed == 0) {
                 throw new InvalidOperationException("Failed to consume reserved inventory. System state inconsistency.");
            }

            // Restore original item inventory
            Product originalProduct = request.getOriginalItem().getProduct();
            if (originalProduct != null) {
                Inventory origInv = inventoryRepository.findByProductId(originalProduct.getId()).orElse(null);
                if (origInv != null) {
                    inventoryRepository.addAvailableQuantitySafely(origInv.getId(), quantity);
                }
            }
            request.setResolvedAt(LocalDateTime.now());
        }

        if (newStatus == RequestStatus.REJECTED) {
             request.setResolvedAt(LocalDateTime.now());
        }

        request.setStatus(newStatus);
        exchangeRequestRepository.save(request);

        auditService.log(staffId, "EXCHANGE_" + newStatus.name(), "ExchangeRequest", id.toString(),
                "Exchange request " + id + " changed from " + oldStatus + " to " + newStatus);

        return toResponse(request);
    }

    private ExchangeResponse toResponse(ExchangeRequest req) {
        return ExchangeResponse.builder()
                .id(req.getId())
                .orderNumber(req.getOrder().getOrderNumber())
                .originalItemId(req.getOriginalItem().getId())
                .originalProductName(req.getOriginalItem().getProductName())
                .replacementProductId(req.getReplacementProduct().getId())
                .replacementProductName(req.getReplacementProduct().getName())
                .quantity(req.getOriginalItem().getQuantity())
                .reason(req.getReason())
                .status(req.getStatus())
                .createdAt(req.getCreatedAt())
                .resolvedAt(req.getResolvedAt())
                .build();
    }
}
