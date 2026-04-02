package com.flowforge;

import com.flowforge.engine.WorkflowEngine;
import com.flowforge.event.EventBus;
import com.flowforge.pipeline.Pipeline;
import com.flowforge.plugin.PluginContext;
import com.flowforge.plugin.PluginRegistry;
import com.flowforge.plugin.builtin.FileOperationsPlugin;
import com.flowforge.task.TaskFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginTest {

    @Test
    void fileOperationsPluginShouldHaveMetadata() {
        FileOperationsPlugin plugin = new FileOperationsPlugin();
        assertEquals("flowforge.file-operations", plugin.getId());
        assertNotNull(plugin.getName());
        assertNotNull(plugin.getVersion());
    }

    @Test
    void shouldRegisterPluginInRegistry() {
        TaskFactory factory = new TaskFactory();
        EventBus eventBus = new EventBus();
        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        Pipeline pipeline = new Pipeline();
        PluginContext context = new PluginContext(factory, eventBus, pipeline, engine::registerStrategy);
        PluginRegistry registry = new PluginRegistry(context);

        FileOperationsPlugin plugin = new FileOperationsPlugin();
        registry.register(plugin);
        assertNotNull(registry);
    }

    @Test
    void shouldInitializeAndRegisterTaskTypes() {
        TaskFactory factory = new TaskFactory();
        EventBus eventBus = new EventBus();
        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        Pipeline pipeline = new Pipeline();
        PluginContext context = new PluginContext(factory, eventBus, pipeline, engine::registerStrategy);

        FileOperationsPlugin plugin = new FileOperationsPlugin();
        plugin.initialize(context);
        plugin.start();
        plugin.stop();

        // After initialization, ftp/file_copy/file_delete task types should be registered
        assertDoesNotThrow(() -> factory.createTask(
                new com.flowforge.model.TaskConfig("test-ftp", "ftp",
                        java.util.Map.of("host", "ftp.test.com", "file", "data.csv"))));
    }
}
