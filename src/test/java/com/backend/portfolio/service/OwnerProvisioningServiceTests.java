package com.backend.portfolio.service;

import com.backend.portfolio.entity.Admin;
import com.backend.portfolio.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.CharBuffer;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OwnerProvisioningServiceTests {

    @Test
    void provisionsHashedCredentialsWithoutInventingAnId() {
        AdminRepository repository = mock(AdminRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        char[] password = "a-long-test-password".toCharArray();

        new OwnerProvisioningService(repository, encoder).provision("owner", password);

        ArgumentCaptor<Admin> saved = ArgumentCaptor.forClass(Admin.class);
        verify(repository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getId()).isNull();
        assertThat(saved.getValue().getRole()).isEqualTo("ROLE_ADMIN");
        assertThat(encoder.matches(CharBuffer.wrap(password), saved.getValue().getPassword())).isTrue();
        assertThat(saved.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void refusesToOverwriteAnyExistingAccount() {
        AdminRepository repository = mock(AdminRepository.class);
        when(repository.count()).thenReturn(1L);

        assertThatThrownBy(() -> new OwnerProvisioningService(repository, new BCryptPasswordEncoder(4))
                .provision("owner", "a-long-test-password".toCharArray()))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void passwordRecoveryCannotPromoteANonAdminAccount() {
        AdminRepository repository = mock(AdminRepository.class);
        when(repository.findByUsername("reader"))
                .thenReturn(Optional.of(Admin.builder().username("reader").role("ROLE_USER").build()));

        assertThatThrownBy(() -> new OwnerProvisioningService(repository, new BCryptPasswordEncoder(4))
                .resetPassword("reader", "a-long-test-password".toCharArray()))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, never()).saveAndFlush(any());
    }
}
