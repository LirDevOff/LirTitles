package org.lirdev.lirtitles.utilities;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class BackgroundExecutor {
    private static final int MAX_THREADS = 8;
    private static final int THREAD_COUNT = Math.max(2, Math.min(MAX_THREADS, Runtime.getRuntime().availableProcessors()));
    private static final ExecutorService INSTANCE = Executors.newFixedThreadPool(THREAD_COUNT, new NamedDaemonThreadFactory());

    private BackgroundExecutor() {
    }

    public static ExecutorService instance() {
        return INSTANCE;
    }

    public static void shutdown() {
        INSTANCE.shutdown();
        try {
            if (!INSTANCE.awaitTermination(5, TimeUnit.SECONDS)) {
                INSTANCE.shutdownNow();
            }
        } catch (InterruptedException exception) {
            INSTANCE.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static final class NamedDaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "lirtitles-file-load-" + nextId.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
