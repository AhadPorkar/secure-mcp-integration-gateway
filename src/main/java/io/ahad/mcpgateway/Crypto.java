package io.ahad.mcpgateway;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public final class Crypto {
    private Crypto() {}
    public static String sha256Hex(String s) {
        try { return hex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static byte[] hmacSha256(byte[] secret, String s) {
        try { Mac m=Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(secret,"HmacSHA256")); return m.doFinal(s.getBytes(StandardCharsets.UTF_8)); }
        catch(Exception e){ throw new IllegalStateException(e); }
    }
    public static boolean constantTime(byte[] a, byte[] b){ return MessageDigest.isEqual(a,b); }
    public static String b64url(byte[] b){ return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    public static byte[] b64urlDecode(String s){ return Base64.getUrlDecoder().decode(s); }
    private static String hex(byte[] b){ StringBuilder x=new StringBuilder(); for(byte v:b)x.append(String.format("%02x",v)); return x.toString(); }
}
