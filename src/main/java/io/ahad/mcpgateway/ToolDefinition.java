package io.ahad.mcpgateway;

import java.util.LinkedHashMap;
import java.util.Map;

public record ToolDefinition(
        String name,
        String revision,
        String description,
        String requiredScope,
        boolean write,
        Map<String,Object> inputSchema) {

    public Map<String,Object> asMcpTool() {
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("description", description);
        m.put("inputSchema", inputSchema);
        m.put("outputSchema", Map.of("type", "object"));
        m.put("annotations", Map.of(
                "title", title(),
                "readOnlyHint", !write,
                "destructiveHint", write,
                "idempotentHint", true,
                "openWorldHint", false));
        m.put("_meta", Map.of(
                "io.ahad/toolRevision", revision,
                "io.ahad/requiresApproval", write));
        return m;
    }

    private String title() {
        return switch (name) {
            case "orders.lookup" -> "Look up order";
            case "inventory.check" -> "Check inventory";
            case "customer.documents.list" -> "List permitted customer documents";
            case "orders.cancel" -> "Request or execute approved order cancellation";
            default -> name;
        };
    }
}
