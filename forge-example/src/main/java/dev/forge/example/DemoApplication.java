package dev.forge.example;

import dev.forge.core.agent.AgentRegistry;
import dev.forge.core.context.ApplicationContext;
import dev.forge.core.tool.ToolRegistry;

/**
 * A runnable demonstration of Forge.
 *
 * <p>Run it from your IDE (Run {@code DemoApplication}), or from the command line
 * after building:
 * <pre>{@code
 *   mvn -q -pl forge-example -am compile
 *   java -cp "forge-core/target/classes:forge-example/target/classes" \
 *        dev.forge.example.DemoApplication
 * }</pre>
 * (On Windows use {@code ;} instead of {@code :} in the classpath.)
 */
public final class DemoApplication {

    private DemoApplication() {}

    public static void main(String[] args) {
        System.out.println("== Forge demo: bootstrapping the container ==");

        // One line starts everything: scan -> register -> wire -> @PostConstruct.
        try (ApplicationContext context = ApplicationContext.run("dev.forge.example")) {

            System.out.println("registered beans: " + context.getBeanDefinitionNames());

            // Dependency injection + a plain bean call.
            GreetingService greeting = context.getBean(GreetingService.class);
            System.out.println("greeting:         " + greeting.greet("world"));

            // Tools: discovered from the beans, invocable by name.
            ToolRegistry tools = ToolRegistry.fromContext(context);
            System.out.println("tools:            " + tools.getToolNames());
            System.out.println("add(2, 3) =>      " + tools.invoke("add", 2, 3));

            // Each tool has an LLM-ready schema.
            tools.schemas().forEach(schema ->
                    System.out.println("tool schema:      " + schema.toMap()));

            // Agents: ordinary wired beans tagged @Agent.
            AgentRegistry agents = AgentRegistry.fromContext(context);
            System.out.println("agents found:     " + agents.count());
            System.out.println("agent says:       " + agents.getAgent(AssistantAgent.class).introduce());

        } // context closes here -> @PreDestroy callbacks fire

        System.out.println("== context closed, demo complete ==");
    }
}
