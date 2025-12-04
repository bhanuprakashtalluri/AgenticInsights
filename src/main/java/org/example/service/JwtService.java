package org.example.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.example.model.User;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private final long EXPIRATION = 86400000; // 1 day

    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("roles", user.getRoles())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String getUsername(String token) {
        return Jwts.parser().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public Set<String> getRoles(String token) {
        Object rolesObj = Jwts.parser().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("roles");
        if (rolesObj instanceof Set) {
            return (Set<String>) rolesObj;
        } else if (rolesObj instanceof java.util.List) {
            return new java.util.HashSet<>((java.util.List<String>) rolesObj);
        }
        return java.util.Collections.emptySet();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("type", "refresh")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION * 7)) // 7 days
                .signWith(key)
                .compact();
    }

    public boolean validateRefreshToken(String token, User user) {
        try {
            String username = Jwts.parser().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
            Object type = Jwts.parser().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("type");
            return username.equals(user.getUsername()) && "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }
}
