package org.lirdev.lirtitles.data.sql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SqliteConnectionSource implements ConnectionSource {
    private static final int BUSY_TIMEOUT_MILLIS = 5_000;
    private final ConnectionPool pool;
    private final AtomicBoolean journalModeInitialized = new AtomicBoolean();

    public SqliteConnectionSource(File databaseFile, int poolSize) {
        databaseFile.getAbsoluteFile().getParentFile().mkdirs();
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        pool = new ConnectionPool(() -> openConnection(url), poolSize);
    }

    @Override
    public Connection connection() throws SQLException {
        return pool.connection();
    }

    @Override
    public void close() {
        pool.close();
    }

    private Connection openConnection(String url) throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            initializeJournalMode(statement);
            statement.execute("PRAGMA synchronous=NORMAL");
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=" + BUSY_TIMEOUT_MILLIS);
        } catch (SQLException exception) {
            try {
                connection.close();
            } catch (SQLException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
        return connection;
    }

    private void initializeJournalMode(Statement statement) throws SQLException {
        if (journalModeInitialized.get()) {
            return;
        }
        synchronized (journalModeInitialized) {
            if (journalModeInitialized.get()) {
                return;
            }
            try (var resultSet = statement.executeQuery("PRAGMA journal_mode")) {
                if (!resultSet.next() || !"wal".equalsIgnoreCase(resultSet.getString(1))) {
                    statement.execute("PRAGMA journal_mode=WAL");
                }
            }
            journalModeInitialized.set(true);
        }
    }

}
