package com.suburbscore.user.controller;

import com.suburbscore.user.dto.*;
import com.suburbscore.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "User registration, authentication, and profile management")
public class UserController {

    private final UserService userService;

    // ── POST /register ────────────────────────────────────────────────────────

    @Operation(summary = "Register a new user",
               description = "Creates a new account and returns a JWT token for immediate use. No prior login needed.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User registered successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Bad Request",
                      "status": 400,
                      "detail": "Validation failed",
                      "instance": "/api/users/register",
                      "errors": {
                        "email": "must be a well-formed email address",
                        "password": "Password must be at least 8 characters",
                        "firstName": "must not be blank"
                      }
                    }"""))),
        @ApiResponse(responseCode = "409", description = "Email is already registered",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Conflict",
                      "status": 409,
                      "detail": "Email already registered: john.smith@example.com",
                      "instance": "/api/users/register"
                    }""")))
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    // ── POST /login ───────────────────────────────────────────────────────────

    @Operation(summary = "Login",
               description = "Authenticates the user and returns a JWT token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — missing or invalid fields",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Bad Request",
                      "status": 400,
                      "detail": "Validation failed",
                      "instance": "/api/users/login",
                      "errors": {
                        "email": "must be a well-formed email address"
                      }
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Invalid email or password",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Unauthorized",
                      "status": 401,
                      "detail": "Invalid email or password",
                      "instance": "/api/users/login"
                    }""")))
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    // ── GET /profile ──────────────────────────────────────────────────────────

    @Operation(summary = "Get current user profile",
               description = "Returns the authenticated user's profile and saved preferences.",
               security = @SecurityRequirement(name = "Bearer"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile returned successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Missing, expired, or invalid token",
            content = @Content(mediaType = "application/problem+json",
                examples = {
                    @ExampleObject(name = "No token", value = """
                        {
                          "type": "about:blank",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Authentication required — provide a Bearer token",
                          "instance": "/api/users/profile"
                        }"""),
                    @ExampleObject(name = "Expired token", value = """
                        {
                          "type": "about:blank",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Token has expired",
                          "instance": "/api/users/profile"
                        }"""),
                    @ExampleObject(name = "Invalid token", value = """
                        {
                          "type": "about:blank",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Invalid token",
                          "instance": "/api/users/profile"
                        }""")
                }))
    })
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }

    // ── PUT /preferences ──────────────────────────────────────────────────────

    @Operation(summary = "Save or update preferences",
               description = "Creates preferences for the first time (201) or updates existing ones (200).",
               security = @SecurityRequirement(name = "Bearer"))
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preferences updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PreferencesResponse.class))),
        @ApiResponse(responseCode = "201", description = "Preferences created for the first time",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PreferencesResponse.class))),
        @ApiResponse(responseCode = "400", description = "Validation failed — importance weights must be 1–5",
            content = @Content(mediaType = "application/problem+json",
                examples = @ExampleObject(value = """
                    {
                      "type": "about:blank",
                      "title": "Bad Request",
                      "status": 400,
                      "detail": "Validation failed",
                      "instance": "/api/users/preferences",
                      "errors": {
                        "importanceCommute": "must be less than or equal to 5",
                        "bedroomsNeeded": "must be greater than or equal to 1"
                      }
                    }"""))),
        @ApiResponse(responseCode = "401", description = "Missing, expired, or invalid token",
            content = @Content(mediaType = "application/problem+json",
                examples = {
                    @ExampleObject(name = "No token", value = """
                        {
                          "type": "about:blank",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Authentication required — provide a Bearer token",
                          "instance": "/api/users/preferences"
                        }"""),
                    @ExampleObject(name = "Expired token", value = """
                        {
                          "type": "about:blank",
                          "title": "Unauthorized",
                          "status": 401,
                          "detail": "Token has expired",
                          "instance": "/api/users/preferences"
                        }""")
                }))
    })
    @PutMapping("/preferences")
    public ResponseEntity<PreferencesResponse> savePreferences(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PreferencesRequest request) {
        return userService.savePreferences(userDetails.getUsername(), request);
    }
}
