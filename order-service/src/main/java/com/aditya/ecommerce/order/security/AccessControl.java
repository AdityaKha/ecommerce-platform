package com.aditya.ecommerce.order.security;

/**
 * Reads the identity api-gateway established (JWT verified at the edge; subject
 * and roles forwarded as X-Auth-Subject / X-Auth-Roles headers) to authorize
 * access to orders. The headers are trustworthy here because
 * {@link InternalTokenFilter} rejects any request that did not come through the
 * gateway, and the gateway strips client-supplied X-Auth-* headers before
 * stamping its own verified values.
 *
 * Orders belong to a customer: a caller may see and change only their own
 * orders unless they hold ROLE_ADMIN.
 */
public final class AccessControl {

    public static final String SUBJECT_HEADER = "X-Auth-Subject";
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

    /** The authenticated username, or throws if the gateway forwarded no subject. */
    public static String requireSubject(String subjectHeader) {
        if (subjectHeader == null || subjectHeader.isBlank()) {
            throw new ForbiddenException("No authenticated user on request");
        }
        return subjectHeader;
    }

    /** Allow only the order's owner or an administrator. */
    public static void requireOwnerOrAdmin(String orderOwner, String subjectHeader, String rolesHeader) {
        if (isAdmin(rolesHeader)) {
            return;
        }
        String subject = requireSubject(subjectHeader);
        if (!subject.equals(orderOwner)) {
            throw new ForbiddenException("You may only access your own orders");
        }
    }
}
