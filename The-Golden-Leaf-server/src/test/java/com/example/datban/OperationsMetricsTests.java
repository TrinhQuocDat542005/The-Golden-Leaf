package com.example.datban;

import com.example.datban.config.OperationsMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperationsMetricsTests {
    @Test void metricsStartUnavailableInsteadOfPretendingFresh() {
        var registry=new SimpleMeterRegistry();new OperationsMetrics(mock(JdbcTemplate.class),Clock.systemUTC(),registry);
        assertThat(registry.get("goldenleaf.operations.snapshot.age.seconds").gauge().value()).isEqualTo(-1);
        assertThat(registry.get("goldenleaf.notification.deliveries").tag("status","FAILED").gauge().value()).isZero();
    }
    @Test void failedSnapshotIsCountedAndNotMarkedFresh() {
        var db=mock(JdbcTemplate.class);var registry=new SimpleMeterRegistry();var metrics=new OperationsMetrics(db,Clock.systemUTC(),registry);
        when(db.queryForObject("SELECT COUNT(*) FROM payments WHERE status='REFUND_REQUIRED'",Long.class)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("secret connection"));
        metrics.refresh();
        assertThat(registry.get("goldenleaf.operations.snapshot.errors").counter().count()).isEqualTo(1);
        assertThat(registry.get("goldenleaf.operations.snapshot.age.seconds").gauge().value()).isEqualTo(-1);
    }
    @Test void successfulSnapshotPublishesCountsAndAge() {
        var db=mock(JdbcTemplate.class);var registry=new SimpleMeterRegistry();
        var clock=Clock.fixed(Instant.parse("2026-10-08T06:00:00Z"),ZoneOffset.UTC);
        when(db.queryForObject("SELECT COUNT(*) FROM payments WHERE status='REFUND_REQUIRED'",Long.class)).thenReturn(3L);
        when(db.queryForObject(eq("SELECT COUNT(*) FROM bookings WHERE status='HOLDING' AND hold_expires_at<=?"),eq(Long.class),any(java.sql.Timestamp.class))).thenReturn(2L);
        var metrics=new OperationsMetrics(db,clock,registry);metrics.refresh();
        assertThat(registry.get("goldenleaf.payment.refunds.required").gauge().value()).isEqualTo(3);
        assertThat(registry.get("goldenleaf.booking.holds.overdue").gauge().value()).isEqualTo(2);
        assertThat(registry.get("goldenleaf.operations.snapshot.age.seconds").gauge().value()).isZero();
    }
}
