package com.balaji.kafkaeventlab.identity.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IdentityTrackedEventRepository extends JpaRepository<IdentityTrackedEvent, UUID> {

    List<IdentityTrackedEvent> findBySubjectUserId(UUID subjectUserId);
}
