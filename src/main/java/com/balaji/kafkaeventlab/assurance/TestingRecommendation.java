package com.balaji.kafkaeventlab.assurance;

import java.util.List;

public record TestingRecommendation(
        String capability,
        String testLayer,
        String whyHere,
        List<String> exampleAssertions
) {
}
