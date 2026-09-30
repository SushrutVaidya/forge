package dev.forge.core.exception;

import java.io.Serial;

/**
 * Base type for all runtime exceptions thrown by the Forge framework.
 *
 * <p>Forge uses unchecked exceptions throughout: its failures are configuration
 * or wiring errors that surface at startup and are not meaningfully recoverable
 * at runtime. Catching {@code ForgeException} catches every framework-originated
 * failure in one place.
 */
public abstract class ForgeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    protected ForgeException(String message) {
        super(message);
    }

    protected ForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
