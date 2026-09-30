package dev.forge.core.agent;

import dev.forge.core.annotation.Agent;
import dev.forge.core.context.ApplicationContext;
import dev.forge.core.exception.NoSuchBeanException;
import dev.forge.core.exception.NoUniqueBeanException;
import java.util.List;
import java.util.Objects;

/**
 * Locates the {@link Agent}-annotated beans in an {@link ApplicationContext}.
 *
 * <p>Agents are ordinary managed beans (an {@code @Agent} class is a component,
 * fully dependency-injected); this registry simply picks out those tagged as
 * agents, so higher layers can enumerate or address them.
 *
 * <p>Immutable once built.
 */
public final class AgentRegistry {

    private final List<Object> agents;

    private AgentRegistry(List<Object> agents) {
        this.agents = agents;
    }

    /** Builds a registry of every {@link Agent} bean in the context. */
    public static AgentRegistry fromContext(ApplicationContext context) {
        Objects.requireNonNull(context, "context must not be null");
        List<Object> found = context.getBeanDefinitionNames().stream()
                .map(context::getBean)
                .filter(bean -> bean.getClass().isAnnotationPresent(Agent.class))
                .toList();
        return new AgentRegistry(found);
    }

    /** @return all agent beans; immutable, never null */
    public List<Object> getAgents() {
        return List.copyOf(agents);
    }

    public int count() {
        return agents.size();
    }

    public boolean isEmpty() {
        return agents.isEmpty();
    }

    /**
     * @return the single agent assignable to {@code type}
     * @throws NoSuchBeanException   if no agent matches
     * @throws NoUniqueBeanException if more than one matches
     */
    public <T> T getAgent(Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        List<Object> matches = agents.stream().filter(type::isInstance).toList();
        if (matches.isEmpty()) {
            throw new NoSuchBeanException("No agent of type " + type.getName());
        }
        if (matches.size() > 1) {
            throw new NoUniqueBeanException("No unique agent of type " + type.getName()
                    + "; found " + matches.size());
        }
        return type.cast(matches.get(0));
    }
}
