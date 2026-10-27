package com.example.loanassistant.web;

import com.example.loanassistant.domain.ApplicationStatus;
import com.example.loanassistant.domain.Decision;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.LoanDocument;
import com.example.loanassistant.repository.AiAnalysisResultRepository;
import com.example.loanassistant.repository.DecisionRecordRepository;
import com.example.loanassistant.repository.LoanApplicationRepository;
import com.example.loanassistant.service.DocumentStorageService;
import com.example.loanassistant.service.OfficerReviewService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/officer")
public class OfficerController {

    private final LoanApplicationRepository applications;
    private final AiAnalysisResultRepository aiResults;
    private final DecisionRecordRepository decisions;
    private final OfficerReviewService reviewService;
    private final DocumentStorageService storageService;

    public OfficerController(LoanApplicationRepository applications,
                             AiAnalysisResultRepository aiResults,
                             DecisionRecordRepository decisions,
                             OfficerReviewService reviewService,
                             DocumentStorageService storageService) {
        this.applications = applications;
        this.aiResults = aiResults;
        this.decisions = decisions;
        this.reviewService = reviewService;
        this.storageService = storageService;
    }

    @GetMapping("/queue")
    public String queue(Model model) {
        model.addAttribute("pending",
                applications.findByStatusOrderByCreatedAtAsc(ApplicationStatus.PENDING_MANUAL_REVIEW));
        model.addAttribute("recent", applications.findTop20ByOrderByCreatedAtDesc());
        return "officer/queue";
    }

    @GetMapping("/applications/{id}")
    public String review(@PathVariable Long id, Model model) {
        LoanApplication app = applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
        app.getDocuments().size();
        app.getRuleEvaluations().size();
        model.addAttribute("app", app);
        model.addAttribute("aiAnalysis", aiResults.findByApplicationId(id).orElse(null));
        model.addAttribute("decisionHistory", decisions.findByApplicationIdOrderByCreatedAtDesc(id));
        return "officer/review";
    }

    @PostMapping("/applications/{id}/decision")
    public String decide(@PathVariable Long id,
                         @RequestParam Decision decision,
                         @RequestParam String rationale,
                         Authentication auth,
                         RedirectAttributes redirect) {
        reviewService.recordDecision(id, decision, rationale, auth.getName());
        redirect.addFlashAttribute("message",
                "Application #" + id + " recorded as " + decision + ".");
        return "redirect:/officer/applications/" + id;
    }

    @GetMapping("/applications/{id}/documents/{docId}")
    public ResponseEntity<Resource> document(@PathVariable Long id, @PathVariable Long docId) {
        LoanApplication app = applications.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));
        LoanDocument doc = app.getDocuments().stream()
                .filter(d -> d.getId().equals(docId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        doc.getContentType() == null ? "application/octet-stream" : doc.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getOriginalFilename() + "\"")
                .body(storageService.load(doc));
    }
}
