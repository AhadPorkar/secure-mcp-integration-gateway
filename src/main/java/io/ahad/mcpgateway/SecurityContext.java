package io.ahad.mcpgateway;

import java.util.Set;

public record SecurityContext(String subject, String tenant, Set<String> scopes, Set<String> roles, long expiresAt) {
    public boolean hasScope(String scope){ return scopes.contains(scope); }
    public boolean hasRole(String role){ return roles.contains(role); }
}
