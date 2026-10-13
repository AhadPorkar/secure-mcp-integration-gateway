package io.ahad.mcpgateway;

import java.time.Instant;
import java.util.*;

public final class AuditLedger {
    private final List<Map<String,Object>> entries=new ArrayList<>();
    private String lastHash="GENESIS";
    public synchronized Map<String,Object> append(String tenant,String actor,String action,String outcome,Object details){
        Map<String,Object> body=new LinkedHashMap<>(); body.put("seq",entries.size()+1);body.put("timestamp",Instant.now().toString());body.put("tenant",tenant);body.put("actor",actor);body.put("action",action);body.put("outcome",outcome);body.put("detailsHash",Crypto.sha256Hex(Json.canonical(details)));body.put("previousHash",lastHash);
        String hash=Crypto.sha256Hex(Json.canonical(body)); body.put("hash",hash); lastHash=hash; Map<String,Object> frozen=Map.copyOf(body); entries.add(frozen); return frozen;
    }
    public synchronized List<Map<String,Object>> forTenant(String tenant){ return entries.stream().filter(e->tenant.equals(e.get("tenant"))).toList(); }
    public synchronized boolean verify(){ String prev="GENESIS"; for(Map<String,Object> e:entries){Map<String,Object> b=new LinkedHashMap<>(e);String h=(String)b.remove("hash");if(!Objects.equals(prev,b.get("previousHash")))return false;if(!Objects.equals(h,Crypto.sha256Hex(Json.canonical(b))))return false;prev=h;}return true; }
}
