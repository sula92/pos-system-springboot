package com.idet.pos.security;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * HTTP Basic authentication with two roles (ADMIN > USER).
 *
 * - USER: read customers/items/inventory/orders, place orders, create/update customers.
 * - ADMIN: everything USER can do, plus item and inventory maintenance, deletes and reporting endpoints.
 * Any endpoint not listed below is denied.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";

    private static final Logger logger = Logger.getLogger(SecurityConfig.class.getName());

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers(HttpMethod.DELETE, "/customer", "/item").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.POST, "/item", "/inventory").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/item", "/inventory").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.GET,
                                "/customer/purchase-stats", "/customer/purchase-stats-dto",
                                "/inventory/stock-view", "/inventory/stock-value-view",
                                "/order/summary", "/order/summary-dto").hasRole(ADMIN)

                        .requestMatchers(HttpMethod.GET,
                                "/customer", "/customer/search", "/customer/by-email",
                                "/item", "/item/search", "/item/by-min-price",
                                "/inventory", "/inventory/low-stock", "/inventory/in-stock",
                                "/order").hasRole(USER)
                        .requestMatchers(HttpMethod.POST, "/customer", "/order").hasRole(USER)
                        .requestMatchers(HttpMethod.PUT, "/customer").hasRole(USER)

                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(ADMIN).implies(USER)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder,
            @Value("${pos.security.admin.username:admin}") String adminUsername,
            @Value("${pos.security.admin.password:}") String adminPassword,
            @Value("${pos.security.user.username:user}") String userUsername,
            @Value("${pos.security.user.password:}") String userPassword) {
        return new InMemoryUserDetailsManager(
                User.withUsername(adminUsername)
                        .password(passwordEncoder.encode(resolvePassword(adminUsername, adminPassword)))
                        .roles(ADMIN)
                        .build(),
                User.withUsername(userUsername)
                        .password(passwordEncoder.encode(resolvePassword(userUsername, userPassword)))
                        .roles(USER)
                        .build());
    }

    private static String resolvePassword(String username, String configured) {
        if (StringUtils.hasText(configured)) {
            return configured;
        }
        String generated = UUID.randomUUID().toString();
        logger.warning("No password configured for '" + username + "'. Using generated password: " + generated
                + " (set POS_ADMIN_PASSWORD / POS_USER_PASSWORD to choose your own)");
        return generated;
    }
}
