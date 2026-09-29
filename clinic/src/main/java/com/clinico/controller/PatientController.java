package com.clinico.controller;

import com.clinico.dto.PatientRequest;
import com.clinico.entity.Appointment;
import com.clinico.entity.Patient;
import com.clinico.service.AppointmentService;
import com.clinico.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients", description = "Endpoints for patient registration and profiles")
public class PatientController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    @GetMapping
    @Operation(summary = "Get all patients", description = "List all patients with optional name search")
    public ResponseEntity<List<Patient>> getAllPatients(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(patientService.getAllPatients(search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get patient by ID")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PostMapping
    @Operation(summary = "Register a new patient")
    public ResponseEntity<Patient> createPatient(@Valid @RequestBody PatientRequest request) {
        Patient created = patientService.createPatient(request);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient profile")
    public ResponseEntity<Patient> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {
        Patient updated = patientService.updatePatient(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete patient")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        patientService.deletePatient(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/appointments")
    @Operation(summary = "Get appointment history for a patient")
    public ResponseEntity<List<Appointment>> getPatientAppointments(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getPatientAppointments(id));
    }
}
