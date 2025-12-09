package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User save(User user) {
        // Hash password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public boolean checkPassword(User user, String rawPassword) {
        // Use BCrypt to match hashed password
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }

    public boolean updatePassword(String username, String newPassword) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public void syncUsersWithEmployees() {
        employeeRepository.findAll().forEach(emp -> {
            Optional<User> userOpt = userRepository.findByUsername(emp.getEmail());
            if (userOpt.isEmpty()) {
                User user = new User();
                user.setUsername(emp.getEmail());
                user.setRoles(new String[]{emp.getRole().toUpperCase()});
                String defaultPassword = switch (emp.getRole().toLowerCase()) {
                    case "admin" -> "Admin@123";
                    case "manager" -> "Manager@123";
                    case "teamlead" -> "Teamlead@123";
                    case "employee" -> "Employee@123";
                    default -> "User@123";
                };
                user.setPassword(passwordEncoder.encode(defaultPassword));
                userRepository.save(user);
            } else {
                User user = userOpt.get();
                user.setRoles(new String[]{emp.getRole().toUpperCase()});
                userRepository.save(user);
            }
        });
    }

    public User findByRefreshToken(String refreshToken) {
        return userRepository.findAll().stream()
            .filter(u -> refreshToken.equals(u.getRefreshToken()))
            .findFirst().orElse(null);
    }
}
