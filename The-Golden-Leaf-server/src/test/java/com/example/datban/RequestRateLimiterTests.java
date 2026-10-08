package com.example.datban;

import com.example.datban.security.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import org.springframework.mock.web.*;

class RequestRateLimiterTests {
    static class MutableClock extends Clock {
        final AtomicLong milliseconds = new AtomicLong(60000);
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(milliseconds.get()); }
    }
    @Test void rejectsAfterLimitAndResetsWindow() {
        var clock=new MutableClock(); var limiter=new RequestRateLimiter(clock,true,2,2);
        assertThat(limiter.allow("client")).isTrue(); assertThat(limiter.allow("client")).isTrue();
        assertThat(limiter.allow("client")).isFalse(); clock.milliseconds.addAndGet(60000);
        assertThat(limiter.allow("client")).isTrue();
    }
    @Test void clientsAreIndependent() {
        var limiter=new RequestRateLimiter(Clock.systemUTC(),true,1,2);
        assertThat(limiter.allow("a")).isTrue(); assertThat(limiter.allow("b")).isTrue(); assertThat(limiter.allow("a")).isFalse();
    }
    @Test void memoryBoundFailsClosedAndReclaimsExpiredClients() {
        var clock=new MutableClock(); var limiter=new RequestRateLimiter(clock,true,1,1);
        assertThat(limiter.allow("a")).isTrue(); assertThat(limiter.allow("b")).isFalse();
        clock.milliseconds.addAndGet(60000); assertThat(limiter.allow("b")).isTrue();
    }
    @Test void disabledDevelopmentLimiterDoesNotBlock() {
        var limiter=new RequestRateLimiter(Clock.systemUTC(),false,1,1);
        for(int i=0;i<20;i++) assertThat(limiter.allow("a")).isTrue();
    }
    @Test void rejectsInvalidConfiguration() {
        assertThatThrownBy(()->new RequestRateLimiter(Clock.systemUTC(),true,0,1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void retryAfterIsBoundedToWindow() {
        var clock=new MutableClock(); var limiter=new RequestRateLimiter(clock,true,1,1);
        assertThat(limiter.retryAfterSeconds()).isEqualTo(60); clock.milliseconds.addAndGet(59000);
        assertThat(limiter.retryAfterSeconds()).isEqualTo(1);
    }
    @Test void forgedForwardedHeadersCannotBypassGate() throws Exception {
        var filter=new RateLimitFilter(new RequestRateLimiter(Clock.systemUTC(),true,1,2));
        var first=new MockHttpServletRequest("GET","/api/thucdon"); first.setRemoteAddr("127.0.0.1");
        filter.doFilter(first,new MockHttpServletResponse(),(q,r)->{});
        var second=new MockHttpServletRequest("GET","/api/thucdon"); second.setRemoteAddr("127.0.0.1"); second.addHeader("X-Forwarded-For","1.2.3.4");
        var response=new MockHttpServletResponse(); filter.doFilter(second,response,(q,r)->{throw new AssertionError("Must not reach auth/controller");});
        assertThat(response.getStatus()).isEqualTo(429); assertThat(response.getHeader("Retry-After")).isNotBlank();
        assertThat(response.getContentAsString()).contains("RATE_LIMITED");
    }
    @Test void nonApiStaticTrafficIsNotRateLimited() throws Exception {
        var filter=new RateLimitFilter(new RequestRateLimiter(Clock.systemUTC(),true,1,1));
        for(int i=0;i<3;i++) {
            var response=new MockHttpServletResponse();filter.doFilter(new MockHttpServletRequest("GET","/staff.html"),response,(q,r)->{});
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }
}
