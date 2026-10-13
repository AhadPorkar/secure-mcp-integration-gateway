package io.ahad.mcpgateway;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

public final class JwtVerifier {
    public static final class AuthException extends RuntimeException { private static final long serialVersionUID=1L; public AuthException(String m){super(m);} }
    private final byte[] secret; private final String issuer; private final String audience;
    public JwtVerifier(byte[] secret, String issuer, String audience){this.secret=secret.clone();this.issuer=issuer;this.audience=audience;}

    public SecurityContext verifyBearer(String header){
        if(header==null || !header.startsWith("Bearer ")) throw new AuthException("missing_bearer_token");
        return verify(header.substring(7).trim());
    }

    public SecurityContext verify(String token){
        try {
            String[] p=token.split("\\."); if(p.length!=3) throw new AuthException("malformed_token");
            Map<String,Object> h=Json.obj(Json.parse(new String(Crypto.b64urlDecode(p[0]), StandardCharsets.UTF_8)));
            if(!"HS256".equals(h.get("alg"))) throw new AuthException("unsupported_alg");
            byte[] expected=Crypto.hmacSha256(secret,p[0]+"."+p[1]);
            if(!Crypto.constantTime(expected,Crypto.b64urlDecode(p[2]))) throw new AuthException("invalid_signature");
            Map<String,Object> c=Json.obj(Json.parse(new String(Crypto.b64urlDecode(p[1]), StandardCharsets.UTF_8)));
            if(!issuer.equals(c.get("iss"))) throw new AuthException("invalid_issuer");
            if(!audienceMatches(c.get("aud"))) throw new AuthException("invalid_audience");
            long exp=longVal(c.get("exp")); if(exp<=Instant.now().getEpochSecond()) throw new AuthException("token_expired");
            String sub=reqString(c,"sub"); String tenant=reqString(c,"tenant");
            Set<String> scopes=splitSpace(c.get("scope")); Set<String> roles=stringSet(c.get("roles"));
            return new SecurityContext(sub,tenant,Set.copyOf(scopes),Set.copyOf(roles),exp);
        } catch(AuthException e){ throw e; } catch(Exception e){ throw new AuthException("invalid_token"); }
    }

    private boolean audienceMatches(Object v){ if(v instanceof String s)return audience.equals(s); if(v instanceof List<?> l) return l.stream().anyMatch(audience::equals); return false; }
    private static String reqString(Map<String,Object> c,String k){ Object v=c.get(k); if(!(v instanceof String s)||s.isBlank()) throw new AuthException("missing_"+k); return s; }
    private static long longVal(Object v){ if(v instanceof Number n)return n.longValue(); throw new AuthException("invalid_exp"); }
    private static Set<String> splitSpace(Object v){ if(!(v instanceof String s)) return Set.of(); Set<String>x=new HashSet<>(); for(String p:s.split("\\s+"))if(!p.isBlank())x.add(p); return x; }
    private static Set<String> stringSet(Object v){ if(v instanceof List<?> l){Set<String>x=new HashSet<>();for(Object o:l)if(o instanceof String s)x.add(s);return x;} return Set.of(); }
}
