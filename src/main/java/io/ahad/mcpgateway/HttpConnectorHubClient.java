package io.ahad.mcpgateway;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Optional adapter for a running Connector Hub instance.
 * The caller bearer token is deliberately not accepted by this class.
 * Only a dedicated service credential is sent upstream.
 */
public final class HttpConnectorHubClient implements ConnectorHubClient {
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final URI base;
    private final String serviceKey;
    private final boolean bearerCredential;
    private final Map<String,String> paths;

    public HttpConnectorHubClient(URI base, String serviceKey, Map<String,String> paths, Set<String> allowedHosts) {
        this(base, serviceKey, paths, allowedHosts, false);
    }

    public HttpConnectorHubClient(
            URI base,
            String serviceKey,
            Map<String,String> paths,
            Set<String> allowedHosts,
            boolean bearerCredential) {
        String scheme = base.getScheme() == null ? "" : base.getScheme().toLowerCase(Locale.ROOT);
        if (!Set.of("http", "https").contains(scheme)) throw new IllegalArgumentException("Unsupported upstream scheme");
        if (base.getHost() == null) throw new IllegalArgumentException("Upstream host is required");
        if (base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null) {
            throw new IllegalArgumentException("Upstream base URL must not contain userinfo, query, or fragment");
        }

        String host = base.getHost().toLowerCase(Locale.ROOT);
        Set<String> normalizedHosts = new HashSet<>();
        for (String h : allowedHosts) normalizedHosts.add(h.toLowerCase(Locale.ROOT));
        boolean loopback = isLoopback(host);
        if (!loopback && !normalizedHosts.contains(host)) throw new IllegalArgumentException("Upstream host is not allowlisted");
        if (!loopback && !"https".equals(scheme)) throw new IllegalArgumentException("HTTPS is required for non-loopback upstreams");

        this.base = base;
        this.serviceKey = Objects.requireNonNull(serviceKey);
        if (serviceKey.isBlank()) throw new IllegalArgumentException("Service credential must not be blank");
        this.bearerCredential = bearerCredential;
        this.paths = Map.copyOf(paths);
        for (String p : paths.values()) validatePath(p);
    }

    @Override public Map<String,Object> lookupOrder(String tenant, String orderId) {
        return call("orders.lookup", tenant, Map.of("orderId", orderId), null);
    }

    @Override public Map<String,Object> checkInventory(String tenant, String sku) {
        return call("inventory.check", tenant, Map.of("sku", sku), null);
    }

    @Override public Map<String,Object> listDocuments(String tenant, String customerId) {
        return call("customer.documents.list", tenant, Map.of("customerId", customerId), null);
    }

    @Override public Map<String,Object> cancelOrder(String tenant, String orderId, String reason, String idempotencyKey) {
        return call("orders.cancel", tenant, Map.of("orderId", orderId, "reason", reason), idempotencyKey);
    }

    private Map<String,Object> call(String operation, String tenant, Map<String,Object> input, String idempotencyKey) {
        String path = paths.get(operation);
        if (path == null) throw new IllegalStateException("No upstream mapping for " + operation);
        try {
            Map<String,Object> body = new java.util.LinkedHashMap<>();
            body.put("tenant", tenant);
            body.put("input", input);
            HttpRequest.Builder b = HttpRequest.newBuilder(base.resolve(path))
                    .timeout(Duration.ofSeconds(8))
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(Json.stringify(body)));
            if (bearerCredential) b.header("Authorization", "Bearer " + serviceKey);
            else b.header("x-connector-service-key", serviceKey);
            if (idempotencyKey != null) b.header("idempotency-key", idempotencyKey);

            // Deliberately no caller Authorization forwarding here.
            HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() / 100 != 2) throw new IllegalStateException("Connector Hub returned HTTP " + r.statusCode());
            return Json.obj(Json.parse(r.body()));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Connector Hub call failed", e);
        }
    }

    private static void validatePath(String p) {
        if (!p.startsWith("/")
                || p.contains("://")
                || p.contains("..")
                || p.contains("?")
                || p.contains("#")
                || p.contains("\\")
                || p.indexOf('\r') >= 0
                || p.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Upstream paths must be fixed safe relative paths");
        }
    }

    private static boolean isLoopback(String h) {
        return h.equals("127.0.0.1") || h.equals("localhost") || h.equals("::1") || h.equals("0:0:0:0:0:0:0:1");
    }
}
