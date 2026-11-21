package com.techpulse.taskmanager.security;

/**
 * Holds the current request's tenant_id in a ThreadLocal.
 * Populated exclusively from the validated JWT by JwtAuthenticationFilter
 * and cleared in a finally block at the end of every request.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
