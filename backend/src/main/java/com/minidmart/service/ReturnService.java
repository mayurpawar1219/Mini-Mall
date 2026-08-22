package com.minidmart.service;

import com.minidmart.dto.EligibleItemResponse;
import com.minidmart.dto.RequestStatusUpdateDto;
import com.minidmart.dto.ReturnCreateRequest;
import com.minidmart.dto.ReturnResponse;
import com.minidmart.entity.*;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.InventoryRepository;
import com.minidmart.repository.OrderItemRepository;
import com.minidmart.repository.OrderRepository;
import com.minidmart.repository.ReturnRequestRepository;
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
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryRepository inventoryRepository;
    private final AuditService auditService;

    // ---- Eligibility ----

    @Transactional(readOnly = true)
    public List<EligibleItemResponse> getEligibleItems(UUID orderId, UUID userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        return order.getItems().stream().map(item -> {
            boolean eligible = true;
            String reason = null;

            if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.PICKED_UP) {
                eligible = false;
                reason = "Order is not completed";
            } else if (order.getCompletedAt() == null || order.getCompletedAt().isBefore(LocalDateTime.now().minusDays(14))) {
                eligible = false;
                reason = "Return window expired";
            } else if (returnRequestRepository.existsByOrderItemIdAndStatusNot(item.getId(), RequestStatus.REJECTED)) {
                eligible = false;
                reason = "Item already has an active return request";
            }

            return EligibleItemResponse.builder()
                    .orderItemId(item.getId())
                    .orderNumber(order.getOrderNumber())
                    .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                    .productName(item.getProductName())
                    .priceAtPurchase(item.getPriceAtPurchase())
                    .quantity(item.getQuantity())
                    .eligibleForReturn(eligible)
                    .ineligibilityReason(reason)
                    .build();
        }).collect(Collectors.toList());
    }

    // ---- Customer ----

    @Transactional
    public ReturnResponse createReturnRequest(ReturnCreateRequest request, UUID userId) {
        OrderItem item = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", request.getOrderItemId()));

        Order order = item.getOrder();

        if (!order.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("OrderItem", "id", request.getOrderItemId()); // IDOR protection
        }

        if (order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.PICKED_UP) {
            throw new InvalidOperationException("Return not allowed for order status: " + order.getStatus());
        }

        if (order.getCompletedAt() == null || order.getCompletedAt().isBefore(LocalDateTime.now().minusDays(14))) {
            throw new InvalidOperationException("Return window of 14 days has expired");
        }

        if (returnRequestRepository.existsByOrderItemIdAndStatusNot(item.getId(), RequestStatus.REJECTED)) {
            throw new InvalidOperationException("Item already has an active return request");
        }

        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(order)
                .orderItem(item)
                .reason(request.getReason())
                .status(RequestStatus.PENDING)
                .build();

        returnRequest = returnRequestRepository.save(returnRequest);

        auditService.log(userId, "RETURN_REQUESTED", "ReturnRequest", returnRequest.getId().toString(),
                "Requested return for order item " + item.getId() + " of order " + order.getOrderNumber());

        return toResponse(returnRequest);
    }

    @Transactional(readOnly = true)
    public Page<ReturnResponse> getCustomerReturns(UUID userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return returnRequestRepository.findByOrderUserId(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public void cancelCustomerReturn(Long id, UUID userId) {
        ReturnRequest request = returnRequestRepository.findByIdAndOrderUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", id));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidOperationException("Can only cancel pending requests");
        }

        returnRequestRepository.delete(request);

        auditService.log(userId, "RETURN_CANCELLED", "ReturnRequest", id.toString(),
                "Customer cancelled pending return request");
    }

    // ---- Staff / Admin ----

    @Transactional(readOnly = true)
    public Page<ReturnResponse> getAllReturns(int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 50), Sort.by(Sort.Direction.DESC, "createdAt"));
        return returnRequestRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public ReturnResponse updateStatus(Long id, RequestStatusUpdateDto dto, UUID staffId) {
        ReturnRequest request = returnRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", "id", id));

        RequestStatus oldStatus = request.getStatus();
        RequestStatus newStatus = dto.getStatus();

        if (oldStatus == newStatus) {
            return toResponse(request);
        }

        if (oldStatus == RequestStatus.COMPLETED || oldStatus == RequestStatus.REJECTED) {
            throw new InvalidOperationException("Cannot change status from terminal state: " + oldStatus);
        }

        if (oldStatus == RequestStatus.PENDING && newStatus == RequestStatus.COMPLETED) {
            throw new InvalidOperationException("Must approve return before completing it");
        }

        request.setStatus(newStatus);
        
        if (newStatus == RequestStatus.COMPLETED || newStatus == RequestStatus.REJECTED) {
            request.setResolvedAt(LocalDateTime.now());
        }

        if (newStatus == RequestStatus.COMPLETED) {
            // Restore inventory
            Product product = request.getOrderItem().getProduct();
            if (product != null) {
                Inventory inventory = inventoryRepository.findByProductId(product.getId())
                        .orElse(null);
                if (inventory != null) {
                    inventory.setAvailableQuantity(inventory.getAvailableQuantity() + request.getOrderItem().getQuantity());
                    inventoryRepository.save(inventory);
                }
            }
        }

        returnRequestRepository.save(request);

        auditService.log(staffId, "RETURN_" + newStatus.name(), "ReturnRequest", id.toString(),
                "Return request " + id + " changed from " + oldStatus + " to " + newStatus);

        return toResponse(request);
    }

    private ReturnResponse toResponse(ReturnRequest req) {
        return ReturnResponse.builder()
                .id(req.getId())
                .orderNumber(req.getOrder().getOrderNumber())
                .orderItemId(req.getOrderItem().getId())
                .productName(req.getOrderItem().getProductName())
                .quantity(req.getOrderItem().getQuantity())
                .reason(req.getReason())
                .status(req.getStatus())
                .createdAt(req.getCreatedAt())
                .resolvedAt(req.getResolvedAt())
                .build();
    }
}
