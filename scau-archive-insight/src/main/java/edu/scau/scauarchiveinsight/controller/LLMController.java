package edu.scau.scauarchiveinsight.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import edu.scau.scauarchiveinsight.dto.R;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "LLM状态检查", description = "LLM 智能提取服务状态检查")
@RestController
@RequestMapping("/api/llm")
public class LLMController {

    @Value("${llm.api-key:}")
    private String apiKey;

    @Value("${llm.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${llm.model:deepseek-chat}")
    private String model;

    @Operation(summary = "检查 LLM 服务配置状态")
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
