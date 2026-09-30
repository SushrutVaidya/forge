package dev.forge.core.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmResponseTest {

    @Test
    @DisplayName("carries its text")
    void carriesText() {
        assertEquals("answer", LlmResponse.of("answer").text());
    }

    @Test
    @DisplayName("rejects null text")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> LlmResponse.of(null));
    }

    @Test
    @DisplayName("equal when text matches")
    void equality() {
        assertEquals(LlmResponse.of("x"), LlmResponse.of("x"));
        assertEquals(LlmResponse.of("x").hashCode(), LlmResponse.of("x").hashCode());
    }
}
