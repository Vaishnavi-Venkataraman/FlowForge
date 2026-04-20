package com.flowforge.web.handler;

import com.flowforge.model.WorkflowDefinition;
import com.flowforge.web.JsonUtil;
import com.flowforge.web.UserStore;
import com.flowforge.web.WorkflowStore;
import com.flowforge.web.WorkflowStore.*;
import com.flowforge.FlowForgeFacade;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * REST API for workflow CRUD and execution.
 */
public class WorkflowApiHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(WorkflowApiHandler.class.getName());

    private static final String KEY_ERROR = "error";
    private static final String KEY_SUCCESS = "success";
    private static final String ERROR_NOT_FOUND = "Workflow not found";
    private static final String ERROR_NOT_AUTHENTICATED = "Not authenticated";

    private final UserStore userStore;
    private final WorkflowStore workflowStore;
    private final FlowForgeFacade flowforge;

    public WorkflowApiHandler(UserStore userStore, WorkflowStore workflowStore, FlowForgeFacade flowforge) {
        this.userStore = userStore;
        this.workflowStore = workflowStore;
        this.flowforge = flowforge;
    }

    /**
     * Re-registers persisted workflows with the engine on startup.
     */
    public void reloadPersistedWorkflows() {
        int count = 0;
        for (String owner : workflowStore.getAllOwners()) {
            for (StoredWorkflow stored : workflowStore.getWorkflows(owner)) {
                try {
                    WorkflowDefinition wfDef = WorkflowBuildHelper.buildWorkflow(stored.id(), 
                            stored.name(), stored.triggerType(), stored.triggerValue(),
                            stored.strategy(), stored.tasks());
                    flowforge.registerWorkflow(wfDef);
                    count++;
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Reload failed for workflow: {0}", stored.name());
                }
            }
        }
        if (count > 0) {
            LOGGER.log(Level.INFO, "Reloaded {0} workflows from disk", count);
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        String sessionId = HttpHelper.getSessionId(exchange);
        String username = userStore.getUsername(sessionId);
        if (username == null) {
            HttpHelper.sendJson(exchange, 401, Map.of(KEY_ERROR, ERROR_NOT_AUTHENTICATED));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            routeRequest(exchange, username, path, method);
        } catch (Exception e) {
            HttpHelper.sendJson(exchange, 500, Map.of(KEY_ERROR, e.getMessage()));
        }
    }

    private void routeRequest(HttpExchange exchange, String username, String path, String method)
            throws IOException {
        if (path.equals("/api/workflows") && "GET".equals(method)) {
            listWorkflows(exchange, username);
        } else if (path.equals("/api/workflows") && "POST".equals(method)) {
            createWorkflow(exchange, username);
        } else if (path.matches("/api/workflows/[^/]+/run") && "POST".equals(method)) {
            runWorkflow(exchange, username, path.split("/")[3]);
        } else if (path.matches("/api/workflows/[^/]+") && "GET".equals(method)) {
            getWorkflow(exchange, username, path.split("/")[3]);
        } else if (path.matches("/api/workflows/[^/]+") && "DELETE".equals(method)) {
            deleteWorkflow(exchange, username, path.split("/")[3]);
        } else {
            HttpHelper.sendJson(exchange, 404, Map.of(KEY_ERROR, "Not found"));
        }
    }

    private void listWorkflows(HttpExchange exchange, String username) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        for (StoredWorkflow wf : workflowStore.getWorkflows(username)) {
            result.add(toSummaryMap(wf));
        }
        HttpHelper.sendJsonArray(exchange, 200, JsonUtil.toJsonArray(result));
    }

    private void createWorkflow(HttpExchange exchange, String username) throws IOException {
        Map<String, String> body = HttpHelper.parseBody(exchange);
        String name = body.get("name");
        String triggerType = body.getOrDefault("triggerType", "MANUAL");
        String triggerValue = body.getOrDefault("triggerValue", "");
        String strategy = body.getOrDefault("strategy", "sequential");
        String tasksJson = body.getOrDefault("tasks", "[]");

        if (name == null || name.isBlank()) {
            HttpHelper.sendJson(exchange, 400, Map.of(KEY_ERROR, "Workflow name is required"));
            return;
        }

        List<TaskInfo> taskInfos = parseTasksFromJson(tasksJson);
        if (taskInfos.isEmpty()) {
            HttpHelper.sendJson(exchange, 400, Map.of(KEY_ERROR, "At least one task is required"));
            return;
        }

        WorkflowDefinition wfDef = WorkflowBuildHelper.buildWorkflow(
                name, triggerType, triggerValue, strategy, taskInfos);
        String wfId = flowforge.registerWorkflow(wfDef);

        StoredWorkflow stored = new StoredWorkflow(
                wfId, name, username, triggerType, triggerValue,
                strategy, taskInfos, System.currentTimeMillis(),
                new CopyOnWriteArrayList<>());
        workflowStore.saveWorkflow(username, stored);
        HttpHelper.sendJson(exchange, 201, toSummaryMap(stored));
    }

    private void getWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        StoredWorkflow wf = workflowStore.getWorkflow(username, id);
        if (wf == null) {
            HttpHelper.sendJson(exchange, 404, Map.of(KEY_ERROR, ERROR_NOT_FOUND));
            return;
        }
        HttpHelper.sendJson(exchange, 200, toDetailMap(wf));
    }

    private void runWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        StoredWorkflow wf = workflowStore.getWorkflow(username, id);
        if (wf == null) {
            HttpHelper.sendJson(exchange, 404, Map.of(KEY_ERROR, ERROR_NOT_FOUND));
            return;
        }

        String execId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();
        String status = "COMPLETED";

        // Capture BOTH stdout AND java.util.logging output during execution.
        // Tasks use LOGGER.info() so we attach a temporary handler to the root logger.
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        Logger rootLogger = Logger.getLogger("com.flowforge");
        Handler logCapture = new StreamHandler(baos, new SimpleFormatter());
        logCapture.setLevel(Level.ALL);
        rootLogger.addHandler(logCapture);

        try {
            flowforge.fireTrigger(id);
        } catch (RuntimeException e) {
            logs.add("ERROR: " + e.getMessage());
        } finally {
            logCapture.flush();
            rootLogger.removeHandler(logCapture);
            logCapture.close();
        }

        String captured = baos.toString();
        if (!captured.isBlank()) {
            logs.addAll(0, Arrays.asList(captured.split("\\r?\\n")));
        }

        // Determine status from captured logs — fireTrigger() is async via ServiceBus
        // so exceptions are swallowed. We detect failure by scanning the log output.
        boolean hasFailed = logs.stream().anyMatch(l ->
                l.contains("WORKFLOW_FAILED") || l.contains("FAILURE") || l.contains("FAILED"));
        boolean hasCompleted = logs.stream().anyMatch(l ->
                l.contains("WORKFLOW_COMPLETED") || l.contains("EXECUTION_COMPLETE"));
        status = hasFailed ? "FAILED" : (hasCompleted ? "COMPLETED" : "COMPLETED");

        long endTime = System.currentTimeMillis();
        ExecutionRecord execRecord = new ExecutionRecord(execId, status, startTime, endTime, logs);
        workflowStore.addExecution(username, id, execRecord);

        LOGGER.log(Level.INFO, "Execute: {0} -> {1} ({2}ms)",
                new Object[]{wf.name(), status, endTime - startTime});

        HttpHelper.sendJson(exchange, 200, toExecutionMap(execRecord));
    }

    private void deleteWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        if (!workflowStore.deleteWorkflow(username, id)) {
            HttpHelper.sendJson(exchange, 404, Map.of(KEY_ERROR, ERROR_NOT_FOUND));
            return;
        }
        HttpHelper.sendJson(exchange, 200, Map.of(KEY_SUCCESS, true));
    }

    // --- Mapping helpers ---

    private Map<String, Object> toSummaryMap(StoredWorkflow wf) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", wf.id());
        m.put("name", wf.name());
        m.put("triggerType", wf.triggerType());
        m.put("triggerValue", wf.triggerValue());
        m.put("strategy", wf.strategy());
        m.put("taskCount", wf.tasks().size());
        m.put("createdAt", wf.createdAt());
        m.put("executionCount", wf.executions().size());

        if (!wf.executions().isEmpty()) {
            ExecutionRecord last = wf.executions().get(wf.executions().size() - 1);
            m.put("lastStatus", last.status());
            m.put("lastRunAt", last.startedAt());
        } else {
            m.put("lastStatus", "NEVER_RUN");
            m.put("lastRunAt", 0);
        }
        return m;
    }

    private Map<String, Object> toDetailMap(StoredWorkflow wf) {
        Map<String, Object> m = toSummaryMap(wf);

        List<Map<String, Object>> tasks = new ArrayList<>();
        for (TaskInfo ti : wf.tasks()) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("name", ti.name());
            tm.put("type", ti.type());
            tm.put("params", ti.params());
            tasks.add(tm);
        }
        m.put("tasks", tasks);

        List<Map<String, Object>> execs = new ArrayList<>();
        for (ExecutionRecord er : wf.executions()) {
            execs.add(toExecutionMap(er));
        }
        m.put("executions", execs);
        return m;
    }

    private Map<String, Object> toExecutionMap(ExecutionRecord er) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("executionId", er.executionId());
        m.put("status", er.status());
        m.put("startedAt", er.startedAt());
        m.put("finishedAt", er.finishedAt());
        m.put("durationMs", er.finishedAt() - er.startedAt());
        m.put("logCount", er.logs().size());
        m.put("logs", String.join("\n", er.logs()));
        return m;
    }

    // --- JSON task parsing ---

    private List<TaskInfo> parseTasksFromJson(String tasksJson) {
        List<TaskInfo> tasks = new ArrayList<>();
        tasksJson = tasksJson.trim();
        if (!tasksJson.startsWith("[")) {
            return tasks;
        }
        tasksJson = tasksJson.substring(1);
        if (tasksJson.endsWith("]")) {
            tasksJson = tasksJson.substring(0, tasksJson.length() - 1);
        }

        int depth = 0;
        int start = 0;
        for (int i = 0; i < tasksJson.length(); i++) {
            char c = tasksJson.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            } else if (c == ',' && depth == 0) {
                addParsedTask(tasks, tasksJson.substring(start, i).trim());
                start = i + 1;
            }
        }
        addParsedTask(tasks, tasksJson.substring(start).trim());
        return tasks;
    }

    private void addParsedTask(List<TaskInfo> tasks, String json) {
        if (json.isEmpty()) {
            return;
        }
        Map<String, String> flat = JsonUtil.parseJsonFlat(json);
        String taskName = flat.get("name");
        String taskType = flat.get("type");
        if (taskName == null || taskType == null) {
            return;
        }
        Map<String, String> params = new HashMap<>();
        String paramsStr = flat.get("params");
        if (paramsStr != null && paramsStr.startsWith("{")) {
            params = JsonUtil.parseJsonFlat(paramsStr);
        }
        tasks.add(new TaskInfo(taskName, taskType, params));
    }
}