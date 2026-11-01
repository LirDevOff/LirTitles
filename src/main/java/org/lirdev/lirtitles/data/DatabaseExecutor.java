package org.lirdev.lirtitles.data;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DatabaseExecutor {
    private static final ExecutorService INSTANCE = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("lirtitles-database-", 0).factory());

    private DatabaseExecutor() {
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
}
