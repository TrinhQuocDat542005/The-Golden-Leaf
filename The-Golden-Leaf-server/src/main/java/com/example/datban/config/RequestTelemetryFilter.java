package com.example.datban.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** No URL/query/body/headers/user identifiers in access logs. Server generates correlation IDs. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestTelemetryFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestTelemetryFilter.class);
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String previous = MDC.get("requestId");
        String id = UUID.randomUUID().toString();
        MDC.put("requestId", id);
        response.setHeader("X-Request-ID", id);
        long start = System.nanoTime();
        int status = 500;
        try { chain.doFilter(request, response); status = response.getStatus(); }
        finally {
            log.info("http_request method={} route_group={} status={} duration_ms={}",
                    safeMethod(request.getMethod()), routeGroup(request.getRequestURI()), status,
                    (System.nanoTime() - start) / 1_000_000);
            if (previous == null) MDC.remove("requestId"); else MDC.put("requestId", previous);
        }
    }
    private String safeMethod(String method) {
        return java.util.Set.of("GET","POST","PUT","PATCH","DELETE","HEAD","OPTIONS").contains(method) ? method : "OTHER";
    }
    private String routeGroup(String path) {
        if (path.startsWith("/actuator/")) return "management";
        if (path.startsWith("/api/admin/")) return "admin";
        if (path.startsWith("/api/staff/")) return "staff";
        if (path.startsWith("/api/auth/")) return "auth";
        if (path.startsWith("/api/")) return "api";
        return "web";
    }
}
