package com.example.datban;

import com.example.datban.demo.DemoTokenVerifier;
import com.example.datban.service.DeliveryJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class DemoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired DemoTokenVerifier verifier;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;
    @Autowired ApplicationContext context;
    private String bearer(String role) { return "Bearer " + verifier.tokenFor(role); }
    @Test void seedIsSyntheticAndHasNoFirebaseOrPushWorker() throws Exception {
        assertEquals(4,db.queryForObject("SELECT COUNT(*) FROM users WHERE email LIKE '%@example.invalid'",Integer.class));
        assertEquals(6,db.queryForObject("SELECT COUNT(*) FROM menu_items",Integer.class));
        assertEquals(4,db.queryForObject("SELECT COUNT(*) FROM restaurant_tables",Integer.class));
        assertEquals(28,db.queryForObject("SELECT COUNT(*) FROM time_slots",Integer.class));
        assertTrue(context.getBeansOfType(DeliveryJob.class).isEmpty());assertFalse(context.containsBean("firebaseConfig"));
        mvc.perform(get("/api/demo/config")).andExpect(status().isOk()).andExpect(jsonPath("$.demo").value(true));
        mvc.perform(get("/demo.html")).andExpect(status().isOk());
    }
    @Test void publicDemoSessionsDoNotBypassBusinessRoles() throws Exception {
        mvc.perform(post("/api/demo/session").contentType("application/json").content("{\"persona\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.profile.uid").value("demo-admin"));
        mvc.perform(post("/api/demo/session").contentType("application/json").content("{\"persona\":\"ROOT\"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/staff/bookings")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/staff/bookings").header("Authorization",bearer("CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").header("Authorization",bearer("STAFF"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/audit").header("Authorization",bearer("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer fixed-demo-admin")).andExpect(status().isUnauthorized());
    }
    @Test void fullBookingPaymentAndRefundPreserveOwnershipAndIdempotency() throws Exception {
        String key=UUID.randomUUID().toString();
        String body=json.writeValueAsString(Map.of("email","customer@example.invalid","ten","Khách demo",
                "ngay",LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(1).toString(),"khungGio","07:00-11:00","soLuong",4,"viTriBan","Trong nhà"));
        var response=mvc.perform(post("/api/datban/save").header("Authorization",bearer("CUSTOMER")).header("Idempotency-Key",key).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();
        long id=json.readTree(response.getResponse().getContentAsString()).get("idDat").asLong();
        mvc.perform(post("/api/datban/save").header("Authorization",bearer("CUSTOMER")).header("Idempotency-Key",key).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.idDat").value(id));
        mvc.perform(get("/api/datban/"+id).header("Authorization",bearer("OTHER_CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/datban/"+id+"/confirm").header("Authorization",bearer("CUSTOMER"))).andExpect(status().isOk());
        mvc.perform(post("/api/payments/bookings/"+id).header("Authorization",bearer("CUSTOMER"))).andExpect(status().isOk()).andExpect(jsonPath("$.accountNumber").value("NOT-A-REAL-ACCOUNT"));
        String transfer="{\"reference\":\"DEMO-"+id+"\",\"amount\":200000}";
        mvc.perform(post("/api/staff/bookings/"+id+"/verify-payment").header("Authorization",bearer("STAFF")).contentType("application/json").content(transfer)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(post("/api/datban/"+id+"/cancel").header("Authorization",bearer("CUSTOMER"))).andExpect(status().isOk());
        mvc.perform(get("/api/payments/bookings/"+id).header("Authorization",bearer("CUSTOMER"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFUND_REQUIRED"));
        mvc.perform(post("/api/staff/bookings/"+id+"/verify-refund").header("Authorization",bearer("STAFF")).contentType("application/json").content(transfer.replace("DEMO-","REFUND-"))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFUNDED"));
    }
    @Test void todaySeedSupportsCheckInThenCompletion() throws Exception {
        long id=db.queryForObject("SELECT id FROM bookings WHERE idempotency_key='demo-seed-today'",Long.class);
        mvc.perform(post("/api/staff/bookings/"+id+"/check-in").header("Authorization",bearer("STAFF"))).andExpect(status().isOk());
        mvc.perform(post("/api/staff/bookings/"+id+"/complete").header("Authorization",bearer("STAFF"))).andExpect(status().isOk());
        assertEquals("COMPLETED",db.queryForObject("SELECT status FROM bookings WHERE id=?",String.class,id));
    }
    @Test void demoCannotWriteUploads() throws Exception {
        mvc.perform(multipart("/api/admin/menu/image").file(new org.springframework.mock.web.MockMultipartFile("image","fixture.png","image/png",new byte[]{1}))
                .header("Authorization",bearer("ADMIN"))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DEMO_UPLOAD_DISABLED"));
    }
}
