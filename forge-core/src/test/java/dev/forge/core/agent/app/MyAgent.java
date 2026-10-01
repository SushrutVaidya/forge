package dev.forge.core.agent.app;

import dev.forge.core.annotation.Agent;

/**
 * An agent that also depends on a plain component, proving agents are ordinary
 * dependency-injected beans that merely carry the {@code @Agent} stereotype.
 */
@Agent
public class MyAgent {

    public final PlainBean helper;

    public MyAgent(PlainBean helper) {
        this.helper = helper;
    }
}
