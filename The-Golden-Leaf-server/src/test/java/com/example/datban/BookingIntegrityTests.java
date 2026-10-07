package com.example.datban;

import com.example.datban.dto.*;
import com.example.datban.exception.BusinessRuleException;
import com.example.datban.model.*;
import com.example.datban.repository.*;
import com.example.datban.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:booking_integrity;MODE=MySQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BookingIntegrityTests {
    private static final Instant NOW = Instant.parse("2026-10-07T03:00:00Z");
    private static final LocalDate DATE = LocalDate.of(2026, 10, 8);
    private static final String SLOT = "11:00-15:00";
    @Autowired BookingLifecycleService lifecycle;
    @Autowired BookingItemService cart;
    @Autowired HoaDonService invoices;
    @Autowired BanSlotRepository slots;
    @Autowired DatBanRepository bookings;
    @Autowired GioHangRepository items;
    @Autowired ThucDonRepository menu;
    @Autowired HoaDonRepository invoiceRepo;
    @Autowired MutableClock clock;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @TestConfiguration
    static class TimeConfig {
        @Bean @Primary MutableClock testClock() { return new MutableClock(NOW, ZoneOffset.UTC); }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;
        private final ZoneId zone;
        MutableClock(Instant instant, ZoneId zone) { this(new AtomicReference<>(instant), zone); }
        private MutableClock(AtomicReference<Instant> instant, ZoneId zone) { this.instant = instant; this.zone = zone; }
        void set(Instant next) { instant.set(next); }
        @Override public ZoneId getZone() { return zone; }
        @Override public Clock withZone(ZoneId next) { return new MutableClock(instant, next); }
        @Override public Instant instant() { return instant.get(); }
    }

    @BeforeEach
    void reset() {
        clock.set(NOW);
        invoiceRepo.deleteAll();
        items.deleteAll();
        bookings.deleteAll();
        menu.deleteAll();
        slots.deleteAll();
        setCapacity(30);
    }

    private void setCapacity(int count) {
        BanSlot slot = slots.findByNgayAndKhungGio(DATE, SLOT).orElseGet(BanSlot::new);
        slot.setNgay(DATE); slot.setKhungGio(SLOT);
        slot.setSoBanBanDau(count); slot.setSoBanConLai(count);
        slots.saveAndFlush(slot);
    }

    private DatBan request(int guests) {
        return new DatBan(null, "guest@example.com", "Khách", DATE, SLOT, guests, "", "Trong nhà");
    }

    private ThucDon dish(String name, String price) {
        ThucDon dish = new ThucDon();
        dish.setTenMon(name); dish.setGia(new BigDecimal(price));
        return menu.saveAndFlush(dish);
    }

    private BookingItemRequest line(Long bookingId, Long dishId, int quantity) {
        return new BookingItemRequest(bookingId, dishId, "Untrusted client name", quantity, BigDecimal.ONE);
    }

    @Test
    void createReservesRoundedCapacityAndReplayDoesNotReserveTwice() {
        DatBan first = lifecycle.create(request(9), "booking-1", null);
        DatBan second = lifecycle.create(request(9), "BOOKING-1", null);
        assertThat(second.getIdDat()).isEqualTo(first.getIdDat());
        assertThat(first.getReservedTables()).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(BookingStatus.HOLDING);
        assertThat(first.getHoldExpiresAt()).isEqualTo(NOW.plusSeconds(900));
        assertThat(remaining()).isEqualTo(28);
        assertThat(bookings.count()).isEqualTo(1);
        assertThatThrownBy(() -> lifecycle.create(request(10), "booking-1", null))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("IDEMPOTENCY_CONFLICT"));
        assertThat(remaining()).isEqualTo(28);
    }

    @Test
    void insufficientCapacityRollsBackBookingAndInventory() {
        setCapacity(1);
        assertThatThrownBy(() -> lifecycle.create(request(9), "too-large", null))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(bookings.count()).isZero();
        assertThat(remaining()).isEqualTo(1);
    }

    @Test
    void concurrentRequestsCannotOverbook() throws Exception {
        setCapacity(2);
        List<Long> results = concurrent(12, i -> {
            try { return lifecycle.create(request(9), "competing-" + i, null).getIdDat(); }
            catch (BusinessRuleException e) {
                assertThat(e.getCode()).isEqualTo("NOT_ENOUGH_TABLES");
                return null;
            }
        });
        assertThat(results.stream().filter(Objects::nonNull).count()).isEqualTo(1);
        assertThat(bookings.count()).isEqualTo(1);
        assertThat(remaining()).isZero();
    }

    @Test
    void concurrentRetriesCreateExactlyOneBooking() throws Exception {
        setCapacity(2);
        List<Long> results = concurrent(8, i -> lifecycle.create(request(8), "shared-retry", null).getIdDat());
        assertThat(new HashSet<>(results)).hasSize(1);
        assertThat(bookings.count()).isEqualTo(1);
        assertThat(remaining()).isEqualTo(1);
    }

    @Test
    void cancelIsIdempotentAndConcurrentCancelsReleaseOnce() throws Exception {
        DatBan booking = lifecycle.create(request(9), "cancel-me", null);
        concurrent(8, i -> lifecycle.cancel(booking.getIdDat(), null).getIdDat());
        assertThat(remaining()).isEqualTo(30);
        assertThat(bookings.findById(booking.getIdDat()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> lifecycle.confirm(booking.getIdDat(), null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void expiredHoldCannotBeConfirmedAndSweepReleasesOnce() {
        DatBan booking = lifecycle.create(request(9), "expires", null);
        clock.set(NOW.plusSeconds(900));
        assertThatThrownBy(() -> lifecycle.confirm(booking.getIdDat(), null))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("BOOKING_HOLD_EXPIRED"));
        lifecycle.expire(booking.getIdDat());
        lifecycle.expire(booking.getIdDat());
        assertThat(remaining()).isEqualTo(30);
        assertThat(bookings.findById(booking.getIdDat()).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void createReclaimsExpiredInventoryWithoutWaitingForScheduler() {
        setCapacity(1);
        DatBan old = lifecycle.create(request(8), "old", null);
        clock.set(NOW.plusSeconds(901));
        lifecycle.create(request(8), "new", null);
        assertThat(remaining()).isZero();
        assertThat(bookings.findById(old.getIdDat()).orElseThrow().getStatus()).isEqualTo(BookingStatus.EXPIRED);
    }

    @Test
    void confirmedBookingSurvivesExpiryAndConfirmRetry() {
        DatBan booking = lifecycle.create(request(8), "confirm", null);
        lifecycle.confirm(booking.getIdDat(), null);
        lifecycle.confirm(booking.getIdDat(), null);
        clock.set(NOW.plusSeconds(1800));
        lifecycle.expire(booking.getIdDat());
        assertThat(remaining()).isEqualTo(29);
        assertThat(bookings.findById(booking.getIdDat()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void cartUsesServerPricesReplacesSnapshotAndRollsBackInvalidBatch() {
        DatBan booking = lifecycle.create(request(8), "cart", null);
        ThucDon dish = dish("Soup", "45000.25");
        var request = List.of(line(booking.getIdDat(), dish.getIdThucDon(), 2));
        var first = cart.replace(booking.getIdDat(), request, null);
        assertThat(first.get(0).getThanhTien()).isEqualByComparingTo("90000.50");
        assertThat(first.get(0).getTenMon()).isEqualTo("Soup");
        assertThat(cart.replace(booking.getIdDat(), request, null).get(0).getId()).isEqualTo(first.get(0).getId());
        assertThatThrownBy(() -> cart.replace(booking.getIdDat(), List.of(
                line(booking.getIdDat(), dish.getIdThucDon(), 3), line(booking.getIdDat(), Long.MAX_VALUE, 1)), null))
                .isInstanceOf(com.example.datban.exception.ResourceNotFoundException.class);
        assertThat(items.findByIdDat(booking.getIdDat())).hasSize(1);
        assertThat(items.findByIdDat(booking.getIdDat()).get(0).getSoLuong()).isEqualTo(2);
        cart.replace(booking.getIdDat(), List.of(line(booking.getIdDat(), dish.getIdThucDon(), 3)), null);
        assertThat(items.findByIdDat(booking.getIdDat())).hasSize(1);
        assertThat(items.findByIdDat(booking.getIdDat()).get(0).getSoLuong()).isEqualTo(3);
    }

    @Test
    void cartRejectsMixedBookingsDuplicateItemsAndUnavailableDishes() {
        DatBan first = lifecycle.create(request(8), "first", null);
        DatBan second = lifecycle.create(request(8), "second", null);
        ThucDon dish = dish("Soup", "45000");
        var line = line(first.getIdDat(), dish.getIdThucDon(), 1);
        assertThatThrownBy(() -> cart.replace(first.getIdDat(), List.of(line, line), null)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> cart.replace(first.getIdDat(), List.of(line(second.getIdDat(), dish.getIdThucDon(), 1)), null))
                .isInstanceOf(BusinessRuleException.class);
        dish.setActive(false); menu.saveAndFlush(dish);
        assertThatThrownBy(() -> cart.replace(first.getIdDat(), List.of(line), null)).isInstanceOf(BusinessRuleException.class);
        assertThat(items.count()).isZero();
    }

    @Test
    void invoiceRejectsForgedTotalsAndConcurrentRetriesCreateOneImmutableInvoice() throws Exception {
        DatBan booking = lifecycle.create(request(8), "invoice", null);
        ThucDon dish = dish("Soup", "45000.25");
        cart.replace(booking.getIdDat(), List.of(line(booking.getIdDat(), dish.getIdThucDon(), 2)), null);
        HoaDonRequest forged = new HoaDonRequest(booking.getIdDat(), BigDecimal.ZERO, BigDecimal.ONE, BigDecimal.ONE);
        lifecycle.confirm(booking.getIdDat(), null);
        assertThatThrownBy(() -> invoices.saveHoaDon(forged, null)).isInstanceOf(BusinessRuleException.class);
        assertThat(invoiceRepo.count()).isZero();
        HoaDonRequest valid = new HoaDonRequest(booking.getIdDat(), new BigDecimal("200000"),
                new BigDecimal("90000.50"), new BigDecimal("290000.50"));
        var ids = concurrent(8, i -> invoices.saveHoaDon(valid, null).getId());
        assertThat(new HashSet<>(ids)).hasSize(1);
        assertThat(invoiceRepo.count()).isEqualTo(1);
        assertThatThrownBy(() -> cart.replace(booking.getIdDat(), List.of(), null)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void authenticatedActorCannotWriteAnotherCustomersBooking() {
        DatBan booking = lifecycle.create(request(8), "owner", "guest@example.com");
        assertThatThrownBy(() -> lifecycle.cancel(booking.getIdDat(), "other@example.com")).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> lifecycle.create(request(8), "other-owner", "other@example.com")).isInstanceOf(AccessDeniedException.class);
        assertThat(remaining()).isEqualTo(29);
    }

    @Test
    void apiRejectsMissingKeyAndUntrackedCapacityMutations() throws Exception {
        mvc.perform(post("/api/datban/save").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/ban-slot/dat").param("ngay", DATE.toString()).param("khungGio", SLOT).param("soLuongKhach", "8"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BOOKING_REQUIRED"));
        mvc.perform(post("/api/ban-slot/tra").param("ngay", DATE.toString()).param("khungGio", SLOT).param("soLuongKhach", "8"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BOOKING_REQUIRED"));
        assertThat(remaining()).isEqualTo(30);
    }

    @Test
    void databaseRejectsNegativeCapacity() {
        assertThatThrownBy(() -> jdbc.update("UPDATE time_slots SET remaining_table_count = -1"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class)
                .satisfies(error -> assertThat(error.getMessage()).containsIgnoringCase("ck_slot_capacity"));
        assertThat(remaining()).isEqualTo(30);
    }

    @Test
    void restFlowCreatesConfirmsAndCancelsTheSameBooking() throws Exception {
        String payload = """
                {"email":"guest@example.com","ten":"Khách","ngay":"2026-10-08",
                 "khungGio":"11:00-15:00","soLuong":9,"ghiChu":"","viTriBan":"Trong nhà"}
                """;
        mvc.perform(post("/api/datban/save").header("Idempotency-Key", "rest-flow")
                .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("HOLDING"))
                .andExpect(jsonPath("$.reservedTables").value(2)).andExpect(jsonPath("$.holdExpiresAt").exists());
        Long id = bookings.findByIdempotencyKey("rest-flow").orElseThrow().getIdDat();
        mvc.perform(put("/api/giohang/{id}", id).contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/datban/{id}/confirm", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(get("/api/datban/{id}", id)).andExpect(status().isOk()).andExpect(jsonPath("$.idDat").value(id));
        mvc.perform(post("/api/datban/{id}/cancel", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(remaining()).isEqualTo(30);
    }

    @Test
    void invalidCartElementUsesValidationEnvelopeAndDoesNotPersist() throws Exception {
        DatBan booking = lifecycle.create(request(8), "invalid-cart", null);
        String payload = "[{\"idDat\":" + booking.getIdDat()
                + ",\"idThucDon\":1,\"tenMon\":\"Soup\",\"soLuong\":0,\"giaMon\":1}]";
        mvc.perform(put("/api/giohang/{id}", booking.getIdDat()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertThat(items.count()).isZero();
    }

    @Test
    void expiredAndCancelledBookingsCannotChangeCartOrCreateInvoice() {
        DatBan booking = lifecycle.create(request(8), "expired-cart", null);
        ThucDon dish = dish("Soup", "10000");
        var lines = List.of(line(booking.getIdDat(), dish.getIdThucDon(), 1));
        clock.set(NOW.plusSeconds(900));
        assertThatThrownBy(() -> cart.replace(booking.getIdDat(), lines, null)).isInstanceOf(BusinessRuleException.class);
        lifecycle.cancel(booking.getIdDat(), null);
        assertThatThrownBy(() -> invoices.saveHoaDon(new HoaDonRequest(booking.getIdDat(),
                new BigDecimal("200000"), BigDecimal.ZERO, new BigDecimal("200000")), null))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(items.count()).isZero();
        assertThat(invoiceRepo.count()).isZero();
    }

    @Test
    void bookingWindowUsesRestaurantTimezoneAndRejectsStartedSlots() {
        clock.set(Instant.parse("2026-10-07T17:01:00Z")); // Oct 8, 00:01 in Vietnam.
        DatBan valid = lifecycle.create(request(8), "vietnam-midnight", null);
        assertThat(valid.getNgay()).isEqualTo(DATE);
        clock.set(Instant.parse("2026-10-08T04:00:00Z")); // At the exact start time.
        assertThatThrownBy(() -> lifecycle.create(request(8), "started", null))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_BOOKING_TIME"));
        assertThat(remaining()).isEqualTo(29);
    }

    @Test
    void concurrentCartRetriesDoNotDuplicateLinesAndPreservePriceSnapshot() throws Exception {
        DatBan booking = lifecycle.create(request(8), "cart-retry", null);
        ThucDon dish = dish("Soup", "45000");
        var lines = List.of(line(booking.getIdDat(), dish.getIdThucDon(), 2));
        concurrent(8, i -> cart.replace(booking.getIdDat(), lines, null).get(0).getId());
        dish.setGia(new BigDecimal("55000")); menu.saveAndFlush(dish);
        var replay = cart.replace(booking.getIdDat(), lines, null);
        assertThat(items.count()).isEqualTo(1);
        assertThat(replay.get(0).getGiaMon()).isEqualByComparingTo("45000");
    }

    private int remaining() { return slots.findByNgayAndKhungGio(DATE, SLOT).orElseThrow().getSoBanConLai(); }

    private <T> List<T> concurrent(int count, java.util.function.IntFunction<T> operation) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int index = i;
                futures.add(pool.submit(() -> { ready.countDown(); start.await(); return operation.apply(index); }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<T> result = new ArrayList<>();
            for (var future : futures) { result.add(future.get(20, TimeUnit.SECONDS)); }
            return result;
        } finally { start.countDown(); pool.shutdownNow(); }
    }
}
