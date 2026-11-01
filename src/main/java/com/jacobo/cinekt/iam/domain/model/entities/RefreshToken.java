package com.jacobo.cinekt.iam.domain.model.entities;

import com.jacobo.cinekt.iam.domain.model.aggregates.User;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false, length = 36)
    private String familyId;

    protected RefreshToken() {
    }

    public RefreshToken(String tokenHash, User user, Instant expiresAt) {
        this(tokenHash, user, expiresAt, UUID.randomUUID().toString());
    }

    private RefreshToken(String tokenHash, User user, Instant expiresAt, String familyId) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
        this.revoked = false;
        this.familyId = familyId;
    }

    public boolean isExpired() {
        return !Instant.now().isBefore(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    public RefreshToken rotate(String newTokenHash, Instant newExpiresAt) {
        this.revoke();
        return new RefreshToken(newTokenHash, this.user, newExpiresAt, this.familyId);
    }
}
