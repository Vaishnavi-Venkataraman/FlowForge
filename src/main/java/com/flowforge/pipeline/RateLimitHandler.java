package com.flowforge.pipeline;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pipeline handler that rate-limits workflow execution.
 *
 * WHY: Prevents a workflow from being triggered too frequently
 * (e.g., webhook spam, runaway cron). If the same workflow was
 * executed within the cooldown period, this handler aborts the pipeline.
 *
 * Demonstrates how operational concerns (rate limiting) are added to
 * the pipeline as a handler — no engine modification needed.
 */
public class RateLimitHandler implements PipelineHandler {

    private final long cooldownMs;
    private final Map<String, Instant> lastExecutionTimes = new ConcurrentHashMap<>();

    /**
     * @param cooldownMs minimum milliseconds between executions of the same workflow
     */
    public RateLimitHandler(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    @Override
    public String getName() {
        return "RateLimitHandler";
    }

    @Override
    public void handle(PipelineContext context, PipelineHandler next) {
        String workflowId = context.getWorkflow().getId();
        Instant now = Instant.now();

        context.addLog("[RateLimitHandler] Checking rate limit (cooldown: " + cooldownMs + "ms)");

        Instant lastRun = lastExecutionTimes.get(workflowId);
        if (lastRun != null) {
            long elapsed = now.toEpochMilli() - lastRun.toEpochMilli();
            if (elapsed < cooldownMs) {
                context.abort("Rate limited: workflow '" + context.getWorkflow().getName()
                        + "' was executed " + elapsed + "ms ago (cooldown: " + cooldownMs + "ms)");
                return;
            }
        }

        lastExecutionTimes.put(workflowId, now);
        context.addLog("[RateLimitHandler] Rate limit OK");
        next.handle(context, null);
    }
}