package com.jacobo.cinekt.iam.infrastructure.authorization.sfs.configuration;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import com.jacobo.cinekt.iam.infrastructure.authorization.sfs.pipeline.BearerAuthorizationRequestFilter;
import com.jacobo.cinekt.iam.infrastructure.authorization.sfs.pipeline.CsrfHeaderRequestFilter;
import com.jacobo.cinekt.iam.infrastructure.authorization.sfs.pipeline.RateLimitRequestFilter;
import com.jacobo.cinekt.iam.infrastructure.hashing.bcrypt.BCryptHashingService;
import com.jacobo.cinekt.iam.infrastructure.tokens.jwt.BearerTokenService;

import jakarta.servlet.DispatcherType;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {
    private final UserDetailsService userDetailsService;
    private final BearerTokenService tokenService;
    private final BCryptHashingService hashingService;
    private final AuthenticationEntryPoint unauthorizedRequestHandler;
    private final List<String> allowedOrigins;

    public WebSecurityConfiguration(@Qualifier("defaultUserDetailsService") UserDetailsService userDetailsService,
            BearerTokenService tokenService, BCryptHashingService hashingService,
            AuthenticationEntryPoint unauthorizedRequestHandler,
            @Value("${cors.allowed-origins}") List<String> allowedOrigins) {
        this.userDetailsService = userDetailsService;
        this.tokenService = tokenService;
        this.hashingService = hashingService;
        this.unauthorizedRequestHandler = unauthorizedRequestHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(hashingService);
        return authenticationProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return hashingService;
    }

    @Bean
    public BearerAuthorizationRequestFilter authorizationRequestFilter() {
        return new BearerAuthorizationRequestFilter(tokenService, userDetailsService);
    }

    @Bean
    public CsrfHeaderRequestFilter csrfHeaderRequestFilter() {
        return new CsrfHeaderRequestFilter();
    }

    @Bean
    public RateLimitRequestFilter rateLimitRequestFilter(
            @Value("${rate-limit.authentication.capacity}") int capacity,
            @Value("${rate-limit.authentication.window-seconds}") int windowSeconds,
            @Value("${rate-limit.authentication.max-tracked-clients}") int maximumTrackedClients) {
        return new RateLimitRequestFilter(capacity, Duration.ofSeconds(windowSeconds), maximumTrackedClients);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, RateLimitRequestFilter rateLimitRequestFilter)
            throws Exception {
        http.cors(configurer -> configurer.configurationSource(request -> {
            var cors = new CorsConfiguration();
            cors.setAllowedOrigins(allowedOrigins);
            cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
            cors.setAllowedHeaders(List.of("*"));
            cors.setAllowCredentials(true);
            cors.setMaxAge(3600L);
            return cors;
        }));
        http.csrf(csrfConfigurer -> csrfConfigurer.disable())
                .exceptionHandling(
                        exceptionHandling -> exceptionHandling.authenticationEntryPoint(unauthorizedRequestHandler))
                .sessionManagement(customizer -> customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorizeRequest -> authorizeRequest
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(
                                "/api/v1/authentication/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()
                        .anyRequest().authenticated());
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(rateLimitRequestFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(authorizationRequestFilter(), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(csrfHeaderRequestFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

}
