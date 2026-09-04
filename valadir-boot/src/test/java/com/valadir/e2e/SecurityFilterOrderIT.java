package com.valadir.e2e;

import com.valadir.web.filter.MdcSecurityFilter;
import com.valadir.web.filter.RateLimitFilter;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.FilterChainProxy;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Structural on purpose: a 429 on the USER rule costs 101 authenticated calls to reach, and the
 * ordering it would prove is decided in one line of {@code SecurityConfig}. Extends the E2E base to
 * join its context rather than boot a second one.
 */
class SecurityFilterOrderIT extends AbstractAuthE2EIT {

    @Autowired
    private FilterChainProxy filterChainProxy;

    @Test
    void securityFilterChain_production_authenticatesIntoTheMdcBeforeTheRateLimiterReadsIt() {

        List<Class<?>> filters = filterChainProxy.getFilterChains().getFirst().getFilters().stream()
            .<Class<?>>map(Filter::getClass)
            .toList();

        assertThat(filters).contains(MdcSecurityFilter.class, RateLimitFilter.class);
        assertThat(filters.indexOf(MdcSecurityFilter.class)).isLessThan(filters.indexOf(RateLimitFilter.class));
    }
}
