package dev.forge.core.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Contract tests for {@link Omnissiah}.
 *
 * <p>These assertions guard the annotation's load-bearing meta-annotations. If a
 * future change accidentally drops {@code RUNTIME} retention or the {@code TYPE}
 * target, component discovery would break silently across the whole framework;
 * these tests turn that into a loud, immediate failure.
 */
class OmnissiahTest {

    @Omnissiah
    private static class DefaultNamed {}

    @Omnissiah("customName")
    private static class ExplicitlyNamed {}

    @Test
    @DisplayName("is retained at runtime so the scanner can read it reflectively")
    void hasRuntimeRetention() {
        Retention retention = Omnissiah.class.getAnnotation(Retention.class);
        assertNotNull(retention, "@Omnissiah must declare a @Retention policy");
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    @DisplayName("targets types only, so misuse on methods or fields is a compile error")
    void targetsTypesOnly() {
        Target target = Omnissiah.class.getAnnotation(Target.class);
        assertNotNull(target, "@Omnissiah must restrict its @Target");
        assertArrayEquals(new ElementType[] {ElementType.TYPE}, target.value());
    }

    @Test
    @DisplayName("defaults the bean name to empty, signalling a derived name")
    void defaultsToEmptyName() {
        Omnissiah annotation = DefaultNamed.class.getAnnotation(Omnissiah.class);
        assertNotNull(annotation);
        assertEquals("", annotation.value());
    }

    @Test
    @DisplayName("carries an explicit bean name when one is provided")
    void carriesExplicitName() {
        Omnissiah annotation = ExplicitlyNamed.class.getAnnotation(Omnissiah.class);
        assertNotNull(annotation);
        assertEquals("customName", annotation.value());
    }
}
