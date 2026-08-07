package com.orcafin.security;

import com.orcafin.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterServiceTest {

    private static final Duration WINDOW = Duration.ofMinutes(15);

    @Test
    void allowsUpToLimitThenBlocks() {
        RateLimiterService limiter = new RateLimiterService();
        String key = "login:1.2.3.4";

        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> limiter.checkAllowed(key, 5, WINDOW, "bloqueado")).doesNotThrowAnyException();
            limiter.recordAttempt(key, WINDOW);
        }

        assertThatThrownBy(() -> limiter.checkAllowed(key, 5, WINDOW, "bloqueado"))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessage("bloqueado");
    }

    @Test
    void resetClearsCounter() {
        RateLimiterService limiter = new RateLimiterService();
        String key = "login:5.6.7.8";

        for (int i = 0; i < 4; i++) {
            limiter.recordAttempt(key, WINDOW);
        }
        limiter.reset(key);

        assertThatCode(() -> limiter.checkAllowed(key, 5, WINDOW, "bloqueado")).doesNotThrowAnyException();
    }

    @Test
    void differentKeysAreIndependent() {
        RateLimiterService limiter = new RateLimiterService();

        for (int i = 0; i < 3; i++) {
            limiter.recordAttempt("register:1.1.1.1", WINDOW);
        }

        assertThatThrownBy(() -> limiter.checkAllowed("register:1.1.1.1", 3, WINDOW, "bloqueado"))
                .isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> limiter.checkAllowed("register:2.2.2.2", 3, WINDOW, "bloqueado"))
                .doesNotThrowAnyException();
        assertThatCode(() -> limiter.checkAllowed("login:1.1.1.1", 5, WINDOW, "bloqueado"))
                .doesNotThrowAnyException();
    }
}
