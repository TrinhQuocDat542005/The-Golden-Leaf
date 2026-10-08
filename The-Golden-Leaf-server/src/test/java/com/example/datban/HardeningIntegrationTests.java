package com.example.datban;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:hardening;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "app.security.require-auth=true","app.monitoring.token=test-monitoring-only-token-32-characters",
    "management.endpoints.web.exposure.include=health,prometheus","springdoc.api-docs.enabled=false","springdoc.swagger-ui.enabled=false"})
@AutoConfigureMockMvc
@org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
@ActiveProfiles("test")
@Import(OperationsTests.Fakes.class)
class HardeningIntegrationTests {
    @Autowired MockMvc mvc;
    @Test void readinessIsPublicButNeverRevealsComponents() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP")).andExpect(jsonPath("$.components").doesNotExist());
    }
    @Test void livenessIsPublicAndHasCorrelationId() throws Exception {
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(header().exists("X-Request-ID"));
    }
    @Test void metricsRejectAnonymousInvalidAndBusinessTokens() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header("Authorization","Bearer wrong")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header("Authorization","Bearer admin")).andExpect(status().isUnauthorized());
    }
    @Test void dedicatedCredentialCanReadMetricsOnly() throws Exception {
        String token="Bearer test-monitoring-only-token-32-characters";
        mvc.perform(get("/actuator/prometheus").header("Authorization",token)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("jvm_memory")));
        mvc.perform(get("/api/staff/bookings").header("Authorization",token)).andExpect(status().isUnauthorized());
    }
    @Test void actuatorMutationsAndOtherEndpointsAreDenied() throws Exception {
        mvc.perform(post("/actuator/health")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env").header("Authorization","Bearer test-monitoring-only-token-32-characters")).andExpect(status().isUnauthorized());
    }
    @Test void disabledSwaggerIsNotExposedAndUnknownResourcesAre404() throws Exception {
        mvc.perform(get("/v3/api-docs").header("Authorization","Bearer admin")).andExpect(status().isNotFound());
        mvc.perform(get("/missing-resource").header("Authorization","Bearer admin")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
    @Test void securityHeadersAndDeniedRequestCorrelationArePresent() throws Exception {
        mvc.perform(get("/api/staff/bookings")).andExpect(status().isUnauthorized()).andExpect(header().exists("X-Request-ID"))
            .andExpect(header().string("X-Content-Type-Options","nosniff"));
        mvc.perform(get("/staff.html")).andExpect(status().isOk()).andExpect(header().exists("Content-Security-Policy"));
    }
    @Test void demoSessionsAreNotInstalledOutsideDemoProfile() throws Exception {
        mvc.perform(get("/api/demo/config")).andExpect(status().isNotFound());
        mvc.perform(post("/api/demo/session").contentType("application/json").content("{\"persona\":\"ADMIN\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/auth/me").header("Authorization","Bearer demo-not-a-firebase-token")).andExpect(status().isUnauthorized());
    }
}
