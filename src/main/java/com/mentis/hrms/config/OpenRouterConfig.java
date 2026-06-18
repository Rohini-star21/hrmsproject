package com.mentis.hrms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class OpenRouterConfig {

    @Value("${groq.api.key}")
    private String apiKey;

    @Bean
    public RestTemplate openRouterRestTemplate() {
        return new RestTemplate();
    }

    // Optional: You can also create a configured bean with default headers
    @Bean
    public RestTemplate groqRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // You can add interceptors for Authorization if needed later
        return restTemplate;
    }
}