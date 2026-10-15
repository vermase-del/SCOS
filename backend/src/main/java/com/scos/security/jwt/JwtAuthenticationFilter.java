package com.scos.security.jwt;

import com.scos.security.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = tokenProvider.extractToken(request);
            if (jwt != null && tokenProvider.validateToken(jwt)) {
                var authentication = tokenProvider.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                UUID tokenCommunityId = tokenProvider.getCommunityIdFromToken(jwt);
                String communityHeader = request.getHeader("X-Community-Id");
                if (communityHeader != null && !communityHeader.isBlank()) {
                    try {
                        UUID headerCommunityId = UUID.fromString(communityHeader.trim());
                        boolean isSuperAdmin = authentication.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN") || a.getAuthority().equals("SUPER_ADMIN"));

                        if (isSuperAdmin || (tokenCommunityId != null && tokenCommunityId.equals(headerCommunityId))) {
                            TenantContext.setCommunityId(headerCommunityId);
                        } else if (tokenCommunityId != null) {
                            // Non-superadmin cannot spoof arbitrary community via header; lock to token
                            TenantContext.setCommunityId(tokenCommunityId);
                        }
                    } catch (IllegalArgumentException ignored) {
                        if (tokenCommunityId != null) {
                            TenantContext.setCommunityId(tokenCommunityId);
                        }
                    }
                } else if (tokenCommunityId != null) {
                    TenantContext.setCommunityId(tokenCommunityId);
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
