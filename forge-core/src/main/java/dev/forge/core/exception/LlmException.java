package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when a call to an {@code LlmClient} fails — typically wrapping a
 * provider or transport error from a concrete adapter.
 *
 * <p>Part of the {@code LlmClient} contract: implementations signal failure with
 * this exception so callers can handle any provider uniformly.
 */
public class LlmException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
