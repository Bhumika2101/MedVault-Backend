package com.medvault.repository;

import com.medvault.model.MedicalRecord;
import com.medvault.model.enums.RecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {
    List<MedicalRecord> findByPatientIdAndIsDeletedFalseOrderByRecordDateDesc(Long patientId);
    List<MedicalRecord> findByPatientIdAndRecordTypeAndIsDeletedFalseOrderByRecordDateDesc(Long patientId, RecordType recordType);
    Long countByPatientIdAndIsDeletedFalse(Long patientId);
}