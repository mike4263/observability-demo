package com.example.obsdemo.service;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class CpuLoadService {

    private static final Logger LOG = Logger.getLogger(CpuLoadService.class);
    private static final int MAX_THREADS = 4;
    private static final int MAX_DURATION_SECONDS = 60;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "cpu-load-coordinator");
        thread.setDaemon(true);
        return thread;
    });

    public record CpuLoadResult(boolean accepted, String message, int threads, int durationSeconds) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "accepted", accepted,
                    "message", message,
                    "threads", threads,
                    "durationSeconds", durationSeconds
            );
        }
    }

    public CpuLoadResult startLoad(int threads, int durationSeconds) {
        int boundedThreads = Math.min(Math.max(threads, 1), MAX_THREADS);
        int boundedDuration = Math.min(Math.max(durationSeconds, 1), MAX_DURATION_SECONDS);

        if (!running.compareAndSet(false, true)) {
            return new CpuLoadResult(false, "CPU load job already running", boundedThreads, boundedDuration);
        }

        LOG.infof("Starting CPU load: threads=%d durationSeconds=%d", boundedThreads, boundedDuration);

        CompletableFuture.runAsync(() -> {
            try {
                runCpuLoad(boundedThreads, boundedDuration);
            } finally {
                running.set(false);
                LOG.infof("CPU load completed: threads=%d durationSeconds=%d", boundedThreads, boundedDuration);
            }
        }, executor);

        return new CpuLoadResult(
                true,
                "CPU load started",
                boundedThreads,
                boundedDuration
        );
    }

    @WithSpan("cpu-load")
    void runCpuLoad(@SpanAttribute("cpu.threads") int threads, @SpanAttribute("cpu.duration_seconds") int durationSeconds) {
        long endTime = System.nanoTime() + (durationSeconds * 1_000_000_000L);
        Thread[] workers = new Thread[threads];

        for (int i = 0; i < threads; i++) {
            int workerId = i;
            workers[i] = new Thread(() -> burnCpu(workerId, endTime), "cpu-load-worker-" + workerId);
            workers[i].start();
        }

        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void burnCpu(int workerId, long endTimeNanos) {
        long counter = 0;
        while (System.nanoTime() < endTimeNanos) {
            counter += (counter * 31) ^ (workerId + 1);
            if ((counter & 0xFFFF) == 0) {
                counter = counter % 1_000_003;
            }
        }
    }
}
