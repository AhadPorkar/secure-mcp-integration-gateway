package io.ahad.mcpgateway;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class GatewayServer implements AutoCloseable {
    public static final String PROTOCOL = "2026-07-28";
    public static final String VERSION = "1.0.0-rc.1";

    private final HttpServer server;
    private final JwtVerifier jwt;
    private final ToolService tools;
    private final ApprovalStore approvals;
    private final AuditLedger audit;
    private final String resourceUrl;
    private final String authorizationServer;
    private final String resourceMetadataUrl;

    public GatewayServer(
            int port,
            JwtVerifier jwt,
            ToolService tools,
            ApprovalStore approvals,
            AuditLedger audit,
            String resourceUrl,
            String authorizationServer) throws IOException {
        this.jwt = jwt;
        this.tools = tools;
        this.approvals = approvals;
        this.audit = audit;
        this.resourceUrl = resourceUrl;
        this.authorizationServer = authorizationServer;
        this.resourceMetadataUrl = protectedResourceMetadataUrl(resourceUrl);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.createContext("/health", x -> json(x, 200, Map.of(
                "status", "ok",
                "protocol", PROTOCOL,
                "version", VERSION)));
        server.createContext("/.well-known/oauth-protected-resource/mcp", this::protectedResourceMetadata);
        server.createContext("/.well-known/oauth-protected-resource", this::protectedResourceMetadata);
        server.createContext("/mcp", this::mcp);
        server.createContext("/admin/api/approvals", this::adminApprovals);
        server.createContext("/admin/api/audit", this::adminAudit);
        server.createContext("/admin/assets/app.css", x -> text(x, 200, "text/css; charset=utf-8", AdminAssets.css()));
        server.createContext("/admin/assets/app.js", x -> text(x, 200, "text/javascript; charset=utf-8", AdminAssets.js()));
        server.createContext("/admin", x -> text(x, 200, "text/html; charset=utf-8", AdminPage.html()));
    }

    public void start() { server.start(); }
    public int port() { return server.getAddress().getPort(); }
    @Override public void close() { server.stop(0); }

    private void protectedResourceMetadata(HttpExchange x) throws IOException {
        if (!"GET".equals(x.getRequestMethod())) {
            metadataJson(x, 405, Map.of("error", "method_not_allowed"));
            return;
        }
        metadataJson(x, 200, Map.of(
                "resource", resourceUrl,
                "authorization_servers", List.of(authorizationServer),
                "scopes_supported", List.of("orders:read", "inventory:read", "documents:read", "orders:cancel"),
                "bearer_methods_supported", List.of("header"),
                "resource_name", "Secure MCP Integration Gateway"));
    }

    private void mcp(HttpExchange x) throws IOException {
        if (!"POST".equals(x.getRequestMethod())) {
            json(x, 405, Map.of("error", "method_not_allowed"));
            return;
        }
        Object id = null;
        try {
            Map<String,Object> req = Json.obj(Json.parse(readBody(x)));
            id = req.get("id");
            String method = (String) req.get("method");
            if (method == null) throw new GatewayException(400, -32600, "invalid_request");
            validateProtocol(x, req, method);

            if ("server/discover".equals(method)) {
                rpcResult(x, id, discover());
                return;
            }

            SecurityContext c = jwt.verifyBearer(x.getRequestHeaders().getFirst("Authorization"));
            if ("tools/list".equals(method)) {
                List<Object> list = tools.list(c).stream()
                        .map(ToolDefinition::asMcpTool)
                        .map(v -> (Object) v)
                        .toList();
                Map<String,Object> out = new LinkedHashMap<>();
                out.put("resultType", "complete");
                out.put("tools", list);
                out.put("ttlMs", 15_000);
                out.put("cacheScope", "private");
                out.put("_meta", serverMeta());
                rpcResult(x, id, out);
                return;
            }

            if ("tools/call".equals(method)) {
                Map<String,Object> params = Json.obj(req.get("params"));
                String name = Json.str(params.get("name"));
                String headerName = x.getRequestHeaders().getFirst("Mcp-Name");
                if (!name.equals(headerName)) throw new GatewayException(400, -32020, "header_mismatch:mcp-name");
                Map<String,Object> args = params.get("arguments") instanceof Map<?,?>
                        ? Json.obj(params.get("arguments"))
                        : Map.of();
                Map<String,Object> result = tools.call(c, name, args);
                Map<String,Object> out = new LinkedHashMap<>();
                out.put("resultType", "complete");
                out.put("content", List.of(Map.of("type", "text", "text", Json.stringify(result))));
                out.put("structuredContent", result);
                out.put("isError", false);
                out.put("_meta", serverMeta());
                rpcResult(x, id, out);
                return;
            }
            throw new GatewayException(404, -32601, "method_not_found");
        } catch (JwtVerifier.AuthException e) {
            bearerChallenge(x, "invalid_token", null);
            rpcError(x, 401, id, -32001, e.getMessage());
        } catch (GatewayException e) {
            if (e.httpStatus() == 403 && "insufficient_scope".equals(e.getMessage())) {
                bearerChallenge(x, "insufficient_scope", null);
            }
            rpcError(x, e.httpStatus(), id, e.rpcCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            rpcError(x, 400, id, -32602, "invalid_params");
        } catch (Exception e) {
            rpcError(x, 500, id, -32603, "internal_error");
        }
    }

    private void validateProtocol(HttpExchange x, Map<String,Object> req, String method) {
        String headerVersion = x.getRequestHeaders().getFirst("MCP-Protocol-Version");
        if (!PROTOCOL.equals(headerVersion)) throw new GatewayException(400, -32020, "unsupported_protocol_version");
        String headerMethod = x.getRequestHeaders().getFirst("Mcp-Method");
        if (!method.equals(headerMethod)) throw new GatewayException(400, -32020, "header_mismatch:mcp-method");

        Map<String,Object> params = req.get("params") instanceof Map<?,?> ? Json.obj(req.get("params")) : Map.of();
        Object metaObject = params.get("_meta");
        if (!(metaObject instanceof Map<?,?>)) throw new GatewayException(400, -32602, "missing_request_meta");
        Map<String,Object> meta = Json.obj(metaObject);
        if (!PROTOCOL.equals(meta.get("io.modelcontextprotocol/protocolVersion"))) {
            throw new GatewayException(400, -32020, "meta_protocol_mismatch");
        }
        if (!(meta.get("io.modelcontextprotocol/clientCapabilities") instanceof Map<?,?>)) {
            throw new GatewayException(400, -32602, "missing_client_capabilities");
        }
    }

    private Map<String,Object> discover() {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("resultType", "complete");
        out.put("supportedVersions", List.of(PROTOCOL));
        out.put("capabilities", Map.of("tools", Map.of("listChanged", false)));
        out.put("instructions", "Tools are tenant-scoped. Consequential writes require independent approval bound to the exact tool revision and canonical arguments.");
        out.put("ttlMs", 60_000);
        out.put("cacheScope", "public");
        out.put("_meta", serverMeta());
        return out;
    }

    private Map<String,Object> serverMeta() {
        return Map.of("io.modelcontextprotocol/serverInfo", Map.of(
                "name", "secure-connector-hub-gateway",
                "title", "Secure MCP Integration Gateway",
                "version", VERSION));
    }

    private void adminApprovals(HttpExchange x) throws IOException {
        try {
            SecurityContext c = jwt.verifyBearer(x.getRequestHeaders().getFirst("Authorization"));
            if (!c.hasRole("approver")) throw new GatewayException(403, -32003, "approver_role_required");
            String path = x.getRequestURI().getPath();
            String base = "/admin/api/approvals";
            if (path.equals(base) && "GET".equals(x.getRequestMethod())) {
                List<Object> items = approvals.forTenant(c.tenant()).stream()
                        .map(this::approvalMap)
                        .map(v -> (Object) v)
                        .toList();
                json(x, 200, Map.of("items", items));
                return;
            }
            if (path.startsWith(base + "/") && path.endsWith("/approve") && "POST".equals(x.getRequestMethod())) {
                String rest = path.substring((base + "/").length());
                String id = rest.substring(0, rest.length() - "/approve".length());
                Map<String,Object> body = Json.obj(Json.parse(readBody(x)));
                String reason = Json.str(body.get("reason"));
                if (reason.isBlank() || reason.length() > 500) throw new IllegalArgumentException("invalid_reason");
                ApprovalStore.Approval a = approvals.approve(id, c, reason);
                audit.append(c.tenant(), c.subject(), "approval.approve", "APPROVED", Map.of(
                        "approvalId", a.id(), "tool", a.tool(), "revision", a.revision()));
                json(x, 200, approvalMap(a));
                return;
            }
            json(x, 404, Map.of("error", "not_found"));
        } catch (JwtVerifier.AuthException e) {
            bearerChallenge(x, "invalid_token", null);
            json(x, 401, Map.of("error", e.getMessage()));
        } catch (ApprovalStore.ApprovalException | GatewayException e) {
            json(x, 403, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            json(x, 400, Map.of("error", "invalid_request"));
        }
    }

    private void adminAudit(HttpExchange x) throws IOException {
        try {
            SecurityContext c = jwt.verifyBearer(x.getRequestHeaders().getFirst("Authorization"));
            if (!c.hasRole("approver")) throw new GatewayException(403, -32003, "approver_role_required");
            json(x, 200, Map.of("chainValid", audit.verify(), "items", audit.forTenant(c.tenant())));
        } catch (JwtVerifier.AuthException e) {
            bearerChallenge(x, "invalid_token", null);
            json(x, 401, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            json(x, 403, Map.of("error", "forbidden"));
        }
    }

    private Map<String,Object> approvalMap(ApprovalStore.Approval a) {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", a.id());
        m.put("tenant", a.tenant());
        m.put("requester", a.requester());
        m.put("tool", a.tool());
        m.put("revision", a.revision());
        m.put("argumentsFingerprint", a.argsHash());
        m.put("expiresAt", a.expiresAt());
        m.put("status", a.status());
        m.put("reviewer", a.reviewer());
        m.put("reviewReason", a.reviewReason());
        m.put("hasReceipt", a.receipt() != null);
        return m;
    }

    private void bearerChallenge(HttpExchange x, String error, String scope) {
        StringBuilder value = new StringBuilder("Bearer");
        if (error != null) value.append(" error=\"").append(error).append("\"");
        value.append(", resource_metadata=\"").append(resourceMetadataUrl).append("\"");
        if (scope != null) value.append(", scope=\"").append(scope).append("\"");
        x.getResponseHeaders().set("WWW-Authenticate", value.toString());
    }

    private static String protectedResourceMetadataUrl(String resourceUrl) {
        java.net.URI u = java.net.URI.create(resourceUrl);
        String path = u.getPath();
        if (path == null || path.isBlank() || "/".equals(path)) path = "";
        return u.getScheme() + "://" + u.getAuthority() + "/.well-known/oauth-protected-resource" + path;
    }

    private void rpcResult(HttpExchange x, Object id, Object result) throws IOException {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("result", result);
        json(x, 200, m);
    }

    private void rpcError(HttpExchange x, int status, Object id, int code, String msg) throws IOException {
        Map<String,Object> err = Map.of("code", code, "message", msg);
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("jsonrpc", "2.0");
        m.put("id", id);
        m.put("error", err);
        json(x, status, m);
    }

    private static String readBody(HttpExchange x) throws IOException {
        return new String(x.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static void json(HttpExchange x, int status, Object body) throws IOException {
        byte[] b = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
        securityHeaders(x);
        x.getResponseHeaders().set("content-type", "application/json; charset=utf-8");
        x.sendResponseHeaders(status, b.length);
        try (OutputStream o = x.getResponseBody()) { o.write(b); }
    }

    private static void metadataJson(HttpExchange x, int status, Object body) throws IOException {
        byte[] b = Json.stringify(body).getBytes(StandardCharsets.UTF_8);
        securityHeaders(x);
        x.getResponseHeaders().set("access-control-allow-origin", "*");
        x.getResponseHeaders().set("cache-control", "public, max-age=300");
        x.getResponseHeaders().set("content-type", "application/json; charset=utf-8");
        x.sendResponseHeaders(status, b.length);
        try (OutputStream o = x.getResponseBody()) { o.write(b); }
    }

    private static void text(HttpExchange x, int status, String contentType, String value) throws IOException {
        byte[] b = value.getBytes(StandardCharsets.UTF_8);
        securityHeaders(x);
        x.getResponseHeaders().set("content-type", contentType);
        x.sendResponseHeaders(status, b.length);
        try (OutputStream o = x.getResponseBody()) { o.write(b); }
    }

    private static void securityHeaders(HttpExchange x) {
        Headers h = x.getResponseHeaders();
        h.set("cache-control", "no-store");
        h.set("x-content-type-options", "nosniff");
        h.set("x-frame-options", "DENY");
        h.set("referrer-policy", "no-referrer");
        h.set("content-security-policy", "default-src 'self'; script-src 'self'; style-src 'self'; connect-src 'self'; object-src 'none'; base-uri 'none'; frame-ancestors 'none'");
    }
}
