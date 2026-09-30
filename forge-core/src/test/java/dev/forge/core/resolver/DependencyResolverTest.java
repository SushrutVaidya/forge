package dev.forge.core.resolver;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.forge.core.definition.BeanDefinition;
import dev.forge.core.exception.BeanInstantiationException;
import dev.forge.core.exception.CircularDependencyException;
import dev.forge.core.exception.NoSuchBeanException;
import dev.forge.core.registry.BeanRegistry;
import dev.forge.core.resolver.fixtures.Graph;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DependencyResolverTest {

    private static BeanRegistry registryWith(Class<?>... components) {
        BeanRegistry registry = new BeanRegistry();
        for (Class<?> component : components) {
            registry.registerDefinition(BeanDefinition.fromComponent(component));
        }
        return registry;
    }

    private static DependencyResolver resolverWith(Class<?>... components) {
        return new DependencyResolver(registryWith(components));
    }

    @Nested
    @DisplayName("wiring")
    class Wiring {

        @Test
        @DisplayName("resolves a bean with no dependencies")
        void simpleBean() {
            Object engine = resolverWith(Graph.Engine.class).resolve("engine");
            assertInstanceOf(Graph.Engine.class, engine);
        }

        @Test
        @DisplayName("injects a constructor dependency")
        void oneDependency() {
            DependencyResolver resolver = resolverWith(Graph.Engine.class, Graph.Car.class);
            Graph.Car car = (Graph.Car) resolver.resolve("car");
            assertNotNull(car.engine);
        }

        @Test
        @DisplayName("resolves a transitive chain A -> B -> C")
        void transitiveChain() {
            DependencyResolver resolver = resolverWith(Graph.A.class, Graph.B.class, Graph.C.class);
            Graph.A a = (Graph.A) resolver.resolve("a");
            assertNotNull(a.b);
            assertNotNull(a.b.c);
        }
    }

    @Nested
    @DisplayName("singleton semantics")
    class Singletons {

        @Test
        @DisplayName("returns the same instance on repeated resolution")
        void idempotent() {
            DependencyResolver resolver = resolverWith(Graph.Engine.class);
            assertSame(resolver.resolve("engine"), resolver.resolve("engine"));
        }

        @Test
        @DisplayName("shares one dependency instance across dependents")
        void sharedDependency() {
            DependencyResolver resolver =
                    resolverWith(Graph.Engine.class, Graph.Car.class, Graph.Garage.class);
            Graph.Car car = (Graph.Car) resolver.resolve("car");
            Graph.Garage garage = (Graph.Garage) resolver.resolve("garage");
            assertSame(car.engine, garage.engine);
        }
    }

    @Nested
    @DisplayName("failure modes")
    class Failures {

        @Test
        @DisplayName("direct self-cycle is detected")
        void selfCycle() {
            DependencyResolver resolver = resolverWith(Graph.SelfLoop.class);
            assertThrows(CircularDependencyException.class, () -> resolver.resolve("selfLoop"));
        }

        @Test
        @DisplayName("indirect cycle is detected and names the full path")
        void indirectCycle() {
            DependencyResolver resolver = resolverWith(Graph.X.class, Graph.Y.class);
            CircularDependencyException ex =
                    assertThrows(CircularDependencyException.class, () -> resolver.resolve("x"));
            assertTrue(ex.getMessage().contains("x -> y -> x"), ex.getMessage());
        }

        @Test
        @DisplayName("missing dependency fails fast")
        void missingDependency() {
            DependencyResolver resolver = resolverWith(Graph.NeedsUnregistered.class);
            assertThrows(NoSuchBeanException.class, () -> resolver.resolve("needsUnregistered"));
        }

        @Test
        @DisplayName("unknown bean throws NoSuchBeanException")
        void unknownBean() {
            assertThrows(NoSuchBeanException.class, () -> resolverWith().resolve("ghost"));
        }

        @Test
        @DisplayName("a throwing constructor is wrapped, preserving the cause")
        void throwingConstructor() {
            DependencyResolver resolver = resolverWith(Graph.Explosive.class);
            BeanInstantiationException ex =
                    assertThrows(BeanInstantiationException.class, () -> resolver.resolve("explosive"));
            assertInstanceOf(IllegalStateException.class, ex.getCause());
        }

        @Test
        @DisplayName("null registry is rejected")
        void nullRegistry() {
            assertThrows(NullPointerException.class, () -> new DependencyResolver(null));
        }
    }

    @Nested
    @DisplayName("concurrency")
    class Concurrency {

        @Test
        @DisplayName("concurrent resolution constructs the singleton exactly once")
        void constructedOnce() throws Exception {
            DependencyResolver resolver = resolverWith(Graph.Engine.class);
            int threadCount = 16;
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            try {
                CountDownLatch startGate = new CountDownLatch(1);
                List<Future<Object>> futures = new ArrayList<>();
                for (int i = 0; i < threadCount; i++) {
                    futures.add(pool.submit(() -> {
                        startGate.await();
                        return resolver.resolve("engine");
                    }));
                }
                startGate.countDown();
                Object first = futures.get(0).get();
                for (Future<Object> future : futures) {
                    assertSame(first, future.get(), "all threads must observe the same singleton");
                }
            } finally {
                pool.shutdownNow();
            }
        }
    }
}
