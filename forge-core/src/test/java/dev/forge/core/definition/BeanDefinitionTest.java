package dev.forge.core.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.forge.core.definition.fixtures.Fixtures;
import dev.forge.core.exception.BeanDefinitionException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BeanDefinitionTest {

    @Nested
    @DisplayName("bean naming")
    class Naming {

        @Test
        @DisplayName("derives a decapitalized name from the simple class name")
        void derivesDefaultName() {
            assertEquals("simpleService", BeanDefinition.fromComponent(Fixtures.SimpleService.class).beanName());
        }

        @Test
        @DisplayName("uses the explicit @Omnissiah value when present")
        void usesExplicitName() {
            assertEquals("custom", BeanDefinition.fromComponent(Fixtures.NamedService.class).beanName());
        }

        @Test
        @DisplayName("preserves acronym prefixes when decapitalizing")
        void preservesAcronyms() {
            assertEquals("URLParser", BeanDefinition.fromComponent(Fixtures.URLParser.class).beanName());
        }
    }

    @Nested
    @DisplayName("dependency resolution")
    class Dependencies {

        @Test
        @DisplayName("a no-arg component has no dependencies")
        void noDependencies() {
            assertEquals(List.of(), BeanDefinition.fromComponent(Fixtures.SimpleService.class).dependencyTypes());
        }

        @Test
        @DisplayName("captures constructor parameter types in order")
        void capturesDependencies() {
            BeanDefinition definition = BeanDefinition.fromComponent(Fixtures.DependentService.class);
            assertEquals(List.of(Fixtures.SimpleService.class), definition.dependencyTypes());
        }

        @Test
        @DisplayName("selects the @Inject constructor when several exist")
        void selectsInjectConstructor() {
            BeanDefinition definition = BeanDefinition.fromComponent(Fixtures.MultiConstructor.class);
            assertEquals(1, definition.injectableConstructor().getParameterCount());
            assertEquals(List.of(Fixtures.SimpleService.class), definition.dependencyTypes());
        }

        @Test
        @DisplayName("the dependency list is immutable")
        void dependenciesImmutable() {
            List<Class<?>> deps = BeanDefinition.fromComponent(Fixtures.DependentService.class).dependencyTypes();
            assertThrows(UnsupportedOperationException.class, () -> deps.add(String.class));
        }
    }

    @Nested
    @DisplayName("validation")
    class Validation {

        @Test
        @DisplayName("rejects a class not annotated with @Omnissiah")
        void rejectsNonComponent() {
            assertThrows(BeanDefinitionException.class,
                    () -> BeanDefinition.fromComponent(Fixtures.PlainClass.class));
        }

        @Test
        @DisplayName("rejects an interface")
        void rejectsInterface() {
            assertThrows(BeanDefinitionException.class,
                    () -> BeanDefinition.of(Fixtures.SomeInterface.class, "x"));
        }

        @Test
        @DisplayName("rejects an abstract class")
        void rejectsAbstract() {
            assertThrows(BeanDefinitionException.class,
                    () -> BeanDefinition.of(Fixtures.AbstractService.class, "x"));
        }

        @Test
        @DisplayName("rejects a blank bean name")
        void rejectsBlankName() {
            assertThrows(BeanDefinitionException.class,
                    () -> BeanDefinition.of(Fixtures.SimpleService.class, "  "));
        }

        @Test
        @DisplayName("rejects multiple constructors with no @Inject")
        void rejectsAmbiguousConstructors() {
            assertThrows(BeanDefinitionException.class,
                    () -> BeanDefinition.fromComponent(Fixtures.AmbiguousConstructors.class));
        }

        @Test
        @DisplayName("rejects multiple @Inject constructors")
        void rejectsDoubleInject() {
            assertThrows(BeanDefinitionException.class,
                    () -> BeanDefinition.fromComponent(Fixtures.DoubleInject.class));
        }
    }

    @Nested
    @DisplayName("identity")
    class Identity {

        @Test
        @DisplayName("equal when name and type match")
        void equality() {
            BeanDefinition a = BeanDefinition.fromComponent(Fixtures.SimpleService.class);
            BeanDefinition b = BeanDefinition.fromComponent(Fixtures.SimpleService.class);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }
}
