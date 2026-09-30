package dev.forge.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a no-argument method to be invoked by the container once a bean has been
 * fully constructed and its dependencies injected.
 *
 * <p>Initialization callbacks run in dependency-first order: a bean's
 * dependencies are initialized before the bean itself. A class may declare at
 * most one {@code @PostConstruct} method, and it must take no parameters.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PostConstruct {}
