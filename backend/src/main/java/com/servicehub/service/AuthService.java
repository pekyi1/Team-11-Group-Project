package com.servicehub.service;

import com.servicehub.dto.*;
import com.servicehub.model.User;
import com.servicehub.model.enums.Role;
import com.servicehub.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms}")
    private Long jwtExpiration;

    public AuthResponse register(RegisterRequest request) {
        // TODO: Implement full registration logic
        throw new UnsupportedOperationException("Registration not yet implemented");
    }

    public AuthResponse login(AuthRequest request) {
        // TODO: Implement full login logic
        throw new UnsupportedOperationException("Login not yet implemented");
    }
}
