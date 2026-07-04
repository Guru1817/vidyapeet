package com.vidyapeet.auth;

import com.vidyapeet.auth.dto.AuthResponse;
import com.vidyapeet.auth.dto.LoginRequest;
import com.vidyapeet.auth.dto.RegisterStudentRequest;
import com.vidyapeet.auth.dto.UserSummary;
import com.vidyapeet.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterStudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerStudent(request));
    }

    @GetMapping("/me")
    public UserSummary me() {
        return authService.currentUser(SecurityUtils.currentUser());
    }
}
