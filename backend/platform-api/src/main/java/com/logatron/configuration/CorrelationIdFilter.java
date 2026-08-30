package com.logatron.configuration;

import jakarta.servlet.*;import jakarta.servlet.http.*;import org.slf4j.MDC;import org.springframework.core.Ordered;import org.springframework.core.annotation.Order;import org.springframework.stereotype.Component;import java.io.IOException;import java.util.UUID;import java.util.regex.Pattern;

@Component @Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    private static final Pattern SAFE=Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException{
        String supplied=request.getHeader("X-Correlation-ID");String id=supplied!=null&&SAFE.matcher(supplied).matches()?supplied:UUID.randomUUID().toString();
        MDC.put("correlationId",id);if(MDC.get("traceId")==null)MDC.put("traceId",id);response.setHeader("X-Correlation-ID",id);
        try{chain.doFilter(request,response);}finally{MDC.remove("correlationId");MDC.remove("traceId");}
    }
}

