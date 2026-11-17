package com.backend.portfolio.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.backend.portfolio.entity.Admin;
import com.backend.portfolio.repository.AdminRepository;
import com.backend.portfolio.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SensitiveLoggingTests {

    @Test
    void adminStringRepresentationDoesNotExposePassword() {
        Admin admin = Admin.builder().id(1L).username("owner")
                .password("test-password-marker").role("ROLE_ADMIN").version(0L).build();

        assertThat(admin.toString()).doesNotContain(admin.getPassword());
    }

    @Test
    void loadingAdminDoesNotLogPassword() {
        Admin admin = Admin.builder().id(1L).username("owner")
                .password("test-password-marker").role("ROLE_ADMIN").version(0L).build();
        AdminRepository repository = mock(AdminRepository.class);
        when(repository.findByUsername("owner")).thenReturn(Optional.of(admin));
        CustomUserDetailsService service = new CustomUserDetailsService(repository);
        Logger logger = (Logger) LoggerFactory.getLogger(CustomUserDetailsService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            UserDetails details = service.loadUserByUsername("owner");

            assertThat(details.getPassword()).isEqualTo(admin.getPassword());
            assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .allSatisfy(message -> assertThat(message).doesNotContain(admin.getPassword()));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void parsingAndValidatingJwtDoesNotLogToken() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", Base64.getEncoder().encodeToString(key));
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
        jwtUtil.init();
        Logger logger = (Logger) LoggerFactory.getLogger(JwtUtil.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            String token = jwtUtil.generateToken("owner", 0);

            assertThat(jwtUtil.extractUsername(token)).isEqualTo("owner");
            assertThat(jwtUtil.parseToken(token)).isEqualTo(new JwtUtil.TokenIdentity("owner", 0));
            assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage)
                    .allSatisfy(message -> assertThat(message).doesNotContain(token));
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
