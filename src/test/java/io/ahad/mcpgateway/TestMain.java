package io.ahad.mcpgateway;

import com.sun.net.httpserver.HttpServer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

public final class TestMain {
    private static int passed;

    private TestMain() {}

    public static void main(String[] args) {
        testJwt();
        testToolScopes();
        testTenantIsolation();
        testApprovalBinding();
        testSelfApproval();
        testApprovalExpiry();
        testIdempotency();
        testConcurrentIdempotency();
        testAudit();
        testQuota();
        testNoBearerForwarding();
        testUpstreamRestrictions();
        System.out.println("JAVA_CONTRACT_TESTS_PASSED=" + passed);
    }

    private static byte[] secret() {
        return "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    }

    private static SecurityContext ctx(String sub, String tenant, Set<String> scopes, Set<String> roles) {
        return new SecurityContext(sub, tenant, scopes, roles, System.currentTimeMillis() / 1000 + 600);
    }

    private static ToolService svc(ApprovalStore approvals, AuditLedger audit, ConnectorHubClient client, int quota) {
        return new ToolService(new ToolRegistry(), client, approvals, new QuotaLimiter(quota), audit);
    }

    private static void ok(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        passed++;
    }

    private static void throwsMsg(Runnable r, String msg, String name) {
        try {
            r.run();
            throw new AssertionError(name + " did not throw");
        } catch (RuntimeException e) {
            ok(msg.equals(e.getMessage()), name + " got " + e.getMessage());
        }
    }

    private static void testJwt() {
        String token = DemoTokens.issue(secret(), "alice", "acme", "orders:read", List.of("operator"), 60);
        JwtVerifier verifier = new JwtVerifier(secret(), "https://demo.identity.local", "secure-mcp-gateway");
        SecurityContext c = verifier.verify(token);
        ok("alice".equals(c.subject()), "jwt.valid");
        throwsMsg(
                () -> new JwtVerifier(secret(), "https://demo.identity.local", "wrong-audience").verify(token),
                "invalid_audience", "jwt.audience");
        throwsMsg(
                () -> new JwtVerifier(secret(), "https://wrong-issuer.local", "secure-mcp-gateway").verify(token),
                "invalid_issuer", "jwt.issuer");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");
        throwsMsg(() -> verifier.verify(tampered), "invalid_signature", "jwt.signature");
    }

    private static void testToolScopes() {
        ToolRegistry registry = new ToolRegistry();
        SecurityContext c = ctx("a", "acme", Set.of("orders:read"), Set.of());
        ok(registry.list(c).size() == 1, "tools.scope-filter");
        ToolDefinition order = registry.get("orders.lookup");
        ok(Boolean.FALSE.equals(Json.obj(order.asMcpTool().get("annotations")).get("openWorldHint")), "tools.closed-world-annotation");
    }

    private static void testTenantIsolation() {
        ApprovalStore approvals = new ApprovalStore(60);
        AuditLedger audit = new AuditLedger();
        ToolService service = svc(approvals, audit, new DemoConnectorHubClient(), 20);
        SecurityContext alpha = ctx("a", "acme", Set.of("orders:read"), Set.of());
        Map<String,Object> found = service.call(alpha, "orders.lookup", Map.of("orderId", "ORD-A100"));
        ok("ORD-A100".equals(found.get("orderId")), "tenant.own-order");
        throwsMsg(
                () -> service.call(alpha, "orders.lookup", Map.of("orderId", "ORD-B200")),
                "not_found", "tenant.cross-order-hidden");
        throwsMsg(
                () -> service.call(alpha, "orders.lookup", Map.of("orderId", "ORD-A100", "tenant", "beta")),
                "unknown_argument:tenant", "tenant.argument-injection-rejected");
    }

    private static void testApprovalBinding() {
        ApprovalStore approvals = new ApprovalStore(60);
        AuditLedger audit = new AuditLedger();
        ToolService service = svc(approvals, audit, new DemoConnectorHubClient(), 50);
        SecurityContext operator = ctx("alice", "acme", Set.of("orders:cancel"), Set.of("operator"));
        SecurityContext reviewer = ctx("bob", "acme", Set.of(), Set.of("approver"));
        Map<String,Object> proposal = service.call(operator, "orders.cancel", Map.of(
                "orderId", "ORD-A100", "reason", "Customer request"));
        String id = (String) proposal.get("approvalId");
        ApprovalStore.Approval approved = approvals.approve(id, reviewer, "Verified");

        throwsMsg(
                () -> service.call(operator, "orders.cancel", Map.of(
                        "orderId", "ORD-A100", "reason", "Changed reason", "approvalId", id)),
                "approval_binding_mismatch", "approval.args-bound");
        throwsMsg(
                () -> approvals.authorize(id, operator, approved.tool(), "different-revision", approved.argsHash()),
                "approval_binding_mismatch", "approval.revision-bound");
        SecurityContext otherRequester = ctx("mallory", "acme", Set.of("orders:cancel"), Set.of("operator"));
        throwsMsg(
                () -> approvals.authorize(id, otherRequester, approved.tool(), approved.revision(), approved.argsHash()),
                "approval_requester_mismatch", "approval.requester-bound");
    }

    private static void testSelfApproval() {
        ApprovalStore approvals = new ApprovalStore(60);
        String hash = Crypto.sha256Hex(Json.canonical(Map.of("x", 1)));
        ApprovalStore.Approval pending = approvals.create("acme", "alice", "x", "r1", hash);
        SecurityContext alice = ctx("alice", "acme", Set.of(), Set.of("approver"));
        throwsMsg(() -> approvals.approve(pending.id(), alice, "no"), "self_approval_forbidden", "approval.self");
    }

    private static void testApprovalExpiry() {
        ApprovalStore approvals = new ApprovalStore(0);
        String hash = Crypto.sha256Hex(Json.canonical(Map.of("x", 1)));
        ApprovalStore.Approval pending = approvals.create("acme", "alice", "x", "r1", hash);
        SecurityContext reviewer = ctx("bob", "acme", Set.of(), Set.of("approver"));
        throwsMsg(() -> approvals.approve(pending.id(), reviewer, "late"), "approval_expired", "approval.expiry");
    }

    private static void testIdempotency() {
        ApprovalStore approvals = new ApprovalStore(60);
        AuditLedger audit = new AuditLedger();
        DemoConnectorHubClient client = new DemoConnectorHubClient();
        ToolService service = svc(approvals, audit, client, 50);
        SecurityContext operator = ctx("alice", "acme", Set.of("orders:cancel"), Set.of("operator"));
        SecurityContext reviewer = ctx("bob", "acme", Set.of(), Set.of("approver"));
        Map<String,Object> proposal = service.call(operator, "orders.cancel", Map.of(
                "orderId", "ORD-A100", "reason", "Customer request"));
        String id = (String) proposal.get("approvalId");
        approvals.approve(id, reviewer, "Verified");
        Map<String,Object> exact = Map.of("orderId", "ORD-A100", "reason", "Customer request", "approvalId", id);
        Map<String,Object> r1 = service.call(operator, "orders.cancel", exact);
        Map<String,Object> r2 = service.call(operator, "orders.cancel", exact);
        ok(r1.get("receiptId").equals(r2.get("receiptId")), "write.idempotent-retry");
    }

    private static void testConcurrentIdempotency() {
        ApprovalStore approvals = new ApprovalStore(60);
        ToolService service = svc(approvals, new AuditLedger(), new DemoConnectorHubClient(), 200);
        SecurityContext operator = ctx("alice", "acme", Set.of("orders:cancel"), Set.of("operator"));
        SecurityContext reviewer = ctx("bob", "acme", Set.of(), Set.of("approver"));
        Map<String,Object> proposal = service.call(operator, "orders.cancel", Map.of(
                "orderId", "ORD-A100", "reason", "Customer request"));
        String id = (String) proposal.get("approvalId");
        approvals.approve(id, reviewer, "Verified");
        Map<String,Object> exact = Map.of("orderId", "ORD-A100", "reason", "Customer request", "approvalId", id);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 0; i < 32; i++) tasks.add(() -> String.valueOf(service.call(operator, "orders.cancel", exact).get("receiptId")));
            Set<String> receipts = new HashSet<>();
            for (Future<String> f : pool.invokeAll(tasks)) receipts.add(f.get());
            ok(receipts.size() == 1, "write.concurrent-idempotent-retry");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testAudit() {
        AuditLedger audit = new AuditLedger();
        audit.append("acme", "a", "x", "ALLOW", Map.of("secret", "sensitive-value"));
        audit.append("acme", "b", "y", "DENY", Map.of("k", "z"));
        ok(audit.verify(), "audit.chain-valid");
        ok(audit.forTenant("acme").size() == 2, "audit.tenant-read");
        ok(!Json.stringify(audit.forTenant("acme")).contains("sensitive-value"), "audit.details-hashed-not-stored");
    }

    private static void testNoBearerForwarding() {
        HttpServer server = null;
        try {
            server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            AtomicReference<String> auth = new AtomicReference<>();
            AtomicReference<String> serviceKey = new AtomicReference<>();
            server.createContext("/lookup", x -> {
                auth.set(x.getRequestHeaders().getFirst("Authorization"));
                serviceKey.set(x.getRequestHeaders().getFirst("x-connector-service-key"));
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                x.sendResponseHeaders(200, body.length);
                x.getResponseBody().write(body);
                x.close();
            });
            server.start();
            URI base = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            Map<String,String> paths = allPaths("/lookup");
            HttpConnectorHubClient client = new HttpConnectorHubClient(base, "service-secret", paths, Set.of());
            client.lookupOrder("acme", "ORD-A100");
            ok(auth.get() == null, "upstream.no-caller-bearer-forwarding");
            ok("service-secret".equals(serviceKey.get()), "upstream.dedicated-service-credential");
        } catch (Exception e) {
            if (e instanceof RuntimeException r) throw r;
            throw new RuntimeException(e);
        } finally {
            if (server != null) server.stop(0);
        }
    }

    private static void testUpstreamRestrictions() {
        throwsMsg(
                () -> new HttpConnectorHubClient(
                        URI.create("https://evil.invalid"), "x", allPaths("/x"), Set.of()),
                "Upstream host is not allowlisted", "upstream.destination-allowlist");
        throwsMsg(
                () -> new HttpConnectorHubClient(
                        URI.create("http://example.com"), "x", allPaths("/x"), Set.of("example.com")),
                "HTTPS is required for non-loopback upstreams", "upstream.https-required");
        throwsMsg(
                () -> new HttpConnectorHubClient(
                        URI.create("http://127.0.0.1:8080"), "x", allPaths("/../admin"), Set.of()),
                "Upstream paths must be fixed safe relative paths", "upstream.path-traversal-rejected");
        throwsMsg(
                () -> new HttpConnectorHubClient(
                        URI.create("http://user:pass@127.0.0.1:8080"), "x", allPaths("/x"), Set.of()),
                "Upstream base URL must not contain userinfo, query, or fragment", "upstream.userinfo-rejected");
    }

    private static Map<String,String> allPaths(String path) {
        return Map.of(
                "orders.lookup", path,
                "inventory.check", path,
                "customer.documents.list", path,
                "orders.cancel", path);
    }

    private static void testQuota() {
        ApprovalStore approvals = new ApprovalStore(60);
        ToolService service = svc(approvals, new AuditLedger(), new DemoConnectorHubClient(), 1);
        SecurityContext c = ctx("a", "acme", Set.of("orders:read"), Set.of());
        service.call(c, "orders.lookup", Map.of("orderId", "ORD-A100"));
        throwsMsg(
                () -> service.call(c, "orders.lookup", Map.of("orderId", "ORD-A100")),
                "quota_exceeded", "quota.enforced");
    }
}
