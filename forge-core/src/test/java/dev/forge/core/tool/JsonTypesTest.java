package dev.forge.core.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JsonTypesTest {

    @Test
    @DisplayName("string-like types map to \"string\"")
    void strings() {
        assertEquals("string", JsonTypes.forJavaType(String.class));
        assertEquals("string", JsonTypes.forJavaType(char.class));
        assertEquals("string", JsonTypes.forJavaType(Character.class));
    }

    @Test
    @DisplayName("boolean types map to \"boolean\"")
    void booleans() {
        assertEquals("boolean", JsonTypes.forJavaType(boolean.class));
        assertEquals("boolean", JsonTypes.forJavaType(Boolean.class));
    }

    @Test
    @DisplayName("integral types map to \"integer\"")
    void integers() {
        for (Class<?> t : List.of(int.class, Integer.class, long.class, Long.class,
                short.class, Short.class, byte.class, Byte.class)) {
            assertEquals("integer", JsonTypes.forJavaType(t), t.getName());
        }
    }

    @Test
    @DisplayName("floating-point types map to \"number\"")
    void numbers() {
        for (Class<?> t : List.of(double.class, Double.class, float.class, Float.class)) {
            assertEquals("number", JsonTypes.forJavaType(t), t.getName());
        }
    }

    @Test
    @DisplayName("anything else falls back to \"object\"")
    void objectFallback() {
        assertEquals("object", JsonTypes.forJavaType(Object.class));
        assertEquals("object", JsonTypes.forJavaType(List.class));
        assertEquals("object", JsonTypes.forJavaType(int[].class));
    }
}
