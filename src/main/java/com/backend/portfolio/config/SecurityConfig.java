package com.backend.portfolio.config;

import com.backend.portfolio.filter.JwtAuthenticationFilter;
import com.backend.portfolio.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtAuthenticationFilter jwtFilter;
        private final CustomUserDetailsService userDetailsService;
        private final ApiSecurityErrors securityErrors;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(
                                                session -> session.sessionCreationPolicy(
                                                                SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/contact")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.GET, "/api/projects/**",
                                                                "/api/skills/**",
                                                                "/api/experiences/**",
                                                                "/api/contact")
                                                .permitAll()

                                                .requestMatchers(HttpMethod.HEAD, "/api/projects/**",
                                                                "/api/skills/**",
                                                                "/api/experiences/**",
                                                                "/api/contact")
                                                .permitAll()

                                                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
                                                .anyRequest().denyAll())

                                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(securityErrors)
                                                .accessDeniedHandler(securityErrors))

                                .userDetailsService(userDetailsService)
                                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(JwtAuthenticationFilter filter) {
                FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
                registration.setEnabled(false);
                return registration;
        }
}
