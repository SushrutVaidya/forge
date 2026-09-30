package dev.forge.core.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.forge.core.context.ApplicationContext;
import dev.forge.core.exception.NoSuchToolException;
import dev.forge.core.exception.ToolDefinitionException;
import dev.forge.core.exception.ToolExecutionException;
import dev.forge.core.tool.fixtures.ToolFixtures;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

    private ToolRegistry calculatorRegistry() {
        return ToolRegistry.fromBeans(List.of(new ToolFixtures.Calculator()));
    }

    @Nested
    @DisplayName("discovery")
    class Discovery {

        @Test
        @DisplayName("discovers @Tool methods, ignoring plain methods")
        void discoversTools() {
            Set<String> names = calculatorRegistry().getTools().stream()
                    .map(ToolMetadata::name)
                    .collect(Collectors.toSet());
            assertEquals(Set.of("add", "sub", "boom"), names);
        }

        @Test
        @DisplayName("defaults the tool name to the method name")
        void defaultName() {
            assertTrue(calculatorRegistry().containsTool("add"));
        }

        @Test
        @DisplayName("uses the explicit tool name when given")
        void explicitName() {
            ToolRegistry registry = calculatorRegistry();
            assertTrue(registry.containsTool("sub"));
            assertTrue(!registry.containsTool("subtract"));
        }

        @Test
        @DisplayName("carries the description")
        void description() {
            assertEquals("adds two integers", calculatorRegistry().getTool("add").description());
        }

        @Test
        @DisplayName("duplicate tool names fail fast")
        void duplicateNames() {
            List<Object> beans = List.of(new ToolFixtures.DuplicateTools());
            assertThrows(ToolDefinitionException.class, () -> ToolRegistry.fromBeans(beans));
        }

        @Test
        @DisplayName("discovers tools from a running context")
        void fromContext() {
            try (ApplicationContext context = ApplicationContext.run("dev.forge.core.tool.app")) {
                ToolRegistry registry = ToolRegistry.fromContext(context);
                assertEquals("hello world", registry.invoke("greet", "world"));
            }
        }
    }

    @Nested
    @DisplayName("invocation")
    class Invocation {

        @Test
        @DisplayName("invokes a tool and returns its result")
        void invokes() {
            assertEquals(5, calculatorRegistry().invoke("add", 2, 3));
        }

        @Test
        @DisplayName("a throwing tool is wrapped, preserving the cause")
        void throwingTool() {
            ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                    () -> calculatorRegistry().invoke("boom"));
            assertInstanceOf(IllegalStateException.class, ex.getCause());
        }

        @Test
        @DisplayName("argument mismatch is wrapped")
        void argumentMismatch() {
            assertThrows(ToolExecutionException.class,
                    () -> calculatorRegistry().invoke("add", "not", "ints"));
        }

        @Test
        @DisplayName("unknown tool throws NoSuchToolException")
        void unknownTool() {
            assertThrows(NoSuchToolException.class, () -> calculatorRegistry().invoke("missing"));
        }
    }

    @Nested
    @DisplayName("contract")
    class Contract {

        @Test
        @DisplayName("tool collections are immutable")
        void immutableCollections() {
            ToolRegistry registry = calculatorRegistry();
            assertThrows(UnsupportedOperationException.class, () -> registry.getToolNames().add("x"));
            assertThrows(UnsupportedOperationException.class, () -> registry.getTools().clear());
        }

        @Test
        @DisplayName("captures parameter types")
        void parameterTypes() {
            assertEquals(List.of(int.class, int.class), calculatorRegistry().getTool("add").parameterTypes());
        }
    }
}
