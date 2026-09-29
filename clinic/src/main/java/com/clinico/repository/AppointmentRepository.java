package com.clinico.repository;

import com.clinico.entity.Appointment;
import com.clinico.entity.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdOrderBySlot_SlotDateAscSlot_StartTimeAsc(Long doctorId);

    List<Appointment> findByPatientIdOrderBySlot_SlotDateDescSlot_StartTimeDesc(Long patientId);

    List<Appointment> findByDoctorIdAndSlot_SlotDateOrderBySlot_StartTimeAsc(Long doctorId, LocalDate date);

    List<Appointment> findByDoctorIdAndSlot_SlotDateAndStatusOrderBySlot_StartTimeAsc(
            Long doctorId, LocalDate date, AppointmentStatus status);

    List<Appointment> findBySlot_SlotDateOrderBySlot_StartTimeAsc(LocalDate date);

    long countBySlot_SlotDate(LocalDate date);

    long countBySlot_SlotDateAndStatusNot(LocalDate date, AppointmentStatus status);

    Optional<Appointment> findBySlotId(Long slotId);

    List<Appointment> findAllByOrderBySlot_SlotDateDescSlot_StartTimeDesc();
}
