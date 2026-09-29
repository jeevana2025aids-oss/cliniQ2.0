package com.clinico.repository;

import com.clinico.entity.Slot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByDoctorIdOrderBySlotDateAscStartTimeAsc(Long doctorId);

    List<Slot> findByIsBookedFalseOrderBySlotDateAscStartTimeAsc();

    List<Slot> findByDoctorIdAndIsBookedFalseOrderBySlotDateAscStartTimeAsc(Long doctorId);

    List<Slot> findBySlotDateAndIsBookedFalseOrderByStartTimeAsc(LocalDate slotDate);

    List<Slot> findByDoctorIdAndSlotDateAndIsBookedFalseOrderByStartTimeAsc(Long doctorId, LocalDate slotDate);

    List<Slot> findByDoctorIdAndSlotDateOrderByStartTimeAsc(Long doctorId, LocalDate slotDate);

    boolean existsByDoctorIdAndSlotDateAndStartTime(Long doctorId, LocalDate slotDate, LocalTime startTime);

    long countByIsBookedFalse();

    long countByIsBookedTrue();
}
