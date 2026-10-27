package com.example.loanassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "anthropic")
public class AnthropicProperties {

    private String apiKey;
    private String baseUrl = "https://api.anthropic.com";
    private String model = "claude-sonnet-4-5-20250929";
    private int maxTokens = 2048;
}
