package com.flowforge.plugin.builtin;

import com.flowforge.model.TaskConfig;
import com.flowforge.model.TaskResult;
import com.flowforge.plugin.Plugin;
import com.flowforge.plugin.PluginContext;
import com.flowforge.task.AbstractTask;
import java.time.Instant;

public class FileOperationsPlugin implements Plugin {

    @Override
    public String getId() {
        return "flowforge.file-operations";
    }

    @Override
    public String getName() {
        return "File Operations Plugin";
    }

    @Override
    public String getVersion() {
        return "1.2.0";
    }

    @Override
    public void initialize(PluginContext context) {
        // Register FTP task type
        context.registerTaskType("ftp", name -> new AbstractTask(name) {
            @Override
            public String getType() {
                return "ftp";
            }

            @Override
            protected void validate(TaskConfig config) {
                config.getRequiredParameter("host");
            }

            @Override
            protected String doExecute(TaskConfig config) {
                String host = config.getRequiredParameter("host");
                String file = config.getParameter("file", "data.csv");
                String direction = config.getParameter("direction", "upload");
                System.out.println("    FTP " + direction + ": " + file + " → " + host);
                return "FTP " + direction + " of " + file + " to " + host + " complete";
            }
        });

        // Register file_copy task type
        context.registerTaskType("file_copy", name -> new AbstractTask(name) {
            @Override
            public String getType() {
                return "file_copy";
            }

            @Override
            protected void validate(TaskConfig config) {
                config.getRequiredParameter("source");
                config.getRequiredParameter("destination");
            }

            @Override
            protected String doExecute(TaskConfig config) {
                String source = config.getRequiredParameter("source");
                String dest = config.getRequiredParameter("destination");
                System.out.println("    Copying " + source + " → " + dest);
                return "Copied " + source + " to " + dest;
            }
        });

        // Register file_delete task type
        context.registerTaskType("file_delete", name -> new AbstractTask(name) {
            @Override
            public String getType() {
                return "file_delete";
            }

            @Override
            protected void validate(TaskConfig config) {
                config.getRequiredParameter("path");
            }

            @Override
            protected String doExecute(TaskConfig config) {
                String path = config.getRequiredParameter("path");
                System.out.println("    Deleting file: " + path);
                return "Deleted: " + path;
            }
        });

        System.out.println("    [FileOpsPlugin] Registered task types: ftp, file_copy, file_delete");
    }

    @Override
    public void start() {
        // No active resources to start
    }

    @Override
    public void stop() {
        // No active resources to stop
    }
}