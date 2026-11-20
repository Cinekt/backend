package com.jacobo.cinekt.iam.infrastructure.authorization.sfs.pipeline;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.TimeMeter;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RateLimitRequestFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitRequestFilter.class);

    private static final Set<String> RATE_LIMITED_PATHS = Set.of(
            "/api/v1/authentication/sign-in",
            "/api/v1/authentication/sign-up",
            "/api/v1/authentication/refresh");

    private final Cache<String, Bucket> budgetsByClient;
    private final int capacity;
    private final Duration window;
    private final TimeMeter clock;

    public RateLimitRequestFilter(int capacity, Duration window, int maximumTrackedClients) {
        this(capacity, window, maximumTrackedClients, TimeMeter.SYSTEM_MILLISECONDS);
    }

    public RateLimitRequestFilter(int capacity, Duration window, int maximumTrackedClients, TimeMeter clock) {
        this.capacity = capacity;
        this.window = window;
        this.clock = clock;
        this.budgetsByClient = Caffeine.newBuilder()
                .maximumSize(maximumTrackedClients)
                .expireAfterAccess(window.multipliedBy(2))
                .build();
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !HttpMethod.POST.matches(request.getMethod())
                || !RATE_LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        var budget = budgetsByClient.get(request.getRemoteAddr(), client -> newBudget());
        var probe = budget.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            LOGGER.warn("Rate limit reached by {} on {}", request.getRemoteAddr(), request.getRequestURI());
            response.setHeader(HttpHeaders.RETRY_AFTER,
                    String.valueOf(TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Too many attempts, try again later");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private Bucket newBudget() {
        return Bucket.builder()
                .withCustomTimePrecision(clock)
                .addLimit(limit -> limit.capacity(capacity).refillIntervally(capacity, window))
                .build();
    }
}
