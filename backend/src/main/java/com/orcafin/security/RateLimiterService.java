package com.orcafin.security;

import com.orcafin.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter genérico em memória, por chave (ex: "login:1.2.3.4").
 * Suficiente para uma instância única (sem Redis); reinicia a contagem
 * quando o backend reinicia.
 */
@Service
public class RateLimiterService {

    private record Attempt(AtomicInteger count, Instant windowStart) {
    }

    private final Map<String, Attempt> attemptsByKey = new ConcurrentHashMap<>();

    public void checkAllowed(String key, int maxAttempts, Duration window, String message) {
        Attempt attempt = attemptsByKey.get(key);
        if (attempt == null) {
            return;
        }
        if (Instant.now().isAfter(attempt.windowStart().plus(window))) {
            attemptsByKey.remove(key);
            return;
        }
        if (attempt.count().get() >= maxAttempts) {
            throw new TooManyRequestsException(message);
        }
    }

    public void recordAttempt(String key, Duration window) {
        attemptsByKey.compute(key, (k, existing) -> {
            if (existing == null || Instant.now().isAfter(existing.windowStart().plus(window))) {
                return new Attempt(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void reset(String key) {
        attemptsByKey.remove(key);
    }
}
