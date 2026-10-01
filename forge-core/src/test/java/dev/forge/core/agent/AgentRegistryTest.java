package dev.forge.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.forge.core.agent.app.MyAgent;
import dev.forge.core.agent.app.PlainBean;
import dev.forge.core.context.ApplicationContext;
import dev.forge.core.exception.NoSuchBeanException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgentRegistryTest {

    private static final String APP = "dev.forge.core.agent.app";

    @Test
    @DisplayName("an @Agent class is discovered and wired as an ordinary bean")
    void agentIsAManagedBean() {
        try (ApplicationContext context = ApplicationContext.run(APP)) {
            MyAgent agent = context.getBean(MyAgent.class);
            assertNotNull(agent.helper, "agent received its injected dependency");
            assertEquals(agent, context.getBean("myAgent"), "agent is named like any bean");
        }
    }

    @Test
    @DisplayName("the registry finds agents and excludes plain components")
    void findsAgentsOnly() {
        try (ApplicationContext context = ApplicationContext.run(APP)) {
            AgentRegistry registry = AgentRegistry.fromContext(context);
            assertEquals(1, registry.count());
            assertTrue(registry.getAgent(MyAgent.class) instanceof MyAgent);
            assertFalse(registry.getAgents().stream().anyMatch(a -> a instanceof PlainBean));
        }
    }

    @Test
    @DisplayName("requesting an absent agent type fails clearly")
    void absentAgentType() {
        try (ApplicationContext context = ApplicationContext.run(APP)) {
            AgentRegistry registry = AgentRegistry.fromContext(context);
            assertThrows(NoSuchBeanException.class, () -> registry.getAgent(String.class));
        }
    }

    @Test
    @DisplayName("the agents list is immutable")
    void agentsImmutable() {
        try (ApplicationContext context = ApplicationContext.run(APP)) {
            AgentRegistry registry = AgentRegistry.fromContext(context);
            assertThrows(UnsupportedOperationException.class, () -> registry.getAgents().add("x"));
        }
    }
}
