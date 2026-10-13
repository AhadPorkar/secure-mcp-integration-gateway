package io.ahad.mcpgateway;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ApprovalStore {
    public static final class ApprovalException extends RuntimeException { private static final long serialVersionUID=1L; public ApprovalException(String m){super(m);} }
    public record Approval(String id,String tenant,String requester,String tool,String revision,String argsHash,long expiresAt,String status,String reviewer,String reviewReason,String receipt){}
    private final ConcurrentHashMap<String,Approval> map=new ConcurrentHashMap<>();
    private final long ttlSeconds;
    public ApprovalStore(long ttlSeconds){this.ttlSeconds=ttlSeconds;}
    public Approval create(String tenant,String requester,String tool,String revision,String argsHash){
        String id=UUID.randomUUID().toString(); Approval a=new Approval(id,tenant,requester,tool,revision,argsHash,Instant.now().getEpochSecond()+ttlSeconds,"PENDING",null,null,null);map.put(id,a);return a;
    }
    public synchronized Approval approve(String id,SecurityContext reviewer,String reason){
        Approval a=require(id); if(!a.tenant().equals(reviewer.tenant()))throw new ApprovalException("approval_not_found"); if(!reviewer.hasRole("approver"))throw new ApprovalException("approver_role_required"); if(a.requester().equals(reviewer.subject()))throw new ApprovalException("self_approval_forbidden"); if(expired(a))throw new ApprovalException("approval_expired"); if(!"PENDING".equals(a.status()))throw new ApprovalException("approval_not_pending");
        Approval n=new Approval(a.id(),a.tenant(),a.requester(),a.tool(),a.revision(),a.argsHash(),a.expiresAt(),"APPROVED",reviewer.subject(),reason,null);map.put(id,n);return n;
    }
    public Approval authorize(String id,SecurityContext caller,String tool,String revision,String argsHash){
        Approval a=require(id); if(!a.tenant().equals(caller.tenant()))throw new ApprovalException("approval_not_found"); if(!a.requester().equals(caller.subject()))throw new ApprovalException("approval_requester_mismatch"); if(expired(a))throw new ApprovalException("approval_expired"); if(!"APPROVED".equals(a.status()) && !"EXECUTED".equals(a.status()))throw new ApprovalException("approval_not_approved"); if(!a.tool().equals(tool)||!a.revision().equals(revision)||!a.argsHash().equals(argsHash))throw new ApprovalException("approval_binding_mismatch"); return a;
    }
    public synchronized Approval markExecuted(String id,String receipt){ Approval a=require(id); if("EXECUTED".equals(a.status()))return a; Approval n=new Approval(a.id(),a.tenant(),a.requester(),a.tool(),a.revision(),a.argsHash(),a.expiresAt(),"EXECUTED",a.reviewer(),a.reviewReason(),receipt);map.put(id,n);return n; }
    public Approval require(String id){ Approval a=map.get(id); if(a==null)throw new ApprovalException("approval_not_found");return a; }
    public List<Approval> forTenant(String tenant){ return map.values().stream().filter(a->tenant.equals(a.tenant())).sorted(Comparator.comparing(Approval::id)).toList(); }
    private boolean expired(Approval a){ return a.expiresAt()<=Instant.now().getEpochSecond(); }
}
