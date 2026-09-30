package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when a bean is requested by a name or type that no definition matches.
 */
public class NoSuchBeanException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoSuchBeanException(String message) {
        super(message);
    }
}
