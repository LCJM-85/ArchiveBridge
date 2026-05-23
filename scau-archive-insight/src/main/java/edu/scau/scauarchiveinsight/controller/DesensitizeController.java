package edu.scau.scauarchiveinsight.controller;

import edu.scau.scauarchiveinsight.config.DesensitizeConfig;
import edu.scau.scauarchiveinsight.dto.R;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/desensitize")
public class DesensitizeController {

    private final DesensitizeConfig config;

    public DesensitizeController(DesensitizeConfig config) {
        this.config = config;
    }

    @GetMapping("/status")
    public R<Boolean> status() {
        return R.ok(config.isEnabled());
    }

    @PostMapping("/toggle")
    public R<Boolean> toggle(@RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", false);
        config.setEnabled(enabled);
        return R.ok(enabled);
    }
}
