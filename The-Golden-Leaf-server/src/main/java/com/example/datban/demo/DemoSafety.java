package com.example.datban.demo;

import java.util.Arrays;
import java.util.Set;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Fail before database/Firebase beans can initialize. CLI overrides cannot target a real DB. */
@Configuration
@Profile("demo")
public class DemoSafety {
    public static final String URL = "jdbc:h2:mem:golden_leaf_demo;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    @Bean
    static BeanFactoryPostProcessor isolatedDemoGuard(Environment environment) {
        return beanFactory -> validate(environment);
    }

    public static void validate(Environment environment) {
        Set<String> profiles = Set.copyOf(Arrays.asList(environment.getActiveProfiles()));
        if (!profiles.equals(Set.of("demo")) || !URL.equals(environment.getProperty("spring.datasource.url"))
                || !"org.h2.Driver".equals(environment.getProperty("spring.datasource.driver-class-name"))
                || !"127.0.0.1".equals(environment.getProperty("server.address"))
                || environment.getProperty("app.firebase.enabled", Boolean.class, true)
                || environment.getProperty("app.notification.delivery-enabled", Boolean.class, true)
                || !environment.getProperty("app.security.require-auth", Boolean.class, false)) {
            throw new IllegalStateException("Demo requires the demo profile alone, fixed in-memory H2, loopback binding, auth enabled and Firebase/push disabled.");
        }
    }
}
