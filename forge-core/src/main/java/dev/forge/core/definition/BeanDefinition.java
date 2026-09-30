package dev.forge.core.definition;

import dev.forge.core.annotation.Inject;
import dev.forge.core.annotation.Omnissiah;
import dev.forge.core.annotation.Stereotypes;
import dev.forge.core.exception.BeanDefinitionException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Immutable metadata describing a single managed bean: its type, its name, the
 * constructor the container will invoke, and the types that constructor depends
 * on.
 *
 * <p>A {@code BeanDefinition} is validated at construction. Once created it is
 * guaranteed to be instantiable and to have a single, unambiguous injectable
 * constructor — an invalid definition can never exist. Instances are immutable
 * and therefore thread-safe.
 */
public final class BeanDefinition {

    private final Class<?> beanType;
    private final String beanName;
    private final Constructor<?> injectableConstructor;
    private final List<Class<?>> dependencyTypes;

    private BeanDefinition(Class<?> beanType, String beanName, Constructor<?> injectableConstructor) {
        this.beanType = beanType;
        this.beanName = beanName;
        this.injectableConstructor = injectableConstructor;
        this.dependencyTypes = List.of(injectableConstructor.getParameterTypes());
    }

    /**
     * Builds a definition for a scanned component, deriving its name from the
     * {@link Omnissiah} annotation.
     *
     * @param beanType a class annotated with {@link Omnissiah}
     * @return a validated definition
     * @throws BeanDefinitionException if the type is not annotated with
     *         {@link Omnissiah} or is not a valid bean
     */
    public static BeanDefinition fromComponent(Class<?> beanType) {
        Objects.requireNonNull(beanType, "beanType must not be null");
        Omnissiah annotation = Stereotypes.findOmnissiah(beanType);
        if (annotation == null) {
            throw new BeanDefinitionException(
                    beanType.getName() + " is not a component (no @Omnissiah, directly or via a stereotype)");
        }
        String explicitName = annotation.value();
        String beanName = explicitName.isBlank() ? defaultName(beanType) : explicitName;
        return of(beanType, beanName);
    }

    /**
     * Builds a definition for a type with an explicit bean name.
     *
     * @param beanType the bean type; must be a concrete, instantiable class
     * @param beanName the bean name; must not be blank
     * @return a validated definition
     * @throws BeanDefinitionException if the type is not instantiable or its
     *         injectable constructor cannot be determined
     */
    public static BeanDefinition of(Class<?> beanType, String beanName) {
        Objects.requireNonNull(beanType, "beanType must not be null");
        if (beanName == null || beanName.isBlank()) {
            throw new BeanDefinitionException("Bean name must not be blank for " + beanType.getName());
        }
        requireInstantiable(beanType);
        Constructor<?> constructor = resolveInjectableConstructor(beanType);
        return new BeanDefinition(beanType, beanName, constructor);
    }

    private static void requireInstantiable(Class<?> type) {
        if (type.isInterface()) {
            throw new BeanDefinitionException(type.getName() + " is an interface and cannot be instantiated");
        }
        if (Modifier.isAbstract(type.getModifiers())) {
            throw new BeanDefinitionException(type.getName() + " is abstract and cannot be instantiated");
        }
        if (type.isEnum() || type.isAnnotation() || type.isArray() || type.isPrimitive()) {
            throw new BeanDefinitionException(type.getName() + " is not a concrete class");
        }
        if (type.isMemberClass() && !Modifier.isStatic(type.getModifiers())) {
            throw new BeanDefinitionException(
                    type.getName() + " is a non-static inner class and cannot be instantiated by the container");
        }
    }

    /**
     * Chooses the constructor to inject through.
     *
     * <ul>
     *   <li>Exactly one constructor: use it.</li>
     *   <li>Several constructors: exactly one must carry {@link Inject}.</li>
     * </ul>
     */
    private static Constructor<?> resolveInjectableConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 1) {
            return constructors[0];
        }
        List<Constructor<?>> annotated = Arrays.stream(constructors)
                .filter(c -> c.isAnnotationPresent(Inject.class))
                .toList();
        if (annotated.size() == 1) {
            return annotated.get(0);
        }
        if (annotated.isEmpty()) {
            throw new BeanDefinitionException(type.getName()
                    + " declares multiple constructors; annotate exactly one with @Inject");
        }
        throw new BeanDefinitionException(type.getName()
                + " declares multiple @Inject constructors; only one is allowed");
    }

    /**
     * Derives the default bean name by decapitalizing the simple class name,
     * preserving the case of acronyms (e.g. {@code URLParser} stays
     * {@code URLParser}). Mirrors the JavaBeans decapitalization rule without
     * pulling in the {@code java.desktop} module.
     */
    private static String defaultName(Class<?> type) {
        String simpleName = type.getSimpleName();
        if (simpleName.length() > 1
                && Character.isUpperCase(simpleName.charAt(0))
                && Character.isUpperCase(simpleName.charAt(1))) {
            return simpleName;
        }
        char[] chars = simpleName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    public Class<?> beanType() {
        return beanType;
    }

    public String beanName() {
        return beanName;
    }

    public Constructor<?> injectableConstructor() {
        return injectableConstructor;
    }

    /** @return the constructor parameter types, in order; never {@code null}, immutable */
    public List<Class<?>> dependencyTypes() {
        return dependencyTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof BeanDefinition other
                && beanName.equals(other.beanName)
                && beanType.equals(other.beanType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(beanType, beanName);
    }

    @Override
    public String toString() {
        return "BeanDefinition{name='" + beanName + "', type=" + beanType.getName()
                + ", dependencies=" + dependencyTypes.size() + "}";
    }
}
