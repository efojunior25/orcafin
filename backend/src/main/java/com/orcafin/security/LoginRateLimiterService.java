package com.orcafin.security;

import com.orcafin.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter simples em memória para o endpoint de login, por IP.
 * Suficiente para uma instância única (sem Redis); reinicia a contagem
 * quando o backend reinicia.
 */
@Service
public class LoginRateLimiterService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofMinutes(15);

    private record Attempt(AtomicInteger count, Instant windowStart) {
    }

    private final Map<String, Attempt> attemptsByIp = new ConcurrentHashMap<>();

    public void checkAllowed(String ip) {
        Attempt attempt = attemptsByIp.get(ip);
        if (attempt == null) {
            return;
        }
        if (Instant.now().isAfter(attempt.windowStart().plus(WINDOW))) {
            attemptsByIp.remove(ip);
            return;
        }
        if (attempt.count().get() >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException(
                    "Muitas tentativas de login. Tente novamente em alguns minutos.");
        }
    }

    public void recordFailure(String ip) {
        attemptsByIp.compute(ip, (key, existing) -> {
            if (existing == null || Instant.now().isAfter(existing.windowStart().plus(WINDOW))) {
                return new Attempt(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void recordSuccess(String ip) {
        attemptsByIp.remove(ip);
    }
}
