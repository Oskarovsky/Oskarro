package com.oskarro.oskarito.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "account_activation")
public class AccountActivation {

    @Id
    @Column(length = 64)
    private String token;

    private Integer userId;

    private Instant expiresAt;

    public AccountActivation() {
    }

    public AccountActivation(String token, Integer userId, Instant expiresAt) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public Integer getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
