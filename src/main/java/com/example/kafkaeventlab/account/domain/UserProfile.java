package com.example.kafkaeventlab.account.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_user_profiles")
public class UserProfile {

    @Id
    private UUID userId;

    private String email;
    private String displayName;
    private String department;
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    protected UserProfile() {
    }

    private UserProfile(
            UUID userId,
            String email,
            String displayName,
            String department,
            String jobTitle,
            UserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.department = department;
        this.jobTitle = jobTitle;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserProfile register(String email, String displayName, String department, String jobTitle) {
        Instant now = Instant.now();
        return new UserProfile(
                UUID.randomUUID(),
                email,
                displayName,
                department,
                jobTitle,
                UserStatus.PENDING_ACTIVATION,
                now,
                now
        );
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void updateProfile(String displayName, String department, String jobTitle) {
        this.displayName = displayName;
        this.department = department;
        this.jobTitle = jobTitle;
        this.updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDepartment() {
        return department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
