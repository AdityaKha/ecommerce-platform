package com.aditya.ecommerce.product.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void requireAdmin_allowsAdminRejectsOthers() {
        assertThatCode(() -> AccessControl.requireAdmin("ROLE_ADMIN")).doesNotThrowAnyException();
        assertThatThrownBy(() -> AccessControl.requireAdmin("ROLE_CUSTOMER"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> AccessControl.requireAdmin(null))
                .isInstanceOf(ForbiddenException.class);
    }
}
