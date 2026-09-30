package dev.forge.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a Forge-managed component.
 *
 * <p>A type annotated with {@code @Omnissiah} is discovered by the
 * {@code ForgeScanner}, registered as a bean, instantiated by the container and
 * has its constructor dependencies injected automatically.
 *
 * <h2>Bean naming</h2>
 * The {@link #value()} attribute assigns an explicit bean name. When left empty,
 * the container derives the name by decapitalizing the simple class name — for
 * example {@code UserService} becomes {@code "userService"}.
 *
 * <h2>Design notes</h2>
 * <ul>
 *   <li>{@link RetentionPolicy#RUNTIME} is load-bearing: the scanner reads this
 *       annotation reflectively at runtime. Without it the marker would be
 *       discarded and no component would ever be discovered.</li>
 *   <li>{@link ElementType#TYPE} restricts the annotation to classes and
 *       interfaces. Applying it to a method or field is a compile error, rather
 *       than being silently ignored at scan time.</li>
 * </ul>
 *
 * @see dev.forge.core.scanner.ForgeScanner
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Omnissiah {

    /**
     * The explicit bean name. When empty (the default), the container derives the
     * name from the decapitalized simple class name.
     *
     * @return the bean name, or an empty string to use the derived default
     */
    String value() default "";
}
