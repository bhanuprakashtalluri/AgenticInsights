package org.example.controller;

import org.example.dto.LoginRequest;
import org.example.model.User;
import org.example.service.AuditLogService;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private UserService userService;
    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return userService.findByUsername(request.getUsername())
                .filter(user -> userService.checkPassword(user, request.getPassword()))
                .map(user -> {
                    auditLogService.log(user.getUsername(), "LOGIN_SUCCESS", "Session started");
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        user.getUsername(),
                        null,
                        user.getRoles() != null ? List.of(new SimpleGrantedAuthority("ROLE_" + user.getRoles()[0])) : List.of()
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    // Persist SecurityContext in session
                    httpRequest.getSession(true)
                        .setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                    return ResponseEntity.ok().build();
                })
                .orElseGet(() -> {
                    auditLogService.log(request.getUsername(), "LOGIN_FAIL", "Invalid credentials");
                    return ResponseEntity.status(401).build();
                });
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestParam String email, @RequestParam String newPassword) {
        boolean updated = userService.updatePassword(email, newPassword);
        if (updated) {
            return ResponseEntity.ok("Password updated successfully");
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }

    @PostMapping("/sync-users")
    public ResponseEntity<?> syncUsersWithEmployees() {
        userService.syncUsersWithEmployees();
        return ResponseEntity.ok("Users synced with employees.");
    }

    @GetMapping("/me")
    public Map<String, Object> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        System.out.println("/api/auth/me lookup username: " + username); // DEBUG
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) {
            return Map.of("error", "User not found", "username", username);
        }
        // Extract plain role string
        String role = null;
        if (user.getRoles() != null && user.getRoles().length > 0) {
            role = user.getRoles()[0];
            if (role != null) {
                role = role.replaceAll("[{}\" ]", "").toLowerCase(); // Remove braces, quotes, spaces, lowercase
            }
        }
        return Map.of(
            "email", user.getUsername(),
            "role", role
        );
    }
}
