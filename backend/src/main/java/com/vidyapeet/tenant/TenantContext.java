package com.vidyapeet.tenant;

/**
 * Holds the current tenant (institute) id for the duration of a request, on a
 * per-thread basis. Populated by {@code JwtAuthenticationFilter} from the
 * authenticated user's token and read by {@link TenantFilterAspect} to scope
 * every persistence query.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();

    /**
     * When true, tenant scoping is bypassed for the current thread. Reserved for
     * SUPER_ADMIN operations (e.g. creating institutes) and the data seeder.
     */
    private static final ThreadLocal<Boolean> BYPASS = ThreadLocal.withInitial(() -> false);

    private TenantContext() {
    }

    public static void setTenantId(Long instituteId) {
        CURRENT_TENANT.set(instituteId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    public static void setBypass(boolean bypass) {
        BYPASS.set(bypass);
    }

    public static boolean isBypass() {
        return Boolean.TRUE.equals(BYPASS.get());
    }

    /** Clears all tenant state. Must be called at the end of every request. */
    public static void clear() {
        CURRENT_TENANT.remove();
        BYPASS.remove();
    }
}
