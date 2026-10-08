package com.example.datban;

import com.example.datban.model.*;
import com.example.datban.repository.*;
import com.example.datban.security.*;
import com.example.datban.service.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:operations;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "app.security.require-auth=true","app.payment.bank-name=Test bank","app.payment.account-number=123456789",
        "app.payment.account-name=TEST RESTAURANT"})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class OperationsTests {
    static final Instant NOW=Instant.parse("2026-10-07T03:00:00Z");
    static final LocalDate DATE=LocalDate.of(2026,10,7);
    @Autowired JdbcTemplate db;
    @Autowired AuthService auth;
    @Autowired BookingLifecycleService lifecycle;
    @Autowired OperationsService ops;
    @Autowired InventoryService inventory;
    @Autowired HoaDonService invoices;
    @Autowired DatBanRepository bookings;
    @Autowired BanSlotRepository slots;
    @Autowired NotificationService notifications;
    @Autowired DeliveryWorker worker;
    @Autowired FakePush push;
    @Autowired TestClock clock;
    @Autowired MockMvc mvc;

    private long engagementMenu() {
        db.update("INSERT INTO menu_items(name,price,active) VALUES('Engagement fixture',50000,TRUE)");
        return db.queryForObject("SELECT MAX(id) FROM menu_items",Long.class);
    }
    @Test void favoritesRequireAuthenticationAndIgnoreForgedIdentity() throws Exception {
        long id=engagementMenu();
        mvc.perform(get("/api/yeu-thich/list")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/yeu-thich/add").param("userId","uid-other").param("idThucDon",""+id).header("Authorization","Bearer customer")).andExpect(status().isOk());
        mvc.perform(get("/api/yeu-thich/list").param("userId","uid-customer").header("Authorization","Bearer other")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/yeu-thich/remove").param("userId","uid-customer").param("idThucDon",""+id).header("Authorization","Bearer other")).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM favorites WHERE user_uid='uid-customer'",Integer.class)).isEqualTo(1);
    }
    @Test void favoriteRetriesAreIdempotentAndInactiveMenuCannotBeAdded() throws Exception {
        long id=engagementMenu();
        for(int i=0;i<2;i++) mvc.perform(post("/api/yeu-thich/add").param("idThucDon",""+id).header("Authorization","Bearer customer")).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM favorites",Integer.class)).isEqualTo(1);
        db.update("UPDATE menu_items SET active=FALSE WHERE id=?",id);
        mvc.perform(get("/api/yeu-thich/list").header("Authorization","Bearer customer")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/yeu-thich/add").param("idThucDon",""+id).header("Authorization","Bearer other")).andExpect(status().isNotFound());
        for(int i=0;i<2;i++) mvc.perform(delete("/api/yeu-thich/remove").param("idThucDon",""+id).header("Authorization","Bearer customer")).andExpect(status().isOk());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM favorites",Integer.class)).isZero();
    }
    @Test void reviewsUpsertOwnFeedbackAndNeverExposeIdentity() throws Exception {
        long id=engagementMenu();
        String body="{\"thucDonId\":"+id+",\"noiDung\":\"  Good food  \",\"rating\":4,\"userId\":\"uid-other\"}";
        for(int i=0;i<2;i++) mvc.perform(post("/api/binhluan/add").header("Authorization","Bearer customer").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.noiDung").value("Good food")).andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.userEmail").doesNotExist()).andExpect(jsonPath("$.userUid").doesNotExist());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM reviews WHERE user_uid='uid-customer'",Integer.class)).isEqualTo(1);
        mvc.perform(post("/api/binhluan/add").header("Authorization","Bearer other").contentType(MediaType.APPLICATION_JSON).content(body.replace("Good food","Other review"))).andExpect(status().isOk());
        mvc.perform(get("/api/binhluan/"+id).header("Authorization","Bearer customer")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        db.update("UPDATE reviews SET status='HIDDEN' WHERE user_uid='uid-customer'");
        mvc.perform(post("/api/binhluan/add").header("Authorization","Bearer customer").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        mvc.perform(get("/api/binhluan/"+id).header("Authorization","Bearer customer")).andExpect(jsonPath("$.length()").value(1));
    }
    @Test void concurrentFavoriteRetriesCreateOneRow() throws Exception {
        long id=engagementMenu(); var pool=Executors.newFixedThreadPool(8);
        try {
            var tasks=new ArrayList<Callable<Integer>>();
            for(int i=0;i<8;i++) tasks.add(()->mvc.perform(post("/api/yeu-thich/add").param("idThucDon",""+id).header("Authorization","Bearer customer")).andReturn().getResponse().getStatus());
            for(var result:pool.invokeAll(tasks,30,TimeUnit.SECONDS)) assertThat(result.get()).isEqualTo(200);
            assertThat(db.queryForObject("SELECT COUNT(*) FROM favorites",Integer.class)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }
    @Test void reviewInputIsBoundedAndProtected() throws Exception {
        long id=engagementMenu();
        mvc.perform(get("/api/binhluan/"+id)).andExpect(status().isUnauthorized());
        for(String body:List.of("{\"thucDonId\":"+id+",\"noiDung\":\" \",\"rating\":5}",
                "{\"thucDonId\":"+id+",\"noiDung\":\"valid\",\"rating\":6}",
                "{\"thucDonId\":"+id+",\"noiDung\":\"valid\"}",
                "{\"thucDonId\":"+id+",\"noiDung\":\""+"x".repeat(2001)+"\",\"rating\":5}"))
            mvc.perform(post("/api/binhluan/add").header("Authorization","Bearer customer").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertThat(db.queryForObject("SELECT COUNT(*) FROM reviews",Integer.class)).isZero();
    }
    @TestConfiguration
    static class Fakes {
        @Bean @Primary TokenVerifier verifier() {
            return token->{
                if (!Set.of("customer","other","staff","admin","unverified").contains(token)) throw new IllegalArgumentException("invalid/revoked");
                return new TokenVerifier.Identity("uid-"+token,token+"@example.com",token,"password",!token.equals("unverified"));
            };
        }
        @Bean @Primary TestClock clock() {return new TestClock();}
        @Bean @Primary FakePush push() {return new FakePush();}
    }
    static class TestClock extends Clock {
        AtomicReference<Instant> value=new AtomicReference<>(NOW);
        public ZoneId getZone(){return ZoneOffset.UTC;}
        public Clock withZone(ZoneId zone){return Clock.fixed(value.get(),zone);}
        public Instant instant(){return value.get();}
    }
    static class FakePush implements PushGateway {
        int sent; Failure failure;
        public void send(String token,Map<String,String> data) {sent++;if(failure!=null)throw failure;}
    }
    @BeforeEach void reset() throws Exception {
        SecurityContextHolder.clearContext();clock.value.set(NOW);push.sent=0;push.failure=null;
        for(String table:List.of("audit_logs","notification_deliveries","notifications","device_tokens","payments","booking_tables","invoices","booking_items","bookings","reviews","favorites","user_roles","users","menu_items","restaurant_tables","time_slots")) db.update("DELETE FROM "+table);
        for(String token:List.of("customer","other","staff","admin")) auth.authenticate(token);
        grantFixture("staff","STAFF");grantFixture("admin","ADMIN");
        BanSlot slot=new BanSlot();slot.setNgay(DATE);slot.setKhungGio("11:00-15:00");slot.setSoBanBanDau(30);slot.setSoBanConLai(30);slots.saveAndFlush(slot);
    }
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    void grantFixture(String token,String role){db.update("INSERT INTO user_roles(user_uid,role_id) SELECT ?,id FROM roles WHERE code=?","uid-"+token,role);}
    void as(String token) throws Exception {var p=auth.authenticate(token);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,List.of()));}
    DatBan booking(String token,int guests) throws Exception {
        as(token);
        try {var b=lifecycle.create(new DatBan(null,token+"@example.com",token,DATE,"11:00-15:00",guests,"","Trong nhà"),UUID.randomUUID().toString(),token+"@example.com");return lifecycle.confirm(b.getIdDat(),token+"@example.com");}
        finally{SecurityContextHolder.clearContext();}
    }
    Long table(String code,int seats){Long area=db.queryForObject("SELECT MIN(id) FROM restaurant_areas",Long.class);ops.createTable(area,code,seats);return db.queryForObject("SELECT id FROM restaurant_tables WHERE code=?",Long.class,code);}
    Long paid(String token) throws Exception {var b=booking(token,8);ops.payment(b.getIdDat(),token+"@example.com",true);ops.verifyPayment(b.getIdDat(),"BANK-"+b.getIdDat(),new BigDecimal("200000"));return b.getIdDat();}
    int count(String table){return db.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    String paymentStatus(Long id){return db.queryForObject("SELECT status FROM payments WHERE booking_id=?",String.class,id);}
    @Test void anonymousCannotReadPrivateOrWriteStaffEvenInTestProfile() throws Exception {
        mvc.perform(get("/api/staff/bookings")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/datban/1/confirm")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ban-slot")).andExpect(status().isOk());
        mvc.perform(get("/staff.html")).andExpect(status().isOk());
    }
    @Test void invalidAndUnverifiedTokensFailClosed() throws Exception {
        mvc.perform(get("/api/notifications").header("Authorization","Bearer revoked")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/notifications").header("Authorization","Bearer unverified")).andExpect(status().isForbidden());
    }
    @Test void rolesComeFromDatabaseNotCustomerClaims() throws Exception {
        mvc.perform(get("/api/staff/bookings").header("Authorization","Bearer customer")).andExpect(status().isForbidden());
        mvc.perform(get("/api/staff/bookings").header("Authorization","Bearer staff")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/audit").header("Authorization","Bearer staff")).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").header("Authorization","Bearer admin")).andExpect(status().isOk());
    }
    @Test void uidOwnershipRejectsOtherAccountAcrossDetailCartInvoiceAndPayment() throws Exception {
        Long id=booking("customer",8).getIdDat();
        for(String path:List.of("/api/datban/"+id,"/api/dondat/"+id,"/api/payments/bookings/"+id+"/quote")) mvc.perform(get(path).header("Authorization","Bearer other")).andExpect(status().isForbidden());
        mvc.perform(put("/api/giohang/"+id).header("Authorization","Bearer other").contentType(MediaType.APPLICATION_JSON).content("[]")).andExpect(status().isForbidden());
        mvc.perform(post("/api/payments/bookings/"+id).header("Authorization","Bearer other")).andExpect(status().isForbidden());
        assertThat(bookings.findById(id).orElseThrow().getUserUid()).isEqualTo("uid-customer");
    }
    @Test void customerCannotVerifyOwnPayment() throws Exception {
        Long id=booking("customer",8).getIdDat();ops.payment(id,"customer@example.com",true);
        mvc.perform(post("/api/staff/bookings/"+id+"/verify-payment").header("Authorization","Bearer customer").contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"BANK-123\",\"amount\":200000}")).andExpect(status().isForbidden());
        assertThat(paymentStatus(id)).isEqualTo("PENDING");
    }
    @Test void quoteAndPaymentAreServerPricedAndIdempotent() throws Exception {
        Long id=booking("customer",8).getIdDat();
        var first=ops.payment(id,"customer@example.com",true);var second=ops.payment(id,"customer@example.com",true);
        assertThat(second.get("id")).isEqualTo(first.get("id"));assertThat((BigDecimal)first.get("amount")).isEqualByComparingTo("200000");
        assertThat(count("payments")).isEqualTo(1);assertThat(count("invoices")).isEqualTo(1);
        assertThat(first.get("status")).isEqualTo("PENDING");
    }
    @Test void paymentCreationRejectsCancelledBooking() throws Exception {
        Long id=booking("customer",8).getIdDat();lifecycle.cancel(id,"customer@example.com");
        assertThatThrownBy(()->ops.payment(id,"customer@example.com",true)).hasMessageContaining("đã đóng");assertThat(count("payments")).isZero();
    }
    @Test void amountMismatchAndDuplicateReferenceRollback() throws Exception {
        Long first=booking("customer",8).getIdDat(),second=booking("other",8).getIdDat();
        ops.payment(first,"customer@example.com",true);ops.payment(second,"other@example.com",true);
        assertThatThrownBy(()->ops.verifyPayment(first,"BANK-123",new BigDecimal("1"))).hasMessageContaining("không khớp");
        ops.verifyPayment(first,"BANK-123",new BigDecimal("200000"));
        assertThatThrownBy(()->ops.verifyPayment(second,"bank-123",new BigDecimal("200000"))).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(paymentStatus(second)).isEqualTo("PENDING");
    }
    @Test void verifiedTransferReplayMustMatchOriginal() throws Exception {
        Long id=paid("customer");
        ops.verifyPayment(id,"bank-"+id,new BigDecimal("200000"));
        assertThatThrownBy(()->ops.verifyPayment(id,"BANK-OTHER",new BigDecimal("200000"))).hasMessageContaining("khác");
        assertThat(db.queryForObject("SELECT COUNT(*) FROM notifications WHERE type='PAID'",Integer.class)).isEqualTo(1);
    }
    @Test void cancellationRequiresActualRefundVerification() throws Exception {
        Long id=paid("customer");lifecycle.cancel(id,"customer@example.com");lifecycle.cancel(id,"customer@example.com");
        assertThat(paymentStatus(id)).isEqualTo("REFUND_REQUIRED");
        assertThatThrownBy(()->ops.refund(id,"REFUND-1",new BigDecimal("1"))).hasMessageContaining("đúng số tiền");
        ops.refund(id,"REFUND-1",new BigDecimal("200000"));ops.refund(id,"refund-1",new BigDecimal("200000"));
        assertThat(paymentStatus(id)).isEqualTo("REFUNDED");
        assertThat(slots.findByNgayAndKhungGio(DATE,"11:00-15:00").orElseThrow().getSoBanConLai()).isEqualTo(30);
    }
    @Test void lateTransferAfterCancellationRequiresRefundWithoutReopeningBooking() throws Exception {
        Long id=booking("customer",8).getIdDat();ops.payment(id,"customer@example.com",true);lifecycle.cancel(id,"customer@example.com");
        assertThat(paymentStatus(id)).isEqualTo("CANCELLED");
        ops.verifyPayment(id,"LATE-123",new BigDecimal("200000"));
        assertThat(paymentStatus(id)).isEqualTo("REFUND_REQUIRED");
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(slots.findByNgayAndKhungGio(DATE,"11:00-15:00").orElseThrow().getSoBanConLai()).isEqualTo(30);
    }
    @Test void parallelPaymentCreationProducesOnePayment() throws Exception {
        var pool=Executors.newFixedThreadPool(6);
        try {
            // Repeat coordinated bursts to expose nested slot-lock contention on MySQL,
            // not just two requests that may execute sequentially on a fast runner.
            for (int round=0;round<12;round++) {
                Long id=booking("customer",8).getIdDat();var start=new CyclicBarrier(6);
                var requests=new ArrayList<Callable<Map<String,Object>>>();
                for(int worker=0;worker<6;worker++) requests.add(()->{
                    start.await(10,TimeUnit.SECONDS);return ops.payment(id,"customer@example.com",true);
                });
                var calls=pool.invokeAll(requests,30,TimeUnit.SECONDS);
                var paymentId=calls.get(0).get().get("id");
                for(var call:calls) assertThat(call.get().get("id")).isEqualTo(paymentId);
                assertThat(count("payments")).isEqualTo(round+1);
            }
        }finally{pool.shutdownNow();}
    }
    @Test void physicalTableCannotBeAssignedTwiceAndConflictRollsBack() throws Exception {
        Long first=booking("customer",8).getIdDat(),second=booking("other",8).getIdDat(),table=table("A01",8);
        ops.assign(first,List.of(table));ops.assign(first,List.of(table));
        assertThatThrownBy(()->ops.assign(second,List.of(table))).hasMessageContaining("đơn khác");
        assertThat(bookings.findById(second).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);assertThat(count("booking_tables")).isEqualTo(1);
    }
    @Test void assignmentChecksTableCountAndSeatCapacity() throws Exception {
        Long id=booking("customer",8).getIdDat(),table=table("SMALL",4);
        assertThatThrownBy(()->ops.assign(id,List.of())).hasMessageContaining("đúng số bàn");
        assertThatThrownBy(()->ops.assign(id,List.of(table))).hasMessageContaining("Không đủ ghế");assertThat(count("booking_tables")).isZero();
    }
    @Test void assignedCancellationReleasesTableAndFlagsRefund() throws Exception {
        Long id=paid("customer"),table=table("A01",8);ops.assign(id,List.of(table));lifecycle.cancel(id,"customer@example.com");
        assertThat(paymentStatus(id)).isEqualTo("REFUND_REQUIRED");assertThat(db.queryForObject("SELECT COUNT(*) FROM booking_tables WHERE released_at IS NULL",Integer.class)).isZero();
    }
    @Test void staffLifecycleRequiresPaymentAndReleasesCapacityOnce() throws Exception {
        Long id=booking("customer",8).getIdDat(),table=table("A01",8);ops.payment(id,"customer@example.com",true);ops.assign(id,List.of(table));
        assertThatThrownBy(()->ops.advance(id,false)).hasMessageContaining("thanh toán");
        ops.verifyPayment(id,"BANK-LIFE",new BigDecimal("200000"));ops.advance(id,false);ops.advance(id,false);
        assertThatThrownBy(()->lifecycle.cancel(id,"customer@example.com")).hasMessageContaining("Không thể hủy");
        ops.advance(id,true);ops.advance(id,true);
        assertThat(bookings.findById(id).orElseThrow().getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(slots.findByNgayAndKhungGio(DATE,"11:00-15:00").orElseThrow().getSoBanConLai()).isEqualTo(30);
    }
    @Test void checkinCannotRunOnDifferentDay() throws Exception {
        Long id=paid("customer"),table=table("A01",8);ops.assign(id,List.of(table));clock.value.set(NOW.plus(Duration.ofDays(1)));
        assertThatThrownBy(()->ops.advance(id,false)).hasMessageContaining("đúng ngày");
    }
    @Test void statusDisableAndRoleRevocationApplyNextRequest() throws Exception {
        ops.role("uid-staff","STAFF",false);
        mvc.perform(get("/api/staff/bookings").header("Authorization","Bearer staff")).andExpect(status().isForbidden());
        ops.userStatus("uid-customer",false);
        mvc.perform(get("/api/notifications").header("Authorization","Bearer customer")).andExpect(status().isForbidden());
    }
    @Test void lastAdminCannotBeRevokedOrDisabled() {
        assertThatThrownBy(()->ops.role("uid-admin","ADMIN",false)).hasMessageContaining("cuối cùng");
        assertThatThrownBy(()->ops.userStatus("uid-admin",false)).hasMessageContaining("cuối cùng");
    }
    @Test void notificationsArePrivateAndReadDoesNotTrustEmailQuery() throws Exception {
        booking("customer",8);
        mvc.perform(get("/api/notifications?userEmail=customer@example.com").header("Authorization","Bearer other")).andExpect(status().isOk()).andExpect(content().json("[]"));
        Long id=db.queryForObject("SELECT MIN(id) FROM notifications",Long.class);
        mvc.perform(post("/api/notifications/"+id+"/read").header("Authorization","Bearer other")).andExpect(status().isNotFound());
        mvc.perform(post("/api/notifications/"+id+"/read").header("Authorization","Bearer customer")).andExpect(status().isOk());
    }
    void deliveryFixture() throws Exception {notifications.register("uid-customer","test-device-token");booking("customer",8);}
    @Test void outboxIsCreatedWithEventAndDeliveredOnce() throws Exception {
        deliveryFixture();assertThat(count("notification_deliveries")).isEqualTo(1);worker.runBatch();worker.runBatch();
        assertThat(push.sent).isEqualTo(1);assertThat(db.queryForObject("SELECT status FROM notification_deliveries",String.class)).isEqualTo("DELIVERED");
    }
    @Test void transientPushFailuresBackOffAndHaveBoundedRetries() throws Exception {
        deliveryFixture();push.failure=new PushGateway.Failure("UNAVAILABLE",false,true);
        worker.runBatch();assertThat(push.sent).isEqualTo(1);worker.runBatch();assertThat(push.sent).isEqualTo(1);
        for(int i=0;i<4;i++){clock.value.set(clock.instant().plusSeconds(3601));worker.runBatch();}
        assertThat(push.sent).isEqualTo(5);assertThat(db.queryForObject("SELECT status FROM notification_deliveries",String.class)).isEqualTo("FAILED");
    }
    @Test void crashedWorkerLeaseCanBeRecovered() throws Exception {
        deliveryFixture();db.update("UPDATE notification_deliveries SET status='SENDING',attempts=1,lease_key='crashed',lease_until=?",Timestamp.from(NOW.minusSeconds(1)));worker.runBatch();
        assertThat(push.sent).isEqualTo(1);assertThat(db.queryForObject("SELECT status FROM notification_deliveries",String.class)).isEqualTo("DELIVERED");
    }
    @Test void invalidPushTokenIsDisabled() throws Exception {
        deliveryFixture();push.failure=new PushGateway.Failure("UNREGISTERED",true,false);worker.runBatch();
        assertThat(db.queryForObject("SELECT active FROM device_tokens",Boolean.class)).isFalse();
        assertThat(db.queryForObject("SELECT status FROM notification_deliveries",String.class)).isEqualTo("FAILED");
    }
    @Test void sharedDeviceNeverReceivesPreviousAccountQueuedPush() throws Exception {
        deliveryFixture();notifications.register("uid-other","test-device-token");worker.runBatch();
        assertThat(push.sent).isZero();assertThat(count("notification_deliveries")).isZero();assertThat(notifications.list("uid-customer")).hasSize(1);
    }
    @Test void historyIsScopedToUidAndContainsRealStateAndPayment() throws Exception {
        Long id=paid("customer");booking("other",8);
        mvc.perform(get("/api/taikhoan/choXacNhan").header("Authorization","Bearer customer")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].idDat").value(id.intValue())).andExpect(jsonPath("$[0].status").value("CONFIRMED")).andExpect(jsonPath("$[0].paymentStatus").value("PAID"));
    }
    @Test void assignmentBeforePaymentDoesNotPreventCustomerCreatingPayment() throws Exception {
        Long id=booking("customer",8).getIdDat();ops.assign(id,List.of(table("A01",8)));
        assertThat(ops.payment(id,"customer@example.com",true).get("status")).isEqualTo("PENDING");
    }
    @Test void reconcilePreservesExistingReservations() throws Exception {
        booking("customer",8);table("A01",8);table("A02",8);assertThat(inventory.reconcile()).isEqualTo(2);
        var slot=slots.findByNgayAndKhungGio(DATE,"11:00-15:00").orElseThrow();
        assertThat(slot.getSoBanBanDau()).isEqualTo(2);assertThat(slot.getSoBanConLai()).isEqualTo(1);
    }
    @Test void unsafeInventoryReductionRollsBack() throws Exception {
        booking("customer",16);table("A01",8);
        assertThatThrownBy(()->inventory.reconcile()).hasMessageContaining("đang giữ");
        assertThat(slots.findByNgayAndKhungGio(DATE,"11:00-15:00").orElseThrow().getSoBanBanDau()).isEqualTo(30);
    }
    @Test void emptyInventoryCannotBeReconciled() {assertThatThrownBy(()->inventory.reconcile()).hasMessageContaining("khai báo");}
    @Test void parallelTableAssignmentAcceptsOnlyOneBooking() throws Exception {
        Long first=booking("customer",8).getIdDat(),second=booking("other",8).getIdDat(),table=table("A01",8);
        var pool=Executors.newFixedThreadPool(2);
        try {
            var calls=pool.invokeAll(List.<Callable<Boolean>>of(()->{try{ops.assign(first,List.of(table));return true;}catch(com.example.datban.exception.BusinessRuleException ex){return false;}},
                    ()->{try{ops.assign(second,List.of(table));return true;}catch(com.example.datban.exception.BusinessRuleException ex){return false;}}));
            assertThat(calls.stream().filter(f->{try{return f.get();}catch(Exception e){throw new RuntimeException(e);}}).count()).isEqualTo(1);
            assertThat(count("booking_tables")).isEqualTo(1);
        }finally{pool.shutdownNow();}
    }
    @Test void staffApiAuditRecordsActorAndOnlyCommittedTransfer() throws Exception {
        Long id=booking("customer",8).getIdDat();ops.payment(id,"customer@example.com",true);
        mvc.perform(post("/api/staff/bookings/"+id+"/verify-payment").header("Authorization","Bearer staff").contentType(MediaType.APPLICATION_JSON).content("{\"reference\":\"BANK-AUDIT\",\"amount\":200000}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        assertThat(db.queryForObject("SELECT actor_uid FROM audit_logs WHERE action='VERIFY_TRANSFER'",String.class)).isEqualTo("uid-staff");
    }
    @Test void rejectedTransferDoesNotEmitPaidNotificationOrAudit() throws Exception {
        Long id=booking("customer",8).getIdDat();ops.payment(id,"customer@example.com",true);
        assertThatThrownBy(()->ops.verifyPayment(id,"BAD-AMOUNT",new BigDecimal("1"))).isInstanceOf(com.example.datban.exception.BusinessRuleException.class);
        assertThat(db.queryForObject("SELECT COUNT(*) FROM notifications WHERE type='PAID'",Integer.class)).isZero();
        assertThat(db.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE action='VERIFY_TRANSFER'",Integer.class)).isZero();
    }
    @Test void staffQueueContractUsesStableLowercaseKeysAndIsoDate() throws Exception {
        Long id=booking("customer",8).getIdDat();
        mvc.perform(get("/api/staff/bookings").header("Authorization","Bearer staff")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(id.intValue())).andExpect(jsonPath("$[0].booking_date").value("2026-10-07"));
    }
    @Test void customerCannotChangeRolesCreateTablesOrRetryDeliveries() throws Exception {
        mvc.perform(post("/api/admin/roles").header("Authorization","Bearer customer").contentType(MediaType.APPLICATION_JSON).content("{\"uid\":\"uid-customer\",\"role\":\"ADMIN\",\"grant\":true}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/admin/tables").header("Authorization","Bearer customer").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/staff/deliveries/1/retry").header("Authorization","Bearer customer")).andExpect(status().isForbidden());
    }
    @Test void pushForUnregisteredDeviceIsCancelledWithoutSending() throws Exception {
        deliveryFixture();notifications.unregister("uid-customer","test-device-token");worker.runBatch();
        assertThat(push.sent).isZero();assertThat(db.queryForObject("SELECT status FROM notification_deliveries",String.class)).isEqualTo("CANCELLED");
    }
    @Test void bankDestinationIsSnapshottedWithPayment() throws Exception {
        Long id=booking("customer",8).getIdDat();var result=ops.payment(id,"customer@example.com",true);
        assertThat(result.get("bankName")).isEqualTo("Test bank");
        assertThat(db.queryForObject("SELECT account_number FROM payments WHERE booking_id=?",String.class,id)).isEqualTo("123456789");
    }
    @Test void alreadyPaidBookingDoesNotReceiveStaleTransferReminder() throws Exception {
        deliveryFixture();Long id=bookings.findTopByUserUidOrderByIdDatDesc("uid-customer").orElseThrow().getIdDat();
        ops.payment(id,"customer@example.com",true);ops.verifyPayment(id,"BANK-PUSH",new BigDecimal("200000"));worker.runBatch();
        assertThat(push.sent).isEqualTo(2); // confirmed + paid; pending reminder is obsolete
        assertThat(db.queryForObject("SELECT COUNT(*) FROM notification_deliveries WHERE last_error='OBSOLETE_PAYMENT_REQUEST'",Integer.class)).isEqualTo(1);
    }
    @Test void invalidSyncPayloadUsesStandardErrorEnvelope() throws Exception {
        mvc.perform(post("/api/auth/sync").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/auth/sync").contentType(MediaType.APPLICATION_JSON).content("{\"idToken\":\"unverified\"}")).andExpect(status().isForbidden()).andExpect(jsonPath("$.path").value("/api/auth/sync"));
    }
    @Test void adminCanManageMenuAndInactiveItemsAreNotPublic() throws Exception {
        String body="{\"tenMon\":\"Soup\",\"gia\":50000,\"nhom\":\"MON_CHINH\",\"active\":true}";
        mvc.perform(post("/api/admin/menu").header("Authorization","Bearer staff").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        var response=mvc.perform(post("/api/admin/menu").header("Authorization","Bearer admin").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn();
        Long id=new com.fasterxml.jackson.databind.ObjectMapper().readTree(response.getResponse().getContentAsString()).get("idThucDon").longValue();
        mvc.perform(get("/api/thucdon/"+id)).andExpect(status().isOk());
        mvc.perform(put("/api/admin/menu/"+id).header("Authorization","Bearer admin").contentType(MediaType.APPLICATION_JSON).content(body.replace("true","false"))).andExpect(status().isOk());
        mvc.perform(get("/api/thucdon/"+id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/thucdon")).andExpect(content().json("[]"));
    }
    @Test void invalidMenuPriceAndExecutableImageUrlAreRejected() throws Exception {
        mvc.perform(post("/api/admin/menu").header("Authorization","Bearer admin").contentType(MediaType.APPLICATION_JSON).content("{\"tenMon\":\"Soup\",\"gia\":-1,\"active\":true}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/menu").header("Authorization","Bearer admin").contentType(MediaType.APPLICATION_JSON).content("{\"tenMon\":\"Soup\",\"gia\":1,\"anh\":\"javascript:alert(1)\",\"active\":true}")).andExpect(status().isBadRequest());
    }
    @Test void unreadCountIsOwnedAndChangesAfterReading() throws Exception {
        booking("customer",8);
        mvc.perform(get("/api/notifications/unread-count").header("Authorization","Bearer customer")).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        mvc.perform(get("/api/notifications/unread-count").header("Authorization","Bearer other")).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        notifications.read("uid-customer",db.queryForObject("SELECT MIN(id) FROM notifications",Long.class));
        assertThat(notifications.unread("uid-customer")).isZero();
    }
    @Test void getQuoteIsReadOnlyAndPaymentPostFreezesInvoice() throws Exception {
        Long id=booking("customer",8).getIdDat();
        mvc.perform(get("/api/payments/bookings/"+id+"/quote").header("Authorization","Bearer customer")).andExpect(status().isOk()).andExpect(jsonPath("$.tongTien").value(200000));
        assertThat(count("invoices")).isZero();assertThat(count("payments")).isZero();
        ops.payment(id,"customer@example.com",true);assertThat(count("invoices")).isEqualTo(1);
    }
    @Test void staffCanReadClosedBookingForReconciliationButCustomerCannotReadOthers() throws Exception {
        Long id=paid("customer");lifecycle.cancel(id,"customer@example.com");
        mvc.perform(get("/api/staff/bookings/"+id).header("Authorization","Bearer staff")).andExpect(status().isOk()).andExpect(jsonPath("$.booking.status").value("CANCELLED")).andExpect(jsonPath("$.payments[0].status").value("REFUND_REQUIRED"));
        mvc.perform(get("/api/staff/bookings/"+id).header("Authorization","Bearer customer")).andExpect(status().isForbidden());
    }
    @Test void legacyMenuCannotBypassAdminPermissionAndEmptyStatusCannotDisableUser() throws Exception {
        mvc.perform(post("/nhahang/thuc_don").header("Authorization","Bearer staff")).andExpect(status().isForbidden());
        mvc.perform(put("/api/admin/users/uid-customer/status").header("Authorization","Bearer admin").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        assertThat(db.queryForObject("SELECT status FROM users WHERE uid='uid-customer'",String.class)).isEqualTo("ACTIVE");
    }
}
