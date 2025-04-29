package com.takuro_tamura.autofx.presentation.controller;

import com.takuro_tamura.autofx.domain.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    /** 200 OK + ユーザ情報 (JSON) / 未認証なら 401 */
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return (auth != null && auth.isAuthenticated())
            ? ResponseEntity.ok(Map.of(
            "username", auth.getName(),
            "roles", auth.getAuthorities()))
            : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request) {
        userService.registerUser(request.getUsername(), request.getPassword());
        return ResponseEntity.ok("User successfully registered");
    }

    @Data
    public static class UserRegistrationRequest {
        private String username;
        private String password;
    }
}

