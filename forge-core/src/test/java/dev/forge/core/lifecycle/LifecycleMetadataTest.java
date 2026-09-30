package dev.forge.core.lifecycle;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.forge.core.annotation.PostConstruct;
import dev.forge.core.annotation.PreDestroy;
import dev.forge.core.exception.LifecycleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LifecycleMetadataTest {

    static class Good {
        boolean initialized;
        boolean destroyed;

        @PostConstruct
        void init() {
            initialized = true;
        }

        @PreDestroy
        void destroy() {
            destroyed = true;
        }
    }

    static class NoCallbacks {}

    static class MultiplePostConstruct {
        @PostConstruct
        void a() {}

        @PostConstruct
        void b() {}
    }

    static class ParameterizedCallback {
        @PostConstruct
        void init(String unexpected) {}
    }

    static class ThrowingCallback {
        @PostConstruct
        void init() {
            throw new IllegalStateException("init failed");
        }
    }

    @Test
    @DisplayName("invokes @PostConstruct and @PreDestroy")
    void invokesCallbacks() {
        Good bean = new Good();
        LifecycleMetadata lifecycle = LifecycleMetadata.forType(Good.class);
        lifecycle.invokePostConstruct(bean);
        lifecycle.invokePreDestroy(bean);
        assertTrue(bean.initialized);
        assertTrue(bean.destroyed);
    }

    @Test
    @DisplayName("no callbacks is a safe no-op")
    void noCallbacksIsNoop() {
        LifecycleMetadata lifecycle = LifecycleMetadata.forType(NoCallbacks.class);
        lifecycle.invokePostConstruct(new NoCallbacks());
        lifecycle.invokePreDestroy(new NoCallbacks());
    }

    @Test
    @DisplayName("rejects multiple callbacks of the same phase")
    void rejectsMultiple() {
        assertThrows(LifecycleException.class,
                () -> LifecycleMetadata.forType(MultiplePostConstruct.class));
    }

    @Test
    @DisplayName("rejects a callback that declares parameters")
    void rejectsParameters() {
        assertThrows(LifecycleException.class,
                () -> LifecycleMetadata.forType(ParameterizedCallback.class));
    }

    @Test
    @DisplayName("wraps a throwing callback, preserving the cause")
    void wrapsThrowingCallback() {
        LifecycleMetadata lifecycle = LifecycleMetadata.forType(ThrowingCallback.class);
        LifecycleException ex = assertThrows(LifecycleException.class,
                () -> lifecycle.invokePostConstruct(new ThrowingCallback()));
        assertInstanceOf(IllegalStateException.class, ex.getCause());
    }
}
