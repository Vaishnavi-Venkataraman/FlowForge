package com.flowforge.plugin.builtin;
import java.util.logging.Logger;
import com.flowforge.model.TaskConfig;
import com.flowforge.plugin.Plugin;
import com.flowforge.plugin.PluginContext;
import com.flowforge.task.AbstractTask;

public class FileOperationsPlugin implements Plugin {
    private static final Logger LOGGER = Logger.getLogger(FileOperationsPlugin.class.getName());
    @Override
    public String getId() { return "flowforge.file-operations"; }

    @Override
    public String getName() { return "File Operations Plugin"; }

    @Override
    public String getVersion() { return "1.2.0"; }

    @Override
    public void initialize(PluginContext context) {
        context.registerTaskType("ftp", FtpTask::new);
        context.registerTaskType("file_copy", FileCopyTask::new);
        context.registerTaskType("file_delete", FileDeleteTask::new);
    }

    @Override
    public void start() { 
        // No active resources to manage 
        }

    @Override
    public void stop() { 
        // No active resources to manage
    }

    // --- Named task classes instead of anonymous inner classes ---

    private static class FtpTask extends AbstractTask {
        FtpTask(String name) { super(name); }

        @Override
        public String getType() { return "ftp"; }

        @Override
        protected void validate(TaskConfig config) {
            config.getRequiredParameter("host");
        }

        @Override
        protected String doExecute(TaskConfig config) {
            String host = config.getRequiredParameter("host");
            String file = config.getParameter("file", "data.csv");
            String direction = config.getParameter("direction", "upload");
            LOGGER.info(() -> "    FTP " + direction + ": " + file + " → " + host);
            return "FTP " + direction + " of " + file + " to " + host;
        }
    }

    private static class FileCopyTask extends AbstractTask {
        FileCopyTask(String name) { super(name); }

        @Override
        public String getType() { return "file_copy"; }

        @Override
        protected void validate(TaskConfig config) {
            config.getRequiredParameter("source");
            config.getRequiredParameter("destination");
        }

        @Override
        protected String doExecute(TaskConfig config) {
            String src = config.getRequiredParameter("source");
            String dst = config.getRequiredParameter("destination");
            LOGGER.info(() -> "    Copying " + src + " → " + dst);
            return "Copied " + src + " to " + dst;
        }
    }

    private static class FileDeleteTask extends AbstractTask {
        FileDeleteTask(String name) { super(name); }

        @Override
        public String getType() { return "file_delete"; }

        @Override
        protected void validate(TaskConfig config) {
            config.getRequiredParameter("path");
        }

        @Override
        protected String doExecute(TaskConfig config) {
            String path = config.getRequiredParameter("path");
            LOGGER.info(() -> "    Deleting file: " + path);
            return "Deleted: " + path;
        }
    }
}
