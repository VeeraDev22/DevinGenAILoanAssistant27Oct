package com.example.loanassistant.repository;

import java.util.List;

import com.example.loanassistant.domain.CustomerNotification;
import com.example.loanassistant.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, Long> {

    List<CustomerNotification> findByRecipientOrderBySentAtDesc(User recipient);
}
