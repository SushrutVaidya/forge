package dev.forge.core.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for the exception hierarchy: every framework exception is a
 * {@link ForgeException}, and the cause-preserving constructors keep their cause.
 */
class ForgeExceptionTest {

    @Test
    @DisplayName("all framework exceptions share the ForgeException base")
    void sharedBase() {
        assertInstanceOf(ForgeException.class, new ForgeScannerException("x"));
        assertInstanceOf(ForgeException.class, new BeanDefinitionException("x"));
        assertInstanceOf(ForgeException.class, new NoSuchBeanException("x"));
        assertInstanceOf(ForgeException.class, new NoUniqueBeanException("x"));
        assertInstanceOf(ForgeException.class, new CircularDependencyException("x"));
        assertInstanceOf(ForgeException.class, new LifecycleException("x"));
        assertInstanceOf(ForgeException.class, new ToolDefinitionException("x"));
        assertInstanceOf(ForgeException.class, new NoSuchToolException("x"));
        assertInstanceOf(ForgeException.class, new LlmException("x"));
    }

    @Test
    @DisplayName("message-only constructors carry their message")
    void messageOnly() {
        assertEquals("boom", new LlmException("boom").getMessage());
        assertEquals("boom", new NoSuchBeanException("boom").getMessage());
    }

    @Test
    @DisplayName("cause-preserving constructors keep message and cause")
    void withCause() {
        Throwable cause = new IllegalStateException("root");
        assertSame(cause, new ForgeScannerException("m", cause).getCause());
        assertSame(cause, new BeanDefinitionException("m", cause).getCause());
        assertSame(cause, new BeanInstantiationException("m", cause).getCause());
        assertSame(cause, new LifecycleException("m", cause).getCause());
        assertSame(cause, new ToolExecutionException("m", cause).getCause());
        assertSame(cause, new LlmException("m", cause).getCause());
    }
}
