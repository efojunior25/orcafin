package com.orcafin.security;

import com.orcafin.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterServiceTest {

    @Test
    void allowsUpToFiveFailuresThenBlocks() {
        LoginRateLimiterService limiter = new LoginRateLimiterService();
        String ip = "1.2.3.4";

        for (int i = 0; i < 5; i++) {
            assertThatCode(() -> limiter.checkAllowed(ip)).doesNotThrowAnyException();
            limiter.recordFailure(ip);
        }

        assertThatThrownBy(() -> limiter.checkAllowed(ip))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void successResetsCounter() {
        LoginRateLimiterService limiter = new LoginRateLimiterService();
        String ip = "5.6.7.8";

        for (int i = 0; i < 4; i++) {
            limiter.recordFailure(ip);
        }
        limiter.recordSuccess(ip);

        assertThatCode(() -> limiter.checkAllowed(ip)).doesNotThrowAnyException();
    }

    @Test
    void differentIpsAreTrackedIndependently() {
        LoginRateLimiterService limiter = new LoginRateLimiterService();

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("1.1.1.1");
        }

        assertThatThrownBy(() -> limiter.checkAllowed("1.1.1.1"))
                .isInstanceOf(TooManyRequestsException.class);
        assertThatCode(() -> limiter.checkAllowed("2.2.2.2")).doesNotThrowAnyException();
    }
}
