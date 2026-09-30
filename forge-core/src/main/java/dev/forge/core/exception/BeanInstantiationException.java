package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when a bean's constructor cannot be invoked — because it threw an
 * exception, or reflection was unable to instantiate the class.
 */
public class BeanInstantiationException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public BeanInstantiationException(String message, Throwable cause) {
        super(message, cause);
    }
}
