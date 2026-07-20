package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.dto.R;
import edu.scau.scauarchiveinsight.service.AIAssistantClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @Operation(summary = "AI 对话（流式）")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody Map<String, Object> body) {
        String question = (String) body.getOrDefault("question", "");
        if (question.isBlank()) {
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().data("{\"type\":\"error\",\"content\":\"请输入问题\"}"));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        List<Map<String, String>> history = (List<Map<String, String>>) body.getOrDefault("history", List.of());
        SseEmitter emitter = new SseEmitter(180_000L);
        aiAssistantClient.chatStream(question, history, emitter);
        return emitter;
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
