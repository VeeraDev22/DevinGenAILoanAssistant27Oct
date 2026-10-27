package com.example.loanassistant.ai;

import java.util.List;
import java.util.Map;

/**
 * Advisory-only AI output. Never contains a decision — by design there is no
 * field that could express one.
 */
public record AiAnalysis(
        String summary,
        List<String> missingInformation,
        List<String> inconsistencies,
        List<String> riskFactors,
        Map<String, Object> extractedFields,
        String modelUsed,
        boolean fallbackUsed) {
}
