package com.clinic.service;

import com.clinic.dto.request.MedicalNotesRequest;
import com.clinic.dto.request.PatientRequest;
import com.clinic.dto.response.PatientResponse;
import com.clinic.entity.Patient;
import com.clinic.entity.User;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public PatientResponse createProfile(String email, PatientRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        // Prevent creating duplicate patient profile for same user (409 Conflict)
        if (patientRepository.findByUserId(user.getId()).isPresent()) {
            throw new DuplicateResourceException("Patient profile already exists for this user");
        }

        Patient patient = Patient.builder()
                .name(request.getName() != null ? request.getName() : user.getName())
                .dateOfBirth(request.getDateOfBirth())
                .phone(request.getPhone())
                .gender(request.getGender())
                .address(request.getAddress())
                .bloodGroup(request.getBloodGroup())
                .allergies(blankToNull(request.getAllergies()))
                .medicalHistory(blankToNull(request.getMedicalHistory()))
                .user(user)
                .build();

        Patient saved = patientRepository.save(patient);

        // Audit initial medical notes if provided during registration
        if (saved.getAllergies() != null || saved.getMedicalHistory() != null) {
            auditService.recordChange(
                    "PATIENT_NOTES",
                    saved.getId(),
                    "CREATE_NOTES",
                    email,
                    null,
                    "Allergies: [" + saved.getAllergies() + "], History: [" + saved.getMedicalHistory() + "]",
                    "Initial profile notes recorded for patient " + saved.getName()
            );
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PatientResponse getMyProfile(String email) {
        Patient patient = patientRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for: " + email));
        return toResponse(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse getById(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
        return toResponse(patient);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> searchPatients(String query, Pageable pageable) {
        return patientRepository.searchByNameOrPhone(query.trim(), pageable).map(this::toResponse);
    }

    @Transactional
    public PatientResponse updateProfile(String email, PatientRequest request) {
        Patient patient = patientRepository.findByUserEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found"));

        String oldAllergies = patient.getAllergies();
        String oldHistory = patient.getMedicalHistory();

        if (request.getName() != null)        patient.setName(request.getName());
        if (request.getDateOfBirth() != null) patient.setDateOfBirth(request.getDateOfBirth());
        if (request.getPhone() != null)       patient.setPhone(request.getPhone());
        if (request.getGender() != null)      patient.setGender(request.getGender());
        if (request.getAddress() != null)     patient.setAddress(request.getAddress());
        if (request.getBloodGroup() != null)  patient.setBloodGroup(request.getBloodGroup());
        if (request.getAllergies() != null)   patient.setAllergies(blankToNull(request.getAllergies()));
        if (request.getMedicalHistory() != null) patient.setMedicalHistory(blankToNull(request.getMedicalHistory()));

        Patient saved = patientRepository.save(patient);

        // Audit medical notes when updated through the patient profile write path
        auditMedicalNotesIfChanged(saved, oldAllergies, oldHistory, saved.getAllergies(), saved.getMedicalHistory(),
                email, "Patient profile updated");

        return toResponse(saved);
    }

    /** Doctor updates the allergies / medical history of any patient. Blank text clears the field. */
    @Transactional
    public PatientResponse updateMedicalNotes(Long id, MedicalNotesRequest request) {
        return updateMedicalNotes(id, request, "DOCTOR");
    }

    @Transactional
    public PatientResponse updateMedicalNotes(Long id, MedicalNotesRequest request, String changedBy) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));

        String oldAllergies = patient.getAllergies();
        String oldHistory = patient.getMedicalHistory();
        String newAllergies = blankToNull(request.allergies());
        String newHistory = blankToNull(request.medicalHistory());

        patient.setAllergies(newAllergies);
        patient.setMedicalHistory(newHistory);
        Patient saved = patientRepository.save(patient);

        // Centralized audit logging for clinician update path
        auditMedicalNotesIfChanged(saved, oldAllergies, oldHistory, newAllergies, newHistory,
                changedBy, "Clinician medical notes updated");

        return toResponse(saved);
    }

    private void auditMedicalNotesIfChanged(Patient patient, String oldAllergies, String oldHistory,
                                           String newAllergies, String newHistory, String actor, String context) {
        boolean allergiesChanged = !Objects.equals(oldAllergies, newAllergies);
        boolean historyChanged = !Objects.equals(oldHistory, newHistory);

        if (allergiesChanged || historyChanged) {
            auditService.recordChange(
                    "PATIENT_NOTES",
                    patient.getId(),
                    "UPDATE_NOTES",
                    actor,
                    "Allergies: [" + oldAllergies + "], History: [" + oldHistory + "]",
                    "Allergies: [" + newAllergies + "], History: [" + newHistory + "]",
                    context + " for patient " + patient.getName()
            );
        }
    }

    private String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private PatientResponse toResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .name(patient.getName())
                .dateOfBirth(patient.getDateOfBirth())
                .phone(patient.getPhone())
                .gender(patient.getGender())
                .address(patient.getAddress())
                .bloodGroup(patient.getBloodGroup())
                .allergies(patient.getAllergies())
                .medicalHistory(patient.getMedicalHistory())
                .email(patient.getUser().getEmail())
                .registeredAt(patient.getRegisteredAt())
                .build();
    }
}
