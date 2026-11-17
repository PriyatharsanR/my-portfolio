package com.backend.portfolio;

import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import jakarta.persistence.EntityManagerFactory;
import java.security.SecureRandom;
import java.util.Base64;
import com.backend.portfolio.entity.Admin;
import com.backend.portfolio.entity.ContactMessage;
import com.backend.portfolio.repository.AdminRepository;
import com.backend.portfolio.repository.ContactMessageRepository;
import com.backend.portfolio.service.OwnerProvisioningService;
import com.backend.portfolio.service.CustomUserDetailsService;
import com.backend.portfolio.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PortfolioApplicationTests {

    @Autowired
    private Flyway flyway;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private OwnerProvisioningService owners;

    @Autowired
    private AdminRepository admins;

    @Autowired
    private CustomUserDetailsService users;

    @Autowired
    private ContactMessageRepository messages;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwt;

    @DynamicPropertySource
    static void isolatedDatabaseProperties(DynamicPropertyRegistry properties) {
        // Never fall back to the developer's application datasource or credentials.
        String port = testEnvironment("PORTFOLIO_TEST_MYSQL_PORT");
        int parsedPort = Integer.parseInt(port);
        if (parsedPort < 1024 || parsedPort > 65535) {
            throw new IllegalArgumentException("PORTFOLIO_TEST_MYSQL_PORT must be an unprivileged local port");
        }
        String password = testEnvironment("PORTFOLIO_TEST_MYSQL_PASSWORD");
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        properties.add("spring.datasource.url", () -> testDatabaseUrl("portfolio_verification"));
        properties.add("spring.datasource.username", () -> "portfolio_test");
        properties.add("spring.datasource.password", () -> password);
        properties.add("jwt.secret", () -> Base64.getEncoder().encodeToString(key));
    }

    @Test
    void migrationsCreateSchemaCompatibleWithAllExistingEntities() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(entityManagerFactory.getMetamodel().getEntities()).hasSize(7);
        flyway.validate();
    }

    @Test
    @Transactional
    void ownerRecoveryChangesCredentialsAndInvalidatesEarlierTokenVersions() {
        owners.provision("owner", "first-test-password".toCharArray());
        Admin owner = admins.findByUsername("owner").orElseThrow();
        assertThat(owner.getId()).isPositive();
        long originalVersion = owner.getVersion();
        String oldToken = jwt.generateToken(owner.getUsername(), originalVersion);
        assertThat(passwordEncoder.matches("first-test-password", owner.getPassword())).isTrue();
        owners.resetPassword("owner", "replacement-test-password".toCharArray());

        assertThat(users.loadUserByUsername("owner").getVersion()).isGreaterThan(jwt.parseToken(oldToken).version());
        assertThat(passwordEncoder.matches("replacement-test-password", owner.getPassword())).isTrue();
    }

    @Test
    @Transactional
    void provisioningRefusesAnExistingAccount() {
        owners.provision("owner", "first-test-password".toCharArray());
        assertThatThrownBy(() -> owners.provision("second-owner", "second-test-password".toCharArray()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @Transactional
    void messagesPersistSeparatelyWithStatusAndSubmissionTime() {
        ContactMessage message = new ContactMessage();
        message.setSenderName("Test sender");
        message.setSenderEmail("sender@example.test");
        message.setBody("Test message body");
        messages.saveAndFlush(message);

        assertThat(message.getId()).isPositive();
        assertThat(message.getStatus()).isEqualTo(ContactMessage.Status.NEW);
        assertThat(message.getSubmittedAt()).isNotNull();
        assertThat(message.toString()).doesNotContain(message.getBody(), message.getSenderEmail());
    }

    @Test
    void rerunningMigrationsDoesNotChangeAnUpToDateSchema() {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

    @Test
    void nonemptyDatabaseWithoutMigrationHistoryIsNotAutomaticallyBaselined() {
        String url = testDatabaseUrl("portfolio_unmanaged_verification");
        String password = testEnvironment("PORTFOLIO_TEST_MYSQL_PASSWORD");
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "portfolio_test", password));
        jdbc.execute("CREATE TABLE IF NOT EXISTS existing_marker (id BIGINT PRIMARY KEY)");
        Flyway unmanagedDatabase = Flyway.configure().dataSource(url, "portfolio_test", password)
                .baselineOnMigrate(false).cleanDisabled(true).load();

        assertThatThrownBy(unmanagedDatabase::migrate)
                .isInstanceOf(FlywayException.class)
                .hasMessageContaining("non-empty schema");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM existing_marker", Integer.class)).isZero();
    }

    private static String testDatabaseUrl(String database) {
        return "jdbc:mysql://127.0.0.1:" + testEnvironment("PORTFOLIO_TEST_MYSQL_PORT") + "/" + database;
    }

    private static String testEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Set " + name + " for an isolated disposable MySQL test server");
        }
        return value;
    }

}
