package com.flowforge.plugin;

/**
 * Each plugin receives a PluginContext that provides hooks into the core
 * (register tasks, subscribe to events, add pipeline handlers, etc.)
 */
public interface Plugin {

    /**
     * Unique identifier for this plugin.
     * Convention: "vendor.plugin-name" (e.g., "flowforge.slack-notifier")
     */
    String getId();

    /**
     * Human-readable name.
     */
    String getName();

    /**
     * Semantic version string (e.g., "1.0.0").
     */
    String getVersion();

    /**
     * Called once when the plugin is loaded. Use this to register
     * task types, event listeners, pipeline handlers, etc.
     *
     * @param context provides hooks into the FlowForge core
     */
    void initialize(PluginContext context);

    /**
     * Called when the plugin should begin active operation.
     * For example, a trigger plugin might start polling here.
     */
    void start();

    /**
     * Called when the plugin should shut down gracefully.
     * Release resources, stop threads, close connections.
     */
    void stop();
}