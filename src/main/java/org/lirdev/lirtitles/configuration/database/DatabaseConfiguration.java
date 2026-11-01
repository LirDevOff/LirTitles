package org.lirdev.lirtitles.configuration.database;

import java.util.Locale;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public record DatabaseConfiguration(Type type, String sqliteFileName, int sqlitePoolSize, MySql mysql) {
    private static final int DEFAULT_SQLITE_POOL_SIZE = 4;
    private static final int DEFAULT_MYSQL_POOL_SIZE = 10;

    public static DatabaseConfiguration load(FileConfiguration configuration) {
        ConfigurationSection section = configuration.getConfigurationSection("storage");
        if (section == null) {
            throw new IllegalStateException("Missing configuration section 'storage'");
        }
        Type type = Type.from(section.getString("type", "sqlite"));
        String sqliteFileName = section.getString("sqlite.file", "data.db");
        int sqlitePoolSize = positiveOrDefault(section.getInt("sqlite.pool-size", DEFAULT_SQLITE_POOL_SIZE), DEFAULT_SQLITE_POOL_SIZE);
        MySql mysql = new MySql(
                section.getString("mysql.host", "localhost"),
                section.getInt("mysql.port", 3306),
                section.getString("mysql.database", "lirtitles"),
                section.getString("mysql.username", "root"),
                section.getString("mysql.password", ""),
                section.getString("mysql.parameters", "useSSL=false&useUnicode=true&characterEncoding=utf8"),
                positiveOrDefault(section.getInt("mysql.pool-size", DEFAULT_MYSQL_POOL_SIZE), DEFAULT_MYSQL_POOL_SIZE)
        );
        return new DatabaseConfiguration(type, sqliteFileName, sqlitePoolSize, mysql);
    }

    private static int positiveOrDefault(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    public enum Type {
        SQLITE,
        MYSQL;

        static Type from(String value) {
            try {
                return Type.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("Unknown storage type '" + value + "'");
            }
        }
    }

    public record MySql(String host, int port, String database, String username, String password, String parameters,
                        int poolSize) {
    }
}
