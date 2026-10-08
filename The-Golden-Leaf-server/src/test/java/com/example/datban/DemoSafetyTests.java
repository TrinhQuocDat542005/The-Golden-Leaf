package com.example.datban;

import com.example.datban.demo.DemoSafety;
import com.example.datban.demo.DemoTokenVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class DemoSafetyTests {
    private MockEnvironment safe() {
        var env = new MockEnvironment().withProperty("spring.datasource.url", DemoSafety.URL)
                .withProperty("spring.datasource.driver-class-name", "org.h2.Driver")
                .withProperty("server.address", "127.0.0.1").withProperty("app.firebase.enabled", "false")
                .withProperty("app.notification.delivery-enabled", "false").withProperty("app.security.require-auth", "true");
        env.setActiveProfiles("demo"); return env;
    }
    @Test void isolatedConfigurationAccepted() { assertDoesNotThrow(() -> DemoSafety.validate(safe())); }
    @Test void mixedProductionProfileRejected() { var env=safe();env.setActiveProfiles("demo","prod");assertThrows(IllegalStateException.class,()->DemoSafety.validate(env)); }
    @Test void mixedDevelopmentProfileRejected() { var env=safe();env.setActiveProfiles("dev","demo");assertThrows(IllegalStateException.class,()->DemoSafety.validate(env)); }
    @Test void externalDatabaseRejected() { assertThrows(IllegalStateException.class,()->DemoSafety.validate(safe().withProperty("spring.datasource.url","jdbc:mysql://db/real"))); }
    @Test void publicBindingRejected() { assertThrows(IllegalStateException.class,()->DemoSafety.validate(safe().withProperty("server.address","0.0.0.0"))); }
    @Test void realFirebaseRejected() { assertThrows(IllegalStateException.class,()->DemoSafety.validate(safe().withProperty("app.firebase.enabled","true"))); }
    @Test void pushDeliveryRejected() { assertThrows(IllegalStateException.class,()->DemoSafety.validate(safe().withProperty("app.notification.delivery-enabled","true"))); }
    @Test void anonymousBusinessAccessRejected() { assertThrows(IllegalStateException.class,()->DemoSafety.validate(safe().withProperty("app.security.require-auth","false"))); }
    @Test void processTokensAreFreshAndOnlyKnownPersonasAccepted() {
        var first=new DemoTokenVerifier();var second=new DemoTokenVerifier();
        assertNotEquals(first.tokenFor("ADMIN"),second.tokenFor("ADMIN"));
        assertEquals("demo-customer",first.verify(first.tokenFor("CUSTOMER")).uid());
        assertThrows(IllegalArgumentException.class,()->first.verify(second.tokenFor("ADMIN")));
        assertThrows(IllegalArgumentException.class,()->first.verify("admin"));
        assertThrows(IllegalArgumentException.class,()->first.tokenFor("ROOT"));
    }
}
