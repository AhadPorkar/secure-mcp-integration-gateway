package io.ahad.mcpgateway;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

public final class QuotaLimiter {
    private record Bucket(long minute,int count){}
    private final ConcurrentHashMap<String,Bucket> buckets=new ConcurrentHashMap<>();
    private final int perMinute;
    public QuotaLimiter(int perMinute){this.perMinute=perMinute;}
    public boolean allow(SecurityContext c,String tool){
        long m=Instant.now().getEpochSecond()/60; String k=c.tenant()+"|"+c.subject()+"|"+tool;
        Bucket b=buckets.compute(k,(kk,old)-> old==null||old.minute()!=m ? new Bucket(m,1) : new Bucket(m,old.count()+1));
        return b.count()<=perMinute;
    }
}
