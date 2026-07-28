package com.balaji.kafkaeventlab.tracking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TrackedEventRepository extends JpaRepository<TrackedEvent, UUID> {
}
