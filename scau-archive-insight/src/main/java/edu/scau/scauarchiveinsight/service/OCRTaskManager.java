package edu.scau.scauarchiveinsight.service;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

@Service
public class OCRTaskManager {

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors())));
    private final Map<Integer, Future<?>> futures = new ConcurrentHashMap<>();
    private final Map<Integer, Process> processes = new ConcurrentHashMap<>();
    private final ThreadLocal<Integer> currentTaskId = new ThreadLocal<>();
    private final Map<Integer, TaskState> states = new ConcurrentHashMap<>();
    private final ThreadLocal<TaskState> currentState = new ThreadLocal<>();

    private static class TaskState {
        boolean running;
        boolean cancelled;
        boolean persisting;
    }

    public void submit(Integer logId, Runnable task) {
        TaskState state = new TaskState();
        FutureTask<Void> future = new FutureTask<>(() -> {
            synchronized (state) {
                if (state.cancelled) return;
                state.running = true;
            }
            currentTaskId.set(logId);
            currentState.set(state);
            try {
                task.run();
            } finally {
                currentTaskId.remove();
                currentState.remove();
                futures.remove(logId);
                processes.remove(logId);
                states.remove(logId, state);
            }
        }, null);
        futures.put(logId, future);
        states.put(logId, state);
        executor.execute(future);
    }

    public Integer getCurrentTaskId() {
        return currentTaskId.get();
    }

    public void registerProcess(Process process) {
        Integer logId = currentTaskId.get();
        TaskState state = currentState.get();
        if (logId != null && state != null) {
            synchronized (state) {
                if (state.cancelled) {
                    destroyProcess(process);
                    throw new CancellationException("任务已取消");
                }
                processes.put(logId, process);
            }
        }
    }

    public void unregisterProcess(Process process) {
        Integer logId = currentTaskId.get();
        if (logId != null) processes.remove(logId, process);
    }

    public boolean cancel(Integer logId) {
        return cancel(logId, () -> true);
    }

    /** 与入库入口共用锁：只有入库前才能取消，日志更新与取消决策保持一致。 */
    public boolean cancel(Integer logId, BooleanSupplier markCancelled) {
        TaskState state = states.get(logId);
        if (state == null) return false;
        synchronized (state) {
            Future<?> future = futures.get(logId);
            if (state.cancelled || state.persisting || future == null || future.isDone()) return false;
            if (!markCancelled.getAsBoolean()) return false;
            state.cancelled = true;
            Process process = processes.remove(logId);
            if (process != null) destroyProcess(process);
            future.cancel(true);
            futures.remove(logId, future);
            if (!state.running) states.remove(logId, state);
            return true;
        }
    }

    public void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("任务已中断");
        TaskState state = currentState.get();
        if (state != null) {
            synchronized (state) {
                if (state.cancelled) throw new CancellationException("任务已取消");
            }
        }
    }

    /** 在任何数据库写入前建立不可取消边界，并一直保留至任务退出。 */
    public void beginPersistence() {
        checkCancelled();
        TaskState state = currentState.get();
        if (state != null) {
            synchronized (state) {
                if (state.cancelled) throw new CancellationException("任务已取消");
                state.persisting = true;
            }
        }
    }

    private void destroyProcess(Process process) {
        if (process.isAlive()) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }
    }

    @PreDestroy
    public void shutdown() {
        processes.values().forEach(process -> {
            if (process.isAlive()) process.destroyForcibly();
        });
        executor.shutdownNow();
    }
}
