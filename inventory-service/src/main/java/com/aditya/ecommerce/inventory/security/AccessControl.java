package com.aditya.ecommerce.inventory.security;

/**
 * Reads the identity api-gateway established (JWT verified at the edge, roles
 * forwarded as the X-Auth-Roles header) to authorize stock mutations. The
 * headers are trustworthy here because {@link InternalTokenFilter} rejects any
 * request that did not come through the gateway, and the gateway strips
 * client-supplied X-Auth-* headers before stamping its own verified values.
 */
public final class AccessControl {

    public static final String ROLES_HEADER = "X-Auth-Roles";
    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    private AccessControl() {
    }

    public static boolean isAdmin(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return false;
        }
        for (String role : rolesHeader.split(",")) {
            if (ADMIN_ROLE.equals(role.trim())) {
                return true;
            }
        }
        return false;
    }

    public static void requireAdmin(String rolesHeader) {
        if (!isAdmin(rolesHeader)) {
            throw new ForbiddenException("Administrator role required for this operation");
        }
    }
}
