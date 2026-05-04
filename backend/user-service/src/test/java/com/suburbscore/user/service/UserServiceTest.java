package com.suburbscore.user.service;

import com.suburbscore.user.dto.*;
import com.suburbscore.user.entity.*;
import com.suburbscore.user.exception.ConflictException;
import com.suburbscore.user.exception.ResourceNotFoundException;
import com.suburbscore.user.repository.UserPreferencesRepository;
import com.suburbscore.user.repository.UserRepository;
import com.suburbscore.user.security.JwtUtil;
import com.suburbscore.user.security.TokenBlacklistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock UserPreferencesRepository preferencesRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;
    @Mock AuthenticationManager authenticationManager;
    @Mock TokenBlacklistService tokenBlacklistService;

    @InjectMocks UserService userService;

    private static final String EMAIL      = "john.smith@example.com";
    private static final String PASSWORD   = "securePass123";
    private static final String TOKEN      = "mock.jwt.token";
    private static final UUID   USER_ID    = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");

    private User mockUser() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmail(EMAIL);
        user.setPasswordHash("encoded-password");
        user.setFirstName("John");
        user.setLastName("Smith");
        return user;
    }

    private RegisterRequest registerRequest() {
        return new RegisterRequest(EMAIL, PASSWORD, "John", "Smith");
    }

    private LoginRequest loginRequest() {
        return new LoginRequest(EMAIL, PASSWORD);
    }

    private PreferencesRequest preferencesRequest() {
        return new PreferencesRequest(
                LookingTo.RENT, 600, null,
                3, PropertyType.HOUSE, true, false, 2,
                "Sydney CBD", "Parramatta", true, false,
                5, 4, 5, 3, 2, false);
    }

    private UserPreferences mockPreferences(User user) {
        UserPreferences prefs = new UserPreferences();
        prefs.setId(UUID.randomUUID());
        prefs.setUser(user);
        prefs.setLookingTo(LookingTo.RENT);
        prefs.setMaxRentPerWeek(600);
        prefs.setBedroomsNeeded(3);
        prefs.setPreferredPropertyType(PropertyType.HOUSE);
        prefs.setWorkplaceSuburb("Sydney CBD");
        prefs.setImportanceCommute(5);
        prefs.setImportanceSafety(4);
        prefs.setImportanceSchools(5);
        prefs.setImportanceWalkability(3);
        prefs.setImportanceParks(2);
        prefs.setUpdatedAt(LocalDateTime.now());
        return prefs;
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("valid token — blacklists the JTI")
        void blacklistsToken() {
            String jti = "some-jti-uuid";
            when(jwtUtil.extractJti(TOKEN)).thenReturn(jti);
            when(jwtUtil.extractExpiration(TOKEN)).thenReturn(new Date(System.currentTimeMillis() + 86400000));

            userService.logout(TOKEN);

            verify(tokenBlacklistService).blacklist(eq(jti), any());
        }
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("success — returns AuthResponse with token and user details")
        void success() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(PASSWORD)).thenReturn("encoded-password");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                u.setId(USER_ID);
                return u;
            });
            when(jwtUtil.generateToken(EMAIL)).thenReturn(TOKEN);
            when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

            AuthResponse response = userService.register(registerRequest());

            assertThat(response.token()).isEqualTo(TOKEN);
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresIn()).isEqualTo(86400L);
            assertThat(response.email()).isEqualTo(EMAIL);
            assertThat(response.firstName()).isEqualTo("John");
            assertThat(response.lastName()).isEqualTo("Smith");
            assertThat(response.userId()).isEqualTo(USER_ID.toString());

            verify(userRepository).save(any(User.class));
            verify(passwordEncoder).encode(PASSWORD);
        }

        @Test
        @DisplayName("duplicate email — throws ConflictException")
        void duplicateEmail() {
            when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

            assertThatThrownBy(() -> userService.register(registerRequest()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Registration could not be completed");

            verify(userRepository, never()).save(any());
        }
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("success — returns AuthResponse")
        void success() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser()));
            when(jwtUtil.generateToken(EMAIL)).thenReturn(TOKEN);
            when(jwtUtil.getExpirationMs()).thenReturn(86400000L);

            AuthResponse response = userService.login(loginRequest());

            assertThat(response.token()).isEqualTo(TOKEN);
            assertThat(response.email()).isEqualTo(EMAIL);
            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        }

        @Test
        @DisplayName("wrong password — throws BadCredentialsException")
        void wrongPassword() {
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authenticationManager).authenticate(any());

            assertThatThrownBy(() -> userService.login(loginRequest()))
                    .isInstanceOf(BadCredentialsException.class);

            verify(userRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("user not found after auth — throws ResourceNotFoundException")
        void userNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login(loginRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(EMAIL);
        }
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProfile()")
    class GetProfile {

        @Test
        @DisplayName("with preferences — returns full profile")
        void withPreferences() {
            User user = mockUser();
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(preferencesRepository.findByUserId(USER_ID))
                    .thenReturn(Optional.of(mockPreferences(user)));

            UserProfileResponse profile = userService.getProfile(EMAIL);

            assertThat(profile.email()).isEqualTo(EMAIL);
            assertThat(profile.firstName()).isEqualTo("John");
            assertThat(profile.preferences()).isNotNull();
            assertThat(profile.preferences().lookingTo()).isEqualTo(LookingTo.RENT);
            assertThat(profile.preferences().maxRentPerWeek()).isEqualTo(600);
        }

        @Test
        @DisplayName("without preferences — returns profile with null preferences")
        void withoutPreferences() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser()));
            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            UserProfileResponse profile = userService.getProfile(EMAIL);

            assertThat(profile.email()).isEqualTo(EMAIL);
            assertThat(profile.preferences()).isNull();
        }

        @Test
        @DisplayName("user not found — throws ResourceNotFoundException")
        void userNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getProfile(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(EMAIL);
        }
    }

    // ── savePreferences ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("savePreferences()")
    class SavePreferences {

        @Test
        @DisplayName("first time save — returns 201 Created")
        void firstTimeSave() {
            User user = mockUser();
            UserPreferences saved = mockPreferences(user);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(preferencesRepository.save(any(UserPreferences.class))).thenReturn(saved);

            ResponseEntity<PreferencesResponse> response =
                    userService.savePreferences(EMAIL, preferencesRequest());

            assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.CREATED.value());
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().lookingTo()).isEqualTo(LookingTo.RENT);
        }

        @Test
        @DisplayName("update existing — returns 200 OK")
        void updateExisting() {
            User user = mockUser();
            UserPreferences existing = mockPreferences(user);
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
            when(preferencesRepository.save(any(UserPreferences.class))).thenReturn(existing);

            ResponseEntity<PreferencesResponse> response =
                    userService.savePreferences(EMAIL, preferencesRequest());

            assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        }

        @Test
        @DisplayName("user not found — throws ResourceNotFoundException")
        void userNotFound() {
            when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.savePreferences(EMAIL, preferencesRequest()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(EMAIL);

            verify(preferencesRepository, never()).save(any());
        }
    }
}
