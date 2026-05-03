package com.suburbscore.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suburbscore.user.config.SecurityConfig;
import com.suburbscore.user.dto.*;
import com.suburbscore.user.entity.LookingTo;
import com.suburbscore.user.entity.PropertyType;
import com.suburbscore.user.exception.ConflictException;
import com.suburbscore.user.exception.GlobalExceptionHandler;
import com.suburbscore.user.exception.ResourceNotFoundException;
import com.suburbscore.user.security.JwtAuthenticationFilter;
import com.suburbscore.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
        }
)
@Import({UserControllerTest.TestSecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("UserController")
class UserControllerTest {

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean UserService userService;

    private static final String EMAIL   = "john.smith@example.com";
    private static final String TOKEN   = "mock.jwt.token";
    private static final UUID   USER_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    private AuthResponse mockAuthResponse() {
        return new AuthResponse(TOKEN, "Bearer", 86400L, USER_ID.toString(), EMAIL, "John", "Smith");
    }

    private PreferencesResponse mockPreferencesResponse() {
        return new PreferencesResponse(
                LookingTo.RENT, 600, null, 3, PropertyType.HOUSE,
                true, false, 2, "Sydney CBD", "Parramatta",
                true, false, 5, 4, 5, 3, 2, false,
                LocalDateTime.of(2026, 5, 3, 10, 0));
    }

    private UserProfileResponse mockProfileResponse() {
        return new UserProfileResponse(USER_ID, EMAIL, "John", "Smith",
                LocalDateTime.of(2026, 5, 3, 9, 0), mockPreferencesResponse());
    }

    // ── POST /register ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users/register")
    class Register {

        @Test
        @DisplayName("valid request — 201 with AuthResponse")
        void validRequest_returns201() throws Exception {
            when(userService.register(any())).thenReturn(mockAuthResponse());

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "email": "john.smith@example.com",
                                  "password": "securePass123",
                                  "firstName": "John",
                                  "lastName": "Smith"
                                }"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.expiresIn").value(86400))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Smith"));
        }

        @Test
        @DisplayName("missing fields — 400 with validation errors")
        void missingFields_returns400() throws Exception {
            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.email").exists())
                    .andExpect(jsonPath("$.errors.password").exists())
                    .andExpect(jsonPath("$.errors.firstName").exists())
                    .andExpect(jsonPath("$.errors.lastName").exists());
        }

        @Test
        @DisplayName("invalid email format — 400 with email error")
        void invalidEmail_returns400() throws Exception {
            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "securePass123",
                                  "firstName": "John",
                                  "lastName": "Smith"
                                }"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        @DisplayName("password too short — 400 with password error")
        void shortPassword_returns400() throws Exception {
            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "email": "john.smith@example.com",
                                  "password": "short",
                                  "firstName": "John",
                                  "lastName": "Smith"
                                }"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.password").exists());
        }

        @Test
        @DisplayName("duplicate email — 409 Conflict")
        void duplicateEmail_returns409() throws Exception {
            when(userService.register(any()))
                    .thenThrow(new ConflictException("Email already registered: " + EMAIL));

            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "email": "john.smith@example.com",
                                  "password": "securePass123",
                                  "firstName": "John",
                                  "lastName": "Smith"
                                }"""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.detail").value("Email already registered: " + EMAIL));
        }
    }

    // ── POST /login ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/users/login")
    class Login {

        @Test
        @DisplayName("valid credentials — 200 with AuthResponse")
        void validCredentials_returns200() throws Exception {
            doReturn(mockAuthResponse()).when(userService).login(any());

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "email": "john.smith@example.com",
                                  "password": "securePass123"
                                }"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN))
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.email").value(EMAIL));
        }

        @Test
        @DisplayName("invalid email format — 400 validation error")
        void invalidEmail_returns400() throws Exception {
            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "securePass123"
                                }"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        @DisplayName("wrong password — 401 Unauthorized")
        void wrongPassword_returns401() throws Exception {
            when(userService.login(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/api/users/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "email": "john.smith@example.com",
                                  "password": "wrongPassword"
                                }"""))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        }
    }

    // ── GET /profile ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/users/profile")
    class GetProfile {

        @Test
        @WithMockUser(username = "john.smith@example.com")
        @DisplayName("authenticated — 200 with full profile")
        void authenticated_returns200() throws Exception {
            when(userService.getProfile(EMAIL)).thenReturn(mockProfileResponse());

            mockMvc.perform(get("/api/users/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                    .andExpect(jsonPath("$.email").value(EMAIL))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.preferences.lookingTo").value("RENT"))
                    .andExpect(jsonPath("$.preferences.maxRentPerWeek").value(600));
        }

        @Test
        @WithMockUser(username = "john.smith@example.com")
        @DisplayName("no preferences saved — 200 with null preferences")
        void noPreferences_returns200WithNullPreferences() throws Exception {
            UserProfileResponse noPrefs = new UserProfileResponse(
                    USER_ID, EMAIL, "John", "Smith",
                    LocalDateTime.of(2026, 5, 3, 9, 0), null);
            when(userService.getProfile(EMAIL)).thenReturn(noPrefs);

            mockMvc.perform(get("/api/users/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.preferences").doesNotExist());
        }

        @Test
        @WithMockUser(username = "john.smith@example.com")
        @DisplayName("user not found — 404")
        void userNotFound_returns404() throws Exception {
            when(userService.getProfile(EMAIL))
                    .thenThrow(new ResourceNotFoundException("User", EMAIL));

            mockMvc.perform(get("/api/users/profile"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.detail").value("User not found: " + EMAIL));
        }
    }

    // ── PUT /preferences ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/users/preferences")
    class SavePreferences {

        private static final String VALID_BODY = """
                {
                  "lookingTo": "RENT",
                  "maxRentPerWeek": 600,
                  "bedroomsNeeded": 3,
                  "preferredPropertyType": "HOUSE",
                  "needsParking": true,
                  "needsGarden": false,
                  "bathroomsNeeded": 2,
                  "workplaceSuburb": "Sydney CBD",
                  "partnerWorkplaceSuburb": "Parramatta",
                  "hasChildren": true,
                  "hasPets": false,
                  "importanceCommute": 5,
                  "importanceSafety": 4,
                  "importanceSchools": 5,
                  "importanceWalkability": 3,
                  "importanceParks": 2,
                  "buyModeWaitlist": false
                }""";

        @Test
        @WithMockUser(username = "john.smith@example.com")
        @DisplayName("first time save — 201 Created")
        void firstTimeSave_returns201() throws Exception {
            when(userService.savePreferences(eq(EMAIL), any()))
                    .thenReturn(ResponseEntity.status(201).body(mockPreferencesResponse()));

            mockMvc.perform(put("/api/users/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.lookingTo").value("RENT"))
                    .andExpect(jsonPath("$.maxRentPerWeek").value(600))
                    .andExpect(jsonPath("$.workplaceSuburb").value("Sydney CBD"));
        }

        @Test
        @WithMockUser(username = "john.smith@example.com")
        @DisplayName("update existing — 200 OK")
        void updateExisting_returns200() throws Exception {
            when(userService.savePreferences(eq(EMAIL), any()))
                    .thenReturn(ResponseEntity.ok(mockPreferencesResponse()));

            mockMvc.perform(put("/api/users/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_BODY))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lookingTo").value("RENT"))
                    .andExpect(jsonPath("$.maxRentPerWeek").value(600));
        }

        @Test
        @WithMockUser(username = "john.smith@example.com")
        @DisplayName("importance weight out of range — 400 Bad Request")
        void invalidWeights_returns400() throws Exception {
            mockMvc.perform(put("/api/users/preferences")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                {
                                  "importanceCommute": 10,
                                  "importanceSafety": 0
                                }"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.detail").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.importanceCommute").exists())
                    .andExpect(jsonPath("$.errors.importanceSafety").exists());
        }
    }
}
