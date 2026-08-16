package com.example.kafkaeventlab.assurance;

import java.util.List;
import java.util.UUID;

public interface EventEvidenceSource {

    List<EventEvidence> findBySubjectId(UUID subjectId);
}
