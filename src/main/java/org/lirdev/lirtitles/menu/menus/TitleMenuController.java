package org.lirdev.lirtitles.menu.menus;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import org.lirdev.lirtitles.LirTitles;
import org.lirdev.lirtitles.configuration.menus.colors.PaletteColor;
import org.lirdev.lirtitles.configuration.titles.Titles;
import org.lirdev.lirtitles.data.PlayerTitleStorage;
import org.lirdev.lirtitles.data.DatabaseExecutor;
import org.lirdev.lirtitles.data.model.CustomTitle;
import org.lirdev.lirtitles.data.model.CustomTitleStatus;
import org.lirdev.lirtitles.data.model.PendingModerationEntry;
import org.lirdev.lirtitles.menu.MenuOpener;
import org.lirdev.lirtitles.menu.input.AnvilTextRequest;
import org.lirdev.lirtitles.menu.menus.categories.CategoriesMenu;
import org.lirdev.lirtitles.menu.menus.colors.ColorSelectionMenu;
import org.lirdev.lirtitles.menu.menus.colors.ColorShadeMenu;
import org.lirdev.lirtitles.menu.menus.customtitles.CustomTitleDeleteMenu;
import org.lirdev.lirtitles.menu.menus.customtitles.CustomTitleDraft;
import org.lirdev.lirtitles.menu.menus.customtitles.CustomTitleEditorMenu;
import org.lirdev.lirtitles.menu.menus.customtitles.CustomTitleMarkup;
import org.lirdev.lirtitles.menu.menus.customtitles.CustomTitlesMenu;
import org.lirdev.lirtitles.menu.menus.customtitles.ConfiguredTextInputLayout;
import org.lirdev.lirtitles.menu.menus.moderation.ModerationMenu;
import org.lirdev.lirtitles.menu.menus.shared.MenuItemFactory;
import org.lirdev.lirtitles.menu.menus.titles.TitleApplyMenu;
import org.lirdev.lirtitles.menu.menus.titles.TitleSelectionMenu;
import org.lirdev.lirtitles.utilities.DurationFormatter;
import org.lirdev.lirtitles.utilities.HexColor;

public final class TitleMenuController implements MenuOpener {
    private final LirTitles plugin;
    private final PlayerTitleStorage storage;
    private final CategoriesMenu categoriesMenu;
    private final TitleSelectionMenu titleSelectionMenu;
    private final TitleApplyMenu titleApplyMenu;
    private final CustomTitlesMenu customTitlesMenu;
    private final CustomTitleEditorMenu customTitleEditorMenu;
    private final CustomTitleDeleteMenu customTitleDeleteMenu;
    private final ColorSelectionMenu colorSelectionMenu;
    private final ColorShadeMenu colorShadeMenu;
    private final ModerationMenu moderationMenu;
    private final CustomTitleMarkup titleMarkup;
    private final MenuItemFactory inputItemFactory;
    private final Map<UUID, String> previewFonts = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Integer> titlePages = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, CustomTitleDraft> customTitleDrafts = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Integer> customTitlePages = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Integer> moderationPages = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, List<PendingModerationEntry>> moderationQueues = new ConcurrentHashMap<>();
    private volatile Map<String, Consumer<Player>> openableMenus;

