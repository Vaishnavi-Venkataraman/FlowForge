package com.flowforge.web;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * File-persisted per-user workflow storage.
 * Data saved to data/workflows/ and data/executions/ — survives restarts.
 */
public class WorkflowStore {

    private static final Path DATA_DIR = Path.of("data");
    private static final Path WF_DIR = DATA_DIR.resolve("workflows");
    private static final Path EXEC_DIR = DATA_DIR.resolve("executions");

    public record StoredWorkflow(
            String id, String name, String owner,
            String triggerType, String triggerValue, String strategy,
            List<TaskInfo> tasks, long createdAt,
            List<ExecutionRecord> executions
    ) {}

    public record TaskInfo(String name, String type, Map<String, String> params) {}

    public record ExecutionRecord(
            String executionId, String status,
            long startedAt, long finishedAt, List<String> logs
    ) {}

    private final Map<String, List<StoredWorkflow>> store = new ConcurrentHashMap<>();

    public WorkflowStore() {
        loadAllFromDisk();
    }

    public void saveWorkflow(String owner, StoredWorkflow workflow) {
        store.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>()).add(workflow);
        persistUserWorkflows(owner);
    }

    public List<StoredWorkflow> getWorkflows(String owner) {
        return store.getOrDefault(owner, Collections.emptyList());
    }

    public StoredWorkflow getWorkflow(String owner, String workflowId) {
        return getWorkflows(owner).stream()
                .filter(w -> w.id().equals(workflowId))
                .findFirst().orElse(null);
    }

    public Set<String> getAllOwners() {
        return store.keySet();
    }

    public void addExecution(String owner, String workflowId, ExecutionRecord execRecord) {
        StoredWorkflow wf = getWorkflow(owner, workflowId);
        if (wf != null) {
            wf.executions().add(execRecord);
            persistExecution(workflowId, execRecord);
        }
    }

    public boolean deleteWorkflow(String owner, String workflowId) {
        List<StoredWorkflow> workflows = store.get(owner);
        if (workflows == null) return false;
        boolean removed = workflows.removeIf(w -> w.id().equals(workflowId));
        if (removed) {
            persistUserWorkflows(owner);
            try { Files.deleteIfExists(EXEC_DIR.resolve(workflowId + ".dat")); } catch (IOException ignored) {}
        }
        return removed;
    }

    // --- Persistence ---

    private void persistUserWorkflows(String owner) {
        try {
            Files.createDirectories(WF_DIR);
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(WF_DIR.resolve(owner + ".dat")))) {
                for (StoredWorkflow wf : getWorkflows(owner)) {
                    StringBuilder tasks = new StringBuilder();
                    for (int i = 0; i < wf.tasks().size(); i++) {
                        TaskInfo t = wf.tasks().get(i);
                        if (i > 0) tasks.append(";");
                        tasks.append(t.name()).append("~").append(t.type()).append("~");
                        t.params().forEach((k, v) -> tasks.append(k).append("=").append(v).append(","));
                    }
                    pw.println(wf.id() + "|" + wf.name() + "|" + wf.triggerType() + "|"
                            + wf.triggerValue() + "|" + wf.strategy() + "|" + wf.createdAt() + "|" + tasks);
                }
            }
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Save failed: " + e.getMessage());
        }
    }

    private void persistExecution(String workflowId, ExecutionRecord execRecord) {
        try {
            Files.createDirectories(EXEC_DIR);
            try (PrintWriter pw = new PrintWriter(new FileWriter(EXEC_DIR.resolve(workflowId + ".dat").toFile(), true))) {
                String logsEncoded = String.join("\\n", execRecord.logs().stream()
                        .map(l -> l.replace("|", "\u00a6").replace("\n", "\\n")).toList());
                pw.println(execRecord.executionId() + "|" + execRecord.status() + "|"
                        + execRecord.startedAt() + "|" + execRecord.finishedAt() + "|" + logsEncoded);
            }
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Exec save failed: " + e.getMessage());
        }
    }

    private void loadAllFromDisk() {
        if (!Files.exists(WF_DIR)) return;
        try (var files = Files.list(WF_DIR)) {
            files.filter(p -> p.toString().endsWith(".dat")).forEach(this::loadUserFile);
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Load failed: " + e.getMessage());
        }
    }

    private void loadUserFile(Path file) {
        String owner = file.getFileName().toString().replace(".dat", "");
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 7) continue;

                List<TaskInfo> tasks = parseTasks(parts[6]);
                List<ExecutionRecord> execs = loadExecutions(parts[0]);

                store.computeIfAbsent(owner, k -> new CopyOnWriteArrayList<>()).add(
                        new StoredWorkflow(parts[0], parts[1], owner, parts[2], parts[3],
                                parts[4], tasks, Long.parseLong(parts[5]),
                                new CopyOnWriteArrayList<>(execs)));
            }
            System.out.println("[WorkflowStore] Loaded " + getWorkflows(owner).size() + " workflows for: " + owner);
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Failed loading " + file + ": " + e.getMessage());
        }
    }

    private List<TaskInfo> parseTasks(String tasksStr) {
        List<TaskInfo> tasks = new ArrayList<>();
        if (tasksStr.isBlank()) return tasks;
        for (String tStr : tasksStr.split(";")) {
            String[] tParts = tStr.split("~", 3);
            if (tParts.length < 2) continue;
            Map<String, String> params = new HashMap<>();
            if (tParts.length == 3 && !tParts[2].isBlank()) {
                for (String kv : tParts[2].split(",")) {
                    String[] kvp = kv.split("=", 2);
                    if (kvp.length == 2) params.put(kvp[0], kvp[1]);
                }
            }
            tasks.add(new TaskInfo(tParts[0], tParts[1], params));
        }
        return tasks;
    }

    private List<ExecutionRecord> loadExecutions(String workflowId) {
        List<ExecutionRecord> execs = new ArrayList<>();
        Path file = EXEC_DIR.resolve(workflowId + ".dat");
        if (!Files.exists(file)) return execs;
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) continue;
                String[] parts = line.split("\\|", 5);
                if (parts.length < 5) continue;
                List<String> logs = new ArrayList<>(Arrays.asList(
                        parts[4].replace("\\n", "\n").replace("\u00a6", "|").split("\n")));
                execs.add(new ExecutionRecord(parts[0], parts[1],
                        Long.parseLong(parts[2]), Long.parseLong(parts[3]), logs));
            }
        } catch (IOException e) {
            System.err.println("[WorkflowStore] Failed loading executions for " + workflowId);
        }
        return execs;
    }
}