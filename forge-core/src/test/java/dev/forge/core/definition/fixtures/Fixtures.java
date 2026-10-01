package dev.forge.core.definition.fixtures;

import dev.forge.core.annotation.Inject;
import dev.forge.core.annotation.Omnissiah;

/** Test fixtures for bean-definition and registry tests. */
public final class Fixtures {

    private Fixtures() {}

    @Omnissiah
    public static class SimpleService {}

    @Omnissiah("custom")
    public static class NamedService {}

    @Omnissiah
    public static class DependentService {
        public final SimpleService dependency;

        public DependentService(SimpleService dependency) {
            this.dependency = dependency;
        }
    }

    /** Acronym-prefixed name: default naming must preserve it. */
    @Omnissiah
    public static class URLParser {}

    @Omnissiah
    public static class MultiConstructor {
        public final SimpleService dependency;

        public MultiConstructor() {
            this.dependency = null;
        }

        @Inject
        public MultiConstructor(SimpleService dependency) {
            this.dependency = dependency;
        }
    }

    @Omnissiah
    public static class AmbiguousConstructors {
        public AmbiguousConstructors() {}

        public AmbiguousConstructors(SimpleService dependency) {}
    }

    @Omnissiah
    public static class DoubleInject {
        @Inject
        public DoubleInject() {}

        @Inject
        public DoubleInject(SimpleService dependency) {}
    }

    /** Not a component. */
    public static class PlainClass {}

    @Omnissiah
    public interface SomeInterface {}

    @Omnissiah
    public abstract static class AbstractService {}

    // For by-type lookup, including ambiguity.
    public interface Repository {}

    @Omnissiah
    public static class UserRepository implements Repository {}

    @Omnissiah
    public static class OrderRepository implements Repository {}

    // Non-instantiable kinds, for requireInstantiable coverage.
    public enum Color { RED, GREEN }

    public @interface Marker {}

    /** A non-static inner class — the container cannot instantiate it. */
    public class NonStaticInner {}
}
