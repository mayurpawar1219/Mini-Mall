package com.minidmart.service;

import com.minidmart.dto.PickupSlotRequest;
import com.minidmart.dto.PickupSlotResponse;
import com.minidmart.entity.PickupSlot;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.PickupSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PickupSlotService {

    private final PickupSlotRepository pickupSlotRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<PickupSlotResponse> getAvailableSlots(LocalDate date) {
        LocalDateTime now = LocalDateTime.now();
        return pickupSlotRepository.findByDateAndEnabledTrueAndStartTimeAfterOrderByStartTimeAsc(date, now)
                .stream()
                .filter(slot -> slot.getCurrentBookings() < slot.getCapacity())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PickupSlotResponse> getAllSlotsByDate(LocalDate date) {
        if (date == null) {
            return pickupSlotRepository.findAll()
                    .stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }
        return pickupSlotRepository.findByDateOrderByStartTimeAsc(date)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PickupSlotResponse createSlot(PickupSlotRequest request, java.util.UUID adminId) {
        validateSlotTime(request.getStartTime(), request.getEndTime());
        
        if (pickupSlotRepository.existsOverlappingSlot(request.getDate(), request.getStartTime(), request.getEndTime(), null)) {
            throw new InvalidOperationException("A pickup slot already exists at or overlapping with this time on this date.");
        }

        PickupSlot slot = PickupSlot.builder()
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .capacity(request.getCapacity())
                .enabled(request.isEnabled())
                .currentBookings(0)
                .build();

        slot = pickupSlotRepository.save(slot);
        
        auditService.log(adminId, "PICKUP_SLOT_CREATED", "PickupSlot", slot.getId().toString(), 
                "Created pickup slot starting at " + slot.getStartTime());
                
        return toResponse(slot);
    }

    @Transactional
    public PickupSlotResponse updateSlot(Long id, PickupSlotRequest request, java.util.UUID adminId) {
        validateSlotTime(request.getStartTime(), request.getEndTime());

        PickupSlot slot = pickupSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PickupSlot", "id", id));

        if (!slot.getDate().equals(request.getDate()) || !slot.getStartTime().equals(request.getStartTime()) || !slot.getEndTime().equals(request.getEndTime())) {
            if (pickupSlotRepository.existsOverlappingSlot(request.getDate(), request.getStartTime(), request.getEndTime(), id)) {
                throw new InvalidOperationException("A pickup slot already exists at or overlapping with this time on this date.");
            }
        }

        if (request.getCapacity() < slot.getCurrentBookings()) {
            throw new InvalidOperationException("Cannot reduce capacity below current bookings (" + slot.getCurrentBookings() + ")");
        }

        slot.setDate(request.getDate());
        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot.setCapacity(request.getCapacity());
        slot.setEnabled(request.isEnabled());

        slot = pickupSlotRepository.save(slot);
        
        auditService.log(adminId, "PICKUP_SLOT_UPDATED", "PickupSlot", slot.getId().toString(), 
                "Updated pickup slot capacity to " + slot.getCapacity());
                
        return toResponse(slot);
    }

    @Transactional
    public void deleteSlot(Long id, java.util.UUID adminId) {
        PickupSlot slot = pickupSlotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PickupSlot", "id", id));

        if (slot.getCurrentBookings() > 0) {
            throw new InvalidOperationException("Cannot delete a pickup slot with active bookings");
        }

        pickupSlotRepository.delete(slot);
        
        auditService.log(adminId, "PICKUP_SLOT_DELETED", "PickupSlot", slot.getId().toString(), 
                "Deleted pickup slot");
    }

    private PickupSlotResponse toResponse(PickupSlot slot) {
        return PickupSlotResponse.builder()
                .id(slot.getId())
                .date(slot.getDate())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .capacity(slot.getCapacity())
                .currentBookings(slot.getCurrentBookings())
                .enabled(slot.isEnabled())
                .available(slot.isEnabled() && slot.getCurrentBookings() < slot.getCapacity())
                .build();
    }

    private void validateSlotTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime.isAfter(endTime) || startTime.isEqual(endTime)) {
            throw new InvalidOperationException("Start time must be before end time.");
        }
        Duration duration = Duration.between(startTime, endTime);
        if (duration.toMinutes() != 60) {
            throw new InvalidOperationException("Pickup slots must be exactly 1 hour in duration.");
        }
    }
}
