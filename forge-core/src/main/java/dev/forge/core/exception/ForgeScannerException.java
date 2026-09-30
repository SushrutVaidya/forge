package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when the {@code ForgeScanner} encounters an unrecoverable error while
 * discovering components on the classpath.
 */
public class ForgeScannerException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ForgeScannerException(String message) {
        super(message);
    }

    public ForgeScannerException(String message, Throwable cause) {
        super(message, cause);
    }
}