    public TitleMenuController(LirTitles plugin, PlayerTitleStorage storage) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.storage = Objects.requireNonNull(storage, "storage");
        categoriesMenu = new CategoriesMenu(plugin);
        titleSelectionMenu = new TitleSelectionMenu(plugin, storage);
        titleApplyMenu = new TitleApplyMenu(plugin);
        customTitlesMenu = new CustomTitlesMenu(plugin);
        customTitleEditorMenu = new CustomTitleEditorMenu(plugin);
        customTitleDeleteMenu = new CustomTitleDeleteMenu(plugin);
        colorSelectionMenu = new ColorSelectionMenu(plugin);
        colorShadeMenu = new ColorShadeMenu(plugin);
        moderationMenu = new ModerationMenu(plugin);
        titleMarkup = new CustomTitleMarkup(plugin);
        inputItemFactory = new MenuItemFactory();
    }

    public void clearPlayerState(UUID playerId) {
        previewFonts.remove(playerId);
        titlePages.remove(playerId);
        customTitleDrafts.remove(playerId);
        customTitlePages.remove(playerId);
        moderationPages.remove(playerId);
        moderationQueues.remove(playerId);
    }

    public void clearConfigurationCaches() {
        openableMenus = null;
        inputItemFactory.clearCaches();
        categoriesMenu.clearCaches();
        titleSelectionMenu.clearCaches();
        titleApplyMenu.clearCaches();
        customTitlesMenu.clearCaches();
        customTitleEditorMenu.clearCaches();
        customTitleDeleteMenu.clearCaches();
        colorSelectionMenu.clearCaches();
        colorShadeMenu.clearCaches();
        moderationMenu.clearCaches();
    }

    public void openCategories(Player player) {
        openWhenPlayerDataLoaded(player, () -> openCategoriesLoaded(player));
    }

    private void openCategoriesLoaded(Player player) {
        plugin.menuNavigator().open(player, categoriesMenu.create(player, this::openTitlesFromFirstPage, previewFont(player),
                (chooser, fontId) -> changePreviewFont(chooser, fontId, () -> openCategories(chooser)), this::openCustomTitles));
    }

    @Override
    public void open(Player player, String name) {
        openWhenPlayerDataLoaded(player, () -> openLoadedMenu(player, name));
    }

    private void openLoadedMenu(Player player, String name) {
        Consumer<Player> handler = openableMenus().get(name.toLowerCase(Locale.ROOT));
        if (handler == null) {
            plugin.getLogger().warning("Menu action [open_menu] referenced unknown or unopenable menu id '" + name + "'");
            return;
        }
        handler.accept(player);
    }

    private Map<String, Consumer<Player>> openableMenus() {
        Map<String, Consumer<Player>> cachedMenus = openableMenus;
        if (cachedMenus != null) {
            return cachedMenus;
        }
        Map<String, Consumer<Player>> menus = new LinkedHashMap<>();
        registerMenu(menus, plugin.configuration().categoriesMenu().id(), this::openCategories);
        registerMenu(menus, plugin.configuration().customTitlesMenu().id(), this::openCustomTitles);
        registerMenu(menus, plugin.configuration().customTitleEditorMenu().id(), this::openCustomTitleEditor);
        registerMenu(menus, plugin.configuration().moderationMenu().id(), this::openModerationQueue);
        cachedMenus = Map.copyOf(menus);
        openableMenus = cachedMenus;
        return cachedMenus;
    }

    private static void registerMenu(Map<String, Consumer<Player>> menus, String id, Consumer<Player> handler) {
        if (id != null && !id.isBlank()) {
            menus.put(id.toLowerCase(Locale.ROOT), handler);
        }
    }

    private void openWhenPlayerDataLoaded(Player player, Runnable action) {
        UUID playerId = player.getUniqueId();
        if (storage.isLoaded(playerId)) {
            action.run();
            return;
        }
        storage.preloadAsync(playerId).whenComplete((ignored, exception) -> {
            if (exception != null) {
                return;
            }
            player.getScheduler().run(plugin, scheduledTask -> action.run(), null);
        });
    }

    private void acceptCustomTitleText(Player player, String text) {
        CustomTitleDraft draft = customTitleDrafts.get(player.getUniqueId());
        if (draft == null) {
            return;
        }
        customTitleDrafts.put(player.getUniqueId(), draft.withText(text));
        openCustomTitleEditor(player);
    }

    private Component renderCustomTitlePreview(CustomTitleDraft draft, String text) {
        return plugin.colorizer().colorize(titleMarkup.render(draft.withText(text)));
    }

    private void openTitles(Player player, Titles.Category category) {
        plugin.menuNavigator().open(player, titleSelectionMenu.create(player, category, this::openCategories,
                (chooser, title) -> chooseTitle(chooser, category, title), previewFont(player),
                (chooser, fontId) -> changePreviewFont(chooser, fontId, () -> openTitles(chooser, category)),
                titlePages.getOrDefault(player.getUniqueId(), 0), chooser -> changeTitlePage(chooser, category, 1),
                chooser -> changeTitlePage(chooser, category, -1)));
    }

    private void openTitlesFromFirstPage(Player player, Titles.Category category) {
        titlePages.remove(player.getUniqueId());
        openTitles(player, category);
    }

    private void changeTitlePage(Player player, Titles.Category category, int offset) {
        changePage(titlePages, player.getUniqueId(), offset);
        openTitles(player, category);
    }

    private void openCustomTitles(Player player) {
        if (!plugin.configuration().customTitles().enabled()) {
            openCategories(player);
            return;
        }
        UUID playerId = player.getUniqueId();
        int page = Math.max(0, customTitlePages.getOrDefault(playerId, 0));
        plugin.menuNavigator().open(player, customTitlesMenu.create(player, storage.customTitles(playerId), storage.selected(playerId), page,
                previewFont(player), storage.customTitleTokens(playerId), this::createCustomTitle, this::activateCustomTitle,
                this::openCustomTitleDeletion, this::openCategories, chooser -> changeCustomPage(chooser, 1),
                chooser -> changeCustomPage(chooser, -1), (chooser, fontId) -> changePreviewFont(chooser, fontId, () -> openCustomTitles(chooser))));
    }

    private void changeCustomPage(Player player, int offset) {
        changePage(customTitlePages, player.getUniqueId(), offset);
        openCustomTitles(player);
    }

    private void createCustomTitle(Player player) {
        if (storage.customTitleTokens(player.getUniqueId()) <= 0) {
            player.sendMessage(plugin.configuration().message(plugin.configuration().messages().noCustomTitleTokens()));
            openCustomTitles(player);
            return;
        }
        customTitleDrafts.put(player.getUniqueId(), CustomTitleDraft.blank(colorSlotCount()));
        openCustomTitleEditor(player);
    }

    private void activateCustomTitle(Player player, CustomTitle title) {
        switch (title.status()) {
            case PENDING ->
                    player.sendMessage(plugin.configuration().message(plugin.configuration().messages().customTitleNotApproved()));
            case REJECTED -> sendRejectionNotice(player, title);
            case APPROVED -> selectExistingCustomTitle(player, title);
        }
        openCustomTitles(player);
    }

    private void selectExistingCustomTitle(Player player, CustomTitle title) {
        UUID playerId = player.getUniqueId();
        String fontId = previewFont(player);
        if (alreadyApplied(playerId, title.key(), fontId)) {
            player.sendMessage(plugin.configuration().message(plugin.configuration().messages().titleAlreadySelected(),
                    Map.of("title", renderedCustomTitle(title, fontId))));
            return;
        }
        if (storage.selectCustomTitle(playerId, title.id())) {
            if (!fontId.isEmpty()) {
                storage.selectFont(playerId, fontId);
            }
            previewFonts.remove(playerId);
            player.sendMessage(plugin.configuration().message(plugin.configuration().messages().titleSelected(),
                    Map.of("title", renderedCustomTitle(title, fontId))));
        }
    }

    private void sendRejectionNotice(Player player, CustomTitle title) {
        Component reason = title.rejectionReason().isBlank()
                ? plugin.configuration().message(plugin.configuration().messages().moderationNoReasonFallback())
                : Component.text(title.rejectionReason());
        player.sendMessage(plugin.configuration().message(plugin.configuration().messages().moderationRejectionNotice(), Map.of(
                "title", plugin.colorizer().colorize(title.text()),
                "reason", reason,
                "time", Component.text(DurationFormatter.elapsedSince(title.decidedAt())))));
    }

    private void openCustomTitleDeletion(Player player, CustomTitle title) {
        plugin.menuNavigator().open(player, customTitleDeleteMenu.create(player, title,
                chooser -> deleteCustomTitle(chooser, title), this::openCustomTitles));
    }

    private void deleteCustomTitle(Player player, CustomTitle title) {
        if (storage.deleteCustomTitle(player.getUniqueId(), title.id())) {
            storage.addCustomTitleTokens(player.getUniqueId(), 1);
            player.sendMessage(plugin.configuration().message(plugin.configuration().messages().customTitleDeleted(),
                    Map.of("title", renderedCustomTitle(title, previewFont(player)))));
        }
        openCustomTitles(player);
    }

    private void openCustomTitleEditor(Player player) {
        CustomTitleDraft draft = customTitleDrafts.get(player.getUniqueId());
        if (draft == null) {
            openCustomTitles(player);
            return;
        }
        plugin.menuNavigator().open(player, customTitleEditorMenu.create(player, titleMarkup.render(draft), draft.formats(), draft.colors(),
                this::beginCustomTitleInput, this::confirmCustomTitle, this::cancelCustomTitle, tag -> toggleFormat(player, tag),
                this::openColorSelection));
    }

    private void beginCustomTitleInput(Player player) {
        CustomTitleDraft draft = customTitleDrafts.get(player.getUniqueId());
        if (draft == null) {
            return;
        }
        var settings = plugin.configuration().customTitles();
        ConfiguredTextInputLayout layout = new ConfiguredTextInputLayout(
                plugin.configuration().customTitleInputMenu(), inputItemFactory,
                text -> settings.acceptsLength(text.length())
                        && !PlainTextComponentSerializer.plainText().serialize(renderCustomTitlePreview(draft, text)).isBlank(),
                text -> renderCustomTitlePreview(draft, text), settings.minLength(), settings.maxLength());
        plugin.anvilTextPrompt().open(player, new AnvilTextRequest(plugin.configuration().customTitleInputMenu().title(), layout,
                text -> acceptCustomTitleText(player, text), () -> openCustomTitleEditor(player)));
    }

    private void toggleFormat(Player player, String tag) {
        CustomTitleDraft draft = customTitleDrafts.get(player.getUniqueId());
        if (draft == null) {
            return;
        }
        Set<String> formats = new LinkedHashSet<>(draft.formats());
        if (!formats.remove(tag)) {
            formats.add(tag);
        }
        customTitleDrafts.put(player.getUniqueId(), draft.withFormats(Set.copyOf(formats)));
        openCustomTitleEditor(player);
    }

    private int colorSlotCount() {
        return plugin.configuration().customTitleEditorMenu().colorSlots().size();
    }

    private void openColorSelection(Player player, int colorSlot) {
        CustomTitleDraft draft = customTitleDrafts.get(player.getUniqueId());
        if (draft == null) {
            return;
        }
        HexColor selected = HexColor.parse(draft.colors().get(colorSlot)).orElse(null);
        plugin.menuNavigator().open(player, colorSelectionMenu.create(player, colorSlot, selected,
                option -> chooseMainColor(player, colorSlot, option),
                chooser -> beginCustomColorInput(chooser, colorSlot, () -> openColorSelection(chooser, colorSlot)),
                this::openCustomTitleEditor));
    }

    private void chooseMainColor(Player player, int colorSlot, PaletteColor option) {
        openColorShades(player, colorSlot, option);
    }

    private void openColorShades(Player player, int colorSlot, PaletteColor mainColor) {
        CustomTitleDraft draft = customTitleDrafts.get(player.getUniqueId());
        if (draft == null) {
            return;
        }
        HexColor selected = HexColor.parse(draft.colors().get(colorSlot)).orElse(null);
        plugin.menuNavigator().open(player, colorShadeMenu.create(player, mainColor, selected,
                shade -> applyColor(player, colorSlot, shade.hex()),
                chooser -> beginCustomColorInput(chooser, colorSlot, () -> openColorShades(chooser, colorSlot, mainColor)),
                chooser -> openColorSelection(chooser, colorSlot)));
    }

    private void applyColor(Player player, int colorSlot, String hex) {
        CustomTitleDraft draft = customTitleDrafts.get(player.getUniqueId());
        if (draft == null) {
            return;
        }
        customTitleDrafts.put(player.getUniqueId(), draft.withColor(colorSlot, hex));
        openCustomTitleEditor(player);
    }

    private void beginCustomColorInput(Player player, int colorSlot, Runnable onCancel) {
        ConfiguredTextInputLayout layout = new ConfiguredTextInputLayout(plugin.configuration().customTitleInputMenu(),
                inputItemFactory, text -> HexColor.parse(text).isPresent(), Component::text, 7, 7);
        plugin.anvilTextPrompt().open(player, new AnvilTextRequest(
                plugin.colorizer().colorize("<#b6c2f1>Enter Hex Color"), layout,
                text -> applyColor(player, colorSlot, HexColor.parse(text).orElseThrow().hex()), onCancel));
    }

    private void confirmCustomTitle(Player player) {
        UUID playerId = player.getUniqueId();
        CustomTitleDraft draft = customTitleDrafts.remove(playerId);
        if (draft == null) {
            openCustomTitles(player);
            return;
        }
        if (draft.text().isBlank()) {
            player.sendMessage(plugin.configuration().message(plugin.configuration().messages().customTitleMissingText()));
            openCustomTitleEditor(player);
            return;
        }
        String finalText = titleMarkup.render(draft);
        CustomTitleStatus status = plugin.configuration().customTitles().moderationEnabled() ? CustomTitleStatus.PENDING : CustomTitleStatus.APPROVED;
        boolean creatingNewTitle = draft.id() == null;
        if (creatingNewTitle && !storage.consumeCustomTitleToken(playerId)) {
            player.sendMessage(plugin.configuration().message(plugin.configuration().messages().noCustomTitleTokens()));
            openCustomTitles(player);
            return;
        }
        CustomTitle title = creatingNewTitle
                ? storage.createCustomTitle(playerId, player.getName(), finalText, storage.customTitles(playerId).size(), status)
                : storage.customTitle(playerId, draft.id());
        if (title != null && (creatingNewTitle || storage.updateCustomTitle(playerId, draft.id(), finalText, status))) {
            storage.selectCustomTitle(playerId, title.id());
            if (creatingNewTitle) {
                player.sendMessage(plugin.configuration().message(plugin.configuration().messages().customTitleCreated(),
                        Map.of("title", renderedCustomTitle(title, previewFont(player)))));
            }
        }
        openCustomTitles(player);
    }

    private void cancelCustomTitle(Player player) {
        customTitleDrafts.remove(player.getUniqueId());
        openCustomTitles(player);
    }

    public void openModerationQueue(Player player) {
        openWhenPlayerDataLoaded(player, () -> openModerationQueue(player, moderationPages.getOrDefault(player.getUniqueId(), 0)));
    }

    private void openModerationQueue(Player player, int page) {
        CompletableFuture.supplyAsync(storage::pendingModeration, DatabaseExecutor.instance())
                .thenAccept(pending -> {
                    moderationQueues.put(player.getUniqueId(), pending);
                    showModerationQueue(player, pending, page);
                })
                .exceptionally(exception -> {
                    plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not load moderation queue", exception);
                    return null;
                });
    }

    private void showModerationQueue(Player player, List<PendingModerationEntry> pending, int page) {
        player.getScheduler().run(plugin, playerTask -> plugin.menuNavigator().open(player, moderationMenu.create(player, pending, page,
                this::approveCustomTitle, this::rejectCustomTitle, this::beginModerationReason,
                Player::closeInventory, chooser -> changeModerationPage(chooser, 1), chooser -> changeModerationPage(chooser, -1))), null);
    }

    private void changeModerationPage(Player player, int offset) {
        changePage(moderationPages, player.getUniqueId(), offset);
        List<PendingModerationEntry> pending = moderationQueues.get(player.getUniqueId());
        if (pending == null) {
            openModerationQueue(player, moderationPages.getOrDefault(player.getUniqueId(), 0));
            return;
        }
        showModerationQueue(player, pending, moderationPages.getOrDefault(player.getUniqueId(), 0));
    }

    private void approveCustomTitle(Player moderator, PendingModerationEntry entry) {
        completeModerationDecision(moderator, entry, CustomTitleStatus.APPROVED, "");
    }

    private void rejectCustomTitle(Player moderator, PendingModerationEntry entry) {
        completeModerationDecision(moderator, entry, CustomTitleStatus.REJECTED, "");
    }

    private void completeModerationDecision(Player moderator, PendingModerationEntry entry,
                                            CustomTitleStatus status, String reason) {
        removeFromModerationQueue(moderator, entry);
        String decision = status == CustomTitleStatus.APPROVED ? "approve" : "reject";
        Component message = plugin.configuration().message(
                status == CustomTitleStatus.APPROVED
                        ? plugin.configuration().messages().moderationApproved()
                        : plugin.configuration().messages().moderationRejected(),
                Map.of("title", plugin.colorizer().colorize(entry.title().text()),
                        "player", Component.text(authorName(entry.playerId()))));
        storage.decideCustomTitle(entry.playerId(), entry.title().id(), status, reason, System.currentTimeMillis())
                .whenComplete((ignored, exception) -> moderator.getScheduler().run(plugin, scheduledTask -> {
                    if (exception != null) {
                        plugin.getLogger().log(java.util.logging.Level.SEVERE,
                                "Could not " + decision + " custom title " + entry.title().id(), exception);
                        openModerationQueue(moderator);
                        return;
                    }
                    moderator.sendMessage(message);
                }, null));
    }

    private void removeFromModerationQueue(Player moderator, PendingModerationEntry entry) {
        UUID moderatorId = moderator.getUniqueId();
        List<PendingModerationEntry> pending = moderationQueues.computeIfPresent(moderatorId, (ignored, entries) -> entries.stream()
                .filter(candidate -> !candidate.title().id().equals(entry.title().id()) || !candidate.playerId().equals(entry.playerId()))
                .toList());
        if (pending != null) {
            int page = pending.isEmpty() ? 0 : moderationPages.getOrDefault(moderatorId, 0);
            if (pending.isEmpty()) {
                moderationPages.remove(moderatorId);
            }
            showModerationQueue(moderator, pending, page);
        }
    }

    private void beginModerationReason(Player moderator, PendingModerationEntry entry) {
        ConfiguredTextInputLayout layout = new ConfiguredTextInputLayout(plugin.configuration().customTitleInputMenu(),
                inputItemFactory, reason -> !reason.isBlank() && reason.length() <= 50, Component::text, 1, 50);
        plugin.anvilTextPrompt().open(moderator, new AnvilTextRequest(
                plugin.colorizer().colorize("<#b6c2f1>Enter Rejection Reason"), layout,
                reason -> completeModerationDecision(moderator, entry, CustomTitleStatus.REJECTED, reason),
                () -> openModerationQueue(moderator)));
    }

    private static String authorName(UUID playerId) {
        OfflinePlayer author = Bukkit.getOfflinePlayer(playerId);
        String name = author.getName();
        return name == null ? playerId.toString() : name;
    }

    private static void changePage(Map<UUID, Integer> pages, UUID playerId, int offset) {
        pages.merge(playerId, offset, (page, change) -> Math.max(0, page + change));
    }

    private void chooseTitle(Player player, Titles.Category category, Titles.Title title) {
        String fontId = previewFont(player);
        if (notifyIfAlreadyApplied(player, category, title, fontId)) {
            return;
        }
        if (plugin.configuration().titleConfirmation()) {
            openApply(player, category, title, fontId);
            return;
        }
        applyTitle(player, category, title, fontId);
    }

    private void openApply(Player player, Titles.Category category, Titles.Title title, String previewFontId) {
        plugin.menuNavigator().open(player, titleApplyMenu.create(player, title, previewFontId,
                (chooser, fontId) -> {
                    previewFonts.put(chooser.getUniqueId(), fontId);
                    openApply(chooser, category, title, fontId);
                },
                (chooser, fontId) -> applyTitle(chooser, category, title, fontId),
                chooser -> openTitles(chooser, category)));
    }

    private void applyTitle(Player player, Titles.Category category, Titles.Title title, String fontId) {
        UUID playerId = player.getUniqueId();
        if (notifyIfAlreadyApplied(player, category, title, fontId)) {
            return;
        }
        if (storage.select(player, title.key(), fontId)) {
            previewFonts.remove(playerId);
            player.sendMessage(plugin.configuration().message(plugin.configuration().messages().titleSelected(),
                    Map.of("title", renderedTitle(title, fontId))));
        }
        openTitles(player, category);
    }

    private boolean notifyIfAlreadyApplied(Player player, Titles.Category category, Titles.Title title, String fontId) {
        if (!alreadyApplied(player.getUniqueId(), title.key(), fontId)) {
            return false;
        }
        player.sendMessage(plugin.configuration().message(plugin.configuration().messages().titleAlreadySelected(),
                Map.of("title", renderedTitle(title, fontId))));
        openTitles(player, category);
        return true;
    }

    private boolean alreadyApplied(UUID playerId, String titleKey, String fontId) {
        boolean sameTitle = titleKey.equals(storage.selected(playerId));
        boolean sameFont = fontId.equals(storage.selectedFont(playerId));
        return sameTitle && sameFont;
    }

    private String previewFont(Player player) {
        return previewFonts.getOrDefault(player.getUniqueId(), storage.selectedFont(player.getUniqueId()));
    }

    private void changePreviewFont(Player player, String fontId, Runnable reopenMenu) {
        previewFonts.put(player.getUniqueId(), fontId);
        reopenMenu.run();
    }

    private Component renderedTitle(Titles.Title title, String fontId) {
        return plugin.configuration().fonts().render(fontId, title.displayName());
    }

    private Component renderedCustomTitle(CustomTitle title, String fontId) {
        return plugin.configuration().fonts().render(fontId, plugin.colorizer().colorize(title.text()));
    }

}
