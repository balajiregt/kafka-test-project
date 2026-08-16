package com.example.kafkaeventlab.assurance;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assurance")
public class EventAssuranceController {

    private final EventAssuranceGraphService graphService;
    private final EventAssuranceAdvisor advisor;
    private final AssuranceAssistantService assistantService;
    private final AssurancePlaygroundService playgroundService;

    public EventAssuranceController(
            EventAssuranceGraphService graphService,
            EventAssuranceAdvisor advisor,
            AssuranceAssistantService assistantService,
            AssurancePlaygroundService playgroundService
    ) {
        this.graphService = graphService;
        this.advisor = advisor;
        this.assistantService = assistantService;
        this.playgroundService = playgroundService;
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
    public List<TestingRecommendation> testingRecommendations(@RequestParam(required = false) UUID subjectId) {
        if (subjectId != null) {
            return advisor.testingRecommendations(graphService.journeyForSubject(subjectId));
        }
        return advisor.testingRecommendations();
    }

    @PostMapping("/config-assistant")
    public AssuranceAssistantResponse configAssistant(@RequestBody AssuranceAssistantRequest request) {
        return assistantService.advise(request);
    }

    @PostMapping("/playground/evidence")
    public AssurancePlaygroundResponse submitPlaygroundEvidence(@RequestBody AssurancePlaygroundRequest request) {
        return playgroundService.submit(request);
    }
}
