package io.ahad.mcpgateway;

import java.util.Map;

public interface ConnectorHubClient {
    Map<String,Object> lookupOrder(String tenant,String orderId);
    Map<String,Object> checkInventory(String tenant,String sku);
    Map<String,Object> listDocuments(String tenant,String customerId);
    Map<String,Object> cancelOrder(String tenant,String orderId,String reason,String idempotencyKey);
}
