package com.clinico.service;

import com.clinico.dto.SlotRangeRequest;
import com.clinico.dto.SlotRequest;
import com.clinico.entity.Doctor;
import com.clinico.entity.Slot;
import com.clinico.exception.InvalidOperationException;
import com.clinico.exception.ResourceNotFoundException;
import com.clinico.repository.DoctorRepository;
import com.clinico.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class SlotService {

    private final SlotRepository slotRepository;
    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public Slot getSlotById(Long id) {
        return slotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found with ID: " + id));
    }

    public Slot createSlot(SlotRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new InvalidOperationException("Slot start time must be before end time.");
        }

        if (slotRepository.existsByDoctorIdAndSlotDateAndStartTime(
                doctor.getId(), request.getSlotDate(), request.getStartTime())) {
            throw new InvalidOperationException(
                    "A slot for doctor " + doctor.getName() + " on " + request.getSlotDate() +
                    " at " + request.getStartTime() + " already exists.");
        }

        Slot slot = Slot.builder()
                .doctor(doctor)
                .slotDate(request.getSlotDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isBooked(false)
                .build();

        return slotRepository.save(slot);
    }

    /**
     * Core Feature: Doctors publish available slots for a date range.
     * Generates slots of fixed duration between daily start and end times for each day in range.
     */
    public List<Slot> publishSlotRange(SlotRangeRequest request) {
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + request.getDoctorId()));

        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new InvalidOperationException("Start date cannot be after end date.");
        }

        if (!request.getDailyStartTime().isBefore(request.getDailyEndTime())) {
            throw new InvalidOperationException("Daily start time must be before daily end time.");
        }

        int duration = (request.getDurationMinutes() != null && request.getDurationMinutes() > 0)
                ? request.getDurationMinutes() : 30;

        List<Slot> createdSlots = new ArrayList<>();
        LocalDate currentDate = request.getStartDate();

        while (!currentDate.isAfter(request.getEndDate())) {
            LocalTime currentSlotStart = request.getDailyStartTime();

            while (true) {
                LocalTime currentSlotEnd = currentSlotStart.plusMinutes(duration);
                if (currentSlotEnd.isAfter(request.getDailyEndTime())) {
                    break;
                }

                // Check if slot already exists
                boolean exists = slotRepository.existsByDoctorIdAndSlotDateAndStartTime(
                        doctor.getId(), currentDate, currentSlotStart);

                if (!exists) {
                    Slot slot = Slot.builder()
                            .doctor(doctor)
                            .slotDate(currentDate)
                            .startTime(currentSlotStart)
                            .endTime(currentSlotEnd)
                            .isBooked(false)
                            .build();
                    createdSlots.add(slot);
                }

                currentSlotStart = currentSlotEnd;
            }

            currentDate = currentDate.plusDays(1);
        }

        if (createdSlots.isEmpty()) {
            throw new InvalidOperationException("No new slots created. All slots in this range already exist.");
        }

        return slotRepository.saveAll(createdSlots);
    }

    @Transactional(readOnly = true)
    public List<Slot> getAvailableSlots(Long doctorId, LocalDate date) {
        if (doctorId != null && date != null) {
            return slotRepository.findByDoctorIdAndSlotDateAndIsBookedFalseOrderByStartTimeAsc(doctorId, date);
        } else if (doctorId != null) {
            return slotRepository.findByDoctorIdAndIsBookedFalseOrderBySlotDateAscStartTimeAsc(doctorId);
        } else if (date != null) {
            return slotRepository.findBySlotDateAndIsBookedFalseOrderByStartTimeAsc(date);
        } else {
            return slotRepository.findByIsBookedFalseOrderBySlotDateAscStartTimeAsc();
        }
    }

    @Transactional(readOnly = true)
    public List<Slot> getSlotsByDoctor(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor not found with ID: " + doctorId);
        }
        return slotRepository.findByDoctorIdOrderBySlotDateAscStartTimeAsc(doctorId);
    }

    public void deleteSlot(Long id) {
        Slot slot = getSlotById(id);
        if (slot.isBooked()) {
            throw new InvalidOperationException("Cannot delete a booked slot. Please cancel the associated appointment first.");
        }
        slotRepository.delete(slot);
    }
}
