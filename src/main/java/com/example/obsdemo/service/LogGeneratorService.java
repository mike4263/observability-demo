package com.example.obsdemo.service;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class LogGeneratorService {

    private static final Logger LOG = Logger.getLogger(LogGeneratorService.class);
    private static final List<String> BURST_LEVELS = List.of("DEBUG", "INFO", "WARN", "ERROR");

    public record LogResult(int count, String level, String message) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "count", count,
                    "level", level,
                    "message", message
            );
        }
    }

    @WithSpan("generate-logs")
    public LogResult generate(@SpanAttribute("log.level") String level, String message, int count) {
        String normalizedLevel = level.toUpperCase(Locale.ROOT);
        int boundedCount = Math.min(Math.max(count, 1), 100);
        String eventId = UUID.randomUUID().toString();

        for (int i = 0; i < boundedCount; i++) {
            emitLog(normalizedLevel, message, eventId, i + 1, boundedCount);
        }

        return new LogResult(boundedCount, normalizedLevel, message);
    }

    @WithSpan("generate-log-burst")
    public LogResult generateBurst(int count) {
        int boundedCount = Math.min(Math.max(count, 1), 100);
        String eventId = UUID.randomUUID().toString();

        for (int i = 0; i < boundedCount; i++) {
            String level = BURST_LEVELS.get(i % BURST_LEVELS.size());
            emitLog(level, "Burst log event", eventId, i + 1, boundedCount);
        }

        return new LogResult(boundedCount, "MIXED", "Burst log event");
    }

    private void emitLog(String level, String message, String eventId, int index, int total) {
        String formattedMessage = String.format(
                "obs-demo eventId=%s level=%s source=button-click index=%d total=%d message=%s",
                eventId,
                level,
                index,
                total,
                message
        );

        switch (level) {
            case "DEBUG" -> LOG.debug(formattedMessage);
            case "WARN" -> LOG.warn(formattedMessage);
            case "ERROR" -> LOG.error(formattedMessage);
            default -> LOG.info(formattedMessage);
        }
    }
}
