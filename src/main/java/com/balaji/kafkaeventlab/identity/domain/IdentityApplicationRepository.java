package com.balaji.kafkaeventlab.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdentityApplicationRepository extends JpaRepository<IdentityApplication, UUID> {

    Optional<IdentityApplication> findByApplicationCode(String applicationCode);
}
