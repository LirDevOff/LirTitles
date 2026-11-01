package org.lirdev.lirtitles.data;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import org.lirdev.lirtitles.configuration.database.DatabaseConfiguration;
import org.lirdev.lirtitles.data.model.CustomTitle;
import org.lirdev.lirtitles.data.model.CustomTitleStatus;
import org.lirdev.lirtitles.data.model.PendingModerationEntry;
import org.lirdev.lirtitles.data.model.PlayerTitleData;
import org.lirdev.lirtitles.data.model.PlayerTitleSnapshot;
import org.lirdev.lirtitles.data.sql.TitleRepositoryFactory;
import org.lirdev.lirtitles.data.store.CustomTitleModerationStore;
import org.lirdev.lirtitles.data.store.CustomTitleStore;
import org.lirdev.lirtitles.data.store.PlayerTitleDataStore;
import org.lirdev.lirtitles.data.store.CustomTitleTokenStore;
import org.lirdev.lirtitles.data.store.PlayerTitleStores;
import org.lirdev.lirtitles.data.store.SelectedFontStore;
import org.lirdev.lirtitles.data.store.SelectedTitleStore;
import org.lirdev.lirtitles.data.store.TitleOwnershipStore;
import org.lirdev.lirtitles.permissions.Permissions;

public final class PlayerTitleStorage implements PlayerTitleSnapshot {
    private static final Duration SHUTDOWN_WAIT_TIMEOUT = Duration.ofSeconds(5);

    private final JavaPlugin plugin;
    private final TitleOwnershipStore ownership;
    private final SelectedTitleStore selection;
    private final SelectedFontStore fonts;
    private final CustomTitleStore customTitleStore;
    private final CustomTitleModerationStore moderationStore;
    private final CustomTitleTokenStore tokenStore;
    private final PlayerTitleDataStore playerDataStore;
    private final AutoCloseable closer;
    private final Map<UUID, Set<String>> ownedTitles = new ConcurrentHashMap<>();
    private final Map<UUID, String> selectedTitles = new ConcurrentHashMap<>();
    private final Map<UUID, String> selectedFonts = new ConcurrentHashMap<>();
    private final Map<UUID, CustomTitlesCache> customTitles = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> customTitleTokens = new ConcurrentHashMap<>();
    private final Set<UUID> loadedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, CompletableFuture<Void>> preloadTasks = new ConcurrentHashMap<>();
    private final Set<CompletableFuture<Void>> activePreloads = ConcurrentHashMap.newKeySet();
    private final Map<UUID, CompletableFuture<Void>> pendingWritesByPlayer = new ConcurrentHashMap<>();
    private final AtomicInteger nextCustomTitleNumericId;
    private final AtomicInteger pendingWrites = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();

    public PlayerTitleStorage(JavaPlugin plugin, DatabaseConfiguration databaseConfiguration) {
        this(plugin, startLoading(plugin, databaseConfiguration));
    }

    public PlayerTitleStorage(JavaPlugin plugin, CompletableFuture<PlayerTitleStores> storesLoad) {
        this.plugin = plugin;
        PlayerTitleStores stores = join(storesLoad);
        ownership = stores.ownership();
        selection = stores.selection();
        fonts = stores.fonts();
        customTitleStore = stores.customTitles();
        moderationStore = stores.customTitleModeration();
        tokenStore = stores.customTitleTokens();
        playerDataStore = stores.playerData();
        closer = stores.closer();
        nextCustomTitleNumericId = new AtomicInteger(stores.maxCustomTitleId());
    }

    public static CompletableFuture<PlayerTitleStores> startLoading(JavaPlugin plugin, DatabaseConfiguration databaseConfiguration) {
        return CompletableFuture.supplyAsync(() -> TitleRepositoryFactory.create(plugin, databaseConfiguration), DatabaseExecutor.instance());
    }

    private static PlayerTitleStores join(CompletableFuture<PlayerTitleStores> storesLoad) {
        try {
            return storesLoad.join();
        } catch (CompletionException exception) {
            throw exception.getCause() instanceof RuntimeException runtimeException ? runtimeException : exception;
        }
    }

    public boolean owns(UUID playerId, String titleId) {
        return owned(playerId).contains(titleId);
    }

    public Set<String> owned(UUID playerId) {
        return ownedTitles.getOrDefault(playerId, Set.of());
    }

