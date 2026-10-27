package com.example.loanassistant.web;

import java.util.Arrays;
import java.util.List;

import com.example.loanassistant.domain.EmploymentStatus;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.LoanDocument;
import com.example.loanassistant.service.ApplicationProcessingService;
import com.example.loanassistant.service.DocumentStorageService;
import com.example.loanassistant.service.LoanApplicationService;
import com.example.loanassistant.web.dto.ApplicationForm;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoanApplicationController {

    private final LoanApplicationService applicationService;
    private final ApplicationProcessingService processingService;
    private final DocumentStorageService storageService;

    public LoanApplicationController(LoanApplicationService applicationService,
                                     ApplicationProcessingService processingService,
                                     DocumentStorageService storageService) {
        this.applicationService = applicationService;
        this.processingService = processingService;
        this.storageService = storageService;
    }

    @GetMapping("/apply")
    public String applyForm(Model model) {
        model.addAttribute("form", new ApplicationForm());
        model.addAttribute("employmentStatuses", EmploymentStatus.values());
        return "apply";
    }

    @PostMapping("/apply")
    public String submit(@Valid @ModelAttribute("form") ApplicationForm form,
                         BindingResult binding,
                         @RequestParam(value = "files", required = false) MultipartFile[] files,
                         Authentication auth,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("employmentStatuses", EmploymentStatus.values());
            return "apply";
        }
        List<MultipartFile> fileList = files == null ? List.of() : Arrays.asList(files);
        LoanApplication app = applicationService.submit(form, fileList, auth.getName());
        processingService.process(app.getId());
        redirect.addFlashAttribute("message", "Application #" + app.getId() + " submitted.");
        return "redirect:/applications/" + app.getId();
    }

    @GetMapping("/applications")
    public String list(Authentication auth, Model model) {
        model.addAttribute("applications", applicationService.forCustomer(auth.getName()));
        model.addAttribute("notifications", applicationService.notificationsFor(auth.getName()));
        return "applications";
    }

    @GetMapping("/applications/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        model.addAttribute("app", applicationService.getForCustomer(id, auth.getName()));
        return "application-detail";
    }

    @GetMapping("/applications/{id}/documents/{docId}")
    public ResponseEntity<Resource> document(@PathVariable Long id, @PathVariable Long docId,
                                             Authentication auth) {
        LoanApplication app = applicationService.getForCustomer(id, auth.getName());
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
