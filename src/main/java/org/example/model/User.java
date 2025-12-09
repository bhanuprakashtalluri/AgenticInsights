package org.example.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "\"user\"")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    @Convert(converter = StringArrayConverter.class)
    @Column(name = "roles")
    private String[] roles;
    private String refreshToken;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String[] getRoles() { return roles; }
    public void setRoles(String[] roles) { this.roles = roles; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
}
