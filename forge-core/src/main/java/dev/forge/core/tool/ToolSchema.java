package dev.forge.core.tool;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable, machine-readable description of a tool: its name, description,
 * and ordered parameters. This is what an LLM is shown so it can choose and call
 * the tool.
 *
 * <p>Provider-neutral: {@link #toInputSchema()} and {@link #toMap()} produce plain
 * maps shaped like JSON schema / a tool definition, which a provider adapter
 * serializes. In the current milestone every parameter is required.
 */
public final class ToolSchema {

    private final String name;
    private final String description;
    private final List<ToolParameter> parameters;

    public ToolSchema(String name, String description, List<ToolParameter> parameters) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description == null ? "" : description;
        this.parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    /** @return the parameters in declared order; immutable, never null */
    public List<ToolParameter> parameters() {
        return parameters;
    }

    /**
     * @return a JSON-schema object describing the parameters:
     *         {@code {"type":"object","properties":{...},"required":[...]}}
     */
    public Map<String, Object> toInputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = parameters.stream().map(ToolParameter::name).toList();
        for (ToolParameter parameter : parameters) {
            properties.put(parameter.name(), Map.of("type", parameter.jsonType()));
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    /**
     * @return the full tool definition:
     *         {@code {"name":..., "description":..., "input_schema": {...}}}
     */
    public Map<String, Object> toMap() {
        Map<String, Object> definition = new LinkedHashMap<>();
        definition.put("name", name);
        definition.put("description", description);
        definition.put("input_schema", toInputSchema());
        return definition;
    }

    @Override
    public String toString() {
        return "ToolSchema{name='" + name + "', parameters=" + parameters.size() + "}";
    }
}
