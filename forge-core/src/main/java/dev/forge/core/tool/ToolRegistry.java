package dev.forge.core.tool;

import dev.forge.core.annotation.Tool;
import dev.forge.core.context.ApplicationContext;
import dev.forge.core.exception.NoSuchToolException;
import dev.forge.core.exception.ToolDefinitionException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A catalog of executable {@link Tool}s discovered from managed beans.
 *
 * <p>Build one from a running {@link ApplicationContext} — every {@code @Tool}
 * method on every bean is collected automatically — or from an explicit
 * collection of bean instances. Tool names must be unique across the catalog.
 *
 * <p>Instances are immutable once built.
 */
public final class ToolRegistry {

    private final Map<String, ToolMetadata> toolsByName;

    private ToolRegistry(Map<String, ToolMetadata> toolsByName) {
        this.toolsByName = toolsByName;
    }

    /** Builds a registry from every {@code @Tool} method on the context's beans. */
    public static ToolRegistry fromContext(ApplicationContext context) {
        Objects.requireNonNull(context, "context must not be null");
        List<Object> beans = context.getBeanDefinitionNames().stream()
                .map(context::getBean)
                .toList();
        return fromBeans(beans);
    }

    /** Builds a registry from every {@code @Tool} method on the given beans. */
    public static ToolRegistry fromBeans(Collection<Object> beans) {
        Objects.requireNonNull(beans, "beans must not be null");
        Map<String, ToolMetadata> tools = new LinkedHashMap<>();
        for (Object bean : beans) {
            for (Method method : bean.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    ToolMetadata tool = ToolMetadata.of(bean, method);
                    ToolMetadata previous = tools.putIfAbsent(tool.name(), tool);
                    if (previous != null) {
                        throw new ToolDefinitionException(
                                "Duplicate tool name '" + tool.name() + "'");
                    }
                }
            }
        }
        return new ToolRegistry(tools);
    }

    /**
     * @return the tool registered under {@code name}
     * @throws NoSuchToolException if no such tool exists
     */
    public ToolMetadata getTool(String name) {
        ToolMetadata tool = toolsByName.get(name);
        if (tool == null) {
            throw new NoSuchToolException("No tool named '" + name + "'");
        }
        return tool;
    }

    public boolean containsTool(String name) {
        return toolsByName.containsKey(name);
    }

    /** @return all tools; immutable, never null */
    public Collection<ToolMetadata> getTools() {
        return List.copyOf(toolsByName.values());
    }

    /** @return all tool names; immutable, never null */
    public List<String> getToolNames() {
        return List.copyOf(toolsByName.keySet());
    }

    /**
     * Invokes a tool by name.
     *
     * @throws NoSuchToolException if the tool is unknown
     * @throws dev.forge.core.exception.ToolExecutionException if invocation fails
     */
    public Object invoke(String name, Object... arguments) {
        return getTool(name).invoke(arguments);
    }
}
