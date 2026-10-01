package dev.forge.core.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ToolParameterTest {

    @Test
    @DisplayName("exposes its name and JSON type")
    void accessors() {
        ToolParameter p = new ToolParameter("count", "integer");
        assertEquals("count", p.name());
        assertEquals("integer", p.jsonType());
    }

    @Test
    @DisplayName("rejects null name or type")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> new ToolParameter(null, "integer"));
        assertThrows(NullPointerException.class, () -> new ToolParameter("count", null));
    }

    @Test
    @DisplayName("value equality on name and type")
    void equality() {
        ToolParameter a = new ToolParameter("count", "integer");
        assertEquals(a, a);
        assertEquals(new ToolParameter("count", "integer"), a);
        assertEquals(new ToolParameter("count", "integer").hashCode(), a.hashCode());
        assertNotEquals(new ToolParameter("other", "integer"), a);
        assertNotEquals(new ToolParameter("count", "string"), a);
        assertNotEquals("not a parameter", a);
        assertNotEquals(null, a);
    }

    @Test
    @DisplayName("toString mentions name and type")
    void string() {
        String s = new ToolParameter("count", "integer").toString();
        assertTrue(s.contains("count") && s.contains("integer"), s);
    }
}
