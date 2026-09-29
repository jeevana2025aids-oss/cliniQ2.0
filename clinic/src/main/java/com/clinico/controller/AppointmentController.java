package com.clinico.controller;

import com.clinico.dto.AppointmentRequest;
import com.clinico.dto.DashboardStatsResponse;
import com.clinico.entity.Appointment;
import com.clinico.entity.AppointmentStatus;
import com.clinico.service.AppointmentService;
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
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Endpoints for booking, cancelling, and viewing appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping({"", "/book"})
    @Operation(summary = "Book an appointment", description = "Core Feature: Patient books an open slot. Slot becomes unavailable.")
    public ResponseEntity<Appointment> bookAppointment(@Valid @RequestBody AppointmentRequest request) {
        Appointment booked = appointmentService.bookAppointment(request);
        return new ResponseEntity<>(booked, HttpStatus.CREATED);
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an appointment", description = "Core Feature: Cancels appointment and immediately frees the slot for rebooking")
    public ResponseEntity<Appointment> cancelAppointment(@PathVariable Long id) {
        Appointment cancelled = appointmentService.cancelAppointment(id);
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping("/doctor/{doctorId}/today")
    @Operation(summary = "Doctor's today appointments", description = "Core Feature: Doctor views today's scheduled appointment list")
    public ResponseEntity<List<Appointment>> getDoctorTodayAppointments(@PathVariable Long doctorId) {
        return ResponseEntity.ok(appointmentService.getDoctorTodayAppointments(doctorId));
    }

    @GetMapping
    @Operation(summary = "Get all appointments", description = "List appointments with optional filters for doctor, patient, date, and status")
    public ResponseEntity<List<Appointment>> getAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.getAppointments(doctorId, patientId, date, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<Appointment> getAppointmentById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/dashboard/stats")
    @Operation(summary = "Dashboard statistics", description = "Get aggregate counts of doctors, patients, slots, and today's appointments")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(appointmentService.getDashboardStats());
    }
}
