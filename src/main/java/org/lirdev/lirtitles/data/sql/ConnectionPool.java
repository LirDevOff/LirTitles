package org.lirdev.lirtitles.data.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

final class ConnectionPool implements ConnectionSource {
    private final ConnectionFactory connectionFactory;
    private final Semaphore permits;
    private final ConcurrentLinkedDeque<Connection> idleConnections = new ConcurrentLinkedDeque<>();
    private volatile boolean closed;

    ConnectionPool(ConnectionFactory connectionFactory, int maxConnections) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory");
        this.permits = new Semaphore(Math.max(1, maxConnections), true);
    }

    @Override
    public Connection connection() throws SQLException {
        if (closed) {
            throw new SQLException("Connection pool is closed");
        }
        acquirePermit();
        try {
            if (closed) {
                throw new SQLException("Connection pool is closed");
            }
            return PooledConnection.wrap(borrowIdleOrOpen(), this);
        } catch (SQLException | RuntimeException | Error exception) {
            permits.release();
            throw exception;
        }
    }

    void release(Connection physical, boolean discard) {
        if (discard || closed) {
            closeQuietly(physical);
        } else {
            idleConnections.addLast(physical);
        }
        permits.release();
    }

    @Override
    public void close() {
        closed = true;
        Connection idle = idleConnections.pollFirst();
        while (idle != null) {
            closeQuietly(idle);
            idle = idleConnections.pollFirst();
        }
    }

    private void acquirePermit() throws SQLException {
        try {
            permits.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SQLException("Interrupted while waiting for a database connection", exception);
        }
    }

    private Connection borrowIdleOrOpen() throws SQLException {
        Connection idle = idleConnections.pollFirst();
        while (idle != null) {
            if (isUsable(idle)) {
                return idle;
            }
            idle = idleConnections.pollFirst();
        }
        return connectionFactory.open();
    }

    private static boolean isUsable(Connection connection) {
        try {
            return !connection.isClosed();
        } catch (SQLException exception) {
            return false;
        }
    }

    private static void closeQuietly(Connection connection) {
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }
}
