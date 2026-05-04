package com.suburbscore.user.service;

import com.suburbscore.user.dto.*;
import com.suburbscore.user.entity.User;
import com.suburbscore.user.entity.UserPreferences;
import com.suburbscore.user.exception.ConflictException;
import com.suburbscore.user.exception.ResourceNotFoundException;
import com.suburbscore.user.repository.UserPreferencesRepository;
import com.suburbscore.user.repository.UserRepository;
import com.suburbscore.user.security.JwtUtil;
import com.suburbscore.user.security.TokenBlacklistService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferencesRepository preferencesRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            log.warn("Registration attempt with already-registered email: {}", request.email());
            throw new ConflictException("Registration could not be completed");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.email()));
        String token = jwtUtil.generateToken(user.getEmail());
        return buildAuthResponse(token, user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        PreferencesResponse prefsResponse = preferencesRepository.findByUserId(user.getId())
                .map(this::toPreferencesResponse)
                .orElse(null);
        return new UserProfileResponse(
                user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getCreatedAt(), prefsResponse);
    }

    @Transactional
    public ResponseEntity<PreferencesResponse> savePreferences(String email, PreferencesRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        boolean isNew = preferencesRepository.findByUserId(user.getId()).isEmpty();
        UserPreferences prefs = preferencesRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserPreferences p = new UserPreferences();
                    p.setUser(user);
                    return p;
                });

        prefs.setLookingTo(request.lookingTo());
        prefs.setMaxRentPerWeek(request.maxRentPerWeek());
        prefs.setMaxPurchasePrice(request.maxPurchasePrice());
        prefs.setBedroomsNeeded(request.bedroomsNeeded());
        prefs.setPreferredPropertyType(request.preferredPropertyType());
        prefs.setNeedsParking(request.needsParking());
        prefs.setNeedsGarden(request.needsGarden());
        prefs.setBathroomsNeeded(request.bathroomsNeeded());
        prefs.setWorkplaceSuburb(request.workplaceSuburb());
        prefs.setPartnerWorkplaceSuburb(request.partnerWorkplaceSuburb());
        prefs.setHasChildren(request.hasChildren());
        prefs.setHasPets(request.hasPets());
        prefs.setImportanceCommute(request.importanceCommute());
        prefs.setImportanceSafety(request.importanceSafety());
        prefs.setImportanceSchools(request.importanceSchools());
        prefs.setImportanceWalkability(request.importanceWalkability());
        prefs.setImportanceParks(request.importanceParks());
        prefs.setBuyModeWaitlist(request.buyModeWaitlist());

        PreferencesResponse response = toPreferencesResponse(preferencesRepository.save(prefs));
        return isNew
                ? ResponseEntity.status(201).body(response)
                : ResponseEntity.ok(response);
    }

    public void logout(String token) {
        try {
            String jti = jwtUtil.extractJti(token);
            Instant expiry = jwtUtil.extractExpiration(token).toInstant();
            tokenBlacklistService.blacklist(jti, expiry);
        } catch (JwtException e) {
            // Token already invalid — nothing to blacklist
        }
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return new AuthResponse(
                token,
                "Bearer",
                jwtUtil.getExpirationMs() / 1000,
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    private PreferencesResponse toPreferencesResponse(UserPreferences p) {
        return new PreferencesResponse(
                p.getLookingTo(),
                p.getMaxRentPerWeek(),
                p.getMaxPurchasePrice(),
                p.getBedroomsNeeded(),
                p.getPreferredPropertyType(),
                p.getNeedsParking(),
                p.getNeedsGarden(),
                p.getBathroomsNeeded(),
                p.getWorkplaceSuburb(),
                p.getPartnerWorkplaceSuburb(),
                p.getHasChildren(),
                p.getHasPets(),
                p.getImportanceCommute(),
                p.getImportanceSafety(),
                p.getImportanceSchools(),
                p.getImportanceWalkability(),
                p.getImportanceParks(),
                p.getBuyModeWaitlist(),
                p.getUpdatedAt());
    }
}
