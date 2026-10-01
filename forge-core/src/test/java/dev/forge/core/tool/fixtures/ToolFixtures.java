package dev.forge.core.tool.fixtures;

import dev.forge.core.annotation.Tool;

/** Tool-bearing fixtures for the registry tests. */
public final class ToolFixtures {

    private ToolFixtures() {}

    public static class Calculator {
        @Tool(description = "adds two integers")
        int add(int a, int b) {
            return a + b;
        }

        @Tool(name = "sub")
        int subtract(int a, int b) {
            return a - b;
        }

        @Tool
        int boom() {
            throw new IllegalStateException("kaboom");
        }

        int notATool() {
            return 0;
        }
    }

    public static class DuplicateTools {
        @Tool(name = "dup")
        void a() {}

        @Tool(name = "dup")
        void b() {}
    }

    public static class Typed {
        @Tool(description = "exercises the type mapping")
        void act(String text, int count, boolean flag, double ratio) {}

        @Tool
        void noArgs() {}
    }
}
