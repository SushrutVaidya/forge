package dev.forge.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as an AI agent — a Forge-managed component intended to reason
 * with a language model and act through tools.
 *
 * <p>{@code @Agent} is a <em>stereotype</em>: it is meta-annotated with
 * {@link Omnissiah}, so an agent is discovered, instantiated, and dependency-
 * injected exactly like any other component. It simply also carries the semantic
 * "this bean is an agent", which the {@code AgentRegistry} uses to locate agents.
 *
 * <p>This milestone establishes agents as a first-class, discoverable category.
 * What an agent <em>does</em> — conversation state, memory, an execution runtime
 * binding its tools and LLM client — is intentionally left to future work, to be
 * shaped by real usage rather than guessed at.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Omnissiah
public @interface Agent {}
