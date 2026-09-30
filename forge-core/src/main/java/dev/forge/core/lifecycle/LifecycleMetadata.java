package dev.forge.core.lifecycle;

import dev.forge.core.annotation.PostConstruct;
import dev.forge.core.annotation.PreDestroy;
import dev.forge.core.exception.LifecycleException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * The validated lifecycle callbacks of a single bean type: an optional
 * {@code @PostConstruct} method and an optional {@code @PreDestroy} method.
 *
 * <p>Resolved once per type via {@link #forType(Class)}, which validates that at
 * most one method carries each annotation and that each takes no parameters.
 *
 * <p>Callback discovery inspects the methods declared on the bean class itself.
 * Inherited callbacks are out of scope for the current milestone.
 */
public final class LifecycleMetadata {

    private static final LifecycleMetadata NONE = new LifecycleMetadata(null, null);

    private final Method postConstruct;
    private final Method preDestroy;

    private LifecycleMetadata(Method postConstruct, Method preDestroy) {
        this.postConstruct = postConstruct;
        this.preDestroy = preDestroy;
    }

    /**
     * Resolves and validates the lifecycle callbacks declared on {@code type}.
     *
     * @throws LifecycleException if a callback annotation is used more than once
     *         on the type, or a callback method declares parameters
     */
    public static LifecycleMetadata forType(Class<?> type) {
        Method postConstruct = findUniqueCallback(type, PostConstruct.class);
        Method preDestroy = findUniqueCallback(type, PreDestroy.class);
        if (postConstruct == null && preDestroy == null) {
            return NONE;
        }
        return new LifecycleMetadata(postConstruct, preDestroy);
    }

    private static Method findUniqueCallback(Class<?> type, Class<? extends Annotation> annotation) {
        List<Method> matches = new ArrayList<>();
        for (Method method : type.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotation)) {
                matches.add(method);
            }
        }
        if (matches.isEmpty()) {
            return null;
        }
        if (matches.size() > 1) {
            throw new LifecycleException(type.getName() + " declares multiple @"
                    + annotation.getSimpleName() + " methods; at most one is allowed");
        }
        Method callback = matches.get(0);
        if (callback.getParameterCount() != 0) {
            throw new LifecycleException("@" + annotation.getSimpleName() + " method "
                    + type.getName() + "#" + callback.getName() + " must take no parameters");
        }
        return callback;
    }

    public void invokePostConstruct(Object bean) {
        invoke(postConstruct, bean, "@PostConstruct");
    }

    public void invokePreDestroy(Object bean) {
        invoke(preDestroy, bean, "@PreDestroy");
    }

    private static void invoke(Method method, Object bean, String phase) {
        if (method == null) {
            return;
        }
        try {
            method.setAccessible(true);
            method.invoke(bean);
        } catch (InvocationTargetException e) {
            throw new LifecycleException(
                    phase + " callback " + describe(method) + " threw an exception", e.getTargetException());
        } catch (ReflectiveOperationException e) {
            throw new LifecycleException(phase + " callback " + describe(method) + " could not be invoked", e);
        }
    }

    private static String describe(Method method) {
        return method.getDeclaringClass().getName() + "#" + method.getName();
    }
}
