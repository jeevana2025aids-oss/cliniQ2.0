package com.clinico.service;

import com.clinico.dto.PatientRequest;
import com.clinico.entity.Patient;
import com.clinico.exception.InvalidOperationException;
import com.clinico.exception.ResourceNotFoundException;
import com.clinico.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;

    @Transactional(readOnly = true)
    public List<Patient> getAllPatients(String search) {
        if (search != null && !search.trim().isEmpty()) {
            return patientRepository.findByNameContainingIgnoreCase(search.trim());
        }
        return patientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + id));
    }

    public Patient createPatient(PatientRequest request) {
        if (patientRepository.existsByEmail(request.getEmail().trim())) {
            throw new InvalidOperationException("A patient with email '" + request.getEmail() + "' already exists.");
        }

        Patient patient = Patient.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhone().trim())
                .age(request.getAge())
                .gender(request.getGender().trim())
                .build();

        return patientRepository.save(patient);
    }

    public Patient updatePatient(Long id, PatientRequest request) {
        Patient patient = getPatientById(id);

        if (patientRepository.existsByEmailAndIdNot(request.getEmail().trim(), id)) {
            throw new InvalidOperationException("Another patient with email '" + request.getEmail() + "' already exists.");
        }

        patient.setName(request.getName().trim());
        patient.setEmail(request.getEmail().trim().toLowerCase());
        patient.setPhone(request.getPhone().trim());
        patient.setAge(request.getAge());
        patient.setGender(request.getGender().trim());

        return patientRepository.save(patient);
    }

    public void deletePatient(Long id) {
        Patient patient = getPatientById(id);
        patientRepository.delete(patient);
    }
}
