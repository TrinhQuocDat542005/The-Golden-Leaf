package com.example.datban.demo;

import com.example.datban.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("demo")
@RequestMapping("/api/demo")
public class DemoController {
    private final DemoTokenVerifier verifier;
    private final AuthService auth;
    private final Clock clock;
    public DemoController(DemoTokenVerifier verifier, AuthService auth, Clock clock) {
        this.verifier = verifier; this.auth = auth; this.clock = clock;
    }
    @GetMapping("/config")
    public ResponseEntity<?> config() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "demo", true, "today", LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))),
                "personas", DemoTokenVerifier.PERSONAS.keySet().stream().sorted().toList()));
    }
    public record SessionRequest(@NotBlank @Pattern(regexp="CUSTOMER|OTHER_CUSTOMER|STAFF|ADMIN") String persona) {}
    @PostMapping("/session")
    public ResponseEntity<?> session(@Valid @RequestBody SessionRequest request) throws Exception {
        String token = verifier.tokenFor(request.persona());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "demo", true, "token", token, "profile", auth.authenticate(token)));
    }
}
