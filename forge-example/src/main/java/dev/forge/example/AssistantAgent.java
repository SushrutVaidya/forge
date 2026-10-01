package dev.forge.example;

import dev.forge.core.annotation.Agent;

/**
 * An agent — an ordinary managed bean (constructor-injected) that also carries
 * the {@code @Agent} stereotype, so the AgentRegistry can find it.
 */
@Agent
public class AssistantAgent {

    private final GreetingService greetingService;

    public AssistantAgent(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    public String introduce() {
        return greetingService.greet("I am the assistant agent");
    }
}
