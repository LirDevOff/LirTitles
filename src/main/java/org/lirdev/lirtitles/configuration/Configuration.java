package org.lirdev.lirtitles.configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import org.lirdev.lirtitles.configuration.database.DatabaseConfiguration;
import org.lirdev.lirtitles.configuration.menus.CategoriesMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.ColorSelectionMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.ColorShadeMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.CustomTitleDeleteMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.CustomTitleEditorMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.CustomTitleInputMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.CustomTitlesMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.ModerationMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.TitleApplyMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.TitleSelectionMenuConfiguration;
import org.lirdev.lirtitles.configuration.menus.items.MenuItemLoader;
import org.lirdev.lirtitles.configuration.messages.Messages;
import org.lirdev.lirtitles.configuration.titles.CustomTitleSettings;
import org.lirdev.lirtitles.configuration.titles.Titles;
import org.lirdev.lirtitles.fonts.FontRegistry;
import org.lirdev.lirtitles.utilities.Colorizer;
import org.lirdev.lirtitles.utilities.PlaceholderReplacer;

public final class Configuration {
    private final JavaPlugin plugin;
    private final Colorizer colorizer;
    private final ConfigurationMigrator migrator;
    private final YamlConfigurationCache yamlCache;
    private final AtomicBoolean reloadInProgress = new AtomicBoolean();
    private volatile Snapshot current;
    private volatile String rootConfigurationSource;

