package dev.forge.core.context.app;

import dev.forge.core.annotation.Omnissiah;
import dev.forge.core.annotation.PostConstruct;
import dev.forge.core.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * An example application graph for the context integration tests:
 * {@code Controller -> Service -> Repository}. Each bean records its lifecycle
 * events so ordering can be asserted.
 */
public final class App {

    private App() {}

    /** Shared, ordered record of lifecycle events. Cleared between tests. */
    public static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    @Omnissiah
    public static class Repository {
        @PostConstruct
        void init() {
            EVENTS.add("Repository.init");
        }

        @PreDestroy
        void destroy() {
            EVENTS.add("Repository.destroy");
        }
    }

    @Omnissiah
    public static class Service {
        public final Repository repository;

        public Service(Repository repository) {
            this.repository = repository;
        }

        @PostConstruct
        void init() {
            EVENTS.add("Service.init");
        }

        @PreDestroy
        void destroy() {
            EVENTS.add("Service.destroy");
        }
    }

    @Omnissiah
    public static class Controller {
        public final Service service;

        public Controller(Service service) {
            this.service = service;
        }

        @PostConstruct
        void init() {
            EVENTS.add("Controller.init");
        }

        @PreDestroy
        void destroy() {
            EVENTS.add("Controller.destroy");
        }
    }
}
