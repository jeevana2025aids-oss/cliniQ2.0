package com.clinico.service;

import com.clinico.dto.DoctorRequest;
import com.clinico.entity.Doctor;
import com.clinico.exception.InvalidOperationException;
import com.clinico.exception.ResourceNotFoundException;
import com.clinico.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorService {

    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public List<Doctor> getAllDoctors(String search, String specialization) {
        if (specialization != null && !specialization.trim().isEmpty()) {
            return doctorRepository.findBySpecializationContainingIgnoreCase(specialization.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            return doctorRepository.findByNameContainingIgnoreCase(search.trim());
        }
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with ID: " + id));
    }

    public Doctor createDoctor(DoctorRequest request) {
        if (doctorRepository.existsByEmail(request.getEmail().trim())) {
            throw new InvalidOperationException("A doctor with email '" + request.getEmail() + "' already exists.");
        }

        Doctor doctor = Doctor.builder()
                .name(request.getName().trim())
                .specialization(request.getSpecialization().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .build();

        return doctorRepository.save(doctor);
    }

    public Doctor updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = getDoctorById(id);

        if (doctorRepository.existsByEmailAndIdNot(request.getEmail().trim(), id)) {
            throw new InvalidOperationException("Another doctor with email '" + request.getEmail() + "' already exists.");
        }

        doctor.setName(request.getName().trim());
        doctor.setSpecialization(request.getSpecialization().trim());
        doctor.setEmail(request.getEmail().trim().toLowerCase());
        doctor.setPhone(request.getPhone().trim());

        return doctorRepository.save(doctor);
    }

    public void deleteDoctor(Long id) {
        Doctor doctor = getDoctorById(id);
        doctorRepository.delete(doctor);
    }
}
