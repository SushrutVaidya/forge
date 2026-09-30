package dev.forge.core.tool.app;

import dev.forge.core.annotation.Omnissiah;
import dev.forge.core.annotation.Tool;

/** A managed component that exposes a tool, for the fromContext test. */
@Omnissiah
public class Greeter {

    @Tool(description = "greets someone by name")
    public String greet(String name) {
        return "hello " + name;
    }
}
