package com.example.loanassistant.ai;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.loanassistant.config.AnthropicProperties;
import com.example.loanassistant.domain.Decision;
import com.example.loanassistant.domain.LoanApplication;
import com.example.loanassistant.domain.LoanDocument;
import com.example.loanassistant.domain.RuleEvaluation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * Advisory AI backed by the Anthropic Messages API. Strictly non-deciding:
 * every public method catches all failures and degrades to the local
 * fallback so that the rules engine — never this class — stays in control
 * of the outcome.
 */
@Service
public class ClaudeAiAnalysisService implements AiAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeAiAnalysisService.class);

    private static final String ADVISORY_SYSTEM = """
            You are an advisory assistant inside a loan-processing system. You NEVER decide \
            whether a loan is approved or declined — a deterministic rules engine and human \
            loan officers make all decisions. You must not use or infer protected personal \
            characteristics (race, religion, gender, disability, marital status, age, \
            national origin or similar) in any output.""";

    private final AnthropicProperties props;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public ClaudeAiAnalysisService(AnthropicProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("content-type", "application/json")
                .build();
    }

    @Override
    public AiAnalysis analyzeApplication(LoanApplication app, List<LoanDocument> documents) {
        if (!enabled()) {
            return FallbackAdvisory.analyze(app, documents);
        }
        try {
            List<Map<String, Object>> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", analysisPrompt(app)));
            if (documents != null) {
                for (LoanDocument doc : documents) {
                    content.addAll(documentBlocks(doc));
                }
            }
            String reply = callClaude(ADVISORY_SYSTEM, content);
            return parseAnalysis(reply);
        } catch (Exception e) {
            log.warn("Claude analysis failed for application {}; using fallback. Cause: {}",
                    app.getId(), e.toString());
            return FallbackAdvisory.analyze(app, documents);
        }
    }

    @Override
    public String explainDecision(LoanApplication app, Decision decision, List<RuleEvaluation> evaluations) {
        if (!enabled()) {
            return FallbackAdvisory.explain(decision, evaluations);
        }
        try {
            StringBuilder prompt = new StringBuilder("""
                    A loan application has already been decided by deterministic business rules \
                    or a human loan officer — not by you. Write a short, plain-language \
                    explanation of that decision for the customer (2-4 sentences plus a brief \
                    bullet list of the key factors). Do not question or change the decision. \
                    Do not invent facts not present below.
                    """);
            prompt.append("\nDecision: ").append(decision);
            prompt.append("\nApplicant: ").append(app.getApplicantName());
            prompt.append("\nRequested: ").append(app.getLoanAmount())
                  .append(" over ").append(app.getTermMonths()).append(" months");
            prompt.append("\nRule findings:");
            for (RuleEvaluation e : evaluations) {
                prompt.append("\n- [").append(e.getOutcome()).append("] ").append(e.getDetail());
            }
            String reply = callClaude(ADVISORY_SYSTEM,
                    List.of(Map.of("type", "text", "text", prompt.toString())));
            return reply != null && !reply.isBlank()
                    ? reply.strip()
                    : FallbackAdvisory.explain(decision, evaluations);
        } catch (Exception e) {
            log.warn("Claude explanation failed for application {}; using fallback. Cause: {}",
                    app.getId(), e.toString());
            return FallbackAdvisory.explain(decision, evaluations);
        }
    }

    @Override
    public String draftCustomerMessage(LoanApplication app, Decision decision, String explanation) {
        if (!enabled()) {
            return FallbackAdvisory.customerMessage(app, decision, explanation);
        }
        try {
            String prompt = """
                    Draft a brief, professional customer notification message for the loan \
                    decision below. The decision is final and was made by business rules or a \
                    loan officer — do not imply otherwise. Include the application reference \
                    and a sentence on how to ask questions. Do not use headers or markdown.
                    Decision: %s
                    Explanation: %s
                    Applicant name: %s
                    Application reference: #%d""".formatted(
                    decision, explanation, app.getApplicantName(), app.getId());
            String reply = callClaude(ADVISORY_SYSTEM,
                    List.of(Map.of("type", "text", "text", prompt)));
            return reply != null && !reply.isBlank()
                    ? reply.strip()
                    : FallbackAdvisory.customerMessage(app, decision, explanation);
        } catch (Exception e) {
            log.warn("Claude message draft failed for application {}; using fallback. Cause: {}",
                    app.getId(), e.toString());
            return FallbackAdvisory.customerMessage(app, decision, explanation);
        }
    }

    private boolean enabled() {
        return StringUtils.hasText(props.getApiKey());
    }

    private String analysisPrompt(LoanApplication app) {
        return """
                Review this loan application and attached documents. Respond with ONLY a JSON \
                object (no markdown, no commentary) of exactly this shape:
                {"summary": string, "missingInformation": [string], "inconsistencies": [string], \
                "riskFactors": [string], "extractedFields": {string: string}}
                - summary: 2-3 sentence factual summary of the application.
                - missingInformation: required items that appear absent (e.g., proof of income, ID).
                - inconsistencies: contradictions between the form data and the documents.
                - riskFactors: potential lending risks visible in the data (financial only).
                - extractedFields: key structured values you extracted from the documents.
                Application data (JSON): %s
                Attached documents follow this message.""".formatted(applicationJson(app));
    }

    private String applicationJson(LoanApplication app) {
        try {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("applicantName", app.getApplicantName());
            fields.put("annualIncome", app.getAnnualIncome());
            fields.put("monthlyDebtPayments", app.getMonthlyDebtPayments());
            fields.put("creditScore", app.getCreditScore());
            fields.put("employmentStatus", app.getEmploymentStatus());
            fields.put("employerName", app.getEmployerName());
            fields.put("monthsEmployed", app.getMonthsEmployed());
            fields.put("loanAmount", app.getLoanAmount());
            fields.put("termMonths", app.getTermMonths());
            fields.put("loanPurpose", app.getLoanPurpose());
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<Map<String, Object>> documentBlocks(LoanDocument doc) throws IOException {
        String type = doc.getContentType() == null ? "" : doc.getContentType();
        if (type.startsWith("text/") || doc.getExtractedText() != null) {
            String text = doc.getExtractedText();
            if (text == null) {
                text = Files.readString(Path.of(doc.getStoragePath()));
            }
            return List.of(Map.of("type", "text", "text",
                    "Document '%s':\n%s".formatted(doc.getOriginalFilename(), text)));
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Path.of(doc.getStoragePath()));
        } catch (IOException e) {
            log.warn("Could not read document {} for AI analysis: {}", doc.getStoragePath(), e.toString());
            return List.of();
        }
        String b64 = Base64.getEncoder().encodeToString(bytes);
        if (type.equals("application/pdf")) {
            return List.of(Map.of("type", "document",
                    "source", Map.of("type", "base64", "media_type", "application/pdf", "data", b64)));
        }
        if (type.startsWith("image/")) {
            return List.of(Map.of("type", "image",
                    "source", Map.of("type", "base64", "media_type", type, "data", b64)));
        }
        return List.of(Map.of("type", "text", "text",
                "Document '%s' attached (type %s, %d bytes; binary content not parseable)."
                        .formatted(doc.getOriginalFilename(), type, doc.getSizeBytes())));
    }

    private String callClaude(String system, List<Map<String, Object>> userContent) {
        Map<String, Object> body = Map.of(
                "model", props.getModel(),
                "max_tokens", props.getMaxTokens(),
                "system", system,
                "messages", List.of(Map.of("role", "user", "content", userContent)));

        String response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", props.getApiKey())
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            for (JsonNode block : root.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText();
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse Anthropic response: {}", e.toString());
        }
        return null;
    }

    private AiAnalysis parseAnalysis(String reply) {
        try {
            int start = reply.indexOf('{');
            int end = reply.lastIndexOf('}');
            JsonNode node = objectMapper.readTree(reply.substring(start, end + 1));
            return new AiAnalysis(
                    node.path("summary").asText(""),
                    textList(node.path("missingInformation")),
                    textList(node.path("inconsistencies")),
                    textList(node.path("riskFactors")),
                    objectMapper.convertValue(node.path("extractedFields"),
                            objectMapper.getTypeFactory()
                                    .constructMapType(LinkedHashMap.class, String.class, Object.class)),
                    props.getModel(), false);
        } catch (Exception e) {
            log.warn("Could not parse Claude analysis JSON: {}", e.toString());
            return new AiAnalysis(reply != null ? reply.strip() : "Analysis unavailable.",
                    List.of(), List.of(), List.of(), Map.of(), props.getModel(), false);
        }
    }

    private List<String> textList(JsonNode array) {
        List<String> items = new ArrayList<>();
        if (array != null && array.isArray()) {
            array.forEach(n -> items.add(n.isTextual() ? n.asText() : n.toString()));
        }
        return items;
    }
}
