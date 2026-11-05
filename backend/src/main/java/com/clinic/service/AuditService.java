package com.clinic.service;

import com.clinic.entity.MedicalRecordAudit;
import com.clinic.repository.MedicalRecordAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final MedicalRecordAuditRepository auditRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordChange(String entityName, Long entityId, String action, String performedBy,
                             String oldValue, String newValue, String details) {
        MedicalRecordAudit record = MedicalRecordAudit.builder()
                .entityName(entityName)
                .entityId(entityId)
                .action(action)
                .performedBy(performedBy != null ? performedBy : "SYSTEM")
                .oldValue(oldValue)
                .newValue(newValue)
                .details(details)
                .performedAt(LocalDateTime.now(clock))
                .build();

        auditRepository.save(record);
        log.info("Audit logged: [{}] on {} id={} by {}", action, entityName, entityId, performedBy);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordAudit> getAuditsForEntity(String entityName, Long entityId) {
        return auditRepository.findByEntityNameAndEntityIdOrderByPerformedAtDesc(entityName, entityId);
    }
}
