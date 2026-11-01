package org.lirdev.lirtitles.menu.menus.shared;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import org.lirdev.lirtitles.configuration.menus.items.MenuItem;
import org.lirdev.lirtitles.menu.bukkit.BukkitCustomItemResolver;
import org.lirdev.lirtitles.utilities.HexColor;
import org.lirdev.lirtitles.utilities.PlaceholderReplacer;

public final class MenuItemFactory {
    private static final int MAX_CACHED_DYED_ITEMS = 2_048;
    private static final ItemFlag[] HIDDEN_FLAGS = ItemFlag.values();
    private static final int MAX_CACHED_PROFILES = 512;
    private static final Map<String, PlayerProfile> PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<String> PROFILE_CACHE_ORDER = new ConcurrentLinkedQueue<>();
    private final Map<MenuItem, List<LoreLine>> loreLines = new IdentityHashMap<>();
    private final LinkedHashMap<DyedItemKey, ItemStack> dyedItems = new LinkedHashMap<>(64, 0.75F, true);
    private final BukkitCustomItemResolver customItems = new BukkitCustomItemResolver();

    public ItemStack create(MenuItem template, MenuItemTokens tokens) {
        return createItem(template, tokens, template.material(), template.baseHead(), null);
    }

    public ItemStack createDyed(MenuItem template, MenuItemTokens tokens, HexColor color) {
        return createItem(template, tokens, template.material(), template.baseHead(), color);
    }

    public synchronized ItemStack createDyedCached(MenuItem template, MenuItemTokens tokens, HexColor color) {
        DyedItemKey key = new DyedItemKey(template, tokens, color);
        ItemStack cached = dyedItems.get(key);
        if (cached != null) {
            return cached;
        }
        ItemStack created = createDyed(template, tokens, color);
        dyedItems.put(key, created);
        if (dyedItems.size() > MAX_CACHED_DYED_ITEMS) {
            dyedItems.remove(dyedItems.keySet().iterator().next());
        }
        return created;
    }

    public synchronized void clearCaches() {
        dyedItems.clear();
        synchronized (loreLines) {
            loreLines.clear();
        }
    }

    public ItemStack create(MenuItem template, MenuItemTokens tokens, String materialOverride, String baseHeadOverride) {
        Material material = materialOverride == null || materialOverride.isBlank() ? null : Material.matchMaterial(materialOverride);
        String baseHead = baseHeadOverride == null || baseHeadOverride.isBlank() ? template.baseHead() : baseHeadOverride;
        return createItem(template, tokens, material == null ? template.material() : material, baseHead, null);
    }

    private ItemStack createItem(MenuItem template, MenuItemTokens tokens, Material material, String baseHead, HexColor dye) {
        Material resolvedMaterial = baseHead.isBlank() ? material : Material.PLAYER_HEAD;
        ItemStack stack = template.customMaterial().isBlank() ? new ItemStack(resolvedMaterial)
                : customItems.resolve(template.customMaterial()).orElseGet(() -> new ItemStack(Material.BARRIER));
        stack.setAmount(Math.min(template.amount(), stack.getMaxStackSize()));
        ItemMeta meta = stack.getItemMeta();
        meta.addItemFlags(HIDDEN_FLAGS);
        meta.displayName(nonItalic(PlaceholderReplacer.replace(template.name(), tokens.inline())));
        meta.lore(lore(template, tokens));
        if (template.customModelData() != null) {
            meta.setCustomModelData(template.customModelData());
        }
        if (template.glow()) {
            meta.setEnchantmentGlintOverride(true);
        }
        if (meta instanceof SkullMeta skullMeta && !baseHead.isBlank()) {
            skullMeta.setPlayerProfile(profile(baseHead));
        }
        applyDye(meta, dye);
        stack.setItemMeta(meta);
        return stack;
    }

    private static void applyDye(ItemMeta meta, HexColor dye) {
        if (dye != null && meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(dye.asBukkitColor());
        }
    }

    private List<Component> lore(MenuItem template, MenuItemTokens tokens) {
        List<LoreLine> configuredLines;
        synchronized (loreLines) {
            configuredLines = loreLines.computeIfAbsent(template, this::compileLore);
        }
        List<Component> lines = new ArrayList<>(configuredLines.size());
        for (LoreLine configuredLine : configuredLines) {
            List<Component> block = configuredLine.blockKey() == null ? null : tokens.blocks().get(configuredLine.blockKey());
            if (block == null) {
                lines.add(nonItalic(PlaceholderReplacer.replace(configuredLine.component(), tokens.inline())));
                continue;
            }
            block.forEach(blockLine -> lines.add(nonItalic(blockLine)));
        }
        return lines;
    }

    private List<LoreLine> compileLore(MenuItem template) {
        List<LoreLine> compiled = new ArrayList<>(template.lore().size());
        for (Component line : template.lore()) {
            Optional<String> blockKey = PlaceholderReplacer.wholeLinePlaceholder(line);
            compiled.add(new LoreLine(line, blockKey.orElse(null)));
        }
        return List.copyOf(compiled);
    }

    private static PlayerProfile profile(String baseHead) {
        PlayerProfile profile = PROFILE_CACHE.computeIfAbsent(baseHead, texture -> {
            PROFILE_CACHE_ORDER.offer(texture);
            return createProfile(texture);
        });
        trimProfileCache();
        return profile;
    }

    private static void trimProfileCache() {
        while (PROFILE_CACHE.size() > MAX_CACHED_PROFILES) {
            String oldest = PROFILE_CACHE_ORDER.poll();
            if (oldest == null) {
                return;
            }
            PROFILE_CACHE.remove(oldest);
        }
    }

    private static PlayerProfile createProfile(String baseHead) {
        UUID profileId = UUID.nameUUIDFromBytes(baseHead.getBytes(StandardCharsets.UTF_8));
        PlayerProfile profile = Bukkit.createProfile(profileId);
        profile.setProperty(new ProfileProperty("textures", baseHead));
        return profile;
    }

    private static Component nonItalic(Component component) {
        return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private record LoreLine(Component component, String blockKey) {
    }

    private static final class DyedItemKey {
        private final MenuItem template;
        private final MenuItemTokens tokens;
        private final HexColor color;

        private DyedItemKey(MenuItem template, MenuItemTokens tokens, HexColor color) {
            this.template = template;
            this.tokens = tokens;
            this.color = color;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof DyedItemKey key && template == key.template
                    && tokens.equals(key.tokens) && color.equals(key.color);
        }

        @Override
        public int hashCode() {
            int result = System.identityHashCode(template);
            result = 31 * result + tokens.hashCode();
            return 31 * result + color.hashCode();
        }
    }
}
