package org.lirdev.lirtitles.data.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.lirdev.lirtitles.configuration.database.DatabaseConfiguration;

public final class MySqlConnectionSource implements ConnectionSource {
    private final ConnectionPool pool;

    public MySqlConnectionSource(DatabaseConfiguration.MySql settings) {
        String parameters = settings.parameters();
        String url = "jdbc:mysql://" + settings.host() + ":" + settings.port() + "/" + settings.database()
                + (parameters.isBlank() ? "" : "?" + parameters);
        pool = new ConnectionPool(() -> DriverManager.getConnection(url, settings.username(), settings.password()), settings.poolSize());
    }

    @Override
    public Connection connection() throws SQLException {
        return pool.connection();
    }

    @Override
    public void close() {
        pool.close();
    }
}
