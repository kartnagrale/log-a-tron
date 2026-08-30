package com.logatron.common.error;
import org.springframework.http.HttpStatus;
public class ApiException extends RuntimeException {
    private final ErrorCode code; private final HttpStatus status;
    public ApiException(HttpStatus status,ErrorCode code,String message){super(message);this.status=status;this.code=code;}
    public ErrorCode code(){return code;} public HttpStatus status(){return status;}
    public static ApiException notFound(){return new ApiException(HttpStatus.NOT_FOUND,ErrorCode.RESOURCE_NOT_FOUND,"The requested resource was not found.");}
    public static ApiException denied(){return notFound();}
    public static ApiException invalid(String message){return new ApiException(HttpStatus.BAD_REQUEST,ErrorCode.INVALID_REQUEST,message);}
    public static ApiException timeout(){return new ApiException(HttpStatus.GATEWAY_TIMEOUT,ErrorCode.QUERY_TIMEOUT,"The log store query timed out; no partial result was returned.");}
    public static ApiException unavailable(String dependency){return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,ErrorCode.DEPENDENCY_UNAVAILABLE,dependency+" is unavailable; no partial result was returned.");}
    public static ApiException rateLimited(){return new ApiException(HttpStatus.TOO_MANY_REQUESTS,ErrorCode.RATE_LIMITED,"The query concurrency limit is currently reached.");}
}
