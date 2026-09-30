package dev.forge.core.resolver.fixtures;

import dev.forge.core.annotation.Omnissiah;

/** Dependency-graph fixtures for the resolver tests. */
public final class Graph {

    private Graph() {}

    // ── simple + one-level dependency ────────────────────────────────────────
    @Omnissiah
    public static class Engine {}

    @Omnissiah
    public static class Car {
        public final Engine engine;

        public Car(Engine engine) {
            this.engine = engine;
        }
    }

    /** A second bean that also depends on Engine, to prove the singleton is shared. */
    @Omnissiah
    public static class Garage {
        public final Engine engine;

        public Garage(Engine engine) {
            this.engine = engine;
        }
    }

    // ── transitive chain A -> B -> C ─────────────────────────────────────────
    @Omnissiah
    public static class C {}

    @Omnissiah
    public static class B {
        public final C c;

        public B(C c) {
            this.c = c;
        }
    }

    @Omnissiah
    public static class A {
        public final B b;

        public A(B b) {
            this.b = b;
        }
    }

    // ── cycles ───────────────────────────────────────────────────────────────
    @Omnissiah
    public static class SelfLoop {
        public SelfLoop(SelfLoop self) {}
    }

    @Omnissiah
    public static class X {
        public X(Y y) {}
    }

    @Omnissiah
    public static class Y {
        public Y(X x) {}
    }

    // ── failure modes ────────────────────────────────────────────────────────
    /** Depends on a type that has no bean definition. */
    @Omnissiah
    public static class NeedsUnregistered {
        public NeedsUnregistered(String notABean) {}
    }

    /** Its constructor always throws, to exercise BeanInstantiationException. */
    @Omnissiah
    public static class Explosive {
        public Explosive() {
            throw new IllegalStateException("boom");
        }
    }
}
