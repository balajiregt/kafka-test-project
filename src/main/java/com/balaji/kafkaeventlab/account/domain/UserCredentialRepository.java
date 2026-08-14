package com.balaji.kafkaeventlab.account.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

    List<UserCredential> findByUserId(UUID userId);

    Optional<UserCredential> findByUserIdAndCredentialReference(UUID userId, String credentialReference);
}