    public boolean give(UUID playerId, String titleId) {
        if (!loadedPlayers.contains(playerId)) {
            preloadAsync(playerId).thenRun(() -> give(playerId, titleId));
            return true;
        }
        Set<String> titles = owned(playerId);
        if (titles.contains(titleId)) {
            return false;
        }
        ownedTitles.computeIfPresent(playerId, (ignored, current) -> withTitle(current, titleId));
        runAsync(playerId, () -> ownership.give(playerId, titleId));
        return true;
    }

    public String selected(UUID playerId) {
        return selectedTitles.getOrDefault(playerId, "");
    }

    public boolean select(UUID playerId, String titleId) {
        return select(playerId, titleId, false);
    }

    public boolean select(Player player, String titleId) {
        return select(player.getUniqueId(), titleId, player.hasPermission(Permissions.UNLOCK_ALL));
    }

    public boolean select(Player player, String titleId, String fontId) {
        UUID playerId = player.getUniqueId();
        if (!loadedPlayers.contains(playerId)) {
            preloadAsync(playerId).thenRun(() -> select(player, titleId, fontId));
            return false;
        }
        if (!player.hasPermission(Permissions.UNLOCK_ALL) && !owns(playerId, titleId)) {
            return false;
        }
        selectedTitles.put(playerId, titleId);
        selectedFonts.put(playerId, fontId);
        runAsync(playerId, () -> selection.select(playerId, titleId, fontId));
        return true;
    }

    private boolean select(UUID playerId, String titleId, boolean bypassOwnership) {
        if (!loadedPlayers.contains(playerId)) {
            preloadAsync(playerId).thenRun(() -> select(playerId, titleId, bypassOwnership));
            return false;
        }
        if (!bypassOwnership && !owns(playerId, titleId)) {
            return false;
        }
        selectedTitles.put(playerId, titleId);
        runAsync(playerId, () -> selection.select(playerId, titleId));
        return true;
    }

    public String selectedFont(UUID playerId) {
        return selectedFonts.getOrDefault(playerId, "");
    }

    public void selectFont(UUID playerId, String fontId) {
        selectedFonts.put(playerId, fontId);
        runAsync(playerId, () -> fonts.selectFont(playerId, fontId));
    }

    @Override
    public String cachedSelectedTitle(UUID playerId) {
        return selectedTitles.getOrDefault(playerId, "");
    }

    @Override
    public String cachedSelectedFont(UUID playerId) {
        return selectedFonts.getOrDefault(playerId, "");
    }

    @Override
    public CustomTitle cachedCustomTitle(UUID playerId, UUID titleId) {
        CustomTitlesCache cachedTitles = customTitles.get(playerId);
        return cachedTitles == null ? null : cachedTitles.byId().get(titleId);
    }

    public List<CustomTitle> customTitles(UUID playerId) {
        return customTitles.getOrDefault(playerId, CustomTitlesCache.EMPTY).ordered();
    }

    public CustomTitle createCustomTitle(UUID playerId, String text, int slot, CustomTitleStatus status) {
        return createCustomTitle(playerId, playerId.toString().substring(0, 8), text, slot, status);
    }

    public CustomTitle createCustomTitle(UUID playerId, String playerName, String text, int slot, CustomTitleStatus status) {
        CustomTitle title = new CustomTitle(UUID.randomUUID(), nextCustomTitleNumericId.incrementAndGet(), text, slot, status, "", 0);
        customTitles.computeIfPresent(playerId, (ignored, titles) -> titles.withTitle(title));
        runAsync(playerId, () -> customTitleStore.createCustomTitle(playerId, title, playerName));
        return title;
    }

    public boolean updateCustomTitle(UUID playerId, UUID titleId, String text, CustomTitleStatus status) {
        customTitles.computeIfPresent(playerId, (ignored, titles) -> titles.update(titleId, title -> title.withText(text, status)));
        runAsync(playerId, () -> customTitleStore.updateCustomTitle(playerId, titleId, text, status));
        return true;
    }

    public List<PendingModerationEntry> pendingModeration() {
        return moderationStore.pendingModeration();
    }

    public CompletableFuture<Void> decideCustomTitle(UUID playerId, UUID titleId, CustomTitleStatus status, String reason, long decidedAt) {
        CustomTitle previousTitle = cachedCustomTitle(playerId, titleId);
        CustomTitle decidedTitle = previousTitle == null ? null : previousTitle.withStatus(status, reason, decidedAt);
        if (decidedTitle != null) {
            customTitles.computeIfPresent(playerId, (ignored, titles) -> titles.update(titleId, ignoredTitle -> decidedTitle));
        }
        CompletableFuture<Void> decision = runAsync(playerId, () -> {
            if (!moderationStore.decideCustomTitle(playerId, titleId, status, reason, decidedAt)) {
                throw new IllegalStateException("Custom title " + titleId + " no longer exists");
            }
        });
        if (decidedTitle == null) {
            return decision;
        }
        return decision.whenComplete((ignored, exception) -> {
            if (exception != null) {
                customTitles.computeIfPresent(playerId, (unused, titles) -> titles.replaceIfCurrent(titleId, decidedTitle, previousTitle));
            }
        });
    }

