package io.ahad.mcpgateway;

public final class GatewayException extends RuntimeException {
    private static final long serialVersionUID=1L;
    private final int httpStatus; private final int rpcCode;
    public GatewayException(int httpStatus,int rpcCode,String message){super(message);this.httpStatus=httpStatus;this.rpcCode=rpcCode;}
    public int httpStatus(){return httpStatus;} public int rpcCode(){return rpcCode;}
}
