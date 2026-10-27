package com.example.loanassistant.repository;

import com.example.loanassistant.domain.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {
}
