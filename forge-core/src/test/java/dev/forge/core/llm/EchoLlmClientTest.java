package dev.forge.core.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EchoLlmClientTest {

    @Test
    @DisplayName("echo mode returns the user message")
    void echoMode() {
        LlmClient client = new EchoLlmClient();
        assertEquals("ping", client.complete(LlmRequest.of("ping")).text());
    }

    @Test
    @DisplayName("canned mode returns the fixed reply regardless of input")
    void cannedMode() {
        LlmClient client = new EchoLlmClient("always this");
        assertEquals("always this", client.complete(LlmRequest.of("anything")).text());
    }

    @Test
    @DisplayName("rejects a null canned reply")
    void rejectsNullCanned() {
        assertThrows(NullPointerException.class, () -> new EchoLlmClient(null));
    }

    @Test
    @DisplayName("rejects a null request")
    void rejectsNullRequest() {
        EchoLlmClient client = new EchoLlmClient();
        assertThrows(NullPointerException.class, () -> client.complete(null));
    }

    @Test
    @DisplayName("records received requests for assertions")
    void recordsRequests() {
        EchoLlmClient client = new EchoLlmClient();
        client.complete(LlmRequest.of("first"));
        client.complete(LlmRequest.of("second"));
        assertEquals(2, client.receivedRequests().size());
        assertEquals("second", client.lastRequest().userMessage());
    }

    @Test
    @DisplayName("lastRequest before any call fails clearly")
    void lastRequestBeforeAnyCall() {
        assertThrows(IllegalStateException.class, () -> new EchoLlmClient().lastRequest());
    }

    @Test
    @DisplayName("received requests list is immutable")
    void recordedRequestsImmutable() {
        EchoLlmClient client = new EchoLlmClient();
        client.complete(LlmRequest.of("x"));
        assertThrows(UnsupportedOperationException.class,
                () -> client.receivedRequests().add(LlmRequest.of("y")));
    }
}
