package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when the ForgeScanner encounters an unrecoverable error
 * while discovering components on the classpath.
 *
 * This exception represents scanner-specific failures within the
 * Forge core runtime.
 */
public class ForgeScannerException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ForgeScannerException(String message){
        super(message);
    }
    public ForgeScannerException(String message, Throwable cause) {
        super(message, cause);
    }
}
