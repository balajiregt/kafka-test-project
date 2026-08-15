package com.balaji.kafkaeventlab.assurance;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@ConditionalOnProperty(prefix = "app.assurance.evidence", name = "source-type", havingValue = "jpa", matchIfMissing = true)
public class JpaEventEvidenceSource implements EventEvidenceSource {

    private final EventEvidenceRepository eventEvidenceRepository;

    public JpaEventEvidenceSource(EventEvidenceRepository eventEvidenceRepository) {
        this.eventEvidenceRepository = eventEvidenceRepository;
    }

    @Override
    public List<EventEvidence> findBySubjectId(UUID subjectId) {
        return eventEvidenceRepository.findBySubjectId(subjectId);
    }
}
