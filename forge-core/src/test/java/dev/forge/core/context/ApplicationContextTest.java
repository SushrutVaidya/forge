package dev.forge.core.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.forge.core.context.app.App;
import dev.forge.core.exception.NoSuchBeanException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApplicationContextTest {

    private static final String APP_PACKAGE = "dev.forge.core.context.app";

    @BeforeEach
    void resetEventLog() {
        App.EVENTS.clear();
    }

    @Nested
    @DisplayName("wiring and lookup")
    class Wiring {

        @Test
        @DisplayName("scans, wires the full graph, and exposes beans by type")
        void wiresGraph() {
            try (ApplicationContext context = ApplicationContext.run(APP_PACKAGE)) {
                App.Controller controller = context.getBean(App.Controller.class);
                assertNotNull(controller.service);
                assertNotNull(controller.service.repository);
            }
        }

        @Test
        @DisplayName("exposes beans by name")
        void getBeanByName() {
            try (ApplicationContext context = ApplicationContext.run(APP_PACKAGE)) {
                assertInstanceOf(App.Repository.class, context.getBean("repository"));
            }
        }

        @Test
        @DisplayName("shares singleton instances across the graph")
        void sharedSingletons() {
            try (ApplicationContext context = ApplicationContext.run(APP_PACKAGE)) {
                App.Controller controller = context.getBean(App.Controller.class);
                App.Service service = context.getBean(App.Service.class);
                assertSame(service, controller.service);
                assertSame(context.getBean(App.Repository.class), service.repository);
            }
        }

        @Test
        @DisplayName("reports all registered bean names")
        void beanNames() {
            try (ApplicationContext context = ApplicationContext.run(APP_PACKAGE)) {
                assertTrue(context.getBeanDefinitionNames()
                        .containsAll(List.of("repository", "service", "controller")));
            }
        }

        @Test
        @DisplayName("unknown bean throws NoSuchBeanException")
        void unknownBean() {
            try (ApplicationContext context = ApplicationContext.run(APP_PACKAGE)) {
                assertThrows(NoSuchBeanException.class, () -> context.getBean("ghost"));
            }
        }
    }

    @Nested
    @DisplayName("lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("@PostConstruct runs in dependency-first order")
        void postConstructOrder() {
            try (ApplicationContext context = ApplicationContext.run(APP_PACKAGE)) {
                assertEquals(
                        List.of("Repository.init", "Service.init", "Controller.init"),
                        List.copyOf(App.EVENTS));
            }
        }

        @Test
        @DisplayName("@PreDestroy runs in reverse creation order on close")
        void preDestroyOrder() {
            ApplicationContext context = ApplicationContext.run(APP_PACKAGE);
            App.EVENTS.clear();
            context.close();
            assertEquals(
                    List.of("Controller.destroy", "Service.destroy", "Repository.destroy"),
                    List.copyOf(App.EVENTS));
        }

        @Test
        @DisplayName("try-with-resources closes the context and fires @PreDestroy")
        void tryWithResourcesCloses() {
            try (ApplicationContext context = ApplicationContext.run(APP_PACKAGE)) {
                App.EVENTS.clear();
            }
            assertTrue(App.EVENTS.contains("Repository.destroy"));
        }

        @Test
        @DisplayName("close is idempotent")
        void closeIsIdempotent() {
            ApplicationContext context = ApplicationContext.run(APP_PACKAGE);
            context.close();
            App.EVENTS.clear();
            context.close();
            assertEquals(List.of(), List.copyOf(App.EVENTS));
            assertTrue(context.isClosed());
        }

        @Test
        @DisplayName("getBean after close throws")
        void getBeanAfterClose() {
            ApplicationContext context = ApplicationContext.run(APP_PACKAGE);
            context.close();
            assertThrows(IllegalStateException.class, () -> context.getBean(App.Service.class));
        }
    }
}
