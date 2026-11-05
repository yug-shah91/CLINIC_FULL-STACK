package com.clinic.repository;

import com.clinic.entity.MedicalRecordAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordAuditRepository extends JpaRepository<MedicalRecordAudit, Long> {

    List<MedicalRecordAudit> findByEntityNameAndEntityIdOrderByPerformedAtDesc(String entityName, Long entityId);

    List<MedicalRecordAudit> findByPerformedByOrderByPerformedAtDesc(String performedBy);
}
