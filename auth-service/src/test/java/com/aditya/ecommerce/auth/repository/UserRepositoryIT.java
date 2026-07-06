package com.aditya.ecommerce.auth.repository;

import com.aditya.ecommerce.auth.domain.Role;
import com.aditya.ecommerce.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private UserRepository userRepository;

    @Test
    void save_thenFindByUsername_returnsPersistedUser() {
        User user = User.builder()
                .username("jdoe")
                .email("jdoe@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.ROLE_CUSTOMER))
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("jdoe");

        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("jdoe");
        assertThat(found.get().getEmail()).isEqualTo("jdoe@example.com");
        assertThat(found.get().getRoles()).containsExactly(Role.ROLE_CUSTOMER);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void existsByUsername_and_existsByEmail_reflectPersistedState() {
        User user = User.builder()
                .username("asmith")
                .email("asmith@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.ROLE_ADMIN))
                .build();

        userRepository.save(user);

        assertThat(userRepository.existsByUsername("asmith")).isTrue();
        assertThat(userRepository.existsByEmail("asmith@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("unknown")).isFalse();
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }
}
