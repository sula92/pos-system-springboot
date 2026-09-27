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
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless JWT resource server secured by Keycloak.
 *
 * Role model (hierarchical, SUPER_ADMIN > ADMIN > USER):
 * - USER: read customers/items/inventory/orders, place orders, create/update customers.
 * - ADMIN: USER access plus item and inventory maintenance and reporting endpoints.
 * - SUPER_ADMIN: ADMIN access plus deleting customers and items.
 * Any endpoint not listed below is denied.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    public static final String USER = "USER";
    public static final String ADMIN = "ADMIN";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    @Value("${pos.security.keycloak.client-id:pos-frontend}")
    private String keycloakClientId;

    @Value("${pos.security.cors.allowed-origins:http://localhost:5173}")
    private List<String> allowedOrigins;

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

                        .requestMatchers(HttpMethod.DELETE, "/customer", "/item").hasRole(SUPER_ADMIN)

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
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter(keycloakClientId));
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }

    @Bean
    public static RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(SUPER_ADMIN).implies(ADMIN)
                .role(ADMIN).implies(USER)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
