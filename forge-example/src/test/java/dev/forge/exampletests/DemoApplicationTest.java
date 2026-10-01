package dev.forge.exampletests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import dev.forge.core.agent.AgentRegistry;
import dev.forge.core.context.ApplicationContext;
import dev.forge.core.tool.ToolRegistry;
import dev.forge.example.AssistantAgent;
import dev.forge.example.DemoApplication;
import dev.forge.example.GreetingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lives in its own package (not {@code dev.forge.example}) on purpose: if it
 * shared the scanned package, its compiled test class would create a
 * {@code dev/forge/example} directory under {@code target/test-classes} that
 * shadows the real components under {@code target/classes} (the scanner resolves
 * a package to a single classpath location).
 */
class DemoApplicationTest {

    private static final String APP_PACKAGE = "dev.forge.example";

    @Test
    @DisplayName("the example app scans, wires, exposes tools and finds its agent")
    void appWiresAndRuns() {
        try (ApplicationContext context = ApplicationContext.run(APP_PACKAGE)) {
            assertEquals("Hello, world!", context.getBean(GreetingService.class).greet("world"));

            ToolRegistry tools = ToolRegistry.fromContext(context);
            assertEquals(5, tools.invoke("add", 2, 3));

            AgentRegistry agents = AgentRegistry.fromContext(context);
            assertEquals(1, agents.count());
            assertNotNull(agents.getAgent(AssistantAgent.class).introduce());
        }
    }

    @Test
    @DisplayName("main() runs end to end without error")
    void mainRuns() {
        assertDoesNotThrow(() -> DemoApplication.main(new String[0]));
    }
}
