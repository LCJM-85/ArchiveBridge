package edu.scau.scauarchiveinsight.util;

import edu.scau.scauarchiveinsight.config.DesensitizeConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DesensitizeContext {

    private static DesensitizeContext instance;
    private final DesensitizeConfig config;

    public DesensitizeContext(DesensitizeConfig config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        instance = this;
    }

    public static boolean isEnabled() {
        return instance != null && instance.config.isEnabled();
    }
}
