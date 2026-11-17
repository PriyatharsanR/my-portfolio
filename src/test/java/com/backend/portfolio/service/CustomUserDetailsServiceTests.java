package com.backend.portfolio.service;

import com.backend.portfolio.entity.Admin;
import com.backend.portfolio.repository.AdminRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTests {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ROLE_DISABLED", "admin", "ROLE_ADMIN\n"})
    void invalidStoredAuthoritiesFailClosed(String role) {
        AdminRepository repository = mock(AdminRepository.class);
        when(repository.findByUsername("owner")).thenReturn(Optional.of(Admin.builder().username("owner")
                .password("encoded").role(role).version(0L).build()));

        assertThatThrownBy(() -> new CustomUserDetailsService(repository).loadUserByUsername("owner"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
