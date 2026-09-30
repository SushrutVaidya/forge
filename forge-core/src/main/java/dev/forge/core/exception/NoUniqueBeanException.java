package dev.forge.core.exception;

import java.io.Serial;

/**
 * Thrown when a bean is requested by type but more than one definition matches,
 * and there is no way to choose between them.
 *
 * <p>The MVP has no qualifier or primary-bean mechanism, so an ambiguous
 * by-type lookup is always an error the developer must resolve.
 */
public class NoUniqueBeanException extends ForgeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public NoUniqueBeanException(String message) {
        super(message);
    }
}
