package com.balaji.kafkaeventlab.assurance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventEvidenceRepository extends JpaRepository<EventEvidence, UUID> {

    List<EventEvidence> findBySubjectId(UUID subjectId);
}
