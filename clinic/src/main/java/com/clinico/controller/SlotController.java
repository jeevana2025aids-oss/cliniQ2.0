package com.clinico.controller;

import com.clinico.dto.SlotRangeRequest;
import com.clinico.dto.SlotRequest;
import com.clinico.entity.Slot;
import com.clinico.service.SlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
@Tag(name = "Slots", description = "Endpoints for doctors to publish and manage appointment slots")
public class SlotController {

    private final SlotService slotService;

    @PostMapping
    @Operation(summary = "Create a single slot", description = "Doctor publishes a single time slot")
    public ResponseEntity<Slot> createSlot(@Valid @RequestBody SlotRequest request) {
        Slot created = slotService.createSlot(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/publish-range")
    @Operation(summary = "Publish slots for a date range", description = "Core Feature: Doctors publish available recurring slots over a date range")
    public ResponseEntity<List<Slot>> publishSlotRange(@Valid @RequestBody SlotRangeRequest request) {
        List<Slot> createdSlots = slotService.publishSlotRange(request);
        return new ResponseEntity<>(createdSlots, HttpStatus.CREATED);
    }

    @GetMapping("/available")
    @Operation(summary = "Get available slots", description = "View open slots with optional filters for doctor and date")
    public ResponseEntity<List<Slot>> getAvailableSlots(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(slotService.getAvailableSlots(doctorId, date));
    }

    @GetMapping("/doctor/{doctorId}")
    @Operation(summary = "Get all slots for a doctor", description = "View both available and booked slots for a specific doctor")
    public ResponseEntity<List<Slot>> getSlotsByDoctor(@PathVariable Long doctorId) {
        return ResponseEntity.ok(slotService.getSlotsByDoctor(doctorId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get slot by ID")
    public ResponseEntity<Slot> getSlotById(@PathVariable Long id) {
        return ResponseEntity.ok(slotService.getSlotById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an unbooked slot")
    public ResponseEntity<Void> deleteSlot(@PathVariable Long id) {
        slotService.deleteSlot(id);
        return ResponseEntity.noContent().build();
    }
}
