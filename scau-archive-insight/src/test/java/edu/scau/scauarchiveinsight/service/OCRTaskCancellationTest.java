package edu.scau.scauarchiveinsight.service;

import org.junit.jupiter.api.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OCRTaskCancellationTest {
    @Test
    void acceptedCancellationSurvivesClearedInterruptFlag() throws Exception {
        OCRTaskManager tasks = new OCRTaskManager();
        CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        try {
            tasks.submit(1, () -> {
                started.countDown();
                try { release.await(); } catch (InterruptedException ignored) { /* 模拟底层库吞掉中断。 */ }
                Thread.interrupted();
                try { tasks.beginPersistence(); } catch (Throwable error) { outcome.set(error); }
                finally { done.countDown(); }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertTrue(tasks.cancel(1));
            release.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertInstanceOf(CancellationException.class, outcome.get());
        } finally { release.countDown(); tasks.shutdown(); }
    }

    @Test
    void cancellationAfterPersistenceGateDoesNotChangeLogOrInterruptWorker() throws Exception {
        OCRTaskManager tasks = new OCRTaskManager();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicInteger logChanges = new AtomicInteger();
        AtomicBoolean interrupted = new AtomicBoolean();
        try {
            tasks.submit(2, () -> {
                tasks.beginPersistence();
                entered.countDown();
                try { release.await(); } catch (InterruptedException error) { interrupted.set(true); }
                finally { done.countDown(); }
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            assertFalse(tasks.cancel(2, () -> { logChanges.incrementAndGet(); return true; }));
            assertEquals(0, logChanges.get());
            release.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertFalse(interrupted.get());
        } finally { release.countDown(); tasks.shutdown(); }
    }

    @Test
    void processRegisteredAfterCancellationIsDestroyedInsteadOfLeaking() throws Exception {
        OCRTaskManager tasks = new OCRTaskManager();
        Process process = mock(Process.class);
        when(process.isAlive()).thenReturn(true);
        when(process.descendants()).thenReturn(java.util.stream.Stream.empty());
        CountDownLatch started = new CountDownLatch(1), done = new CountDownLatch(1);
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        try {
            tasks.submit(3, () -> {
                started.countDown();
                try { new CountDownLatch(1).await(); } catch (InterruptedException ignored) {}
                try { tasks.registerProcess(process); } catch (Throwable error) { outcome.set(error); }
                finally { done.countDown(); }
            });
            assertTrue(started.await(5, TimeUnit.SECONDS));
            assertTrue(tasks.cancel(3));
            assertTrue(done.await(5, TimeUnit.SECONDS));
            assertInstanceOf(CancellationException.class, outcome.get());
            verify(process).destroyForcibly();
        } finally { tasks.shutdown(); }
    }

    @Test
    void cancelAndPersistenceRaceHaveOnlyOneWinner() throws Exception {
        OCRTaskManager tasks = new OCRTaskManager();
        try {
            for (int id = 10; id < 30; id++) {
                CountDownLatch started = new CountDownLatch(1), race = new CountDownLatch(1), done = new CountDownLatch(1);
                AtomicBoolean persisted = new AtomicBoolean();
                AtomicReference<Throwable> error = new AtomicReference<>();
                tasks.submit(id, () -> {
                    started.countDown();
                    try { race.await(); } catch (InterruptedException ignored) {}
                    try { tasks.beginPersistence(); persisted.set(true); }
                    catch (Throwable failure) { error.set(failure); }
                    finally { done.countDown(); }
                });
                assertTrue(started.await(5, TimeUnit.SECONDS));
                race.countDown();
                boolean cancelled = tasks.cancel(id);
                assertTrue(done.await(5, TimeUnit.SECONDS));
                assertEquals(!cancelled, persisted.get());
                if (cancelled) assertInstanceOf(CancellationException.class, error.get());
            }
        } finally { tasks.shutdown(); }
    }
}
