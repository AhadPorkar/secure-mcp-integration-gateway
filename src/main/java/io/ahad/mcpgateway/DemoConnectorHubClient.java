package io.ahad.mcpgateway;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class DemoConnectorHubClient implements ConnectorHubClient {
    private final ConcurrentHashMap<String,Map<String,Object>> orders=new ConcurrentHashMap<>();
    private final Map<String,Integer> stock=new ConcurrentHashMap<>(); private final Map<String,List<Map<String,Object>>> docs=new ConcurrentHashMap<>(); private final ConcurrentHashMap<String,Map<String,Object>> receipts=new ConcurrentHashMap<>();
    public DemoConnectorHubClient(){
        orders.put("acme|ORD-A100",new LinkedHashMap<>(Map.of("orderId","ORD-A100","customerId","CUS-A1","sku","SKU-RED","status","OPEN","amount",149.00)));
        orders.put("beta|ORD-B200",new LinkedHashMap<>(Map.of("orderId","ORD-B200","customerId","CUS-B2","sku","SKU-BLUE","status","OPEN","amount",79.00)));
        stock.put("acme|SKU-RED",7); stock.put("beta|SKU-BLUE",4);
        docs.put("acme|CUS-A1",List.of(Map.of("documentId","DOC-A1","title","Order confirmation","classification","customer")));
        docs.put("beta|CUS-B2",List.of(Map.of("documentId","DOC-B2","title","Invoice","classification","customer")));
    }
    public Map<String,Object> lookupOrder(String tenant,String orderId){ Map<String,Object> x=orders.get(tenant+"|"+orderId); if(x==null)throw new NoSuchElementException("not_found"); return Map.copyOf(x); }
    public Map<String,Object> checkInventory(String tenant,String sku){ Integer q=stock.get(tenant+"|"+sku); if(q==null)throw new NoSuchElementException("not_found"); return Map.of("sku",sku,"available",q,"tenant",tenant); }
    public Map<String,Object> listDocuments(String tenant,String customerId){ List<Map<String,Object>> d=docs.get(tenant+"|"+customerId); if(d==null)throw new NoSuchElementException("not_found"); return Map.of("customerId",customerId,"documents",d); }
    public synchronized Map<String,Object> cancelOrder(String tenant,String orderId,String reason,String idempotencyKey){
        Map<String,Object> prior=receipts.get(idempotencyKey); if(prior!=null)return prior;
        String k=tenant+"|"+orderId; Map<String,Object> o=orders.get(k); if(o==null)throw new NoSuchElementException("not_found");
        if("CANCELLED".equals(o.get("status"))){ Map<String,Object> r=Map.of("receiptId","existing-"+orderId,"orderId",orderId,"status","CANCELLED","idempotentReplay",true); receipts.put(idempotencyKey,r); return r; }
        Map<String,Object> n=new LinkedHashMap<>(o); n.put("status","CANCELLED"); n.put("cancelReason",reason);orders.put(k,n);
        Map<String,Object> r=Map.of("receiptId","rcpt-"+Crypto.sha256Hex(idempotencyKey).substring(0,12),"orderId",orderId,"status","CANCELLED","idempotentReplay",false);receipts.put(idempotencyKey,r);return r;
    }
}
