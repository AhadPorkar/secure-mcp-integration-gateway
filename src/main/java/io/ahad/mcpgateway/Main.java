package io.ahad.mcpgateway;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        int port = intEnv("MCP_GATEWAY_PORT", 8096, 1, 65535);
        String secret = System.getenv("MCP_DEMO_HS256_SECRET");
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("MCP_DEMO_HS256_SECRET must be at least 32 characters");
        }

        String issuer = System.getenv().getOrDefault("MCP_TOKEN_ISSUER", "https://demo.identity.local");
        String audience = System.getenv().getOrDefault("MCP_TOKEN_AUDIENCE", "secure-mcp-gateway");
        String authorizationServer = System.getenv().getOrDefault("MCP_AUTHORIZATION_SERVER", issuer);
        String resourceUrl = System.getenv().getOrDefault("MCP_RESOURCE_URL", "http://127.0.0.1:" + port + "/mcp");
        validateHttpUrl(resourceUrl, "MCP_RESOURCE_URL");
        validateHttpUrl(authorizationServer, "MCP_AUTHORIZATION_SERVER");

        byte[] key = secret.getBytes(StandardCharsets.UTF_8);
        JwtVerifier jwt = new JwtVerifier(key, issuer, audience);
        ToolRegistry registry = new ToolRegistry();
        ApprovalStore approvals = new ApprovalStore(intEnv("MCP_APPROVAL_TTL_SECONDS", 600, 5, 86_400));
        AuditLedger audit = new AuditLedger();
        ConnectorHubClient upstream = upstreamFromEnvironment();
        ToolService tools = new ToolService(
                registry,
                upstream,
                approvals,
                new QuotaLimiter(intEnv("MCP_QUOTA_PER_MINUTE", 30, 1, 100_000)),
                audit);

        GatewayServer server = new GatewayServer(port, jwt, tools, approvals, audit, resourceUrl, authorizationServer);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.println("Secure MCP Integration Gateway " + GatewayServer.VERSION + " listening on http://127.0.0.1:" + server.port());
        System.out.println("MCP resource: " + resourceUrl);
        System.out.println("Upstream mode: " + (upstream instanceof DemoConnectorHubClient ? "synthetic demo" : "Connector Hub HTTP"));
        System.out.println("Admin console: http://127.0.0.1:" + server.port() + "/admin");
        Thread.currentThread().join();
    }

    private static ConnectorHubClient upstreamFromEnvironment() {
        String base = System.getenv("CONNECTOR_HUB_BASE_URL");
        if (base == null || base.isBlank()) return new DemoConnectorHubClient();

        String key = req("CONNECTOR_HUB_SERVICE_KEY");
        Map<String,String> paths = new LinkedHashMap<>();
        paths.put("orders.lookup", req("CONNECTOR_HUB_ORDER_LOOKUP_PATH"));
        paths.put("inventory.check", req("CONNECTOR_HUB_INVENTORY_CHECK_PATH"));
        paths.put("customer.documents.list", req("CONNECTOR_HUB_DOCUMENTS_LIST_PATH"));
        paths.put("orders.cancel", req("CONNECTOR_HUB_ORDER_CANCEL_PATH"));

        Set<String> hosts = new HashSet<>();
        String raw = System.getenv().getOrDefault("CONNECTOR_HUB_ALLOWED_HOSTS", "");
        for (String h : raw.split(",")) if (!h.isBlank()) hosts.add(h.trim().toLowerCase(java.util.Locale.ROOT));

        boolean bearer = "bearer".equalsIgnoreCase(System.getenv().getOrDefault("CONNECTOR_HUB_CREDENTIAL_MODE", "service-header"));
        return new HttpConnectorHubClient(URI.create(base), key, paths, hosts, bearer);
    }

    private static int intEnv(String name, int fallback, int min, int max) {
        String raw = System.getenv(name);
        int value = raw == null || raw.isBlank() ? fallback : Integer.parseInt(raw);
        if (value < min || value > max) throw new IllegalArgumentException(name + " is out of range");
        return value;
    }

    private static void validateHttpUrl(String value, String name) {
        URI u = URI.create(value);
        if (!("http".equalsIgnoreCase(u.getScheme()) || "https".equalsIgnoreCase(u.getScheme())) || u.getHost() == null) {
            throw new IllegalArgumentException(name + " must be an absolute HTTP(S) URL");
        }
    }

    private static String req(String name) {
        String v = System.getenv(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException(name + " is required when CONNECTOR_HUB_BASE_URL is configured");
        }
        return v;
    }
}
