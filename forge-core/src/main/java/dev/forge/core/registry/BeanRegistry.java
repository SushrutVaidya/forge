package dev.forge.core.registry;

import dev.forge.core.definition.BeanDefinition;
import dev.forge.core.exception.BeanDefinitionException;
import dev.forge.core.exception.NoSuchBeanException;
import dev.forge.core.exception.NoUniqueBeanException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * The container's store of {@link BeanDefinition}s and the cache of instantiated
 * singletons.
 *
 * <p>The registry knows <em>what</em> beans exist and holds the single instance
 * of each once it has been created; it does not itself instantiate or wire
 * beans — that is the resolver's responsibility.
 *
 * <h2>Thread safety</h2>
 * Both the definition map and the singleton cache are backed by
 * {@link ConcurrentHashMap}, so registration and lookup are safe under
 * concurrent access.
 */
public final class BeanRegistry {

    private final Map<String, BeanDefinition> definitionsByName = new ConcurrentHashMap<>();
    private final Map<String, Object> singletonsByName = new ConcurrentHashMap<>();

    /**
     * Registers a bean definition.
     *
     * @throws BeanDefinitionException if a definition is already registered under
     *         the same name
     */
    public void registerDefinition(BeanDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        BeanDefinition existing = definitionsByName.putIfAbsent(definition.beanName(), definition);
        if (existing != null) {
            throw new BeanDefinitionException(
                    "A bean named '" + definition.beanName() + "' is already registered ("
                            + existing.beanType().getName() + " vs " + definition.beanType().getName() + ")");
        }
    }

    /**
     * @return the definition registered under {@code name}
     * @throws NoSuchBeanException if no such definition exists
     */
    public BeanDefinition getDefinition(String name) {
        BeanDefinition definition = definitionsByName.get(name);
        if (definition == null) {
            throw new NoSuchBeanException("No bean definition named '" + name + "'");
        }
        return definition;
    }

    /**
     * Finds the single definition whose bean type is assignable to {@code type}.
     *
     * @throws NoSuchBeanException   if no definition matches
     * @throws NoUniqueBeanException if more than one definition matches
     */
    public BeanDefinition getDefinitionByType(Class<?> type) {
        Objects.requireNonNull(type, "type must not be null");
        List<BeanDefinition> matches = definitionsByName.values().stream()
                .filter(definition -> type.isAssignableFrom(definition.beanType()))
                .toList();
        if (matches.isEmpty()) {
            throw new NoSuchBeanException("No bean definition of type " + type.getName());
        }
        if (matches.size() > 1) {
            String candidates = matches.stream()
                    .map(BeanDefinition::beanName)
                    .collect(Collectors.joining(", "));
            throw new NoUniqueBeanException(
                    "No unique bean of type " + type.getName() + "; candidates: [" + candidates + "]");
        }
        return matches.get(0);
    }

    public boolean containsDefinition(String name) {
        return definitionsByName.containsKey(name);
    }

    /** @return an immutable snapshot of all registered definitions; never {@code null} */
    public List<BeanDefinition> getAllDefinitions() {
        return List.copyOf(definitionsByName.values());
    }

    /** @return an immutable snapshot of all registered bean names; never {@code null} */
    public List<String> getBeanDefinitionNames() {
        return List.copyOf(definitionsByName.keySet());
    }

    // ── singleton cache ───────────────────────────────────────────────────────
    // These methods support the resolver, which owns instantiation. getSingleton
    // returns null on a cache miss (a normal, expected outcome), by design.

    /**
     * Caches a singleton instance.
     *
     * @throws BeanDefinitionException if a singleton is already cached under the name
     */
    public void registerSingleton(String name, Object instance) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(instance, "instance must not be null");
        Object existing = singletonsByName.putIfAbsent(name, instance);
        if (existing != null) {
            throw new BeanDefinitionException("A singleton named '" + name + "' is already registered");
        }
    }

    /** @return the cached singleton, or {@code null} if none has been created yet */
    public Object getSingleton(String name) {
        return singletonsByName.get(name);
    }

    public boolean containsSingleton(String name) {
        return singletonsByName.containsKey(name);
    }

    /** Clears the singleton cache. Definitions are retained. */
    public void clearSingletons() {
        singletonsByName.clear();
    }
}
