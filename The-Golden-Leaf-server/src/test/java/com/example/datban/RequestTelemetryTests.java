package com.example.datban;

import com.example.datban.config.RequestTelemetryFilter;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.*;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import static org.assertj.core.api.Assertions.*;

class RequestTelemetryTests {
    @Test void accessLogExcludesSensitiveDataAndGeneratesOwnCorrelation() throws Exception {
        var logger=(Logger)LoggerFactory.getLogger(RequestTelemetryFilter.class);
        var appender=new ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();appender.start();logger.addAppender(appender);
        try {
            var request=new MockHttpServletRequest("GET","/api/dondat/private-customer-id");
            request.setQueryString("email=secret@example.com");request.addHeader("Authorization","Bearer secret-token");request.addHeader("X-Request-ID","forged-value");
            var response=new MockHttpServletResponse();
            new RequestTelemetryFilter().doFilter(request,response,(q,r)->{((jakarta.servlet.http.HttpServletResponse)r).setStatus(403);});
            assertThat(response.getHeader("X-Request-ID")).matches("[a-f0-9-]{36}");
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage()).contains("status=403","route_group=api")
                .doesNotContain("private-customer-id","secret@example.com","secret-token","forged-value");
            assertThat(MDC.get("requestId")).isNull();
        } finally {logger.detachAppender(appender);appender.stop();MDC.clear();}
    }
    @Test void restoresPreviousMdcEvenWhenChainThrows() {
        MDC.put("requestId","outer-context");
        try {
            assertThatThrownBy(()->new RequestTelemetryFilter().doFilter(new MockHttpServletRequest("POST","/api/staff/bookings/1"),new MockHttpServletResponse(),
                (q,r)->{throw new java.io.IOException("secret-body");})).isInstanceOf(java.io.IOException.class);
            assertThat(MDC.get("requestId")).isEqualTo("outer-context");
        } finally {MDC.clear();}
    }
}
