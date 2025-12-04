package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User save(User user) {
        // Store password as plain text or hardcoded value
        // For hardcoded, you can set user.setPassword("password");
        return userRepository.save(user);
    }

    public boolean checkPassword(User user, String rawPassword) {
        // Simple string comparison
        return rawPassword.equals(user.getPassword());
    }

    public User findByRefreshToken(String refreshToken) {
        return userRepository.findAll().stream()
            .filter(u -> refreshToken.equals(u.getRefreshToken()))
            .findFirst().orElse(null);
    }
}
