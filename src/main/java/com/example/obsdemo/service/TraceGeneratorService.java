package com.example.obsdemo.service;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class TraceGeneratorService {

    private static final Logger LOG = Logger.getLogger(TraceGeneratorService.class);
    private static final int MAX_DELAY_MS = 5_000;
    private static final int MAX_BURST = 20;
    private static final int NESTED_CHILD_COUNT = 3;

    private final Tracer tracer;

    @Inject
    public TraceGeneratorService(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer("observability-demo");
    }

    public record TraceResult(String type, int spanCount, String traceId, String spanId, String message) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "type", type,
                    "spanCount", spanCount,
                    "traceId", traceId,
                    "spanId", spanId,
                    "message", message
            );
        }
    }

    public TraceResult generateSimple(String name) {
        String eventName = name == null || name.isBlank() ? "demo-trace-event" : name;
        String eventId = UUID.randomUUID().toString();

        Span span = tracer.spanBuilder("obs-demo-simple")
                .setAttribute("obs.event.name", eventName)
                .setAttribute("obs.event.id", eventId)
                .setAttribute("obs.source", "button-click")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            LOG.infof("obs-demo trace eventId=%s type=simple name=%s", eventId, eventName);
            return result("simple", 1, span, "Simple trace exported");
        } finally {
            span.end();
        }
    }

    public TraceResult generateNested() {
        String eventId = UUID.randomUUID().toString();
        List<String> childSpanIds = new ArrayList<>();

        Span parent = tracer.spanBuilder("obs-demo-parent")
                .setAttribute("obs.event.id", eventId)
                .setAttribute("obs.source", "button-click")
                .startSpan();

        try (Scope parentScope = parent.makeCurrent()) {
            for (int i = 1; i <= NESTED_CHILD_COUNT; i++) {
                Span child = tracer.spanBuilder("obs-demo-child-" + i)
                        .setAttribute("obs.child.index", i)
                        .setAttribute("obs.event.id", eventId)
                        .startSpan();

                try (Scope childScope = child.makeCurrent()) {
                    childSpanIds.add(child.getSpanContext().getSpanId());
                    sleep(25);
                } finally {
                    child.end();
                }
            }

            LOG.infof("obs-demo trace eventId=%s type=nested children=%d", eventId, NESTED_CHILD_COUNT);
            return result("nested", 1 + NESTED_CHILD_COUNT, parent,
                    "Nested trace with " + NESTED_CHILD_COUNT + " child spans exported");
        } finally {
            parent.end();
        }
    }

    public TraceResult generateError(String message) {
        String eventId = UUID.randomUUID().toString();
        String errorMessage = message == null || message.isBlank()
                ? "Simulated error for observability demo"
                : message;

        Span span = tracer.spanBuilder("obs-demo-error")
                .setAttribute("obs.event.id", eventId)
                .setAttribute("obs.source", "button-click")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            RuntimeException exception = new RuntimeException(errorMessage);
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, errorMessage);
            LOG.warnf("obs-demo trace eventId=%s type=error message=%s", eventId, errorMessage);
            return result("error", 1, span, "Error trace exported");
        } finally {
            span.end();
        }
    }

    public TraceResult generateSlow(int delayMs) {
        int boundedDelay = Math.min(Math.max(delayMs, 50), MAX_DELAY_MS);
        String eventId = UUID.randomUUID().toString();

        Span span = tracer.spanBuilder("obs-demo-slow")
                .setAttribute("obs.event.id", eventId)
                .setAttribute("obs.delay.ms", boundedDelay)
                .setAttribute("obs.source", "button-click")
                .startSpan();

        try (Scope scope = span.makeCurrent()) {
            sleep(boundedDelay);
            LOG.infof("obs-demo trace eventId=%s type=slow delayMs=%d", eventId, boundedDelay);
            return result("slow", 1, span, "Slow trace (" + boundedDelay + "ms) exported");
        } finally {
            span.end();
        }
    }

    public TraceResult generateBurst(int count) {
        int boundedCount = Math.min(Math.max(count, 1), MAX_BURST);
        String lastTraceId = "";
        String lastSpanId = "";

        for (int i = 1; i <= boundedCount; i++) {
            Span span = tracer.spanBuilder("obs-demo-burst-" + i)
                    .setAttribute("obs.burst.index", i)
                    .setAttribute("obs.burst.total", boundedCount)
                    .setAttribute("obs.source", "button-click")
                    .startSpan();

            try (Scope scope = span.makeCurrent()) {
                lastTraceId = span.getSpanContext().getTraceId();
                lastSpanId = span.getSpanContext().getSpanId();
            } finally {
                span.end();
            }
        }

        LOG.infof("obs-demo trace type=burst count=%d", boundedCount);
        return new TraceResult(
                "burst",
                boundedCount,
                lastTraceId,
                lastSpanId,
                "Burst of " + boundedCount + " traces exported"
        );
    }

    private TraceResult result(String type, int spanCount, Span span, String message) {
        return new TraceResult(
                type,
                spanCount,
                span.getSpanContext().getTraceId(),
                span.getSpanContext().getSpanId(),
                message
        );
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
