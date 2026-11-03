package com.eventrelay.serviceb.delivery;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// Per-instance limits. A shared Redis limiter is a separate planned phase.
@Component
public class EndpointGates {
    private final ConcurrentHashMap<UUID, Gate> gates = new ConcurrentHashMap<>();
    public Permit acquire(UUID id, int rate, int concurrency) throws InterruptedException {
        Gate gate = gates.computeIfAbsent(id, ignored -> new Gate());
        gate.acquire(rate, concurrency);
        return gate::release;
    }
    public interface Permit extends AutoCloseable { @Override void close(); }
    private static class Gate {
        private int inFlight;
        private int used;
        private long window = System.nanoTime();
        synchronized void acquire(int rate, int concurrency) throws InterruptedException {
            for (;;) {
                long elapsed = System.nanoTime() - window;
                if (elapsed >= 1_000_000_000L) { window = System.nanoTime(); used = 0; elapsed = 0; }
                if (inFlight < concurrency && used < rate) { inFlight++; used++; return; }
                long waitMs = used >= rate ? Math.max(1, (1_000_000_000L - elapsed) / 1_000_000L) : 1000;
                wait(waitMs);
            }
        }
        synchronized void release() { inFlight--; notifyAll(); }
    }
}
