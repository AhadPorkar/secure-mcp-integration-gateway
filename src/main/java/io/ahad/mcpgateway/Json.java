package io.ahad.mcpgateway;

import java.math.BigDecimal;
import java.util.*;

public final class Json {
    private Json() {}

    public static Object parse(String text) {
        return new Parser(text).parse();
    }

    @SuppressWarnings("unchecked")
    public static Map<String,Object> obj(Object v) {
        if (!(v instanceof Map<?,?> m)) throw new IllegalArgumentException("Expected JSON object");
        return (Map<String,Object>) m;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object v) {
        if (!(v instanceof List<?> l)) throw new IllegalArgumentException("Expected JSON array");
        return (List<Object>) l;
    }

    public static String str(Object v) {
        if (!(v instanceof String s)) throw new IllegalArgumentException("Expected string");
        return s;
    }

    public static String stringify(Object value) {
        StringBuilder b = new StringBuilder();
        write(value, b, false);
        return b.toString();
    }

    public static String canonical(Object value) {
        StringBuilder b = new StringBuilder();
        write(value, b, true);
        return b.toString();
    }

    private static void write(Object v, StringBuilder b, boolean canonical) {
        if (v == null) { b.append("null"); return; }
        if (v instanceof String s) { quote(s,b); return; }
        if (v instanceof Boolean || v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte) { b.append(v); return; }
        if (v instanceof Float f) { b.append(BigDecimal.valueOf(f.doubleValue()).stripTrailingZeros().toPlainString()); return; }
        if (v instanceof Double d) { b.append(BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()); return; }
        if (v instanceof BigDecimal bd) { b.append(bd.stripTrailingZeros().toPlainString()); return; }
        if (v instanceof Number n) { b.append(n); return; }
        if (v instanceof Map<?,?> m) {
            b.append('{');
            List<String> keys = new ArrayList<>();
            for (Object k : m.keySet()) keys.add(String.valueOf(k));
            if (canonical) Collections.sort(keys);
            boolean first = true;
            for (String k : keys) {
                if (!first) b.append(','); first = false;
                quote(k,b); b.append(':'); write(m.get(k),b,canonical);
            }
            b.append('}'); return;
        }
        if (v instanceof Iterable<?> it) {
            b.append('['); boolean first = true;
            for (Object x : it) { if (!first) b.append(','); first=false; write(x,b,canonical); }
            b.append(']'); return;
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + v.getClass());
    }

    private static void quote(String s, StringBuilder b) {
        b.append('"');
        for (int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            switch(c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\b' -> b.append("\\b");
                case '\f' -> b.append("\\f");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> {
                    if (c < 0x20) b.append(String.format("\\u%04x", (int)c)); else b.append(c);
                }
            }
        }
        b.append('"');
    }

    private static final class Parser {
        private final String s; private int p;
        Parser(String s){this.s=Objects.requireNonNull(s);}
        Object parse(){ ws(); Object v=value(); ws(); if(p!=s.length()) err("Trailing data"); return v; }
        Object value(){ ws(); if(p>=s.length()) err("Unexpected end"); char c=s.charAt(p);
            if(c=='{') return object(); if(c=='[') return array(); if(c=='"') return string();
            if(c=='t'&&take("true")) return true; if(c=='f'&&take("false")) return false; if(c=='n'&&take("null")) return null;
            if(c=='-' || Character.isDigit(c)) return number(); err("Unexpected token"); return null; }
        Map<String,Object> object(){ p++; ws(); Map<String,Object> m=new LinkedHashMap<>(); if(peek('}')){p++;return m;} while(true){ ws(); String k=string(); ws(); expect(':'); Object v=value(); m.put(k,v); ws(); if(peek('}')){p++;return m;} expect(','); } }
        List<Object> array(){ p++; ws(); List<Object> l=new ArrayList<>(); if(peek(']')){p++;return l;} while(true){l.add(value()); ws(); if(peek(']')){p++;return l;} expect(',');} }
        String string(){ expect('"'); StringBuilder b=new StringBuilder(); while(p<s.length()){ char c=s.charAt(p++); if(c=='"') return b.toString(); if(c=='\\'){ if(p>=s.length())err("Bad escape"); char e=s.charAt(p++); switch(e){case '"'->b.append('"');case '\\'->b.append('\\');case '/'->b.append('/');case 'b'->b.append('\b');case 'f'->b.append('\f');case 'n'->b.append('\n');case 'r'->b.append('\r');case 't'->b.append('\t');case 'u'->{if(p+4>s.length())err("Bad unicode"); b.append((char)Integer.parseInt(s.substring(p,p+4),16));p+=4;}default->err("Bad escape");}} else b.append(c);} err("Unclosed string"); return null; }
        Number number(){ int st=p; if(peek('-'))p++; while(p<s.length()&&Character.isDigit(s.charAt(p)))p++; boolean dec=false; if(peek('.')){dec=true;p++;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;} if(p<s.length()&&(s.charAt(p)=='e'||s.charAt(p)=='E')){dec=true;p++;if(p<s.length()&&(s.charAt(p)=='+'||s.charAt(p)=='-'))p++;while(p<s.length()&&Character.isDigit(s.charAt(p)))p++;} String n=s.substring(st,p); try{return dec?new BigDecimal(n):Long.parseLong(n);}catch(Exception e){err("Bad number");return 0;} }
        boolean take(String x){ if(s.startsWith(x,p)){p+=x.length();return true;}return false; }
        boolean peek(char c){return p<s.length()&&s.charAt(p)==c;}
        void expect(char c){ ws(); if(!peek(c))err("Expected "+c); p++; }
        void ws(){ while(p<s.length()&&Character.isWhitespace(s.charAt(p)))p++; }
        void err(String m){ throw new IllegalArgumentException(m+" at "+p); }
    }
}
