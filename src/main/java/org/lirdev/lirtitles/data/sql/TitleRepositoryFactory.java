package org.lirdev.lirtitles.data.sql;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

import org.lirdev.lirtitles.configuration.database.DatabaseConfiguration;
import org.lirdev.lirtitles.data.store.PlayerTitleStores;

public final class TitleRepositoryFactory {
    private TitleRepositoryFactory() {
    }

    public static PlayerTitleStores create(JavaPlugin plugin, DatabaseConfiguration configuration) {
        ConnectionSource connectionSource = switch (configuration.type()) {
            case SQLITE -> new SqliteConnectionSource(
                    new File(plugin.getDataFolder(), configuration.sqliteFileName()), configuration.sqlitePoolSize());
            case MYSQL -> new MySqlConnectionSource(configuration.mysql());
        };
        try {
            SqlTitleRepository repository = new SqlTitleRepository(connectionSource);
            int maxCustomTitleId = repository.maxNumericId();
            return new PlayerTitleStores(repository, repository, repository, repository, repository, repository, repository,
                    maxCustomTitleId, repository);
        } catch (RuntimeException exception) {
            connectionSource.close();
            throw exception;
        }
    }
}
