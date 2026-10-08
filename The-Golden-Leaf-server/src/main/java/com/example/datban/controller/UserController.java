package com.example.datban.controller;

import com.example.datban.dto.*;
import com.example.datban.exception.ApiError;
import com.example.datban.service.AuthService;
import com.example.datban.security.RestaurantPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class UserController {
    private final AuthService auth;
    private final String webApiKey;
    private final org.springframework.core.env.Environment environment;
    public UserController(AuthService auth,@Value("${app.firebase.web-api-key:}") String webApiKey,
            org.springframework.core.env.Environment environment) {
        this.auth=auth;this.webApiKey=webApiKey;this.environment=environment;
    }
    @GetMapping("/web-config") public Object config() {return Map.of("apiKey",webApiKey,"demo",environment.matchesProfiles("demo"));}
    @GetMapping("/me") public Object me() {return RestaurantPrincipal.required();}
    @PostMapping("/sync")
    public ResponseEntity<?> sync(@Valid @RequestBody TokenRequest token,HttpServletRequest request) {
        try {
            var user=auth.synchronizeUser(token.getIdToken());
            return ResponseEntity.ok(new UserResponse(user.getUid(),user.getEmail(),user.getTen(),user.getFirebaseProvider()));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return error(403,"ACCESS_DENIED","Cần xác minh email hoặc tài khoản đã bị khóa",request);
        } catch (org.springframework.dao.DataAccessException | IllegalStateException ex) {
            return error(503,"AUTH_UNAVAILABLE","Dịch vụ xác thực chưa sẵn sàng",request);
        } catch (Exception ex) {
            return error(401,"UNAUTHENTICATED","Token không hợp lệ hoặc đã hết hạn",request);
        }
    }
    private ResponseEntity<ApiError> error(int status,String code,String message,HttpServletRequest request) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(),status,HttpStatus.valueOf(status).getReasonPhrase(),code,message,request.getRequestURI(),Map.of()));
    }
}
