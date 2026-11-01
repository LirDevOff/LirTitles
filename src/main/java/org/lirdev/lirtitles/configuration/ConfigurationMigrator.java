package org.lirdev.lirtitles.configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigurationMigrator {
    private static final int CURRENT_VERSION = 1;
    public static final String CONFIG_FILE_NAME = "config.yml";
    private final JavaPlugin plugin;

    public ConfigurationMigrator(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public FileConfiguration loadAndMigrate() {
        File configurationFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(configurationFile);
        migrate(configuration);
        return configuration;
    }

    public boolean migrate(FileConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        int version = configuration.getInt("config-version", 0);
        if (version >= CURRENT_VERSION) {
            return false;
        }
        File configurationFile = new File(plugin.getDataFolder(), CONFIG_FILE_NAME);
        createBackup(configurationFile);
        FileConfiguration defaults = defaultConfiguration();
        mergeMissingValues(configuration, defaults);
        configuration.set("config-version", CURRENT_VERSION);
        save(configuration, configurationFile);
        return true;
    }

    private FileConfiguration defaultConfiguration() {
        InputStream resource = plugin.getResource(CONFIG_FILE_NAME);
        if (resource == null) {
            throw new IllegalStateException("Missing bundled " + CONFIG_FILE_NAME);
        }
        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            return YamlConfiguration.loadConfiguration(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bundled " + CONFIG_FILE_NAME, exception);
        }
    }

    private static void mergeMissingValues(FileConfiguration target, FileConfiguration defaults) {
        for (String path : defaults.getKeys(true)) {
            if (!target.contains(path)) {
                target.set(path, defaults.get(path));
            }
        }
    }

    private void createBackup(File configurationFile) {
        if (!configurationFile.isFile()) {
            return;
        }
        File backupFile = new File(plugin.getDataFolder(), "config-backup-v" + System.currentTimeMillis() + ".yml");
        try {
            Files.copy(configurationFile.toPath(), backupFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not back up " + configurationFile.getName(), exception);
        }
    }

    private static void save(FileConfiguration configuration, File file) {
        try {
            configuration.save(file);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save " + file.getName(), exception);
        }
    }
}
