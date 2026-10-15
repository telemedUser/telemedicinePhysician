package com.telemedicine.common.audit;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe in-process chain head used by {@link AuditedAspect}. */
public class AuditChainState {

    private final AtomicLong sequence = new AtomicLong(0L);
    private final AtomicReference<String> lastHash = new AtomicReference<>("");

    public ChainLink nextLink() {
        long nextSequence = sequence.incrementAndGet();
        return new ChainLink(nextSequence, lastHash.get());
    }

    public void advance(String hash) {
        lastHash.set(hash == null ? "" : hash);
    }

    public long lastSequence() {
        return sequence.get();
    }

    public String lastHash() {
        return lastHash.get();
    }

    public record ChainLink(long sequence, String previousHash) {}
}
