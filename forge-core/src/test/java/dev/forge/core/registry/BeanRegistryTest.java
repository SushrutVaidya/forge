package dev.forge.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.forge.core.definition.BeanDefinition;
import dev.forge.core.definition.fixtures.Fixtures;
import dev.forge.core.exception.BeanDefinitionException;
import dev.forge.core.exception.NoSuchBeanException;
import dev.forge.core.exception.NoUniqueBeanException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BeanRegistryTest {

    private BeanRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new BeanRegistry();
    }

    private BeanDefinition definitionOf(Class<?> type) {
        return BeanDefinition.fromComponent(type);
    }

    @Nested
    @DisplayName("definitions")
    class Definitions {

        @Test
        @DisplayName("registers and retrieves by name")
        void registerAndGet() {
            BeanDefinition definition = definitionOf(Fixtures.SimpleService.class);
            registry.registerDefinition(definition);
            assertEquals(definition, registry.getDefinition("simpleService"));
            assertTrue(registry.containsDefinition("simpleService"));
        }

        @Test
        @DisplayName("rejects a duplicate bean name")
        void rejectsDuplicateName() {
            registry.registerDefinition(definitionOf(Fixtures.SimpleService.class));
            BeanDefinition clash = BeanDefinition.of(Fixtures.NamedService.class, "simpleService");
            assertThrows(BeanDefinitionException.class, () -> registry.registerDefinition(clash));
        }

        @Test
        @DisplayName("unknown name throws NoSuchBeanException")
        void unknownName() {
            assertThrows(NoSuchBeanException.class, () -> registry.getDefinition("missing"));
        }

        @Test
        @DisplayName("getAllDefinitions is an immutable snapshot")
        void allDefinitionsImmutable() {
            registry.registerDefinition(definitionOf(Fixtures.SimpleService.class));
            var all = registry.getAllDefinitions();
            assertEquals(1, all.size());
            assertThrows(UnsupportedOperationException.class, () -> all.add(null));
        }

        @Test
        @DisplayName("exposes registered names")
        void names() {
            registry.registerDefinition(definitionOf(Fixtures.SimpleService.class));
            assertTrue(registry.getBeanDefinitionNames().contains("simpleService"));
        }
    }

    @Nested
    @DisplayName("lookup by type")
    class ByType {

        @Test
        @DisplayName("resolves a unique type match, including via an interface")
        void uniqueMatch() {
            registry.registerDefinition(definitionOf(Fixtures.UserRepository.class));
            assertEquals("userRepository",
                    registry.getDefinitionByType(Fixtures.Repository.class).beanName());
            assertEquals("userRepository",
                    registry.getDefinitionByType(Fixtures.UserRepository.class).beanName());
        }

        @Test
        @DisplayName("no match throws NoSuchBeanException")
        void noMatch() {
            assertThrows(NoSuchBeanException.class,
                    () -> registry.getDefinitionByType(Fixtures.Repository.class));
        }

        @Test
        @DisplayName("ambiguous match throws NoUniqueBeanException")
        void ambiguousMatch() {
            registry.registerDefinition(definitionOf(Fixtures.UserRepository.class));
            registry.registerDefinition(definitionOf(Fixtures.OrderRepository.class));
            assertThrows(NoUniqueBeanException.class,
                    () -> registry.getDefinitionByType(Fixtures.Repository.class));
        }
    }

    @Nested
    @DisplayName("singleton cache")
    class Singletons {

        @Test
        @DisplayName("registers, retrieves and reports containment")
        void registerAndGet() {
            Object instance = new Object();
            registry.registerSingleton("bean", instance);
            assertSame(instance, registry.getSingleton("bean"));
            assertTrue(registry.containsSingleton("bean"));
        }

        @Test
        @DisplayName("returns null on a cache miss")
        void missReturnsNull() {
            assertNull(registry.getSingleton("absent"));
            assertFalse(registry.containsSingleton("absent"));
        }

        @Test
        @DisplayName("rejects a duplicate singleton")
        void rejectsDuplicate() {
            registry.registerSingleton("bean", new Object());
            assertThrows(BeanDefinitionException.class,
                    () -> registry.registerSingleton("bean", new Object()));
        }

        @Test
        @DisplayName("clear removes singletons but keeps definitions")
        void clear() {
            registry.registerDefinition(definitionOf(Fixtures.SimpleService.class));
            registry.registerSingleton("bean", new Object());
            registry.clearSingletons();
            assertFalse(registry.containsSingleton("bean"));
            assertTrue(registry.containsDefinition("simpleService"));
        }
    }
}
