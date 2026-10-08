package com.example.datban.config;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cached low-cardinality gauges: scraping never scans business tables itself. */
@Component
@ConditionalOnProperty(name="app.operations.metrics-enabled", havingValue="true", matchIfMissing=true)
public class OperationsMetrics {
    private final JdbcTemplate db;
    private final Clock clock;
    private final MeterRegistry registry;
    private final Map<String, AtomicLong> deliveries = Map.of("PENDING", new AtomicLong(), "SENDING", new AtomicLong(), "FAILED", new AtomicLong());
    private final AtomicLong refunds = new AtomicLong(), overdue = new AtomicLong(), lastSuccess = new AtomicLong();
    public OperationsMetrics(JdbcTemplate db, Clock clock, MeterRegistry registry) {
        this.db = db; this.clock = clock; this.registry = registry;
        deliveries.forEach((state, count) -> registry.gauge("goldenleaf.notification.deliveries", java.util.List.of(io.micrometer.core.instrument.Tag.of("status",state)), count));
        registry.gauge("goldenleaf.payment.refunds.required", refunds);
        registry.gauge("goldenleaf.booking.holds.overdue", overdue);
        registry.gauge("goldenleaf.operations.snapshot.age.seconds", this,
                self -> self.lastSuccess.get() == 0 ? -1 : Math.max(0,(self.clock.millis()-self.lastSuccess.get())/1000.0));
    }
    @Scheduled(fixedDelay=30000, initialDelay=5000)
    public void refresh() {
        try {
            // Gather first; a failed read never publishes a partial apparently fresh snapshot.
            Map<String,Long> counts = new java.util.HashMap<>();
            db.query("SELECT status,COUNT(*) AS total FROM notification_deliveries GROUP BY status", row -> { counts.put(row.getString("status"),row.getLong("total")); });
            long refundCount = db.queryForObject("SELECT COUNT(*) FROM payments WHERE status='REFUND_REQUIRED'", Long.class);
            long overdueCount = db.queryForObject("SELECT COUNT(*) FROM bookings WHERE status='HOLDING' AND hold_expires_at<=?", Long.class,Timestamp.from(clock.instant()));
            deliveries.forEach((state,count) -> count.set(counts.getOrDefault(state,0L)));
            refunds.set(refundCount); overdue.set(overdueCount); lastSuccess.set(clock.millis());
        } catch (org.springframework.dao.DataAccessException failure) {
            registry.counter("goldenleaf.operations.snapshot.errors").increment();
        }
    }
}
