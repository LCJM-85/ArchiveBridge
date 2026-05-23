package edu.scau.scauarchiveinsight.config;

import org.springframework.stereotype.Component;

@Component
public class DesensitizeConfig {
    private volatile boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
