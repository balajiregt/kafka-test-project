package com.balaji.kafkaeventlab.account.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccountTrackedEventRepository extends JpaRepository<AccountTrackedEvent, UUID> {

    List<AccountTrackedEvent> findBySubjectUserId(UUID subjectUserId);
}
