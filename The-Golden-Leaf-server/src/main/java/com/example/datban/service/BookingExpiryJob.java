package com.example.datban.service;

import com.example.datban.model.BookingStatus;
import com.example.datban.repository.DatBanRepository;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.booking.expiry-enabled", havingValue = "true", matchIfMissing = true)
public class BookingExpiryJob {
    private static final Logger log = LoggerFactory.getLogger(BookingExpiryJob.class);
    private final DatBanRepository bookings;
    private final BookingLifecycleService lifecycle;
    private final Clock clock;

    public BookingExpiryJob(DatBanRepository bookings, BookingLifecycleService lifecycle, Clock clock) {
        this.bookings = bookings;
        this.lifecycle = lifecycle;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.booking.expiry-delay-ms:60000}")
    public void expireHolds() {
        for (Long id : bookings.expiredIds(BookingStatus.HOLDING, clock.instant(), PageRequest.of(0, 100))) {
            try {
                lifecycle.expire(id);
            } catch (RuntimeException exception) {
                log.error("Could not expire booking {}", id, exception);
            }
        }
    }
}
