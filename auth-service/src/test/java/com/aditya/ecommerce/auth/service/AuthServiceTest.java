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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_success_savesEncodedPasswordWithCustomerRoleAndReturnsToken() {
        RegisterRequest request = new RegisterRequest("jdoe", "jdoe@example.com", "plainPassword");

        when(userRepository.existsByUsername("jdoe")).thenReturn(false);
        when(userRepository.existsByEmail("jdoe@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(jwtUtil.generateToken(eq("jdoe"), eq(Set.of(Role.ROLE_CUSTOMER)))).thenReturn("token-123");
        when(jwtUtil.getExpirationMillis()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("jdoe");
        assertThat(savedUser.getEmail()).isEqualTo("jdoe@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRoles()).containsExactly(Role.ROLE_CUSTOMER);

        assertThat(response.token()).isEqualTo("token-123");
        assertThat(response.username()).isEqualTo("jdoe");
        assertThat(response.expiresInMillis()).isEqualTo(3600000L);
    }

    @Test
    void register_usernameAlreadyTaken_throwsAndNeverEncodesOrSaves() {
        RegisterRequest request = new RegisterRequest("jdoe", "jdoe@example.com", "plainPassword");
        when(userRepository.existsByUsername("jdoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username already taken");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_emailAlreadyRegistered_throwsAndNeverEncodesOrSaves() {
        RegisterRequest request = new RegisterRequest("jdoe", "jdoe@example.com", "plainPassword");
        when(userRepository.existsByUsername("jdoe")).thenReturn(false);
        when(userRepository.existsByEmail("jdoe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email already registered");

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest request = new LoginRequest("jdoe", "plainPassword");
        User user = User.builder()
                .id(1L)
                .username("jdoe")
                .email("jdoe@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.ROLE_CUSTOMER))
                .build();

        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPassword", "encodedPassword")).thenReturn(true);
        when(jwtUtil.generateToken("jdoe", Set.of(Role.ROLE_CUSTOMER))).thenReturn("token-456");
        when(jwtUtil.getExpirationMillis()).thenReturn(3600000L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("token-456");
        assertThat(response.username()).isEqualTo("jdoe");
        assertThat(response.expiresInMillis()).isEqualTo(3600000L);
    }

    @Test
    void login_unknownUsername_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("ghost", "whatever");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void login_wrongPassword_throwsBadCredentials() {
        LoginRequest request = new LoginRequest("jdoe", "wrongPassword");
        User user = User.builder()
                .id(1L)
                .username("jdoe")
                .email("jdoe@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.ROLE_CUSTOMER))
                .build();

        when(userRepository.findByUsername("jdoe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void listUsers_mapsEntitiesToUserResponses() {
        Instant now = Instant.now();
        User user1 = User.builder().id(1L).username("jdoe").email("jdoe@example.com")
                .roles(Set.of(Role.ROLE_CUSTOMER)).createdAt(now).build();
        User user2 = User.builder().id(2L).username("admin").email("admin@example.com")
                .roles(Set.of(Role.ROLE_ADMIN)).createdAt(now).build();

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        List<UserResponse> responses = authService.listUsers();

        assertThat(responses).containsExactly(
                new UserResponse(1L, "jdoe", "jdoe@example.com", Set.of(Role.ROLE_CUSTOMER), now),
                new UserResponse(2L, "admin", "admin@example.com", Set.of(Role.ROLE_ADMIN), now)
        );
    }

    @Test
    void validate_validToken_returnsRolesFromClaims() {
        Claims claims = mock(Claims.class);
        when(jwtUtil.isValid("valid-token")).thenReturn(true);
        when(jwtUtil.extractClaims("valid-token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("jdoe");
        when(claims.get("roles", List.class)).thenReturn(List.of("ROLE_CUSTOMER", "ROLE_ADMIN"));

        TokenValidationResponse response = authService.validate("valid-token");

        assertThat(response.valid()).isTrue();
        assertThat(response.username()).isEqualTo("jdoe");
        assertThat(response.roles()).containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    void validate_invalidToken_returnsInvalidResponse() {
        when(jwtUtil.isValid("bad-token")).thenReturn(false);

        TokenValidationResponse response = authService.validate("bad-token");

        assertThat(response).isEqualTo(TokenValidationResponse.invalid());
    }
}
