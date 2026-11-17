package com.backend.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

@Component
public class LoginAttemptLimiter {

    private static final int MAX_CLIENTS = 10_000;
    private final int maxAttempts;
    private final long windowMillis;
    private final Clock clock;
    private final Map<String, Integer> attempts = new HashMap<>();
    private long windowStarted;

    @Autowired
    public LoginAttemptLimiter(@Value("${security.login.max-attempts:10}") int maxAttempts,
                               @Value("${security.login.window-seconds:60}") int windowSeconds) {
        this(maxAttempts, windowSeconds, Clock.systemUTC());
    }

    LoginAttemptLimiter(int maxAttempts, int windowSeconds, Clock clock) {
        if (maxAttempts <= 0 || windowSeconds <= 0) {
            throw new IllegalArgumentException("Login limits must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowSeconds * 1000L;
        this.clock = clock;
        this.windowStarted = clock.millis();
    }

    public synchronized void check(String address) {
        long now = clock.millis();
        if (now < windowStarted || now - windowStarted >= windowMillis) {
            attempts.clear();
            windowStarted = now;
        }
        String client = address == null ? "unknown" : address;
        int count = attempts.getOrDefault(client, 0);
        if (count >= maxAttempts || (count == 0 && attempts.size() >= MAX_CLIENTS)) {
            throw new LimitExceeded(Math.max(1, (windowMillis - (now - windowStarted) + 999) / 1000));
        }
        attempts.put(client, count + 1);
    }

    public static class LimitExceeded extends RuntimeException {
        private final long retryAfter;

        public LimitExceeded(long retryAfter) {
            super("Login rate limit exceeded");
            this.retryAfter = retryAfter;
        }

        public long getRetryAfter() {
            return retryAfter;
        }
    }
}
