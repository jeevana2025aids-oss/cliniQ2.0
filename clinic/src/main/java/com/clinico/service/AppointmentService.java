package com.clinico.service;

import com.clinico.dto.AppointmentRequest;
import com.clinico.dto.DashboardStatsResponse;
import com.clinico.entity.Appointment;
import com.clinico.entity.AppointmentStatus;
import com.clinico.entity.Patient;
import com.clinico.entity.Slot;
import com.clinico.exception.InvalidOperationException;
import com.clinico.exception.ResourceNotFoundException;
import com.clinico.exception.SlotAlreadyBookedException;
import com.clinico.repository.AppointmentRepository;
import com.clinico.repository.DoctorRepository;
import com.clinico.repository.PatientRepository;
import com.clinico.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final SlotRepository slotRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public Appointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with ID: " + id));
    }

    /**
     * Core Rule 1: A slot can only be booked by one patient at a time.
     * When booked, the slot is immediately marked unavailable.
     */
    public Appointment bookAppointment(AppointmentRequest request) {
        Patient patient = patientRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + request.getPatientId()));

        Slot slot = slotRepository.findById(request.getSlotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: " + request.getSlotId()));

        // Enforce business rule: check if slot is already booked
        if (slot.isBooked()) {
            throw new SlotAlreadyBookedException(
                    "Slot on " + slot.getSlotDate() + " from " + slot.getStartTime() + " to " + slot.getEndTime() +
                    " is already booked by another patient.");
        }

        // Verify no active confirmed appointment exists on this slot
        Optional<Appointment> existingAppointment = appointmentRepository.findBySlotId(slot.getId());
        if (existingAppointment.isPresent() && existingAppointment.get().getStatus() == AppointmentStatus.CONFIRMED) {
            throw new SlotAlreadyBookedException("An active appointment already exists for this slot.");
        }

        // Mark slot as booked
        slot.setBooked(true);
        slotRepository.save(slot);

        // If an old cancelled appointment record exists for this slot, we can update or create a new one
        Appointment appointment = Appointment.builder()
                .doctor(slot.getDoctor())
                .patient(patient)
                .slot(slot)
                .reason(request.getReason() != null ? request.getReason().trim() : "General Consultation")
                .status(AppointmentStatus.CONFIRMED)
                .bookedAt(LocalDateTime.now())
                .cancelledAt(null)
                .build();

        return appointmentRepository.save(appointment);
    }

    /**
     * Core Rule 2: A cancelled appointment must free the slot immediately for rebooking.
     */
    public Appointment cancelAppointment(Long appointmentId) {
        Appointment appointment = getAppointmentById(appointmentId);

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new InvalidOperationException("Appointment ID " + appointmentId + " is already cancelled.");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());

        // Immediately free the slot for rebooking
        Slot slot = appointment.getSlot();
        if (slot != null) {
            slot.setBooked(false);
            slotRepository.save(slot);
        }

        return appointmentRepository.save(appointment);
    }

    /**
     * Core Feature: Doctors can view today's appointment list.
     */
    @Transactional(readOnly = true)
    public List<Appointment> getDoctorTodayAppointments(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with ID: " + doctorId);
        }
        LocalDate today = LocalDate.now();
        return appointmentRepository.findByDoctorIdAndSlot_SlotDateOrderBySlot_StartTimeAsc(doctorId, today);
    }

    /**
     * List appointments with optional filters for doctor, patient, date, and status.
     */
    @Transactional(readOnly = true)
    public List<Appointment> getAppointments(Long doctorId, Long patientId, LocalDate date, AppointmentStatus status) {
        List<Appointment> all = appointmentRepository.findAllByOrderBySlot_SlotDateDescSlot_StartTimeDesc();

        return all.stream()
                .filter(a -> doctorId == null || a.getDoctor().getId().equals(doctorId))
                .filter(a -> patientId == null || a.getPatient().getId().equals(patientId))
                .filter(a -> date == null || a.getSlot().getSlotDate().equals(date))
                .filter(a -> status == null || a.getStatus() == status)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Appointment> getPatientAppointments(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found with ID: " + patientId);
        }
        return appointmentRepository.findByPatientIdOrderBySlot_SlotDateDescSlot_StartTimeDesc(patientId);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        LocalDate today = LocalDate.now();
        return DashboardStatsResponse.builder()
                .totalDoctors(doctorRepository.count())
                .totalPatients(patientRepository.count())
                .totalSlots(slotRepository.count())
                .availableSlots(slotRepository.countByIsBookedFalse())
                .bookedSlots(slotRepository.countByIsBookedTrue())
                .todayAppointments(appointmentRepository.countBySlot_SlotDateAndStatusNot(today, AppointmentStatus.CANCELLED))
                .totalAppointments(appointmentRepository.count())
                .build();
    }
}
