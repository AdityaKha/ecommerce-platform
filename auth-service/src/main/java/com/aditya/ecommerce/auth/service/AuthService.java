package com.aditya.ecommerce.auth.service;

import com.aditya.ecommerce.auth.domain.Role;
import com.aditya.ecommerce.auth.domain.User;
import com.aditya.ecommerce.auth.dto.AuthResponse;
import com.aditya.ecommerce.auth.dto.LoginRequest;
import com.aditya.ecommerce.auth.dto.RegisterRequest;
import com.aditya.ecommerce.auth.dto.TokenValidationResponse;
import com.aditya.ecommerce.auth.dto.UserResponse;
import com.aditya.ecommerce.auth.repository.UserRepository;
import com.aditya.ecommerce.auth.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(Set.of(Role.ROLE_CUSTOMER))
                .build();

        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());
        return new AuthResponse(token, user.getUsername(), jwtUtil.getExpirationMillis());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRoles());
        return new AuthResponse(token, user.getUsername(), jwtUtil.getExpirationMillis());
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
                        user.getRoles(), user.getCreatedAt()))
                .toList();
    }

    public TokenValidationResponse validate(String token) {
        if (!jwtUtil.isValid(token)) {
            return TokenValidationResponse.invalid();
        }
        Claims claims = jwtUtil.extractClaims(token);
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return new TokenValidationResponse(true, claims.getSubject(), Set.copyOf(roles));
    }
}
