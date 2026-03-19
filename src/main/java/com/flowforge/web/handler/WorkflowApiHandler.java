package com.flowforge.web.handler;

import com.flowforge.engine.WorkflowBuilder;
import com.flowforge.exception.FlowForgeException;
import com.flowforge.model.TaskConfig;
import com.flowforge.model.WorkflowDefinition;
import com.flowforge.web.JsonUtil;
import com.flowforge.web.UserStore;
import com.flowforge.web.WorkflowStore;
import com.flowforge.web.WorkflowStore.*;
import com.flowforge.FlowForgeFacade;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.*;

/**
 * REST API for workflow CRUD and execution.
 *
 * Endpoints:
 *   GET    /api/workflows          — list user's workflows
 *   POST   /api/workflows          — create workflow
 *   GET    /api/workflows/{id}     — get workflow detail + execution history
 *   POST   /api/workflows/{id}/run — execute workflow
 *   DELETE /api/workflows/{id}     — delete workflow
 */
public class WorkflowApiHandler implements HttpHandler {

    private final UserStore userStore;
    private final WorkflowStore workflowStore;
    private final FlowForgeFacade flowforge;

    public WorkflowApiHandler(UserStore userStore, WorkflowStore workflowStore, FlowForgeFacade flowforge) {
        this.userStore = userStore;
        this.workflowStore = workflowStore;
        this.flowforge = flowforge;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpHelper.handleCors(exchange);
            return;
        }

        // Authenticate
        String sessionId = HttpHelper.getSessionId(exchange);
        String username = userStore.getUsername(sessionId);
        if (username == null) {
            HttpHelper.sendJson(exchange, 401, Map.of("error", "Not authenticated"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/api/workflows") && "GET".equals(method)) {
                listWorkflows(exchange, username);
            } else if (path.equals("/api/workflows") && "POST".equals(method)) {
                createWorkflow(exchange, username);
            } else if (path.matches("/api/workflows/[^/]+/run") && "POST".equals(method)) {
                String id = path.split("/")[3];
                runWorkflow(exchange, username, id);
            } else if (path.matches("/api/workflows/[^/]+") && "GET".equals(method)) {
                String id = path.split("/")[3];
                getWorkflow(exchange, username, id);
            } else if (path.matches("/api/workflows/[^/]+") && "DELETE".equals(method)) {
                String id = path.split("/")[3];
                deleteWorkflow(exchange, username, id);
            } else {
                HttpHelper.sendJson(exchange, 404, Map.of("error", "Not found"));
            }
        } catch (Exception e) {
            HttpHelper.sendJson(exchange, 500, Map.of("error", e.getMessage()));
        }
    }

    private void listWorkflows(HttpExchange exchange, String username) throws IOException {
        List<StoredWorkflow> workflows = workflowStore.getWorkflows(username);
        List<Map<String, Object>> result = new ArrayList<>();
        for (StoredWorkflow wf : workflows) {
            result.add(workflowToMap(wf, false));
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
            HttpHelper.sendJson(exchange, 400, Map.of("error", "Workflow name is required"));
            return;
        }

        // Parse tasks from JSON array
        List<TaskInfo> taskInfos = parseTasks(tasksJson);
        if (taskInfos.isEmpty()) {
            HttpHelper.sendJson(exchange, 400, Map.of("error", "At least one task is required"));
            return;
        }

        // Build workflow via engine
        WorkflowBuilder builder = WorkflowBuilder.create().name(name);

        switch (triggerType.toUpperCase()) {
            case "CRON" -> builder.cronTrigger(triggerValue);
            case "WEBHOOK" -> builder.webhookTrigger(triggerValue);
            case "EVENT" -> builder.eventTrigger(triggerValue);
            case "FILE_WATCH" -> builder.fileWatchTrigger(triggerValue);
            default -> builder.manualTrigger();
        }

        for (TaskInfo ti : taskInfos) {
            builder.addTask(ti.name(), ti.type(), ti.params());
        }
        builder.strategy(strategy);

        WorkflowDefinition wfDef = builder.build();
        String wfId = flowforge.registerWorkflow(wfDef);

        // Store for persistence
        StoredWorkflow stored = new StoredWorkflow(
                wfId, name, username, triggerType, triggerValue, strategy,
                taskInfos, System.currentTimeMillis(), new CopyOnWriteArrayList<>()
        );
        workflowStore.saveWorkflow(username, stored);

        HttpHelper.sendJson(exchange, 201, workflowToMap(stored, false));
    }

