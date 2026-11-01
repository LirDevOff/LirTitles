package org.lirdev.lirtitles.utilities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.lirdev.lirtitles.placeholders.PlaceholderRegistry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public final class Colorizer {
    private static final int MAX_CACHE_ENTRIES = 2_000;
    private final MiniMessage miniMessage;
    private final Method placeholderResolver;
    private final ConcurrentMap<String, Component> cachedComponents = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> cacheOrder = new ConcurrentLinkedQueue<>();

    public Colorizer() {
        miniMessage = MiniMessage.miniMessage();
        placeholderResolver = findPlaceholderResolver();
    }

    public Component colorize(String text) {
        Objects.requireNonNull(text, "text");
        return cached(text);
    }

    public Component colorize(Player player, String text) {
        Objects.requireNonNull(text, "text");
        return cached(replacePlaceholders(player, text));
    }

    public String escape(String text) {
        return miniMessage.escapeTags(Objects.requireNonNull(text, "text"));
    }

    private Component cached(String text) {
        Component cached = cachedComponents.get(text);
        if (cached != null) {
            return cached;
        }
        Component created = cachedComponents.computeIfAbsent(text, key -> {
            Component parsed = miniMessage.deserialize(key);
            cacheOrder.offer(key);
            return parsed;
        });
        trimCache();
        return created;
    }

    private void trimCache() {
        while (cachedComponents.size() > MAX_CACHE_ENTRIES) {
            String oldest = cacheOrder.poll();
            if (oldest == null) {
                return;
            }
            cachedComponents.remove(oldest);
        }
    }

    private Method findPlaceholderResolver() {
        Plugin placeholderApi = Bukkit.getPluginManager().getPlugin(PlaceholderRegistry.PLUGIN_NAME);
        if (placeholderApi == null || !placeholderApi.isEnabled()) {
            return null;
        }
        try {
            ClassLoader classLoader = placeholderApi.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI", false, classLoader);
            return apiClass.getMethod("setPlaceholders", Player.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return null;
        }
    }

    private String replacePlaceholders(Player player, String text) {
        if (player == null || placeholderResolver == null) {
            return text;
        }
        try {
            return (String) placeholderResolver.invoke(null, player, text);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            return text;
        }
    }
}
