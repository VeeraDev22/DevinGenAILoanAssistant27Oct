package com.example.loanassistant.repository;

import java.util.List;

import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByCustomerOrderByCreatedAtDesc(User customer);

    List<LoanApplication> findByStatusOrderByCreatedAtAsc(ApplicationStatus status);

    List<LoanApplication> findTop20ByOrderByCreatedAtDesc();
}
