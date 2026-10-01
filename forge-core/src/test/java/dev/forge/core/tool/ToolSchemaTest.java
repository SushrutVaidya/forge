package dev.forge.core.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.forge.core.tool.fixtures.ToolFixtures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ToolSchemaTest {

    private ToolMetadata tool(String name) {
        return ToolRegistry.fromBeans(List.of(new ToolFixtures.Typed())).getTool(name);
    }

    @Test
    @DisplayName("captures real parameter names (compiled with -parameters)")
    void realParameterNames() {
        List<String> names = tool("act").schema().parameters().stream().map(ToolParameter::name).toList();
        assertEquals(List.of("text", "count", "flag", "ratio"), names);
    }

    @Test
    @DisplayName("maps Java types to JSON-schema types")
    void typeMapping() {
        Map<String, String> byName = new java.util.HashMap<>();
        for (ToolParameter p : tool("act").schema().parameters()) {
            byName.put(p.name(), p.jsonType());
        }
        assertEquals("string", byName.get("text"));
        assertEquals("integer", byName.get("count"));
        assertEquals("boolean", byName.get("flag"));
        assertEquals("number", byName.get("ratio"));
    }

    @Test
    @DisplayName("carries the tool name and description")
    void nameAndDescription() {
        ToolSchema schema = tool("act").schema();
        assertEquals("act", schema.name());
        assertEquals("exercises the type mapping", schema.description());
    }

    @Test
    @DisplayName("toInputSchema yields an object schema with properties and required")
    void inputSchema() {
        Map<String, Object> schema = tool("act").schema().toInputSchema();
        assertEquals("object", schema.get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        assertTrue(properties.containsKey("text"));
        assertEquals(Map.of("type", "integer"), properties.get("count"));
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.get("required");
        assertEquals(List.of("text", "count", "flag", "ratio"), required);
    }

    @Test
    @DisplayName("toMap yields a full tool definition")
    void fullDefinition() {
        Map<String, Object> definition = tool("act").schema().toMap();
        assertEquals("act", definition.get("name"));
        assertEquals("exercises the type mapping", definition.get("description"));
        assertTrue(definition.get("input_schema") instanceof Map);
    }

    @Test
    @DisplayName("a no-arg tool has an empty parameter list and empty required")
    void noArgs() {
        ToolSchema schema = tool("noArgs").schema();
        assertEquals(List.of(), schema.parameters());
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) schema.toInputSchema().get("required");
        assertEquals(List.of(), required);
    }

    @Test
    @DisplayName("registry exposes a schema per tool")
    void registrySchemas() {
        List<ToolSchema> schemas = ToolRegistry.fromBeans(List.of(new ToolFixtures.Typed())).schemas();
        assertEquals(2, schemas.size());
    }

    @Test
    @DisplayName("a null description becomes empty")
    void nullDescription() {
        ToolSchema schema = new ToolSchema("t", null, List.of());
        assertEquals("", schema.description());
        assertTrue(schema.toString().contains("t"));
    }
}
