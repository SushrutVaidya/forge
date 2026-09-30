package dev.forge.core.context;

import dev.forge.core.definition.BeanDefinition;
import dev.forge.core.lifecycle.LifecycleMetadata;
import dev.forge.core.registry.BeanRegistry;
import dev.forge.core.resolver.DependencyResolver;
import dev.forge.core.scanner.ForgeScanner;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The entry point to Forge: scans a package for components, wires them, and owns
 * their lifecycle from startup to shutdown.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try (ApplicationContext context = ApplicationContext.run("com.example.app")) {
 *     UserService service = context.getBean(UserService.class);
 *     service.doWork();
 * } // @PreDestroy callbacks fire here
 * }</pre>
 *
 * <h2>Startup</h2>
 * {@link #run(String)} scans the base package, registers a definition per
 * {@code @Omnissiah} component, <em>eagerly</em> instantiates every singleton
 * (so configuration errors surface at startup, not at first use), and then
 * invokes {@code @PostConstruct} callbacks in dependency-first order.
 *
 * <h2>Shutdown</h2>
 * {@link #close()} invokes {@code @PreDestroy} callbacks in reverse creation
 * order and clears the singletons. It is idempotent, and the context implements
 * {@link AutoCloseable} for use with try-with-resources.
 */
public final class ApplicationContext implements AutoCloseable {

    private final BeanRegistry registry;
    private final DependencyResolver resolver;

    /** Lifecycle metadata per bean name, in creation order. */
    private final Map<String, LifecycleMetadata> lifecycleByName = new LinkedHashMap<>();

    private volatile boolean closed;

    private ApplicationContext(BeanRegistry registry, DependencyResolver resolver) {
        this.registry = registry;
        this.resolver = resolver;
    }

    /**
     * Bootstraps a context from a base package.
     *
     * @param basePackage the root package to scan for {@code @Omnissiah} components
     * @return a running context with every singleton instantiated, wired and initialized
     */
    public static ApplicationContext run(String basePackage) {
        BeanRegistry registry = new BeanRegistry();
        List<Class<?>> components = new ForgeScanner().scan(basePackage);
        for (Class<?> component : components) {
            registry.registerDefinition(BeanDefinition.fromComponent(component));
        }

        ApplicationContext context = new ApplicationContext(registry, new DependencyResolver(registry));
        context.instantiateSingletons();
        context.initializeSingletons();
        return context;
    }

    /** Eagerly creates and wires every registered singleton. */
    private void instantiateSingletons() {
        for (BeanDefinition definition : registry.getAllDefinitions()) {
            resolver.resolve(definition.beanName());
        }
    }

    /** Invokes {@code @PostConstruct} in creation order (dependencies first). */
    private void initializeSingletons() {
        for (String name : registry.getSingletonNamesInCreationOrder()) {
            Object bean = registry.getSingleton(name);
            LifecycleMetadata lifecycle = LifecycleMetadata.forType(bean.getClass());
            lifecycleByName.put(name, lifecycle);
            lifecycle.invokePostConstruct(bean);
        }
    }

    /**
     * Returns the singleton of the given type.
     *
     * @throws dev.forge.core.exception.NoSuchBeanException   if no bean matches
     * @throws dev.forge.core.exception.NoUniqueBeanException if more than one matches
     * @throws IllegalStateException                          if the context is closed
     */
    public <T> T getBean(Class<T> type) {
        ensureOpen();
        BeanDefinition definition = registry.getDefinitionByType(type);
        return type.cast(resolver.resolve(definition.beanName()));
    }

    /**
     * Returns the singleton registered under the given name.
     *
     * @throws dev.forge.core.exception.NoSuchBeanException if no such bean exists
     * @throws IllegalStateException                        if the context is closed
     */
    public Object getBean(String name) {
        ensureOpen();
        return resolver.resolve(name);
    }

    /** @return all registered bean names; immutable, never null */
    public List<String> getBeanDefinitionNames() {
        return registry.getBeanDefinitionNames();
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Invokes {@code @PreDestroy} callbacks in reverse creation order and clears
     * the singletons. Idempotent.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;

        List<String> creationOrder = registry.getSingletonNamesInCreationOrder();
        for (int i = creationOrder.size() - 1; i >= 0; i--) {
            String name = creationOrder.get(i);
            Object bean = registry.getSingleton(name);
            if (bean != null) {
                lifecycleByName.getOrDefault(name, LifecycleMetadata.forType(bean.getClass()))
                        .invokePreDestroy(bean);
            }
        }
        registry.clearSingletons();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("ApplicationContext is closed");
        }
    }
}
