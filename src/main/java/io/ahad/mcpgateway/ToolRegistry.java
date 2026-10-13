package io.ahad.mcpgateway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolRegistry {
    private final Map<String,ToolDefinition> tools;

    public ToolRegistry() {
        Map<String,ToolDefinition> m = new LinkedHashMap<>();
        m.put("orders.lookup", new ToolDefinition(
                "orders.lookup", "2026-10-12.1",
                "Look up one order in the caller tenant.",
                "orders:read", false,
                schema(Map.of("orderId", idProp("Order identifier", 80)), List.of("orderId"))));
        m.put("inventory.check", new ToolDefinition(
                "inventory.check", "2026-10-12.1",
                "Check stock for one SKU in the caller tenant.",
                "inventory:read", false,
                schema(Map.of("sku", idProp("Stock keeping unit", 80)), List.of("sku"))));
        m.put("customer.documents.list", new ToolDefinition(
                "customer.documents.list", "2026-10-12.1",
                "List customer documents already authorized for the caller tenant.",
                "documents:read", false,
                schema(Map.of("customerId", idProp("Customer identifier", 80)), List.of("customerId"))));

        Map<String,Object> cancelProps = new LinkedHashMap<>();
        cancelProps.put("orderId", idProp("Order identifier", 80));
        cancelProps.put("reason", Map.of(
                "type", "string", "minLength", 1, "maxLength", 160,
                "description", "Business reason for the cancellation"));
        cancelProps.put("approvalId", idProp("Approval identifier returned by a prior call", 80));
        m.put("orders.cancel", new ToolDefinition(
                "orders.cancel", "2026-10-12.1",
                "Cancel an order only after independent approval bound to this tool revision and exact canonical arguments.",
                "orders:cancel", true,
                schema(cancelProps, List.of("orderId", "reason"))));
        tools = Map.copyOf(m);
    }

    public Collection<ToolDefinition> list(SecurityContext c) {
        List<ToolDefinition> x = new ArrayList<>();
        for (ToolDefinition t : tools.values()) if (c.hasScope(t.requiredScope())) x.add(t);
        return x;
    }

    public ToolDefinition get(String name) { return tools.get(name); }

    private static Map<String,Object> idProp(String description, int max) {
        return Map.of(
                "type", "string",
                "minLength", 1,
                "maxLength", max,
                "pattern", "^[A-Za-z0-9._-]+$",
                "description", description);
    }

    private static Map<String,Object> schema(Map<String,Object> props, List<String> required) {
        Map<String,Object> s = new LinkedHashMap<>();
        s.put("$schema", "https://json-schema.org/draft/2020-12/schema");
        s.put("type", "object");
        s.put("properties", props);
        s.put("required", required);
        s.put("additionalProperties", false);
        return s;
    }
}
