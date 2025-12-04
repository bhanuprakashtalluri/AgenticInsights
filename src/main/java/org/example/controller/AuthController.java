package org.example.controller;

import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.RefreshTokenRequest;
import org.example.model.User;
import org.example.service.AuditLogService;
import org.example.service.JwtService;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return userService.findByUsername(request.getUsername())
                .filter(user -> userService.checkPassword(user, request.getPassword()))
                .map(user -> {
                    String token = jwtService.generateToken(user);
                    auditLogService.log(user.getUsername(), "LOGIN_SUCCESS", "Token issued");
                    LoginResponse response = new LoginResponse();
                    response.setToken(token);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    auditLogService.log(request.getUsername(), "LOGIN_FAIL", "Invalid credentials");
                    return ResponseEntity.status(401).build();
                });
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestBody RefreshTokenRequest request) {
        User user = userService.findByRefreshToken(request.getRefreshToken());
        if (user != null && jwtService.validateRefreshToken(request.getRefreshToken(), user)) {
            String newToken = jwtService.generateToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);
            user.setRefreshToken(newRefreshToken);
            userService.save(user);
            auditLogService.log(user.getUsername(), "REFRESH_SUCCESS", "New token issued");
            LoginResponse response = new LoginResponse();
            response.setToken(newToken);
            return ResponseEntity.ok(response);
        }
        auditLogService.log("UNKNOWN", "REFRESH_FAIL", "Invalid refresh token");
        return ResponseEntity.status(401).build();
    }
}
