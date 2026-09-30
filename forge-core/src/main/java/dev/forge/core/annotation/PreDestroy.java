package dev.forge.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a no-argument method to be invoked by the container when the context is
 * closing, giving a bean the chance to release resources.
 *
 * <p>Destruction callbacks run in reverse creation order: a bean is destroyed
 * before the dependencies it was built from. A class may declare at most one
 * {@code @PreDestroy} method, and it must take no parameters.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PreDestroy {}
