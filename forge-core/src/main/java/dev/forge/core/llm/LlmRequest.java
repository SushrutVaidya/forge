package dev.forge.core.llm;

import java.util.Objects;

/**
 * An immutable request to a language model: a required user message and an
 * optional system prompt.
 *
 * <p>Deliberately minimal for the current milestone — single-turn completion.
 * Multi-turn history, tool declarations, and generation parameters will be added
 * when the agent layer needs them.
 */
public final class LlmRequest {

    private final String systemPrompt;
    private final String userMessage;

    private LlmRequest(String systemPrompt, String userMessage) {
        this.systemPrompt = systemPrompt;
        this.userMessage = userMessage;
    }

    /**
     * Creates a request with just a user message.
     *
     * @param userMessage the user message; must not be null or blank
     */
    public static LlmRequest of(String userMessage) {
        return of("", userMessage);
    }

    /**
     * Creates a request with a system prompt and a user message.
     *
     * @param systemPrompt the system prompt; {@code null} is treated as empty
     * @param userMessage  the user message; must not be null or blank
     */
    public static LlmRequest of(String systemPrompt, String userMessage) {
        Objects.requireNonNull(userMessage, "userMessage must not be null");
        if (userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }
        return new LlmRequest(systemPrompt == null ? "" : systemPrompt, userMessage);
    }

    /** @return the system prompt; never {@code null}, may be empty */
    public String systemPrompt() {
        return systemPrompt;
    }

    /** @return the user message; never {@code null} or blank */
    public String userMessage() {
        return userMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof LlmRequest other
                && systemPrompt.equals(other.systemPrompt)
                && userMessage.equals(other.userMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemPrompt, userMessage);
    }

    @Override
    public String toString() {
        return "LlmRequest{systemPrompt='" + systemPrompt + "', userMessage='" + userMessage + "'}";
    }
}
