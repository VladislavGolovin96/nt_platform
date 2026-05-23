package com.loadtest.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * Read-only view of auth-service's users table (shared PostgreSQL DB).
 */
@Entity
@Immutable
@Table(name = "users")
public class UserRecord {

    @Id
    private UUID id;

    @Column(name = "username")
    private String username;

    @Column(name = "email")
    private String email;

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
