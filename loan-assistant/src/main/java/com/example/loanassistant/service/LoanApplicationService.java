package com.example.loanassistant.service;

import java.util.List;

import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.LoanDocument;
import com.example.loanassistant.domain.User;
import com.example.loanassistant.repository.CustomerNotificationRepository;
import com.example.loanassistant.repository.LoanApplicationRepository;
import com.example.loanassistant.repository.UserRepository;
import com.example.loanassistant.web.dto.ApplicationForm;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository applications;
    private final UserRepository users;
    private final DocumentStorageService storage;
    private final CustomerNotificationRepository notifications;
    private final NotificationService notificationService;

    public LoanApplicationService(LoanApplicationRepository applications,
                                  UserRepository users,
                                  DocumentStorageService storage,
                                  CustomerNotificationRepository notifications,
                                  NotificationService notificationService) {
        this.applications = applications;
        this.users = users;
        this.storage = storage;
        this.notifications = notifications;
        this.notificationService = notificationService;
    }

    @Transactional
    public LoanApplication submit(ApplicationForm form, List<MultipartFile> files, String username) {
        User customer = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Unknown user " + username));

        LoanApplication app = new LoanApplication();
        app.setCustomer(customer);
        app.setApplicantName(form.getApplicantName());
        app.setApplicantEmail(form.getApplicantEmail());
        app.setAnnualIncome(form.getAnnualIncome());
        app.setMonthlyDebtPayments(form.getMonthlyDebtPayments());
        app.setCreditScore(form.getCreditScore());
        app.setEmploymentStatus(form.getEmploymentStatus());
        app.setEmployerName(form.getEmployerName());
        app.setMonthsEmployed(form.getMonthsEmployed());
        app.setLoanAmount(form.getLoanAmount());
        app.setTermMonths(form.getTermMonths());
        app.setLoanPurpose(form.getLoanPurpose());
        app.setStatus(ApplicationStatus.SUBMITTED);
        applications.save(app);

        List<LoanDocument> docs = storage.store(app, files);
        docs.forEach(doc -> app.getDocuments().add(doc));

        notificationService.notifyCustomer(app,
                "Loan application received",
                "Your loan application #%d has been received and is being processed."
                        .formatted(app.getId()));
        return app;
    }

    @Transactional(readOnly = true)
    public List<LoanApplication> forCustomer(String username) {
        User customer = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Unknown user " + username));
        return applications.findByCustomerOrderByCreatedAtDesc(customer);
    }

    @Transactional(readOnly = true)
    public LoanApplication getForCustomer(Long id, String username) {
        LoanApplication app = applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
        if (!app.getCustomer().getUsername().equals(username)) {
            throw new AccessDeniedException("Not your application");
        }
        app.getDocuments().size();
        app.getDecisions().size();
        return app;
    }

    @Transactional(readOnly = true)
    public List<com.example.loanassistant.domain.CustomerNotification> notificationsFor(String username) {
        User customer = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Unknown user " + username));
        return notifications.findByRecipientOrderBySentAtDesc(customer);
    }
}
