package org.lirdev.lirtitles;

import java.util.concurrent.CompletableFuture;

import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.lirdev.lirtitles.bukkit.BukkitDevelopmentNoticeListener;
import org.lirdev.lirtitles.commands.CommandTree;
import org.lirdev.lirtitles.commands.Subcommands;
import org.lirdev.lirtitles.commands.TitlesCommandRegistrar;
import org.lirdev.lirtitles.commands.implementation.player.OpenTitlesMenuCommand;
import org.lirdev.lirtitles.configuration.Configuration;
import org.lirdev.lirtitles.configuration.ConfigurationMigrator;
import org.lirdev.lirtitles.configuration.database.DatabaseConfiguration;
import org.lirdev.lirtitles.data.DatabaseExecutor;
import org.lirdev.lirtitles.data.PlayerTitleStorage;
import org.lirdev.lirtitles.data.bukkit.BukkitPlayerTitleCacheListener;
import org.lirdev.lirtitles.data.store.PlayerTitleStores;
import org.lirdev.lirtitles.fonts.FontRegistry;
import org.lirdev.lirtitles.menu.MenuCloser;
import org.lirdev.lirtitles.menu.MenuNavigator;
import org.lirdev.lirtitles.menu.bukkit.BukkitMenuClickListener;
import org.lirdev.lirtitles.menu.bukkit.BukkitMenuCloser;
import org.lirdev.lirtitles.menu.bukkit.BukkitMenuInventoryFactory;
import org.lirdev.lirtitles.menu.bukkit.BukkitMenuPresenter;
import org.lirdev.lirtitles.menu.bukkit.BukkitPlayerMenuStateListener;
import org.lirdev.lirtitles.menu.input.AnvilTextPrompt;
import org.lirdev.lirtitles.menu.input.bukkit.BukkitAnvilTextListener;
import org.lirdev.lirtitles.menu.input.bukkit.BukkitAnvilTextPrompt;
import org.lirdev.lirtitles.menu.input.bukkit.BukkitAnvilTextSessions;
import org.lirdev.lirtitles.menu.menus.TitleMenuController;
import org.lirdev.lirtitles.metadata.PluginInfo;
import org.lirdev.lirtitles.placeholders.PlaceholderRegistry;
import org.lirdev.lirtitles.utilities.BackgroundExecutor;
import org.lirdev.lirtitles.utilities.Branding;
import org.lirdev.lirtitles.utilities.Colorizer;
import org.lirdev.lirtitles.utilities.Logger;
import org.lirdev.lirtitles.utilities.UpdateChecker;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public final class LirTitles extends JavaPlugin {
    private MenuNavigator menuNavigator;
    private AnvilTextPrompt anvilTextPrompt;
    private BukkitAnvilTextSessions anvilTextSessions;
    private MenuCloser menuCloser;
    private Colorizer colorizer;
    private Logger logger;
    private Configuration configuration;
    private CompletableFuture<Configuration> configurationLoad;
    private PlayerTitleStorage titleStorage;
    private PlaceholderRegistry placeholderRegistry;
    private TitleMenuController titleMenus;
    private UpdateChecker updateChecker;
    private Metrics metrics;
    private FileConfiguration rawConfiguration;
    private CompletableFuture<PlayerTitleStores> titleStoresLoad;
    private CompletableFuture<Void> fontInstall;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        fontInstall = CompletableFuture.runAsync(() -> {
            FontRegistry.install(this);
        }, BackgroundExecutor.instance());
        rawConfiguration = new ConfigurationMigrator(this).loadAndMigrate();
        DatabaseConfiguration databaseConfiguration = DatabaseConfiguration.load(rawConfiguration);
        titleStoresLoad = PlayerTitleStorage.startLoading(this, databaseConfiguration);
        colorizer = new Colorizer();
        FileConfiguration initialConfiguration = rawConfiguration;
        Colorizer initialColorizer = colorizer;
        configurationLoad = CompletableFuture.supplyAsync(
                () -> new Configuration(this, initialColorizer, initialConfiguration), BackgroundExecutor.instance());
    }

    @Override
    public void onEnable() {
        long startedAt = System.nanoTime();
        BukkitMenuInventoryFactory menuInventoryFactory = new BukkitMenuInventoryFactory();
        menuNavigator = new BukkitMenuPresenter(menuInventoryFactory);
        anvilTextSessions = new BukkitAnvilTextSessions();
        anvilTextPrompt = new BukkitAnvilTextPrompt(anvilTextSessions);
        menuCloser = new BukkitMenuCloser(this, anvilTextSessions);
        fontInstall.join();

        configuration = configurationLoad.join();

        logger = new Logger(this);
        getServer().getPluginManager().registerEvents(new BukkitDevelopmentNoticeListener(this, logger), this);
        logger.raw("");
        logger.raw(Component.text(" LirTitles v" + getPluginMeta().getVersion(), Branding.PRIMARY, TextDecoration.BOLD));
        logger.raw(Component.textOfChildren(
                Component.text(" Running on "),
                Component.text(getServer().getName() + " - " + getServer().getVersion(), Branding.PRIMARY)));
        logger.raw("");
        logger.raw(" Configuration loaded.");
        logger.raw(Component.textOfChildren(
                Component.text(" Loaded "),
                Component.text(configuration.fonts().fonts().size() + " font(s).", Branding.PRIMARY)));
        logger.raw(" Setting up player data storage...");

        titleStorage = new PlayerTitleStorage(this, titleStoresLoad);

        logger.raw("   ↳ Storage: " + storageDescription());

        titleMenus = new TitleMenuController(this, titleStorage);
        getServer().getPluginManager().registerEvents(new BukkitPlayerTitleCacheListener(this, titleStorage), this);
        getServer().getPluginManager().registerEvents(new BukkitPlayerMenuStateListener(titleMenus), this);
        preloadOnlinePlayerData();
        getServer().getPluginManager().registerEvents(
                new BukkitMenuClickListener(this, menuNavigator, configuration::message, titleMenus), this);
        getServer().getPluginManager().registerEvents(new BukkitAnvilTextListener(this, anvilTextSessions), this);
        logger.raw(" Registered storage, menu.");

        OpenTitlesMenuCommand openTitlesMenu = new OpenTitlesMenuCommand(configuration, titleMenus);
        CommandTree commandTree = new CommandTree(
                PluginInfo.ID,
                java.util.List.of("lt"),
                configuration);
        Subcommands.configure(this, configuration, menuCloser, titleMenus, commandTree);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> {
                    commandTree.registerWith(event.registrar());
                    TitlesCommandRegistrar.register(event.registrar(), openTitlesMenu);
                });
        logger.raw(" Registered commands.");

        placeholderRegistry = new PlaceholderRegistry(this);
        boolean placeholderHooked = placeholderRegistry.register(titleStorage);
        if (placeholderHooked) {
            logger.raw(" Hooked into PlaceholderAPI.");
        } else {
            logger.raw(" PlaceholderAPI integration is unavailable.");
        }

        updateChecker = new UpdateChecker(this, logger, PluginInfo.ID, configuration.updateCheckerEnabled());
        updateChecker.start();

        metrics = new Metrics(this, PluginInfo.METRICS_ID);
        logger.raw(" Enabled bStats metrics.");

        if (configuration.updateCheckerEnabled()) {
            logger.raw(" Update checker enabled.");
        }
        logger.raw("");
        logger.raw(Component.textOfChildren(Component.space(), brandName(),
                Component.text(" has been enabled in ", NamedTextColor.WHITE),
                Component.text(elapsedMillis(startedAt) + "ms!", Branding.PRIMARY)));
        logger.raw("");
    }

    @Override
    public void onDisable() {
        Configuration.shutdownExecutor();
        BackgroundExecutor.shutdown();
        if (anvilTextSessions != null) {
            anvilTextSessions.disposeAll();
        }
        if (placeholderRegistry != null) {
            placeholderRegistry.unregister();
        }
        if (titleStorage != null) {
            titleStorage.close();
        }
        DatabaseExecutor.shutdown();
        if (logger != null) {
            logger.raw(Component.textOfChildren(brandName(), Component.text(" has been disabled!", NamedTextColor.WHITE)));
        }
    }

    public MenuNavigator menuNavigator() {
        return menuNavigator;
    }

    public AnvilTextPrompt anvilTextPrompt() {
        return anvilTextPrompt;
    }

    public Colorizer colorizer() {
        return colorizer;
    }

    public Logger logger() {
        return logger;
    }

    public Configuration configuration() {
        return configuration;
    }

    public PlayerTitleStorage titleStorage() {
        return titleStorage;
    }

    private String storageDescription() {
        return switch (configuration.database().type()) {
            case SQLITE -> "SQLite (" + configuration.database().sqliteFileName() + ")";
            case MYSQL -> "MySQL (" + configuration.database().mysql().host() + ":"
                    + configuration.database().mysql().port() + "/" + configuration.database().mysql().database() + ")";
        };
    }

    private static Component brandName() {
        return Component.text("LirTitles", Branding.PRIMARY, TextDecoration.BOLD);
    }

    private static long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private void preloadOnlinePlayerData() {
        getServer().getOnlinePlayers().forEach(player -> titleStorage.preloadAsync(player.getUniqueId()));
    }
}
