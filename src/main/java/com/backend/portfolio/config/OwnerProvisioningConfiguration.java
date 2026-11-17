package com.backend.portfolio.config;

import com.backend.portfolio.entity.Admin;
import com.backend.portfolio.repository.AdminRepository;
import com.backend.portfolio.service.OwnerProvisioningService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@Profile("owner-command")
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@EntityScan(basePackageClasses = Admin.class)
@EnableJpaRepositories(basePackageClasses = AdminRepository.class)
@Import(OwnerProvisioningService.class)
public class OwnerProvisioningConfiguration {

    @Bean
    public PasswordEncoder ownerPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
