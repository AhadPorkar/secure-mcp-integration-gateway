package io.ahad.mcpgateway;

import java.util.*;

public final class ToolService {
    private final ToolRegistry registry; private final ConnectorHubClient upstream; private final ApprovalStore approvals; private final QuotaLimiter quotas; private final AuditLedger audit;
    public ToolService(ToolRegistry registry,ConnectorHubClient upstream,ApprovalStore approvals,QuotaLimiter quotas,AuditLedger audit){this.registry=registry;this.upstream=upstream;this.approvals=approvals;this.quotas=quotas;this.audit=audit;}
    public Collection<ToolDefinition> list(SecurityContext c){return registry.list(c);}

    public Map<String,Object> call(SecurityContext c,String name,Map<String,Object> args){
        ToolDefinition t=registry.get(name); if(t==null)throw new GatewayException(404,-32602,"unknown_tool");
        if(!c.hasScope(t.requiredScope())){audit.append(c.tenant(),c.subject(),name,"DENY",Map.of("reason","missing_scope"));throw new GatewayException(403,-32003,"insufficient_scope");}
        if(!quotas.allow(c,name)){audit.append(c.tenant(),c.subject(),name,"DENY",Map.of("reason","quota"));throw new GatewayException(429,-32029,"quota_exceeded");}
        validate(name,args);
        try {
            Map<String,Object> result=switch(name){
                case "orders.lookup" -> upstream.lookupOrder(c.tenant(),str(args,"orderId"));
                case "inventory.check" -> upstream.checkInventory(c.tenant(),str(args,"sku"));
                case "customer.documents.list" -> upstream.listDocuments(c.tenant(),str(args,"customerId"));
                case "orders.cancel" -> cancel(c,t,args);
                default -> throw new GatewayException(404,-32602,"unknown_tool");
            };
            audit.append(c.tenant(),c.subject(),name,"ALLOW",Map.of("args",redact(args),"revision",t.revision())); return result;
        } catch(NoSuchElementException e){audit.append(c.tenant(),c.subject(),name,"NOT_FOUND",Map.of("revision",t.revision()));throw new GatewayException(404,-32004,"not_found");}
        catch(ApprovalStore.ApprovalException e){audit.append(c.tenant(),c.subject(),name,"DENY",Map.of("reason",e.getMessage(),"revision",t.revision()));throw new GatewayException(403,-32040,e.getMessage());}
    }

    private Map<String,Object> cancel(SecurityContext c,ToolDefinition t,Map<String,Object> args){
        String orderId=str(args,"orderId"), reason=str(args,"reason");
        Map<String,Object> bound=Map.of("orderId",orderId,"reason",reason); String hash=Crypto.sha256Hex(Json.canonical(bound));
        Object aid=args.get("approvalId");
        if(!(aid instanceof String approvalId)||approvalId.isBlank()){
            ApprovalStore.Approval a=approvals.create(c.tenant(),c.subject(),t.name(),t.revision(),hash);
            audit.append(c.tenant(),c.subject(),"approval.request","PENDING",Map.of("approvalId",a.id(),"tool",t.name(),"revision",t.revision(),"argsHash",hash));
            Map<String,Object> out=new LinkedHashMap<>();out.put("approvalRequired",true);out.put("approvalId",a.id());out.put("tool",t.name());out.put("toolRevision",t.revision());out.put("argumentsFingerprint",hash);out.put("expiresAt",a.expiresAt());return out;
        }
        ApprovalStore.Approval a=approvals.authorize(approvalId,c,t.name(),t.revision(),hash);
        if(a.receipt()!=null)return Json.obj(Json.parse(a.receipt()));
        Map<String,Object> receipt=upstream.cancelOrder(c.tenant(),orderId,reason,approvalId);
        approvals.markExecuted(approvalId,Json.canonical(receipt)); return receipt;
    }

    private static void validate(String name,Map<String,Object> args){
        Set<String> allowed=switch(name){case "orders.lookup"->Set.of("orderId");case "inventory.check"->Set.of("sku");case "customer.documents.list"->Set.of("customerId");case "orders.cancel"->Set.of("orderId","reason","approvalId");default->Set.of();};
        for(String k:args.keySet())if(!allowed.contains(k))throw new GatewayException(400,-32602,"unknown_argument:"+k);
        switch(name){case "orders.lookup" -> identifier(args,"orderId",80);
            case "inventory.check" -> identifier(args,"sku",80);
            case "customer.documents.list" -> identifier(args,"customerId",80);
            case "orders.cancel" -> {
                identifier(args,"orderId",80);
                freeText(args,"reason",160);
                if(args.containsKey("approvalId")) identifier(args,"approvalId",80);
            }
            default -> { }
        }
    }
    private static void identifier(Map<String,Object> a,String k,int max){
        String s=str(a,k);
        if(s.isBlank()||s.length()>max||!s.matches("[A-Za-z0-9._-]+"))throw new GatewayException(400,-32602,"invalid_argument:"+k);
    }
    private static void freeText(Map<String,Object> a,String k,int max){
        String s=str(a,k);
        if(s.isBlank()||s.length()>max||s.chars().anyMatch(ch->ch<0x20 && ch!='\t'))throw new GatewayException(400,-32602,"invalid_argument:"+k);
    }
    private static String str(Map<String,Object>a,String k){Object v=a.get(k);if(!(v instanceof String s))throw new GatewayException(400,-32602,"invalid_argument:"+k);return s;}
    private static Map<String,Object> redact(Map<String,Object> a){Map<String,Object>x=new LinkedHashMap<>(a);if(x.containsKey("approvalId"))x.put("approvalId","[present]");return x;}
}
