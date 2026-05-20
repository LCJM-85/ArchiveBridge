package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/llm")
public class LLMController {

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${llm.model:deepseek-chat}")
    private String model;

    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        boolean configured = apiKey != null && !apiKey.isBlank();
        return R.ok(Map.of(
                "configured", configured,
                "baseUrl", baseUrl,
                "model", configured ? model : ""
        ));
    }
}
