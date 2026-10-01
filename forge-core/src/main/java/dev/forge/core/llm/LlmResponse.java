package dev.forge.core.llm;

import java.util.Objects;

/**
 * An immutable response from a language model.
 *
 * <p>Minimal for the current milestone — the generated text. Token usage, stop
 * reasons, and tool-call results will be added when consumers need them.
 */
public final class LlmResponse {

    private final String text;

    private LlmResponse(String text) {
        this.text = text;
    }

    /**
     * @param text the generated text; must not be null
     */
    public static LlmResponse of(String text) {
        Objects.requireNonNull(text, "text must not be null");
        return new LlmResponse(text);
    }

    /** @return the generated text; never {@code null} */
    public String text() {
        return text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof LlmResponse other && text.equals(other.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "LlmResponse{text='" + text + "'}";
    }
}