    private void getWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        StoredWorkflow wf = workflowStore.getWorkflow(username, id);
        if (wf == null) {
            HttpHelper.sendJson(exchange, 404, Map.of("error", "Workflow not found"));
            return;
        }
        HttpHelper.sendJson(exchange, 200, workflowToMap(wf, true));
    }

    private void runWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        StoredWorkflow wf = workflowStore.getWorkflow(username, id);
        if (wf == null) {
            HttpHelper.sendJson(exchange, 404, Map.of("error", "Workflow not found"));
            return;
        }

        String execId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();
        String status;

        // Capture stdout during execution for logs
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.PrintStream capture = new java.io.PrintStream(baos);
        java.io.PrintStream original = System.out;

        try {
            System.setOut(capture);
            flowforge.fireTrigger(id);
            System.out.flush();
            status = "COMPLETED";
        } catch (FlowForgeException e) {
            System.out.flush();
            status = "FAILED";
            logs.add("ERROR: " + e.getMessage());
        } catch (Exception e) {
            System.out.flush();
            status = "FAILED";
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logs.add("ERROR: " + e.getMessage());
        } finally {
            System.setOut(original);
        }

        // Parse captured output into log lines
        String captured = baos.toString();
        if (!captured.isBlank()) {
            logs.addAll(0, Arrays.asList(captured.split("\n")));
        }

        long endTime = System.currentTimeMillis();
        ExecutionRecord record = new ExecutionRecord(execId, status, startTime, endTime, logs);
        workflowStore.addExecution(username, id, record);

        // Also print to real stdout for server console
        System.out.println("[Execute] " + wf.name() + " → " + status + " (" + (endTime - startTime) + "ms)");

        HttpHelper.sendJson(exchange, 200, executionToMap(record));
    }

    private void deleteWorkflow(HttpExchange exchange, String username, String id) throws IOException {
        boolean deleted = workflowStore.deleteWorkflow(username, id);
        if (!deleted) {
            HttpHelper.sendJson(exchange, 404, Map.of("error", "Workflow not found"));
            return;
        }
        HttpHelper.sendJson(exchange, 200, Map.of("success", true));
    }

    // --- Helpers ---

    private Map<String, Object> workflowToMap(StoredWorkflow wf, boolean includeDetails) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", wf.id());
        map.put("name", wf.name());
        map.put("triggerType", wf.triggerType());
        map.put("triggerValue", wf.triggerValue());
        map.put("strategy", wf.strategy());
        map.put("taskCount", wf.tasks().size());
        map.put("createdAt", wf.createdAt());
        map.put("executionCount", wf.executions().size());

        if (!wf.executions().isEmpty()) {
            ExecutionRecord last = wf.executions().get(wf.executions().size() - 1);
            map.put("lastStatus", last.status());
            map.put("lastRunAt", last.startedAt());
        } else {
            map.put("lastStatus", "NEVER_RUN");
            map.put("lastRunAt", 0);
        }

        if (includeDetails) {
            List<Map<String, Object>> tasks = new ArrayList<>();
            for (TaskInfo ti : wf.tasks()) {
                Map<String, Object> taskMap = new LinkedHashMap<>();
                taskMap.put("name", ti.name());
                taskMap.put("type", ti.type());
                taskMap.put("params", ti.params());
                tasks.add(taskMap);
            }
            map.put("tasks", tasks);

            List<Map<String, Object>> execs = new ArrayList<>();
            for (ExecutionRecord er : wf.executions()) {
                execs.add(executionToMap(er));
            }
            map.put("executions", execs);
        }

        return map;
    }

    private Map<String, Object> executionToMap(ExecutionRecord er) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("executionId", er.executionId());
        map.put("status", er.status());
        map.put("startedAt", er.startedAt());
        map.put("finishedAt", er.finishedAt());
        map.put("durationMs", er.finishedAt() - er.startedAt());
        map.put("logCount", er.logs().size());
        // Send logs as a single string to avoid complex JSON array of strings
        map.put("logs", String.join("\n", er.logs()));
        return map;
    }

    private List<TaskInfo> parseTasks(String tasksJson) {
        List<TaskInfo> tasks = new ArrayList<>();
        // Simple parsing: expect [{"name":"x","type":"y","params":{"k":"v"}}]
        tasksJson = tasksJson.trim();
        if (!tasksJson.startsWith("[")) return tasks;
        tasksJson = tasksJson.substring(1);
        if (tasksJson.endsWith("]")) tasksJson = tasksJson.substring(0, tasksJson.length() - 1);

        // Split by },{ pattern
        int depth = 0;
        int start = 0;
        for (int i = 0; i < tasksJson.length(); i++) {
            char c = tasksJson.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            else if (c == ',' && depth == 0) {
                String chunk = tasksJson.substring(start, i).trim();
                TaskInfo ti = parseOneTask(chunk);
                if (ti != null) tasks.add(ti);
                start = i + 1;
            }
        }
        String last = tasksJson.substring(start).trim();
        if (!last.isEmpty()) {
            TaskInfo ti = parseOneTask(last);
            if (ti != null) tasks.add(ti);
        }
        return tasks;
    }

    private TaskInfo parseOneTask(String json) {
        Map<String, String> flat = JsonUtil.parseJsonFlat(json);
        String name = flat.get("name");
        String type = flat.get("type");
        if (name == null || type == null) return null;

        // Extract params sub-object
        Map<String, String> params = new HashMap<>();
        String paramsStr = flat.get("params");
        if (paramsStr != null && paramsStr.startsWith("{")) {
            params = JsonUtil.parseJsonFlat(paramsStr);
        }

        return new TaskInfo(name, type, params);
    }

    // Need this import for CopyOnWriteArrayList usage in record construction
    private static class CopyOnWriteArrayList<E> extends java.util.concurrent.CopyOnWriteArrayList<E> {}
}