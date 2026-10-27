package com.example.loanassistant.service;

import com.example.loanassistant.domain.CustomerNotification;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.repository.CustomerNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Delivers customer-facing communications as in-app notifications. In a real
 * deployment this is where an email/SMS gateway would also be invoked; the
 * message body is AI-drafted but the decision it carries is not.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final CustomerNotificationRepository notifications;

    public NotificationService(CustomerNotificationRepository notifications) {
        this.notifications = notifications;
    }

    public CustomerNotification notifyCustomer(LoanApplication app, String subject, String body) {
        CustomerNotification n = new CustomerNotification();
        n.setApplication(app);
        n.setRecipient(app.getCustomer());
        n.setSubject(subject);
        n.setBody(body);
        log.info("Notification to {} [application #{}]: {}", app.getApplicantEmail(), app.getId(), subject);
        return notifications.save(n);
    }
}
