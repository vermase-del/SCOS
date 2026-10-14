package com.scos.security.multitenancy;

import java.util.UUID;

public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_COMMUNITY = new ThreadLocal<>();

    private TenantContext() {}

    public static void setCommunityId(UUID communityId) {
        CURRENT_COMMUNITY.set(communityId);
    }

    public static UUID getCommunityId() {
        return CURRENT_COMMUNITY.get();
    }

    public static void clear() {
        CURRENT_COMMUNITY.remove();
    }
}
