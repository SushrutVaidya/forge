package dev.forge.core.llm;

import dev.forge.core.exception.LlmException;

/**
 * A provider-agnostic abstraction over a large language model.
 *
 * <p>Forge core depends only on this interface, never on a specific vendor.
 * Concrete adapters (Anthropic Claude first) live in separate modules and are
 * swapped without touching agent code. The in-memory {@link EchoLlmClient} keeps
 * core tests offline and deterministic.
 *
 * <p>Deliberately minimal for the current milestone: single-turn completion.
 * Streaming, tool-use, and multi-turn conversation will be added when the agent
 * layer requires them.
 */
public interface LlmClient {

    /**
     * Sends a request to the model and returns its completion.
     *
     * @param request the request; must not be {@code null}
     * @return the model's response
     * @throws LlmException if the underlying provider or transport fails
     */
    LlmResponse complete(LlmRequest request);
}
