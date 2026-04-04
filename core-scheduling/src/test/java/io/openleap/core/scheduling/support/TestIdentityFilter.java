package io.openleap.core.scheduling.support;

import io.openleap.core.common.identity.IdentityHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@TestComponent
public class TestIdentityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader(TestFixtures.TENANT_HEADER);
        if (tenantId != null) {
            IdentityHolder.setTenantId(UUID.fromString(tenantId));
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            IdentityHolder.clear();
        }
    }
}
