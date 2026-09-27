package com.idet.pos.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTests {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter("pos-frontend");

    @Test
    void mapsRealmAndClientRolesToPrefixedAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("user", "super_admin")))
                .claim("resource_access", Map.of(
                        "pos-frontend", Map.of("roles", List.of("admin")),
                        "other-client", Map.of("roles", List.of("ignored"))))
                .build();

        assertThat(authorities(jwt)).containsExactlyInAnyOrder("ROLE_USER", "ROLE_SUPER_ADMIN", "ROLE_ADMIN");
    }

    @Test
    void returnsNoAuthoritiesWhenTokenHasNoRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "someone")
                .build();

        assertThat(authorities(jwt)).isEmpty();
    }

    private Set<String> authorities(Jwt jwt) {
        return converter.convert(jwt).stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
