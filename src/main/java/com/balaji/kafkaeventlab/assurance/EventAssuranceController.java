package com.balaji.kafkaeventlab.assurance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assurance")
public class EventAssuranceController {

    private final EventAssuranceGraphService graphService;
    private final EventAssuranceAdvisor advisor;

    public EventAssuranceController(
            EventAssuranceGraphService graphService,
            EventAssuranceAdvisor advisor
    ) {
        this.graphService = graphService;
        this.advisor = advisor;
    }

    @GetMapping("/subjects/{subjectId}/journey")
    public AssuranceJourneyResponse journey(@PathVariable UUID subjectId) {
        return graphService.journeyForSubject(subjectId);
    }

    @GetMapping("/subjects/{subjectId}/findings")
    public List<AssuranceFinding> findings(@PathVariable UUID subjectId) {
        return advisor.findings(graphService.journeyForSubject(subjectId));
    }

    @GetMapping("/testing-recommendations")
    public List<TestingRecommendation> testingRecommendations() {
        return advisor.testingRecommendations();
    }
}
