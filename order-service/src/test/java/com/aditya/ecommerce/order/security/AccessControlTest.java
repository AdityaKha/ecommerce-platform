package com.aditya.ecommerce.order.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class AccessControlTest {

    @Test
    void isAdmin_detectsAdminAmongMultipleRoles() {
        assertThat(AccessControl.isAdmin("ROLE_CUSTOMER,ROLE_ADMIN")).isTrue();
        assertThat(AccessControl.isAdmin("ROLE_ADMIN")).isTrue();
    }

    @Test
    void isAdmin_falseForNonAdminOrMissing() {
        assertThat(AccessControl.isAdmin("ROLE_CUSTOMER")).isFalse();
        assertThat(AccessControl.isAdmin("")).isFalse();
        assertThat(AccessControl.isAdmin(null)).isFalse();
    }

    @Test
    void requireSubject_rejectsMissingSubject() {
        assertThatThrownBy(() -> AccessControl.requireSubject(null))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> AccessControl.requireSubject("  "))
                .isInstanceOf(ForbiddenException.class);
        assertThat(AccessControl.requireSubject("jdoe")).isEqualTo("jdoe");
    }

    @Test
    void requireOwnerOrAdmin_allowsOwner() {
        assertThatCode(() -> AccessControl.requireOwnerOrAdmin("jdoe", "jdoe", "ROLE_CUSTOMER"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOrAdmin_allowsAdminForAnyOrder() {
        assertThatCode(() -> AccessControl.requireOwnerOrAdmin("someone-else", "admin", "ROLE_ADMIN"))
                .doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOrAdmin_rejectsNonOwnerNonAdmin() {
        assertThatThrownBy(() -> AccessControl.requireOwnerOrAdmin("victim", "attacker", "ROLE_CUSTOMER"))
                .isInstanceOf(ForbiddenException.class);
    }
}
