package com.example.loanassistant.repository;

import java.util.List;

import com.example.loanassistant.domain.RuleEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleEvaluationRepository extends JpaRepository<RuleEvaluation, Long> {

    List<RuleEvaluation> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
