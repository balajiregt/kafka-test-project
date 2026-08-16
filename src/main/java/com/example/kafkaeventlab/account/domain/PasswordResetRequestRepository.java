package com.example.kafkaeventlab.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, UUID> {

    List<PasswordResetRequest> findByUserId(UUID userId);
}