    public boolean deleteCustomTitle(UUID playerId, UUID titleId) {
        CustomTitlesCache currentTitles = customTitles.get(playerId);
        if (currentTitles == null || !currentTitles.byId().containsKey(titleId)) {
            return false;
        }
        customTitles.computeIfPresent(playerId, (ignored, titles) -> titles.without(titleId));
        selectedTitles.computeIfPresent(playerId, (ignored, selected) -> selected.equals(CustomTitle.keyFor(titleId)) ? "" : selected);
        runAsync(playerId, () -> customTitleStore.deleteCustomTitle(playerId, titleId));
        return true;
    }

    public CustomTitle customTitle(UUID playerId, UUID titleId) {
        CustomTitlesCache cachedTitles = customTitles.get(playerId);
        if (cachedTitles == null) {
            return customTitleStore.customTitle(playerId, titleId);
        }
        return cachedTitles.byId().get(titleId);
    }

    public boolean selectCustomTitle(UUID playerId, UUID titleId) {
        CustomTitle title = customTitle(playerId, titleId);
        if (title == null || title.status() != CustomTitleStatus.APPROVED) {
            return false;
        }
        selectedTitles.put(playerId, CustomTitle.keyFor(titleId));
        runAsync(playerId, () -> customTitleStore.selectCustomTitle(playerId, titleId));
        return true;
    }

    public int customTitleTokens(UUID playerId) {
        return customTitleTokens.getOrDefault(playerId, 0);
    }

    public void addCustomTitleTokens(UUID playerId, int amount) {
        if (!loadedPlayers.contains(playerId)) {
            preloadAsync(playerId).thenRun(() -> addCustomTitleTokens(playerId, amount));
            return;
        }
        customTitleTokens.compute(playerId, (ignored, current) -> (current == null ? 0 : current) + amount);
        runAsync(playerId, () -> tokenStore.addCustomTitleTokens(playerId, amount));
    }

    public boolean consumeCustomTitleToken(UUID playerId) {
        java.util.concurrent.atomic.AtomicBoolean consumed = new java.util.concurrent.atomic.AtomicBoolean();
        customTitleTokens.computeIfPresent(playerId, (ignored, current) -> {
            if (current <= 0) {
                return current;
            }
            consumed.set(true);
            return current - 1;
        });
        if (!consumed.get()) {
            return false;
        }
        runAsync(playerId, () -> tokenStore.consumeCustomTitleToken(playerId));
        return true;
    }

