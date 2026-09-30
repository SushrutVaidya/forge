package dev.forge.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exposes a method of a managed bean as an executable tool — the unit of action
 * an LLM can invoke by name.
 *
 * <p>The container discovers {@code @Tool} methods on its beans and publishes
 * them in a {@code ToolRegistry}. The tool's name defaults to the method name
 * and may be overridden; the description is intended for the model.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Tool {

    /** The tool name; defaults to the method name when blank. */
    String name() default "";

    /** A human- and model-readable description of what the tool does. */
    String description() default "";
}
