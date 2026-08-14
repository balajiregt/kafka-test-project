package com.balaji.kafkaeventlab.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationAccessGrantRepository extends JpaRepository<ApplicationAccessGrant, UUID> {

    List<ApplicationAccessGrant> findByUserId(UUID userId);
}
