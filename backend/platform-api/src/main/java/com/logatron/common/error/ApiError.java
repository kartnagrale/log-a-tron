package com.logatron.common.error;
import java.time.Instant;import java.util.List;
public record ApiError(Instant timestamp,int status,String errorCode,String message,String traceId,List<FieldError> fieldErrors){public record FieldError(String field,String code,String message){}}

