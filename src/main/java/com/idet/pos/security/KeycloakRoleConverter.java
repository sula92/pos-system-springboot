package com.idet.pos.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Maps Keycloak roles in an access token to Spring Security authorities.
 * Reads realm roles (realm_access.roles) and client roles for the configured
 * client (resource_access.{clientId}.roles), e.g. "super_admin" -> "ROLE_SUPER_ADMIN".
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final String clientId;

    public KeycloakRoleConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        addRoles(realmAccess, authorities);

        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess != null && clientId != null && resourceAccess.get(clientId) instanceof Map<?, ?> client) {
            addRoles(client, authorities);
        }
        return authorities;
    }

    private void addRoles(Map<?, ?> container, Set<GrantedAuthority> authorities) {
        if (container == null || !(container.get("roles") instanceof Collection<?> roles)) {
            return;
        }
        for (Object role : roles) {
            if (role instanceof String name && !name.isBlank()) {
                String normalized = name.trim().toUpperCase(Locale.ROOT).replace('-', '_');
                authorities.add(new SimpleGrantedAuthority(ROLE_PREFIX + normalized));
            }
        }
    }
}
