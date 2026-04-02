package com.flowforge;
import com.flowforge.engine.WorkflowEngine;
import com.flowforge.event.EventBus;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.pipeline.Pipeline;
import com.flowforge.plugin.PluginContext;
import com.flowforge.plugin.PluginRegistry;
import com.flowforge.plugin.builtin.FileOperationsPlugin;
import com.flowforge.task.Task;
import com.flowforge.task.TaskFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PluginLifecycleTest {

    private TaskFactory factory;
    private PluginRegistry registry;

    @BeforeEach
    void setUp() {
        factory = new TaskFactory();
        EventBus eventBus = new EventBus();
        WorkflowEngine engine = new WorkflowEngine(factory, eventBus);
        Pipeline pipeline = new Pipeline();
        PluginContext context = new PluginContext(factory, eventBus, pipeline, engine::registerStrategy);
        registry = new PluginRegistry(context);
    }

    @Test
    void shouldRegisterAndInitializePlugin() {
        FileOperationsPlugin plugin = new FileOperationsPlugin();
        registry.register(plugin);
        assertDoesNotThrow(() -> registry.initializeAll());
    }

    @Test
    void shouldStartAndStopPlugins() {
        FileOperationsPlugin plugin = new FileOperationsPlugin();
        registry.register(plugin);
        registry.initializeAll();
        assertDoesNotThrow(() -> registry.startAll());
        assertDoesNotThrow(() -> registry.stopAll());
    }

    @Test
    void ftpTaskShouldExecuteAfterPluginInit() {
        FileOperationsPlugin plugin = new FileOperationsPlugin();
        registry.register(plugin);
        registry.initializeAll();

        Task task = factory.createTask(new TaskConfig("ftp-test", "ftp",
                Map.of("host", "ftp.test.com", "file", "data.csv")));
        TaskResult result = task.execute(new TaskConfig("ftp-test", "ftp",
                Map.of("host", "ftp.test.com", "file", "data.csv")));
        assertTrue(result.isSuccess());
    }

    @Test
    void fileCopyTaskShouldExecute() {
        FileOperationsPlugin plugin = new FileOperationsPlugin();
        registry.register(plugin);
        registry.initializeAll();

        TaskResult result = factory.createTask(new TaskConfig("cp", "file_copy",
                Map.of("source", "/a", "destination", "/b")))
                .execute(new TaskConfig("cp", "file_copy",
                        Map.of("source", "/a", "destination", "/b")));
        assertTrue(result.isSuccess());
    }

    @Test
    void fileDeleteTaskShouldExecute() {
        FileOperationsPlugin plugin = new FileOperationsPlugin();
        registry.register(plugin);
        registry.initializeAll();

        TaskResult result = factory.createTask(new TaskConfig("del", "file_delete",
                Map.of("path", "/tmp/old.csv")))
                .execute(new TaskConfig("del", "file_delete",
                        Map.of("path", "/tmp/old.csv")));
        assertTrue(result.isSuccess());
    }

    @Test
    void pluginRegistryShouldListPlugins() {
        registry.register(new FileOperationsPlugin());
        registry.initializeAll();
        assertDoesNotThrow(() -> registry.listPlugins());
    }
}
