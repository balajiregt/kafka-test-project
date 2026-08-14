package com.balaji.kafkaeventlab.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

    List<UserCredential> findByUserId(UUID userId);
}
