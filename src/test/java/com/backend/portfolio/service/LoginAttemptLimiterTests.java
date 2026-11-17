package com.backend.portfolio.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoginAttemptLimiterTests {

    @Test
    void throttlesOneClientAndAllowsRetriesAfterTheWindowExpires() {
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(0L);
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(2, 60, clock);
        limiter.check("client-one");
        limiter.check("client-one");

        assertThatThrownBy(() -> limiter.check("client-one"))
                .isInstanceOf(LoginAttemptLimiter.LimitExceeded.class)
                .satisfies(error -> org.assertj.core.api.Assertions.assertThat(
                        ((LoginAttemptLimiter.LimitExceeded) error).getRetryAfter()).isEqualTo(60));
        assertThatCode(() -> limiter.check("client-two")).doesNotThrowAnyException();

        when(clock.millis()).thenReturn(60_000L);
        assertThatCode(() -> limiter.check("client-one")).doesNotThrowAnyException();
    }

    @Test
    void capsClientStateInsteadOfGrowingWithoutBound() {
        LoginAttemptLimiter limiter = new LoginAttemptLimiter(1, 60,
                Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
        for (int client = 0; client < 10_000; client++) {
            limiter.check("client-" + client);
        }
        assertThatThrownBy(() -> limiter.check("extra-client"))
                .isInstanceOf(LoginAttemptLimiter.LimitExceeded.class);
    }
}
