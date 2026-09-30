package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when a class cannot be turned into a valid {@code BeanDefinition} —
 * for example when it is not instantiable, or its injectable constructor cannot
 * be determined unambiguously.
 */
public class BeanDefinitionException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
