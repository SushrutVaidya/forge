package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when a bean lifecycle callback ({@code @PostConstruct} or
 * {@code @PreDestroy}) is invalid or fails when invoked.
 */
public class LifecycleException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public LifecycleException(String message) {
        super(message);
    }

    public LifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
