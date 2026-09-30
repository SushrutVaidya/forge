package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when the tool catalog is invalid — for example when two tools resolve
 * to the same name.
 */
public class ToolDefinitionException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ToolDefinitionException(String message) {
        super(message);
    }
}
