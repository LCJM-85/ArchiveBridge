package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.config.DesensitizeConfig;
import edu.scau.scauarchiveinsight.dto.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/desensitize")
@Tag(name = "脱敏控制", description = "数据脱敏开关控制")
public class DesensitizeController {

    private final DesensitizeConfig config;

    public DesensitizeController(DesensitizeConfig config) {
        this.config = config;
    }

    @GetMapping("/status")
    @Operation(summary = "获取脱敏状态")
    public R<Boolean> status() {
        return R.ok(config.isEnabled());
    }

    @PostMapping("/toggle")
    @Operation(summary = "切换脱敏状态")
    public R<Boolean> toggle(@RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", false);
        config.setEnabled(enabled);
        return R.ok(enabled);
    }
}
