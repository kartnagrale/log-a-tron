package com.logatron.logquery.domain;
import com.logatron.common.error.ApiException;import java.util.*;
public enum CorrelationIdentifier{TRACE_ID("trace_id"),REQUEST_ID("request_id"),CORRELATION_ID("correlation_id"),TRANSACTION_ID("transaction_id"),ORDER_TOKEN("order_token"),AUCTION_ID("auction_id");
 private final String column;CorrelationIdentifier(String column){this.column=column;}public String column(){return column;}
 public static CorrelationIdentifier parse(String value){if(value==null)throw ApiException.invalid("identifierType is required.");try{return valueOf(value.trim().replaceAll("([a-z])([A-Z])","$1_$2").toUpperCase(Locale.ROOT));}catch(IllegalArgumentException ex){throw ApiException.invalid("identifierType is not supported.");}}
}
