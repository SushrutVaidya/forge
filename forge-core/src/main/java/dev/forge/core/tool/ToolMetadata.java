package dev.forge.core.tool;

import dev.forge.core.annotation.Tool;
import dev.forge.core.exception.ToolExecutionException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * An immutable descriptor of a single executable tool: its name, description,
 * the bean instance that owns it, and the method to invoke.
 *
 * <p>Created from a {@link Tool}-annotated method; {@link #invoke(Object...)}
 * calls that method reflectively on the owning bean.
 */
public final class ToolMetadata {

    private final String name;
    private final String description;
    private final Object bean;
    private final Method method;
    private final List<Class<?>> parameterTypes;

    private ToolMetadata(String name, String description, Object bean, Method method) {
        this.name = name;
        this.description = description;
        this.bean = bean;
        this.method = method;
        this.parameterTypes = List.of(method.getParameterTypes());
    }

    /**
     * Builds metadata for a {@link Tool}-annotated method.
     *
     * @param bean   the instance the method will be invoked on
     * @param method a method annotated with {@link Tool}
     */
    public static ToolMetadata of(Object bean, Method method) {
        Tool tool = method.getAnnotation(Tool.class);
        if (tool == null) {
            throw new IllegalArgumentException(
                    "Method " + method + " is not annotated with @Tool");
        }
        String name = tool.name().isBlank() ? method.getName() : tool.name();
        return new ToolMetadata(name, tool.description(), bean, method);
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    /** @return the tool's parameter types, in order; never {@code null}, immutable */
    public List<Class<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Invokes the tool.
     *
     * @param arguments the arguments, matching the method signature
     * @return the method's return value ({@code null} for a void tool)
     * @throws ToolExecutionException if the tool throws, or the arguments do not match
     */
    public Object invoke(Object... arguments) {
        try {
            method.setAccessible(true);
            return method.invoke(bean, arguments);
        } catch (InvocationTargetException e) {
            throw new ToolExecutionException(
                    "Tool '" + name + "' threw an exception", e.getTargetException());
        } catch (IllegalArgumentException e) {
            throw new ToolExecutionException("Illegal arguments for tool '" + name + "'", e);
        } catch (ReflectiveOperationException e) {
            throw new ToolExecutionException("Tool '" + name + "' could not be invoked", e);
        }
    }

    @Override
    public String toString() {
        return "ToolMetadata{name='" + name + "', parameters=" + parameterTypes.size() + "}";
    }
}
