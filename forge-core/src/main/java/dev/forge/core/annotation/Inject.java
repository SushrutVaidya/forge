package dev.forge.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the constructor the container should use to instantiate a bean.
 *
 * <p>Only required to disambiguate: a class with a single constructor is used
 * automatically. When a class declares multiple constructors, exactly one must
 * be annotated with {@code @Inject} so the container knows which to invoke.
 *
 * <p>Forge performs constructor injection only. Constructor injection keeps
 * dependencies explicit, allows {@code final} fields, and lets a class be
 * instantiated in a test without the container.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface Inject {}
