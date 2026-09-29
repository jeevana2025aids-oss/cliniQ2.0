package com.clinico.config;

import com.clinico.entity.Appointment;
import com.clinico.entity.AppointmentStatus;
import com.clinico.entity.Doctor;
import com.clinico.entity.Patient;
import com.clinico.entity.Slot;
import com.clinico.repository.AppointmentRepository;
import com.clinico.repository.DoctorRepository;
import com.clinico.repository.PatientRepository;
import com.clinico.repository.SlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public void run(String... args) {
        if (doctorRepository.count() > 0) {
            log.info("Database already contains data, skipping initial seeding.");
            return;
        }

        log.info("Seeding initial demo data for CliniQ system...");

        // 1. Doctors
        Doctor d1 = Doctor.builder()
                .name("Dr. Sarah Jenkins")
                .specialization("Cardiology")
                .email("sarah.jenkins@clinico.org")
                .phone("+1-555-0101")
                .build();

        Doctor d2 = Doctor.builder()
                .name("Dr. Marcus Chen")
                .specialization("General Medicine")
                .email("marcus.chen@clinico.org")
                .phone("+1-555-0102")
                .build();

        Doctor d3 = Doctor.builder()
                .name("Dr. Priya Patel")
                .specialization("Pediatrics")
                .email("priya.patel@clinico.org")
                .phone("+1-555-0103")
                .build();

        doctorRepository.saveAll(List.of(d1, d2, d3));

        // 2. Patients
        Patient p1 = Patient.builder()
                .name("John Doe")
                .email("john.doe@example.com")
                .phone("+1-555-0201")
                .age(34)
                .gender("Male")
                .build();

        Patient p2 = Patient.builder()
                .name("Alice Smith")
                .email("alice.smith@example.com")
                .phone("+1-555-0202")
                .age(29)
                .gender("Female")
                .build();

        Patient p3 = Patient.builder()
                .name("Robert Johnson")
                .email("robert.j@example.com")
                .phone("+1-555-0203")
                .age(48)
                .gender("Male")
                .build();

        patientRepository.saveAll(List.of(p1, p2, p3));

        // 3. Slots for today and tomorrow
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        Slot s1 = Slot.builder().doctor(d1).slotDate(today).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(9, 30)).isBooked(true).build();
        Slot s2 = Slot.builder().doctor(d1).slotDate(today).startTime(LocalTime.of(9, 30)).endTime(LocalTime.of(10, 0)).isBooked(false).build();
        Slot s3 = Slot.builder().doctor(d1).slotDate(today).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).isBooked(false).build();
        Slot s4 = Slot.builder().doctor(d1).slotDate(tomorrow).startTime(LocalTime.of(14, 0)).endTime(LocalTime.of(14, 30)).isBooked(false).build();

        Slot s5 = Slot.builder().doctor(d2).slotDate(today).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(10, 30)).isBooked(true).build();
        Slot s6 = Slot.builder().doctor(d2).slotDate(today).startTime(LocalTime.of(10, 30)).endTime(LocalTime.of(11, 0)).isBooked(false).build();
        Slot s7 = Slot.builder().doctor(d2).slotDate(tomorrow).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).isBooked(false).build();

        Slot s8 = Slot.builder().doctor(d3).slotDate(today).startTime(LocalTime.of(11, 0)).endTime(LocalTime.of(11, 30)).isBooked(false).build();
        Slot s9 = Slot.builder().doctor(d3).slotDate(today).startTime(LocalTime.of(11, 30)).endTime(LocalTime.of(12, 0)).isBooked(false).build();

        slotRepository.saveAll(List.of(s1, s2, s3, s4, s5, s6, s7, s8, s9));

        // 4. Initial confirmed appointments
        Appointment a1 = Appointment.builder()
                .doctor(d1)
                .patient(p1)
                .slot(s1)
                .reason("Routine cardiovascular health checkup")
                .status(AppointmentStatus.CONFIRMED)
                .bookedAt(LocalDateTime.now().minusHours(2))
                .build();

        Appointment a2 = Appointment.builder()
                .doctor(d2)
                .patient(p2)
                .slot(s5)
                .reason("Seasonal flu symptoms and headache")
                .status(AppointmentStatus.CONFIRMED)
                .bookedAt(LocalDateTime.now().minusHours(1))
                .build();

        appointmentRepository.saveAll(List.of(a1, a2));

        log.info("CliniQ demo data successfully initialized!");
    }
}
