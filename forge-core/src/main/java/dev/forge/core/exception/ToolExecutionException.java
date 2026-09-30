package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when invoking a tool fails — because the tool threw, or its arguments
 * did not match its signature.
 */
public class ToolExecutionException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ToolExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
