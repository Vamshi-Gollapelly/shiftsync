package com.shiftsync.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This test starts a REAL, disposable PostgreSQL container (via Testcontainers
 * + Docker) for the duration of the test class, lets Flyway migrate it exactly
 * like production would, then drives requests through the actual Spring MVC
 * layer (MockMvc) hitting the real AuthController -> AuthService -> real
 * repositories -> real database. Nothing here is mocked. This is what proves
 * the whole registration/login flow genuinely works end-to-end, not just that
 * each class works in isolation.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shiftsync_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerBusiness_thenLogin_bothSucceedAgainstRealDatabase() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "businessName", "Integration Test Cafe",
                "slug", "integration-test-cafe",
                "ownerFullName", "Test Owner",
                "ownerEmail", "owner@integrationtest.com",
                "password", "IntegrationTest123"
        );

        // Registration should succeed and return real tokens
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.businessSlug").value("integration-test-cafe"));

        // Logging in with the same credentials should also succeed
        Map<String, String> loginRequest = Map.of(
                "businessSlug", "integration-test-cafe",
                "email", "owner@integrationtest.com",
                "password", "IntegrationTest123"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void registerBusiness_rejectsDuplicateSlug() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "businessName", "First Business",
                "slug", "duplicate-slug-test",
                "ownerFullName", "Owner One",
                "ownerEmail", "one@test.com",
                "password", "TestPassword123"
        );

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Second registration with the SAME slug should be rejected
        Map<String, String> duplicateRequest = Map.of(
                "businessName", "Second Business",
                "slug", "duplicate-slug-test",
                "ownerFullName", "Owner Two",
                "ownerEmail", "two@test.com",
                "password", "TestPassword123"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    void login_rejectsWrongPassword_withoutRevealingWhichFieldWasWrong() throws Exception {
        Map<String, String> registerRequest = Map.of(
                "businessName", "Wrong Password Test",
                "slug", "wrong-password-test",
                "ownerFullName", "Test Owner",
                "ownerEmail", "wrongpass@test.com",
                "password", "CorrectPassword123"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        Map<String, String> badLogin = Map.of(
                "businessSlug", "wrong-password-test",
                "email", "wrongpass@test.com",
                "password", "TotallyWrongPassword"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid business, email, or password"));
    }
}