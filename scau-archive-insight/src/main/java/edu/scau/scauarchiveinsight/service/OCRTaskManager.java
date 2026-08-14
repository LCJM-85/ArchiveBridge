package edu.scau.scauarchiveinsight.service;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@Service
public class OCRTaskManager {

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())));
    private final Map<Integer, Future<?>> futures = new ConcurrentHashMap<>();
    private final Map<Integer, Process> processes = new ConcurrentHashMap<>();
    private final ThreadLocal<Integer> currentTaskId = new ThreadLocal<>();

    public void submit(Integer logId, Runnable task) {
        FutureTask<Void> future = new FutureTask<>(() -> {
            currentTaskId.set(logId);
            try {
                task.run();
            } finally {
                currentTaskId.remove();
                futures.remove(logId);
                processes.remove(logId);
            }
        }, null);
        futures.put(logId, future);
        executor.execute(future);
    }

    public Integer getCurrentTaskId() {
        return currentTaskId.get();
    }

    public void registerProcess(Process process) {
        Integer logId = currentTaskId.get();
        if (logId != null) processes.put(logId, process);
    }

    public void unregisterProcess(Process process) {
        Integer logId = currentTaskId.get();
        if (logId != null) processes.remove(logId, process);
    }

    public boolean cancel(Integer logId) {
        Process process = processes.remove(logId);
        if (process != null && process.isAlive()) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
        Future<?> future = futures.remove(logId);
        return future != null && future.cancel(true);
    }

    @PreDestroy
    public void shutdown() {
        processes.values().forEach(process -> {
            if (process.isAlive()) process.destroyForcibly();
        });
        executor.shutdownNow();
    }
}
