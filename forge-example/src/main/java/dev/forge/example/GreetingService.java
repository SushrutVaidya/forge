package dev.forge.example;

import dev.forge.core.annotation.Omnissiah;
import dev.forge.core.annotation.PostConstruct;
import dev.forge.core.annotation.PreDestroy;

/** A simple managed component with lifecycle callbacks. */
@Omnissiah
public class GreetingService {

    @PostConstruct
    void ready() {
        System.out.println("[lifecycle] GreetingService initialized (@PostConstruct)");
    }

    @PreDestroy
    void shutdown() {
        System.out.println("[lifecycle] GreetingService closing (@PreDestroy)");
    }

    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
