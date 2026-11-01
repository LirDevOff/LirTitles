package org.lirdev.lirtitles.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class ConfigurationExecutor {
    private static final int MAX_THREAD_COUNT = 6;
    private static final int THREAD_COUNT = Math.max(2, Math.min(MAX_THREAD_COUNT, Runtime.getRuntime().availableProcessors()));
    private static final ExecutorService INSTANCE = Executors.newFixedThreadPool(THREAD_COUNT, new ConfigThreadFactory());

    private ConfigurationExecutor() {
    }

    static ExecutorService instance() {
        return INSTANCE;
    }

    static void shutdown() {
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

    private static final class ConfigThreadFactory implements ThreadFactory {
        private final AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, "lirtitles-config-load-" + nextId.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
