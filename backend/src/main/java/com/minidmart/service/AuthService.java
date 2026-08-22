package com.minidmart.service;

import com.minidmart.dto.*;
import com.minidmart.entity.Role;
import com.minidmart.entity.User;
import com.minidmart.exception.DuplicateResourceException;
import com.minidmart.repository.UserRepository;
import com.minidmart.security.CustomUserDetails;
import com.minidmart.security.CustomUserDetailsService;
import com.minidmart.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.UUID;

/**
 * Authentication service handling registration, login, and token refresh.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final AuditService auditService;

    @Value("${mini-dmart.security.admin-setup-token:admin-secret-123}")
    private String adminSetupToken;

    @Value("${mini-dmart.security.staff-setup-token:staff-secret-123}")
    private String staffSetupToken;

    /**
     * Register a new admin.
     * Validates the setup token before creating an ADMIN account.
     */
    @Transactional
    public AuthResponse registerAdmin(RegisterRequest request) {
        if (!adminSetupToken.equals(request.getSetupToken())) {
            throw new BadCredentialsException("Invalid setup token");
        }
        return registerWithRole(request, Role.ADMIN);
    }

    /**
     * Register a new staff member.
     * Validates the setup token before creating a STAFF account.
     */
    @Transactional
    public AuthResponse registerStaff(RegisterRequest request) {
        if (!staffSetupToken.equals(request.getSetupToken())) {
            throw new BadCredentialsException("Invalid setup token");
        }
        return registerWithRole(request, Role.STAFF);
    }

    /**
     * Register a new customer.
     * Role is ALWAYS set to CUSTOMER regardless of any request manipulation.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        return registerWithRole(request, Role.CUSTOMER);
    }

    private AuthResponse registerWithRole(RegisterRequest request, Role role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .role(role) // Uses the securely passed role from the backend
                .build();

        user = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        auditService.log(user.getId(), "USER_REGISTERED", "User",
                user.getId().toString(), "email: " + user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(toUserResponse(user))
                .build();
    }

    /**
     * Authenticate a user and return tokens.
     */
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), request.getPassword()));

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            
            // Validate expected role if provided by the frontend
            if (request.getExpectedRole() != null && userDetails.getUser().getRole() != request.getExpectedRole()) {
                throw new BadCredentialsException("This account does not belong to this workspace.");
            }

            String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
            String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

            auditService.log(userDetails.getId(), "USER_LOGIN", "User",
                    userDetails.getId().toString(), "email: " + request.getEmail());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(toUserResponse(userDetails.getUser()))
                    .build();

        } catch (BadCredentialsException ex) {
            auditService.log(null, "USER_LOGIN_FAILED", "User",
                    "unknown", "email: " + request.getEmail());
            throw ex;
        }
    }

    /**
     * Refresh the access token using a valid refresh token.
     */
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid or expired refresh token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserById(UUID.fromString(userId));

        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);

        CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return same refresh token
                .user(toUserResponse(customUserDetails.getUser()))
                .build();
    }

    /**
     * Get the current user profile from an authenticated principal.
     */
    public UserResponse getCurrentUser(CustomUserDetails userDetails) {
        return toUserResponse(userDetails.getUser());
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
