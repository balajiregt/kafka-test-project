package com.balaji.kafkaeventlab.identity.projection;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAccessSummaryRepository extends JpaRepository<UserAccessSummaryProjection, UUID> {

    List<UserAccessSummaryProjection> findByUserId(UUID userId);
}
