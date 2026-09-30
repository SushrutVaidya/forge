package dev.forge.core.resolver;

import dev.forge.core.definition.BeanDefinition;
import dev.forge.core.exception.BeanInstantiationException;
import dev.forge.core.exception.CircularDependencyException;
import dev.forge.core.registry.BeanRegistry;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Instantiates beans and wires their dependencies through constructor injection.
 *
 * <p>Given a bean name, the resolver looks up its definition, recursively
 * resolves each constructor dependency by type, invokes the constructor, and
 * caches the resulting singleton in the {@link BeanRegistry}. Each singleton is
 * created exactly once and shared thereafter.
 *
 * <h2>Circular dependencies</h2>
 * The resolver tracks the beans currently under construction on the resolution
 * path. If a bean is requested while it is already being created, the graph
 * contains a cycle that constructor injection cannot satisfy, and a
 * {@link CircularDependencyException} is thrown naming the full cycle.
 *
 * <h2>Thread safety</h2>
 * Singleton creation is serialized on a single reentrant lock. Recursion on the
 * owning thread re-enters the lock freely, while other threads wait for the
 * in-flight graph to finish. This guarantees each singleton is constructed
 * exactly once with no race, at the cost of serializing construction — an
 * acceptable trade for a startup-time operation.
 */
public final class DependencyResolver {

    private final BeanRegistry registry;
    private final ReentrantLock creationLock = new ReentrantLock();

    /**
     * Names of beans currently being created, in path order. Guarded by
     * {@link #creationLock}; only ever touched while the lock is held.
     */
    private final Set<String> beansInCreation = new LinkedHashSet<>();

    public DependencyResolver(BeanRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * Returns the fully-wired singleton for {@code beanName}, creating it (and
     * its dependencies) on first request.
     *
     * @throws dev.forge.core.exception.NoSuchBeanException if no such bean is defined
     * @throws CircularDependencyException  if the dependency graph contains a cycle
     * @throws BeanInstantiationException   if a constructor cannot be invoked
     */
    public Object resolve(String beanName) {
        Object cached = registry.getSingleton(beanName);
        if (cached != null) {
            return cached;
        }
        creationLock.lock();
        try {
            return createSingleton(beanName);
        } finally {
            creationLock.unlock();
        }
    }

    private Object createSingleton(String beanName) {
        // Re-check the cache now that we hold the lock: another thread may have
        // created it while we waited, and our own recursion may revisit a bean.
        Object cached = registry.getSingleton(beanName);
        if (cached != null) {
            return cached;
        }

        BeanDefinition definition = registry.getDefinition(beanName);

        if (!beansInCreation.add(beanName)) {
            throw new CircularDependencyException("Circular dependency: " + describeCycle(beanName));
        }
        try {
            Object[] arguments = resolveConstructorArguments(definition);
            Object instance = instantiate(definition, arguments);
            registry.registerSingleton(beanName, instance);
            return instance;
        } finally {
            beansInCreation.remove(beanName);
        }
    }

    private Object[] resolveConstructorArguments(BeanDefinition definition) {
        List<Class<?>> dependencyTypes = definition.dependencyTypes();
        Object[] arguments = new Object[dependencyTypes.size()];
        for (int i = 0; i < dependencyTypes.size(); i++) {
            BeanDefinition dependency = registry.getDefinitionByType(dependencyTypes.get(i));
            arguments[i] = createSingleton(dependency.beanName());
        }
        return arguments;
    }

    private Object instantiate(BeanDefinition definition, Object[] arguments) {
        Constructor<?> constructor = definition.injectableConstructor();
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(arguments);
        } catch (InvocationTargetException e) {
            // Unwrap so the caller sees the exception their constructor actually
            // threw, not the reflection wrapper around it.
            throw new BeanInstantiationException(instantiationFailureMessage(definition), e.getTargetException());
        } catch (ReflectiveOperationException e) {
            throw new BeanInstantiationException(instantiationFailureMessage(definition), e);
        }
    }

    private static String instantiationFailureMessage(BeanDefinition definition) {
        return "Failed to instantiate bean '" + definition.beanName() + "' ("
                + definition.beanType().getName() + ")";
    }

    private String describeCycle(String repeated) {
        return beansInCreation.stream().collect(Collectors.joining(" -> ")) + " -> " + repeated;
    }
}
