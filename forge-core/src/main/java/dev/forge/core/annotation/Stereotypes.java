package dev.forge.core.annotation;

/**
 * Resolves component stereotypes: whether a type is a Forge component, and which
 * {@link Omnissiah} annotation governs it.
 *
 * <p>A type is a component if it is annotated with {@link Omnissiah} <em>directly</em>
 * or with a <em>stereotype</em> — an annotation that is itself meta-annotated with
 * {@link Omnissiah}. This is how {@link Agent} works: it carries {@code @Omnissiah}
 * as a meta-annotation, so an {@code @Agent} class is discovered and managed like
 * any other component.
 *
 * <p>The search is one level deep (direct, then meta-annotations on the type's own
 * annotations), which is sufficient for the current stereotypes.
 */
public final class Stereotypes {

    private Stereotypes() {}

    /** @return {@code true} if {@code type} is a component (directly or via a stereotype) */
    public static boolean isComponent(Class<?> type) {
        return findOmnissiah(type) != null;
    }

    /**
     * Finds the {@link Omnissiah} governing {@code type}, whether declared directly
     * or contributed by a stereotype meta-annotation.
     *
     * @return the {@link Omnissiah} instance, or {@code null} if {@code type} is not a component
     */
    public static Omnissiah findOmnissiah(Class<?> type) {
        Omnissiah direct = type.getAnnotation(Omnissiah.class);
        if (direct != null) {
            return direct;
        }
        for (java.lang.annotation.Annotation annotation : type.getAnnotations()) {
            Omnissiah meta = annotation.annotationType().getAnnotation(Omnissiah.class);
            if (meta != null) {
                return meta;
            }
        }
        return null;
    }
}