    public CompletableFuture<Void> preloadAsync(UUID playerId) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Title storage is closed"));
        }
        if (loadedPlayers.contains(playerId)) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Void> preload = new CompletableFuture<>();
        CompletableFuture<Void> existing = preloadTasks.putIfAbsent(playerId, preload);
        if (existing != null) {
            return existing;
        }
        activePreloads.add(preload);
        DatabaseExecutor.instance().execute(() -> {
            try {
                PlayerTitleData data = playerDataStore.loadPlayerData(playerId);
                if (preloadTasks.get(playerId) != preload) {
                    preload.complete(null);
                    return;
                }
                ownedTitles.put(playerId, data.ownedTitles());
                selectedTitles.put(playerId, data.selectedTitle());
                selectedFonts.put(playerId, data.selectedFont());
                customTitles.put(playerId, CustomTitlesCache.from(data.customTitles()));
                customTitleTokens.put(playerId, data.customTitleTokens());
                loadedPlayers.add(playerId);
                preload.complete(null);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not preload title data for " + playerId, exception);
                preload.completeExceptionally(exception);
            } finally {
                preloadTasks.remove(playerId, preload);
                activePreloads.remove(preload);
            }
        });
        return preload;
    }

    public boolean isLoaded(UUID playerId) {
        return loadedPlayers.contains(playerId);
    }

    public void invalidate(UUID playerId) {
        loadedPlayers.remove(playerId);
        preloadTasks.remove(playerId);
        ownedTitles.remove(playerId);
        selectedTitles.remove(playerId);
        selectedFonts.remove(playerId);
        customTitles.remove(playerId);
        customTitleTokens.remove(playerId);
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        long deadline = System.nanoTime() + SHUTDOWN_WAIT_TIMEOUT.toNanos();
        CompletableFuture<?>[] pendingPreloads = activePreloads.toArray(CompletableFuture[]::new);
        boolean preloadsCompleted = awaitPreloads(pendingPreloads, deadline);
        boolean writesCompleted = awaitPendingWrites(System.nanoTime() + SHUTDOWN_WAIT_TIMEOUT.toNanos());
        if (!preloadsCompleted || !writesCompleted) {
            plugin.getLogger().warning("Timed out waiting for title storage operations during shutdown; pending changes may not be saved.");
        }
        ownedTitles.clear();
        selectedTitles.clear();
        selectedFonts.clear();
        customTitles.clear();
        customTitleTokens.clear();
        try {
            closer.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not close title storage", exception);
        }
    }

    private CompletableFuture<Void> runAsync(UUID playerId, Runnable task) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Title storage is closed"));
        }
        CompletableFuture<Void> write = pendingWritesByPlayer.compute(playerId, (ignored, previous) -> {
            pendingWrites.incrementAndGet();
            CompletableFuture<Void> ready = previous == null ? CompletableFuture.completedFuture(null) : previous.handle((result, exception) -> null);
            return ready.thenRunAsync(task, DatabaseExecutor.instance()).whenComplete((result, exception) -> {
                pendingWrites.decrementAndGet();
                if (exception != null) {
                    plugin.getLogger().log(Level.SEVERE, "Could not save player title data for " + playerId, exception);
                }
            });
        });
        write.whenComplete((result, exception) -> pendingWritesByPlayer.remove(playerId, write));
        return write;
    }

    private boolean awaitPreloads(CompletableFuture<?>[] preloads, long deadline) {
        if (preloads.length == 0) {
            return true;
        }
        long remainingNanos = deadline - System.nanoTime();
        if (remainingNanos <= 0) {
            return false;
        }
        try {
            CompletableFuture.allOf(preloads).get(remainingNanos, TimeUnit.NANOSECONDS);
            return true;
        } catch (ExecutionException exception) {
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        } catch (TimeoutException exception) {
            return false;
        }
    }

    private boolean awaitPendingWrites(long deadline) {
        while (pendingWrites.get() > 0 && System.nanoTime() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return pendingWrites.get() == 0;
    }

    private static Set<String> withTitle(Set<String> titles, String titleId) {
        Set<String> updatedTitles = new HashSet<>(titles);
        updatedTitles.add(titleId);
        return Set.copyOf(updatedTitles);
    }

    private record CustomTitlesCache(List<CustomTitle> ordered, Map<UUID, CustomTitle> byId) {
        private static final CustomTitlesCache EMPTY = new CustomTitlesCache(List.of(), Map.of());

        private static CustomTitlesCache from(List<CustomTitle> titles) {
            if (titles.isEmpty()) {
                return EMPTY;
            }
            Map<UUID, CustomTitle> byId = new java.util.HashMap<>(titles.size());
            for (CustomTitle title : titles) {
                byId.put(title.id(), title);
            }
            return new CustomTitlesCache(List.copyOf(titles), Map.copyOf(byId));
        }

        private CustomTitlesCache withTitle(CustomTitle title) {
            java.util.ArrayList<CustomTitle> updated = new java.util.ArrayList<>(ordered);
            updated.add(title);
            return from(updated);
        }

        private CustomTitlesCache update(UUID titleId, java.util.function.UnaryOperator<CustomTitle> updater) {
            CustomTitle current = byId.get(titleId);
            if (current == null) {
                return this;
            }
            CustomTitle replacement = updater.apply(current);
            java.util.ArrayList<CustomTitle> updated = new java.util.ArrayList<>(ordered.size());
            for (CustomTitle title : ordered) {
                updated.add(title.id().equals(titleId) ? replacement : title);
            }
            return from(updated);
        }

        private CustomTitlesCache replaceIfCurrent(UUID titleId, CustomTitle expected, CustomTitle replacement) {
            CustomTitle current = byId.get(titleId);
            return current != null && current.equals(expected) ? update(titleId, ignored -> replacement) : this;
        }

        private CustomTitlesCache without(UUID titleId) {
            if (!byId.containsKey(titleId)) {
                return this;
            }
            return from(ordered.stream().filter(title -> !title.id().equals(titleId)).toList());
        }
    }
}
