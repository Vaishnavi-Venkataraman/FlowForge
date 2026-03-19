package com.flowforge.pipeline;

/**
 * Handler in the Chain of Responsibility pipeline.
 * Also implements the Pipes & Filters architectural pattern:
 *   Input → [Validate] → [Transform] → [Authorize] → [Log] → [Execute] → Output
 */
public interface PipelineHandler {

    /**
     * @return human-readable name for this handler
     */
    String getName();

    /**
     * Process the context and optionally pass to the next handler.
     *
     * @param context the pipeline context (mutable, shared across handlers)
     * @param next    the next handler in the chain (call next.handle() to continue)
     */
    void handle(PipelineContext context, PipelineHandler next);
}