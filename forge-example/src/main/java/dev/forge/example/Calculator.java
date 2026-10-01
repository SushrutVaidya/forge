package dev.forge.example;

import dev.forge.core.annotation.Omnissiah;
import dev.forge.core.annotation.Tool;

/** A component that exposes a tool an LLM could call. */
@Omnissiah
public class Calculator {

    @Tool(description = "adds two integers and returns the sum")
    public int add(int a, int b) {
        return a + b;
    }
}
