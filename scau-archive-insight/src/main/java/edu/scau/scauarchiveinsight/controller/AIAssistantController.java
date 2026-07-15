package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.AIAssistantClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AI 助手", description = "AI 智能数据问答")
@RestController
@RequestMapping("/api/ai")
public class AIAssistantController {

    @Autowired
    private AIAssistantClient aiAssistantClient;

    @Operation(summary = "AI 对话")
    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String question = (String) body.getOrDefault("question", "");
        if (question.isBlank()) {
            return R.error(400, "请输入问题");
        }

        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", List.of());

        String answer = aiAssistantClient.chat(question, history);
        if (answer == null) {
            return R.error(503, "AI 助手服务暂不可用，请稍后再试");
        }

        return R.ok(Map.of("answer", answer));
    }

    @Operation(summary = "AI 报告分析解读")
    @PostMapping("/analyze-report")
    public R<Map<String, Object>> analyzeReport(@RequestBody Map<String, Object> body) {
        Map<String, Object> reportData = (Map<String, Object>) body.get("reportData");
        if (reportData == null) {
            return R.error(400, "请提供报告数据");
        }

        String analysis = aiAssistantClient.analyzeReport(reportData);
        if (analysis.isBlank()) {
            return R.error(503, "AI 分析服务暂不可用");
        }

        return R.ok(Map.of("analysis", analysis));
    }

    @Operation(summary = "检查 AI 服务状态")
    @GetMapping("/status")
    public R<Map<String, Object>> status() {
        boolean available = aiAssistantClient.isAvailable();
        return R.ok(Map.of("available", available));
    }
}
