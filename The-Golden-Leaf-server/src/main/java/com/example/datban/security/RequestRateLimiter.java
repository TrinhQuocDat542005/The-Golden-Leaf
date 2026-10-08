package com.example.datban.security;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Bounded, instance-local safety net. Edge proxy provides per-IP limits across replicas. */
@Component
public class RequestRateLimiter {
    private record Bucket(long window, int count) {}
    private final Map<String, Bucket> clients = new HashMap<>();
    private final Clock clock;
    private final boolean enabled;
    private final int limit, maxClients;
    public RequestRateLimiter(Clock clock, @Value("${app.security.rate-limit.enabled:false}") boolean enabled,
            @Value("${app.security.rate-limit.requests-per-minute:120}") int limit,
            @Value("${app.security.rate-limit.max-clients:10000}") int maxClients) {
        if (limit < 1 || maxClients < 1) throw new IllegalArgumentException("Invalid rate limit configuration");
        this.clock = clock; this.enabled = enabled; this.limit = limit; this.maxClients = maxClients;
    }
    public synchronized boolean allow(String remoteAddress) {
        if (!enabled) return true;
        long window = Math.floorDiv(clock.millis(), 60000);
        Bucket bucket = clients.get(remoteAddress);
        if (bucket == null || bucket.window != window) {
            // No attacker-controlled unbounded map; full capacity fails closed for new clients.
            if (clients.size() >= maxClients) clients.entrySet().removeIf(e -> e.getValue().window != window);
            if (bucket == null && clients.size() >= maxClients) return false;
            clients.put(remoteAddress, new Bucket(window, 1));
            return true;
        }
        if (bucket.count >= limit) return false;
        clients.put(remoteAddress, new Bucket(window, bucket.count + 1));
        return true;
    }
    public int retryAfterSeconds() { return (int)(60 - Math.floorMod(clock.millis() / 1000, 60)); }
}
