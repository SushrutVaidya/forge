package dev.forge.core.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LlmRequestTest {

    @Test
    @DisplayName("a user-message-only request has an empty system prompt")
    void userMessageOnly() {
        LlmRequest request = LlmRequest.of("hello");
        assertEquals("", request.systemPrompt());
        assertEquals("hello", request.userMessage());
    }

    @Test
    @DisplayName("captures both system prompt and user message")
    void systemAndUser() {
        LlmRequest request = LlmRequest.of("be terse", "hello");
        assertEquals("be terse", request.systemPrompt());
        assertEquals("hello", request.userMessage());
    }

    @Test
    @DisplayName("a null system prompt becomes empty")
    void nullSystemPromptBecomesEmpty() {
        assertEquals("", LlmRequest.of(null, "hello").systemPrompt());
    }

    @Test
    @DisplayName("rejects a null user message")
    void rejectsNullUserMessage() {
        assertThrows(NullPointerException.class, () -> LlmRequest.of(null));
    }

    @Test
    @DisplayName("rejects a blank user message")
    void rejectsBlankUserMessage() {
        assertThrows(IllegalArgumentException.class, () -> LlmRequest.of("   "));
    }

    @Test
    @DisplayName("equal when both fields match")
    void equality() {
        assertEquals(LlmRequest.of("s", "u"), LlmRequest.of("s", "u"));
        assertEquals(LlmRequest.of("s", "u").hashCode(), LlmRequest.of("s", "u").hashCode());
        LlmRequest r = LlmRequest.of("s", "u");
        assertEquals(r, r);
    }

    @Test
    @DisplayName("not equal when a field differs, or against null/other types")
    void inequality() {
        LlmRequest r = LlmRequest.of("s", "u");
        assertNotEquals(r, LlmRequest.of("other", "u"));
        assertNotEquals(r, LlmRequest.of("s", "different"));
        assertNotEquals(r, null);
        assertNotEquals(r, "not a request");
    }
}
