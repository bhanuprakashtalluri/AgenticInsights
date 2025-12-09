package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DevModeService {
    private final AtomicBoolean enabled;

    public DevModeService(@Value("${app.dev.enabled:false}") boolean initial) {
        this.enabled = new AtomicBoolean(initial);
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public boolean setEnabled(boolean value) {
        enabled.set(value);
        return value;
    }

    public String getMode() {
        return enabled.get() ? "DEV_ENABLED" : "DEV_DISABLED";
    }
}

