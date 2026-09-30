package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when the container detects a circular dependency between beans — a
 * chain of constructor dependencies that leads back to a bean already being
 * created. Such a graph cannot be instantiated with constructor injection and
 * is an unrecoverable configuration error.
 *
 * <p>The message names the full cycle, e.g. {@code a -> b -> a}.
 */
public class CircularDependencyException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public CircularDependencyException(String message) {
        super(message);
    }
}
