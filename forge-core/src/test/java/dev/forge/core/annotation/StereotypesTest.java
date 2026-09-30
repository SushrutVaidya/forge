package dev.forge.core.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StereotypesTest {

    @Omnissiah
    static class DirectComponent {}

    @Omnissiah("named")
    static class NamedComponent {}

    @Agent
    static class AgentComponent {}

    static class PlainType {}

    @Test
    @DisplayName("a directly-annotated class is a component")
    void directIsComponent() {
        assertTrue(Stereotypes.isComponent(DirectComponent.class));
    }

    @Test
    @DisplayName("an @Agent class is a component via the stereotype meta-annotation")
    void agentIsComponent() {
        assertTrue(Stereotypes.isComponent(AgentComponent.class));
    }

    @Test
    @DisplayName("a plain class is not a component")
    void plainIsNotComponent() {
        assertFalse(Stereotypes.isComponent(PlainType.class));
        assertNull(Stereotypes.findOmnissiah(PlainType.class));
    }

    @Test
    @DisplayName("findOmnissiah returns the direct annotation, preserving its value")
    void findsDirect() {
        assertEquals("named", Stereotypes.findOmnissiah(NamedComponent.class).value());
    }

    @Test
    @DisplayName("findOmnissiah resolves the meta-annotation on a stereotype")
    void findsViaStereotype() {
        assertNotNull(Stereotypes.findOmnissiah(AgentComponent.class));
    }
}
