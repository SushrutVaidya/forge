package dev.forge.core.tool;

/**
 * Maps Java parameter types to JSON-schema type names.
 *
 * <p>Deliberately small: it covers the scalar types that map cleanly, and falls
 * back to {@code "object"} for anything structured. Rich mapping (collections,
 * enums, nested objects) is out of scope for the current milestone.
 */
final class JsonTypes {

    private JsonTypes() {}

    static String forJavaType(Class<?> type) {
        if (type == String.class || type == char.class || type == Character.class) {
            return "string";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "boolean";
        }
        if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class
                || type == short.class || type == Short.class
                || type == byte.class || type == Byte.class) {
            return "integer";
        }
        if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) {
            return "number";
        }
        return "object";
    }
}
