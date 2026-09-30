package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when a tool is requested by a name that the registry does not contain.
 */
public class NoSuchToolException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoSuchToolException(String message) {
        super(message);
    }
}
