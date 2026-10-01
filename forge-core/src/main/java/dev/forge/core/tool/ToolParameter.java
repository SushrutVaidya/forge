package dev.forge.core.tool;

import java.util.Objects;

/**
 * An immutable description of one tool parameter: its name and its JSON-schema
 * type (e.g. {@code "string"}, {@code "integer"}).
 */
public final class ToolParameter {

    private final String name;
    private final String jsonType;

    public ToolParameter(String name, String jsonType) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.jsonType = Objects.requireNonNull(jsonType, "jsonType must not be null");
    }

    public String name() {
        return name;
    }

    public String jsonType() {
        return jsonType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof ToolParameter other
                && name.equals(other.name)
                && jsonType.equals(other.jsonType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, jsonType);
    }

    @Override
    public String toString() {
        return "ToolParameter{name='" + name + "', jsonType='" + jsonType + "'}";
    }
}
