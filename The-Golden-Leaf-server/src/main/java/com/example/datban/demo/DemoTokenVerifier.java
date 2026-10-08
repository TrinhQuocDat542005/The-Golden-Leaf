package com.example.datban.demo;

import com.example.datban.security.TokenVerifier;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Opaque, process-local fixture tokens. No fixed passwords, Firebase claims or client-granted roles. */
@Component
@Primary
@Profile("demo")
public class DemoTokenVerifier implements TokenVerifier {
    public static final Map<String, Identity> PERSONAS = Map.of(
            "CUSTOMER", new Identity("demo-customer", "customer@example.invalid", "Khách demo", "portfolio", true),
            "OTHER_CUSTOMER", new Identity("demo-other", "other@example.invalid", "Khách demo khác", "portfolio", true),
            "STAFF", new Identity("demo-staff", "staff@example.invalid", "Nhân viên demo", "portfolio", true),
            "ADMIN", new Identity("demo-admin", "admin@example.invalid", "Quản trị demo", "portfolio", true));
    private final Map<String, String> tokens = new LinkedHashMap<>();

    public DemoTokenVerifier() {
        PERSONAS.keySet().stream().sorted().forEach(role -> tokens.put(role, "demo-" + UUID.randomUUID()));
    }

    public String tokenFor(String persona) {
        String token = tokens.get(persona);
        if (token == null) throw new IllegalArgumentException("Unknown demo persona");
        return token;
    }

    @Override
    public Identity verify(String token) {
        if (token == null || token.length() > 100) throw new IllegalArgumentException("Invalid demo token");
        for (var entry : tokens.entrySet()) {
            if (MessageDigest.isEqual(entry.getValue().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
                return PERSONAS.get(entry.getKey());
            }
        }
        throw new IllegalArgumentException("Invalid demo token");
    }
}
