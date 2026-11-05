package com.clinic.service;

import com.clinic.dto.request.PrescriptionRequest;
import com.clinic.dto.response.PrescriptionResponse;
import com.clinic.entity.Appointment;
import com.clinic.entity.Medicine;
import com.clinic.entity.Prescription;
import com.clinic.entity.enums.AppointmentStatus;
import com.clinic.exception.DuplicateResourceException;
import com.clinic.exception.InvalidStateTransitionException;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.PatientRepository;
import com.clinic.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    @Transactional
    public PrescriptionResponse writePrescription(PrescriptionRequest request) {
        return writePrescription(request, "DOCTOR");
    }

    @Transactional
    public PrescriptionResponse writePrescription(PrescriptionRequest request, String doctorEmail) {

        Appointment appointment = appointmentRepository.findById(request.getAppointmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Appointment not found: " + request.getAppointmentId()));

        // Business rule: only write prescription for completed appointments
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new InvalidStateTransitionException(
                "Prescription can only be written for COMPLETED appointments. " +
                "Current status: " + appointment.getStatus());
        }

        // Prevent duplicate prescriptions
        if (prescriptionRepository.existsByAppointmentId(request.getAppointmentId())) {
            throw new DuplicateResourceException("A prescription already exists for this appointment.");
        }

        // Build prescription
        Prescription prescription = Prescription.builder()
                .appointment(appointment)
                .diagnosis(request.getDiagnosis())
                .notes(request.getNotes())
                .followUpDate(request.getFollowUpDate())
                .build();

        // Map medicines from request → entity
        if (request.getMedicines() != null) {
            List<Medicine> medicines = request.getMedicines().stream()
                    .map(m -> Medicine.builder()
                            .name(m.getName())
                            .dosage(m.getDosage())
                            .frequency(m.getFrequency())
                            .durationDays(m.getDurationDays())
                            .instructions(m.getInstructions())
                            .prescription(prescription) // set back-reference
                            .build())
                    .collect(Collectors.toList());
            prescription.setMedicines(medicines);
        }

        Prescription saved = prescriptionRepository.save(prescription);

        // Medical record audit trail
        String medicineSummary = saved.getMedicines() == null ? "none" :
                saved.getMedicines().stream()
                        .map(m -> m.getName() + " (" + m.getDosage() + ")")
                        .collect(Collectors.joining(", "));
        auditService.recordChange(
                "PRESCRIPTION",
                saved.getId(),
                "CREATE_PRESCRIPTION",
                doctorEmail,
                null,
                "Diagnosis: " + saved.getDiagnosis() + "; Meds: " + medicineSummary,
                "Prescription created for appointment " + appointment.getId() + " (Patient ID: "
                        + appointment.getPatient().getId() + ")"
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionByAppointment(Long appointmentId) {
        Prescription prescription = prescriptionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "No prescription found for appointment: " + appointmentId));
        return toResponse(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPatientHistory(Long patientId) {
        patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));

        return prescriptionRepository.findAllByPatientId(patientId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getMyPrescription(Long appointmentId, String email) {
        Prescription prescription = prescriptionRepository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found"));

        // Security check: verify this prescription belongs to the requesting patient
        String prescriptionOwnerEmail = prescription.getAppointment().getPatient().getUser().getEmail();
        if (!prescriptionOwnerEmail.equals(email)) {
            throw new AccessDeniedException("Access denied: This prescription does not belong to you.");
        }

        return toResponse(prescription);
    }

    private PrescriptionResponse toResponse(Prescription p) {
        List<PrescriptionResponse.MedicineResponse> medicines =
                p.getMedicines() == null ? Collections.emptyList() :
                p.getMedicines().stream()
                        .map(m -> PrescriptionResponse.MedicineResponse.builder()
                                .id(m.getId())
                                .name(m.getName())
                                .dosage(m.getDosage())
                                .frequency(m.getFrequency())
                                .durationDays(m.getDurationDays())
                                .instructions(m.getInstructions())
                                .build())
                        .collect(Collectors.toList());

        return PrescriptionResponse.builder()
                .id(p.getId())
                .appointmentId(p.getAppointment().getId())
                .patientName(p.getAppointment().getPatient().getName())
                .diagnosis(p.getDiagnosis())
                .notes(p.getNotes())
                .followUpDate(p.getFollowUpDate())
                .appointmentTime(p.getAppointment().getAppointmentTime())
                .medicines(medicines)
                .createdAt(p.getCreatedAt())
                .build();
    }
}
