package dev.forge.core.llm;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A deterministic, in-memory {@link LlmClient} for tests and local development.
 *
 * <p>In <em>echo</em> mode (the no-arg constructor) it returns the request's user
 * message unchanged. In <em>canned</em> mode it returns a fixed reply for every
 * request. Either way it records the requests it received, so a test can assert
 * what was sent to the model — the reason an in-memory client exists.
 *
 * <p>Thread-safe.
 */
public final class EchoLlmClient implements LlmClient {

    /** Non-null selects canned mode; null selects echo mode. */
    private final String cannedReply;
    private final List<LlmRequest> receivedRequests = new CopyOnWriteArrayList<>();

    /** Echo mode: replies with the request's user message. */
    public EchoLlmClient() {
        this.cannedReply = null;
    }

    /**
     * Canned mode: replies with {@code cannedReply} for every request.
     *
     * @param cannedReply the fixed reply; must not be null
     */
    public EchoLlmClient(String cannedReply) {
        this.cannedReply = Objects.requireNonNull(cannedReply, "cannedReply must not be null");
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        receivedRequests.add(request);
        return LlmResponse.of(cannedReply != null ? cannedReply : request.userMessage());
    }

    /** @return the requests received so far, in order; immutable, never null */
    public List<LlmRequest> receivedRequests() {
        return List.copyOf(receivedRequests);
    }

    /**
     * @return the most recent request
     * @throws IllegalStateException if no request has been received yet
     */
    public LlmRequest lastRequest() {
        if (receivedRequests.isEmpty()) {
            throw new IllegalStateException("No request has been received yet");
        }
        return receivedRequests.get(receivedRequests.size() - 1);
    }
}
