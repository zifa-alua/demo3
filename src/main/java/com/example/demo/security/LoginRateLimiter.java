package com.example.demo.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private static final class Window {
        int count;
        Instant resetAt;
    }

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allow(String key) {
        Instant now = Instant.now();
        Window w = windows.computeIfAbsent(key, k -> {
            Window nw = new Window();
            nw.resetAt = now.plusSeconds(WINDOW_SECONDS);
            return nw;
        });
        synchronized (w) {
            if (now.isAfter(w.resetAt)) {
                w.count = 0;
                w.resetAt = now.plusSeconds(WINDOW_SECONDS);
            }
            if (w.count >= MAX_ATTEMPTS) {
                return false;
            }
            w.count++;
            return true;
        }
    }
}