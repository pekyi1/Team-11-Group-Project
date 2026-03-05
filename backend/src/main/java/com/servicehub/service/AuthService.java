package com.servicehub.service;

import com.servicehub.config.JwtService;
import com.servicehub.dto.request.LoginRequest;
import com.servicehub.dto.request.RegisterRequest;
import com.servicehub.dto.response.AuthResponse;
import com.servicehub.dto.response.UserResponse;
import com.servicehub.exception.BadRequestException;
import com.servicehub.exception.DuplicateResourceException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.Location;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.LocationRepository;
import com.servicehub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.servicehub.dto.request.UpdateNotificationPreferencesRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        Location location = locationRepository.findById(request.getLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("Location not found with id: " + request.getLocationId()));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(Role.USER)
                .location(location)
                .isActive(true)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password");
        }

        log.info("User logged in successfully: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .locationName(user.getLocation() != null ? user.getLocation().getName() : null)
                .locationId(user.getLocation() != null ? user.getLocation().getId() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        String role = jwtService.extractRole(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return buildAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getNotificationPreferences(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return parseNotificationPreferences(user.getNotificationPreferences());
    }

    @Transactional
    public Map<String, Object> updateNotificationPreferences(UUID userId, UpdateNotificationPreferencesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Map<String, Object> prefs = parseNotificationPreferences(user.getNotificationPreferences());

        if (request.getRequestCreated() != null) {
            prefs.put("requestCreated", request.getRequestCreated());
        }
        if (request.getStatusUpdates() != null) {
            prefs.put("statusUpdates", request.getStatusUpdates());
        }

        // Critical notifications are always enabled
        prefs.put("ticketAssigned", true);
        prefs.put("slaWarning", true);
        prefs.put("slaBreach", true);
        prefs.put("transferred", true);

        try {
            ObjectMapper mapper = new ObjectMapper();
            user.setNotificationPreferences(mapper.writeValueAsString(prefs));
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to serialize notification preferences");
        }
        userRepository.save(user);
        return prefs;
    }

    private Map<String, Object> parseNotificationPreferences(String json) {
        if (json == null || json.isBlank() || "{}".equals(json)) {
            Map<String, Object> defaults = new HashMap<>();
            defaults.put("requestCreated", true);
            defaults.put("statusUpdates", true);
            defaults.put("ticketAssigned", true);
            defaults.put("slaWarning", true);
            defaults.put("slaBreach", true);
            defaults.put("transferred", true);
            return defaults;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Failed to parse notification preferences");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail(), user.getRole().name());
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtExpirationMs / 1000)
                .refreshToken(refreshToken)
                .user(toUserResponse(user))
                .build();
    }
}
