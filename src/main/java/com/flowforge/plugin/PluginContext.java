package com.flowforge.plugin;

import com.flowforge.engine.strategy.ExecutionStrategy;
import com.flowforge.event.EventBus;
import com.flowforge.event.EventListener;
import com.flowforge.event.WorkflowEvent;
import com.flowforge.pipeline.Pipeline;
import com.flowforge.pipeline.PipelineHandler;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;

import java.util.function.Function;

/**
 * This is the "service provider interface" (SPI) of the Microkernel.
 * Plugins depend on PluginContext, not on the engine directly.
 */
public class PluginContext {

    private final TaskFactory taskFactory;
    private final EventBus eventBus;
    private final Pipeline pipeline;
    private final StrategyRegistrar strategyRegistrar;

    /**
     * Functional interface for registering strategies without exposing the engine.
     */
    @FunctionalInterface
    public interface StrategyRegistrar {
        void register(ExecutionStrategy strategy);
    }

    public PluginContext(TaskFactory taskFactory, EventBus eventBus,
                         Pipeline pipeline, StrategyRegistrar strategyRegistrar) {
        this.taskFactory = taskFactory;
        this.eventBus = eventBus;
        this.pipeline = pipeline;
        this.strategyRegistrar = strategyRegistrar;
    }

    // ===== Task Extension =====

    /**
     * Register a new task type that becomes available in all workflows.
     */
    public void registerTaskType(String type, Function<String, Task> creator) {
        taskFactory.registerTaskType(type, creator);
    }

    // ===== Event Extension =====

    /**
     * Subscribe to a specific event type.
     */
    public void subscribeToEvent(WorkflowEvent.Type eventType, EventListener listener) {
        eventBus.subscribe(eventType, listener);
    }

    /**
     * Subscribe to all events.
     */
    public void subscribeToAllEvents(EventListener listener) {
        eventBus.subscribeAll(listener);
    }

    /**
     * Publish a custom event.
     */
    public void publishEvent(WorkflowEvent event) {
        eventBus.publish(event);
    }

    // ===== Pipeline Extension =====

    /**
     * Add a handler to the pre-execution pipeline.
     */
    public void addPipelineHandler(PipelineHandler handler) {
        if (pipeline != null) {
            pipeline.addHandler(handler);
        }
    }

    // ===== Strategy Extension =====

    /**
     * Register a custom execution strategy.
     */
    public void registerStrategy(ExecutionStrategy strategy) {
        if (strategyRegistrar != null) {
            strategyRegistrar.register(strategy);
        }
    }
}