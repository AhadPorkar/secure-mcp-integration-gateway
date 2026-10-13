package io.ahad.mcpgateway;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public final class DemoTokens {
    private DemoTokens() {}
    public static String issue(byte[] secret,String sub,String tenant,String scope,List<String> roles,long ttlSeconds){
        String h=Crypto.b64url(Json.stringify(Map.of("alg","HS256","typ","JWT")).getBytes(StandardCharsets.UTF_8));
        Map<String,Object> claims=new LinkedHashMap<>(); claims.put("iss","https://demo.identity.local"); claims.put("aud","secure-mcp-gateway"); claims.put("sub",sub); claims.put("tenant",tenant); claims.put("scope",scope); claims.put("roles",roles); claims.put("exp",Instant.now().getEpochSecond()+ttlSeconds);
        String p=Crypto.b64url(Json.stringify(claims).getBytes(StandardCharsets.UTF_8));
        return h+"."+p+"."+Crypto.b64url(Crypto.hmacSha256(secret,h+"."+p));
    }
}