    public Configuration(JavaPlugin plugin, Colorizer colorizer, FileConfiguration initial) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.colorizer = Objects.requireNonNull(colorizer, "colorizer");
        migrator = new ConfigurationMigrator(plugin);
        yamlCache = new YamlConfigurationCache(plugin);
        FileConfiguration configuration = Objects.requireNonNull(initial, "initial");
        current = buildSnapshot(configuration, null, Set.of(), null);
        rootConfigurationSource = configuration.saveToString();
    }

    public void load() {
        FileConfiguration configuration = migrator.loadAndMigrate();
        yamlCache.clear();
        current = buildSnapshot(configuration, null, Set.of(), null);
        rootConfigurationSource = configuration.saveToString();
    }

    public CompletableFuture<ReloadResult> loadAsync() {
        if (!reloadInProgress.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A configuration reload is already in progress"));
        }
        long startedAt = System.nanoTime();
        return CompletableFuture.supplyAsync(() -> {
            long fileLoadStartedAt = System.nanoTime();
            FileConfiguration configuration = migrator.loadAndMigrate();
            String source = configuration.saveToString();
            Snapshot previous = current;
            long fileLoadNanos = System.nanoTime() - fileLoadStartedAt;
            long changeCheckStartedAt = System.nanoTime();
            Set<String> changedYaml = yamlCache.findChanges();
            FontRegistry.ReloadResult fontReload = FontRegistry.reload(plugin, previous.sections().fonts());
            boolean rootChanged = !source.equals(rootConfigurationSource);
            long changeCheckNanos = System.nanoTime() - changeCheckStartedAt;
            if (!rootChanged && changedYaml.isEmpty() && !fontReload.changed()) {
                long durationNanos = System.nanoTime() - startedAt;
                logReload(durationNanos, fileLoadNanos, changeCheckNanos, 0, 0, false);
                return new ReloadResult(durationNanos, false);
            }
            long buildStartedAt = System.nanoTime();
            Snapshot reloaded = buildSnapshot(configuration, previous, changedYaml, fontReload.registry());
            long buildNanos = System.nanoTime() - buildStartedAt;
            current = reloaded;
            rootConfigurationSource = source;
            long durationNanos = System.nanoTime() - startedAt;
            logReload(durationNanos, fileLoadNanos, changeCheckNanos, buildNanos, changedYaml.size(), true);
            return new ReloadResult(durationNanos, true);
        }, ConfigurationExecutor.instance()).whenComplete((result, exception) -> {
            if (exception != null) {
                yamlCache.clear();
            }
            reloadInProgress.set(false);
        });
    }

    public record ReloadResult(long durationNanos, boolean changed) {
    }

    public static void shutdownExecutor() {
        ConfigurationExecutor.shutdown();
    }

    private Snapshot buildSnapshot(FileConfiguration configuration, Snapshot previous, Set<String> changedYaml,
                                   FontRegistry fonts) {
        Component snapshotPrefix = colorizer.colorize(requiredString(configuration, "prefix"));
        boolean snapshotTitleConfirmation = configuration.getBoolean("settings.title-confirmation");
        CustomTitleSettings snapshotCustomTitles = CustomTitleSettings.load(configuration);
        boolean snapshotUpdateCheckerEnabled = configuration.getBoolean("update-checker", true);
        Messages snapshotMessages = Messages.read(configuration, colorizer);
        DatabaseConfiguration snapshotDatabase = DatabaseConfiguration.load(configuration);
        Sections sections = loadSectionsConcurrently(previous == null ? null : previous.sections(), changedYaml, fonts);
        return new Snapshot(snapshotPrefix, snapshotTitleConfirmation, snapshotCustomTitles,
                snapshotUpdateCheckerEnabled, snapshotMessages, snapshotDatabase, sections);
    }

    private Sections loadSectionsConcurrently(Sections previous, Set<String> changedYaml, FontRegistry fonts) {
        ExecutorService executor = ConfigurationExecutor.instance();
        MenuItemLoader menuItemLoader = new MenuItemLoader(plugin, colorizer, yamlCache);
        boolean initialLoad = previous == null;
        CompletableFuture<Titles> titlesLoad = loadWhen(initialLoad || changedYaml.contains("titles.yml"),
                previous == null ? null : previous.titles(), () -> Titles.load(colorizer, yamlCache), executor);
        boolean navigationChanged = initialLoad || changedYaml.stream().anyMatch(path -> path.startsWith("menus/titles/"));
        CompletableFuture<NavigationMenus> navigationMenusLoad = loadWhen(navigationChanged, previous == null ? null
                : new NavigationMenus(previous.categoriesMenu(), previous.titleSelectionMenu(), previous.titleApplyMenu()),
                () -> new NavigationMenus(
                CategoriesMenuConfiguration.load(menuItemLoader),
                TitleSelectionMenuConfiguration.load(menuItemLoader),
                TitleApplyMenuConfiguration.load(menuItemLoader)), executor);
        boolean customTitlesChanged = initialLoad || changedYaml.stream().anyMatch(path -> path.startsWith("menus/custom-titles/"));
        CompletableFuture<CustomTitleMenus> customTitleMenusLoad = loadWhen(customTitlesChanged, previous == null ? null
                : new CustomTitleMenus(previous.customTitlesMenu(), previous.customTitleEditorMenu(),
                previous.customTitleInputMenu(), previous.customTitleDeleteMenu()),
                () -> new CustomTitleMenus(
                CustomTitlesMenuConfiguration.load(menuItemLoader),
                CustomTitleEditorMenuConfiguration.load(menuItemLoader),
                CustomTitleInputMenuConfiguration.load(menuItemLoader),
                CustomTitleDeleteMenuConfiguration.load(menuItemLoader)), executor);
        boolean colorsChanged = initialLoad || changedYaml.stream().anyMatch(path -> path.startsWith("menus/colors/")
                || path.startsWith("menus/moderation/"));
        CompletableFuture<ColorMenus> colorMenusLoad = loadWhen(colorsChanged, previous == null ? null
                : new ColorMenus(previous.colorSelectionMenu(), previous.colorShadeMenu(), previous.moderationMenu()),
                () -> new ColorMenus(
                ColorSelectionMenuConfiguration.load(menuItemLoader),
                ColorShadeMenuConfiguration.load(menuItemLoader),
                ModerationMenuConfiguration.load(menuItemLoader)), executor);
        CompletableFuture<FontRegistry> fontsLoad = fonts == null
                ? CompletableFuture.supplyAsync(() -> FontRegistry.load(plugin), executor)
                : CompletableFuture.completedFuture(fonts);
        CompletableFuture.allOf(titlesLoad, navigationMenusLoad, customTitleMenusLoad, colorMenusLoad, fontsLoad).join();
        NavigationMenus navigation = join(navigationMenusLoad);
        CustomTitleMenus customTitles = join(customTitleMenusLoad);
        ColorMenus colors = join(colorMenusLoad);
        return new Sections(
                join(titlesLoad),
                navigation.categories(),
                navigation.titleSelection(),
                navigation.titleApply(),
                customTitles.customTitles(),
                customTitles.editor(),
                customTitles.input(),
                customTitles.delete(),
                colors.selection(),
                colors.shades(),
                colors.moderation(),
                join(fontsLoad));
    }

    private static <T> CompletableFuture<T> loadWhen(boolean changed, T previous, java.util.function.Supplier<T> loader,
                                                      ExecutorService executor) {
        if (!changed) {
            return CompletableFuture.completedFuture(Objects.requireNonNull(previous, "previous"));
        }
        return CompletableFuture.supplyAsync(loader, executor);
    }

    private void logReload(long durationNanos, long fileLoadNanos, long changeCheckNanos, long buildNanos,
                           int changedYamlCount, boolean changed) {
        plugin.getLogger().fine("Configuration reload " + (changed ? "applied" : "unchanged")
                + " in " + durationNanos / 1_000_000.0 + " ms (root=" + fileLoadNanos / 1_000_000.0
                + " ms, change-check=" + changeCheckNanos / 1_000_000.0 + " ms, build="
                + buildNanos / 1_000_000.0 + " ms, changed-yaml=" + changedYamlCount + ")");
    }

    private record NavigationMenus(
            CategoriesMenuConfiguration categories,
            TitleSelectionMenuConfiguration titleSelection,
            TitleApplyMenuConfiguration titleApply) {
    }

    private record CustomTitleMenus(
            CustomTitlesMenuConfiguration customTitles,
            CustomTitleEditorMenuConfiguration editor,
            CustomTitleInputMenuConfiguration input,
            CustomTitleDeleteMenuConfiguration delete) {
    }

    private record ColorMenus(
            ColorSelectionMenuConfiguration selection,
            ColorShadeMenuConfiguration shades,
            ModerationMenuConfiguration moderation) {
    }

    private record Sections(
            Titles titles,
            CategoriesMenuConfiguration categoriesMenu,
            TitleSelectionMenuConfiguration titleSelectionMenu,
            TitleApplyMenuConfiguration titleApplyMenu,
            CustomTitlesMenuConfiguration customTitlesMenu,
            CustomTitleEditorMenuConfiguration customTitleEditorMenu,
            CustomTitleInputMenuConfiguration customTitleInputMenu,
            CustomTitleDeleteMenuConfiguration customTitleDeleteMenu,
            ColorSelectionMenuConfiguration colorSelectionMenu,
            ColorShadeMenuConfiguration colorShadeMenu,
            ModerationMenuConfiguration moderationMenu,
            FontRegistry fonts) {
    }

    private record Snapshot(
            Component prefix,
            boolean titleConfirmation,
            CustomTitleSettings customTitles,
            boolean updateCheckerEnabled,
            Messages messages,
            DatabaseConfiguration database,
            Sections sections) {
    }

    private static <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            throw exception.getCause() instanceof RuntimeException runtimeException ? runtimeException : exception;
        }
    }

    public boolean titleConfirmation() {
        return current.titleConfirmation();
    }

    public CustomTitleSettings customTitles() {
        return current.customTitles();
    }

    public boolean updateCheckerEnabled() {
        return current.updateCheckerEnabled();
    }

    public Messages messages() {
        return current.messages();
    }

    public Titles titles() {
        return current.sections().titles();
    }

    public CategoriesMenuConfiguration categoriesMenu() {
        return current.sections().categoriesMenu();
    }

    public TitleSelectionMenuConfiguration titleSelectionMenu() {
        return current.sections().titleSelectionMenu();
    }

    public TitleApplyMenuConfiguration titleApplyMenu() {
        return current.sections().titleApplyMenu();
    }

    public CustomTitlesMenuConfiguration customTitlesMenu() {
        return current.sections().customTitlesMenu();
    }

    public CustomTitleEditorMenuConfiguration customTitleEditorMenu() {
        return current.sections().customTitleEditorMenu();
    }

    public CustomTitleInputMenuConfiguration customTitleInputMenu() {
        return current.sections().customTitleInputMenu();
    }

    public CustomTitleDeleteMenuConfiguration customTitleDeleteMenu() {
        return current.sections().customTitleDeleteMenu();
    }

    public ColorSelectionMenuConfiguration colorSelectionMenu() {
        return current.sections().colorSelectionMenu();
    }

    public ColorShadeMenuConfiguration colorShadeMenu() {
        return current.sections().colorShadeMenu();
    }

    public ModerationMenuConfiguration moderationMenu() {
        return current.sections().moderationMenu();
    }

    public FontRegistry fonts() {
        return current.sections().fonts();
    }

    public DatabaseConfiguration database() {
        return current.database();
    }

    public Component message(Component template) {
        return message(template, Map.of());
    }

    public Component message(List<Component> lines) {
        return message(Component.join(JoinConfiguration.newlines(), lines));
    }

    public Component message(Component template, Map<String, ? extends ComponentLike> placeholders) {
        Snapshot snapshot = current;
        Map<String, ComponentLike> values = new LinkedHashMap<>();
        values.put("prefix", snapshot.prefix());
        placeholders.forEach((name, value) -> values.put(
                Objects.requireNonNull(name, "placeholder name"),
                Objects.requireNonNull(value, "placeholder value")));
        return PlaceholderReplacer.replace(Objects.requireNonNull(template, "template"), values);
    }

    private static String requiredString(FileConfiguration configuration, String path) {
        String value = configuration.getString(path);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing non-blank string configuration value '" + path + "'");
        }
        return value;
    }
}
