package com.example.loanassistant.repository;

import java.util.List;
import java.util.Optional;

import com.example.loanassistant.domain.DecisionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionRecordRepository extends JpaRepository<DecisionRecord, Long> {

    List<DecisionRecord> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    Optional<DecisionRecord> findTopByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
