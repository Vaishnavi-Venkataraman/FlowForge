package com.flowforge.pipeline;

import java.util.ArrayList;
import java.util.List;

/**
 * The Pipeline chains PipelineHandlers together and executes them in order.
 * Each handler receives the context and a reference to the next handler.
 * The terminal handler (end of chain) is a no-op that simply returns.
 */
public class Pipeline {

    private final List<PipelineHandler> handlers = new ArrayList<>();

    /**
     * Adds a handler to the end of the pipeline.
     */
    public Pipeline addHandler(PipelineHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }
        handlers.add(handler);
        return this;
    }

    /**
     * Executes the pipeline: passes the context through all handlers in order.
     *
     * @param context the pipeline context
     * @return the context after processing (may be aborted)
     */
    public PipelineContext execute(PipelineContext context) {
        if (handlers.isEmpty()) {
            context.addLog("Pipeline has no handlers — nothing to process");
            return context;
        }

        // Build the chain from back to front
        // The last handler's "next" is a terminal no-op
        PipelineHandler chain = buildChain(0);
        chain.handle(context, buildTerminal());

        return context;
    }

    /**
     * Recursively builds the handler chain.
     * Handler 0's next is Handler 1, whose next is Handler 2, ..., whose next is terminal.
     */
    private PipelineHandler buildChain(int index) {
        if (index >= handlers.size()) {
            return buildTerminal();
        }

        PipelineHandler current = handlers.get(index);
        PipelineHandler next = buildChain(index + 1);

        return new PipelineHandler() {
            @Override
            public String getName() {
                return current.getName();
            }

            @Override
            public void handle(PipelineContext ctx, PipelineHandler ignored) {
                // Pass the real next handler, not the ignored one
                if (!ctx.isAborted()) {
                    current.handle(ctx, next);
                }
            }
        };
    }

    private PipelineHandler buildTerminal() {
        return new PipelineHandler() {
            @Override
            public String getName() {
                return "terminal";
            }

            @Override
            public void handle(PipelineContext ctx, PipelineHandler next) {
                ctx.addLog("Pipeline processing complete");
            }
        };
    }

    /**
     * Returns the registered handler names (for debugging/logging).
     */
    public List<String> getHandlerNames() {
        return handlers.stream().map(PipelineHandler::getName).toList();
    }
}