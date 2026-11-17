package com.backend.portfolio.config;

import com.backend.portfolio.filter.JwtAuthenticationFilter;
import com.backend.portfolio.service.CustomUserDetailsService;
import com.backend.portfolio.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Stream;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SecurityConfigTests.ProbeController.class)
@Import({SecurityConfig.class, ApiSecurityErrors.class, JwtAuthenticationFilter.class, SecurityConfigTests.ProbeController.class})
class SecurityConfigTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @ParameterizedTest
    @ValueSource(strings = {"/api/projects", "/api/projects/1", "/api/skills", "/api/skills/1",
            "/api/experiences", "/api/experiences/1", "/api/contact"})
    void publicReadsAndHeadRequestsDoNotRequireAuthentication(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
        mvc.perform(head(path)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("blockedPublicMutations")
    void publicReadPathsDoNotAllowAnonymousMutations(String path, HttpMethod method) throws Exception {
        mvc.perform(request(method, path)).andExpect(status().isUnauthorized());
    }

    private static Stream<Arguments> blockedPublicMutations() {
        return Stream.of("/api/projects", "/api/projects/1", "/api/skills", "/api/skills/1",
                        "/api/experiences", "/api/experiences/1", "/api/contact/1")
                .flatMap(path -> Stream.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)
                        .map(method -> Arguments.of(path, method)));
    }

    @Test
    void onlyTheExactContactSubmissionPathAllowsAnonymousPost() throws Exception {
        mvc.perform(post("/api/contact")).andExpect(status().isOk());
        mvc.perform(get("/api/contact/1")).andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.PUT, "/api/contact")).andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.DELETE, "/api/contact")).andExpect(status().isUnauthorized());
    }

    @Test
    void onlyPostIsAllowedForAnonymousLogin() throws Exception {
        mvc.perform(post("/api/auth/login")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/login")).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void adminPathsRequireAdminAuthorityForEveryMethod(String method) throws Exception {
        HttpMethod httpMethod = HttpMethod.valueOf(method);
        mvc.perform(request(httpMethod, "/api/admin/probe")).andExpect(status().isUnauthorized());
        mvc.perform(request(httpMethod, "/api/admin/probe").with(user("reader").roles("USER")))
                .andExpect(status().isForbidden());
        mvc.perform(request(httpMethod, "/api/admin/probe").with(user("owner").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void authenticatedUsersCannotBypassTheAllowlist() throws Exception {
        mvc.perform(post("/api/projects").with(user("owner").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/unlisted").with(user("owner").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/unlisted")).andExpect(status().isUnauthorized());
    }

    // Test-only handlers ensure authorization is exercised even before real content APIs exist.
    @RestController
    static class ProbeController {

        @RequestMapping({"/api/projects", "/api/projects/1", "/api/skills", "/api/skills/1",
                "/api/experiences", "/api/experiences/1", "/api/contact", "/api/contact/1",
                "/api/admin/probe", "/api/auth/login", "/unlisted"})
        String respond() {
            return "ok";
        }
    }
}
