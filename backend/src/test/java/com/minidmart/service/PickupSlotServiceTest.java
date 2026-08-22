package com.minidmart.service;

import com.minidmart.dto.PickupSlotRequest;
import com.minidmart.dto.PickupSlotResponse;
import com.minidmart.entity.PickupSlot;
import com.minidmart.exception.InvalidOperationException;
import com.minidmart.exception.ResourceNotFoundException;
import com.minidmart.repository.PickupSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickupSlotServiceTest {

    @Mock
    private PickupSlotRepository pickupSlotRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PickupSlotService pickupSlotService;

    private UUID adminId;
    private PickupSlot pickupSlot;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        pickupSlot = PickupSlot.builder()
                .id(1L)
                .date(LocalDate.now().plusDays(1))
                .startTime(LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0))
                .endTime(LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0))
                .capacity(10)
                .currentBookings(2)
                .enabled(true)
                .build();
    }

    @Test
    void getAvailableSlots_ReturnsFutureSlots() {
        when(pickupSlotRepository.findByDateAndEnabledTrueAndStartTimeAfterOrderByStartTimeAsc(any(LocalDate.class), any(LocalDateTime.class)))
                .thenReturn(List.of(pickupSlot));

        List<PickupSlotResponse> slots = pickupSlotService.getAvailableSlots(LocalDate.now().plusDays(1));

        assertNotNull(slots);
        assertEquals(1, slots.size());
        assertEquals(1L, slots.get(0).getId());
        assertTrue(slots.get(0).isAvailable());
    }

    @Test
    void createSlot_ValidRequest_Success() {
        PickupSlotRequest request = new PickupSlotRequest(
                LocalDate.now().plusDays(2),
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(2).withHour(11).withMinute(0).withSecond(0).withNano(0),
                5,
                true
        );
        when(pickupSlotRepository.save(any(PickupSlot.class))).thenReturn(pickupSlot);

        PickupSlotResponse response = pickupSlotService.createSlot(request, adminId);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(pickupSlotRepository, times(1)).save(any(PickupSlot.class));
        verify(auditService, times(1)).log(eq(adminId), eq("PICKUP_SLOT_CREATED"), anyString(), anyString(), anyString());
    }

    @Test
    void createSlot_InvalidTime_ThrowsException() {
        PickupSlotRequest request = new PickupSlotRequest(
                LocalDate.now().plusDays(2),
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(2).withHour(10).withMinute(30).withSecond(0).withNano(0), // not 1 hour
                5,
                true
        );

        assertThrows(InvalidOperationException.class, () -> pickupSlotService.createSlot(request, adminId));
    }

    @Test
    void updateSlot_ReduceCapacityBelowBookings_ThrowsException() {
        pickupSlot.setCurrentBookings(5);
        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(pickupSlot));

        PickupSlotRequest request = new PickupSlotRequest(
                LocalDate.now().plusDays(1),
                pickupSlot.getStartTime(),
                pickupSlot.getEndTime(),
                3, // less than 5 bookings
                true
        );

        assertThrows(InvalidOperationException.class, () -> pickupSlotService.updateSlot(1L, request, adminId));
    }

    @Test
    void deleteSlot_ActiveBookings_ThrowsException() {
        pickupSlot.setCurrentBookings(1);
        when(pickupSlotRepository.findById(1L)).thenReturn(Optional.of(pickupSlot));

        assertThrows(InvalidOperationException.class, () -> pickupSlotService.deleteSlot(1L, adminId));
    }
}
