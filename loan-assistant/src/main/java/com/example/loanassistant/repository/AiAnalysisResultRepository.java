package com.example.loanassistant.repository;

import java.util.Optional;

import com.example.loanassistant.domain.AiAnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisResultRepository extends JpaRepository<AiAnalysisResult, Long> {

    Optional<AiAnalysisResult> findByApplicationId(Long applicationId);
}
