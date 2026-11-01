package org.lirdev.lirtitles.menu.bukkit;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public final class BukkitCustomItemResolver {
    public Optional<ItemStack> resolve(String configuredMaterial) {
        CustomMaterial material = CustomMaterial.parse(configuredMaterial);
        if (material == null) {
            return Optional.empty();
        }
        Plugin provider = Bukkit.getPluginManager().getPlugin(material.pluginName());
        if (provider == null || !provider.isEnabled()) {
            warn("Custom item material '" + configuredMaterial + "' requires " + material.pluginName());
            return Optional.empty();
        }
        try {
            ItemStack item = switch (material.provider()) {
                case NEXO -> buildItem("com.nexomc.nexo.api.NexoItems", "itemFromId", material.itemId());
                case ORAXEN -> buildItem("io.th0rgal.oraxen.api.OraxenItems", "getItemById", material.itemId());
                case ITEMSADDER -> itemsAdderItem(material.itemId());
            };
            if (item == null) {
                warn("Custom item '" + configuredMaterial + "' was not found");
                return Optional.empty();
            }
            return Optional.of(item);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            Bukkit.getLogger().log(Level.WARNING, "Could not load custom item '" + configuredMaterial + "'", exception);
            return Optional.empty();
        }
    }

    private static ItemStack itemsAdderItem(String itemId) throws ReflectiveOperationException {
        Class<?> customStack = Class.forName("dev.lone.itemsadder.api.CustomStack");
        Object stack = customStack.getMethod("getInstance", String.class).invoke(null, itemId);
        return stack == null ? null : (ItemStack) stack.getClass().getMethod("getItemStack").invoke(stack);
    }

    private static ItemStack buildItem(String className, String methodName, String itemId) throws ReflectiveOperationException {
        Class<?> api = Class.forName(className);
        Object builder = api.getMethod(methodName, String.class).invoke(null, itemId);
        if (builder == null) {
            return null;
        }
        Method build = builder.getClass().getMethod("build");
        return (ItemStack) build.invoke(builder);
    }

    private static void warn(String message) {
        Bukkit.getLogger().warning("[LirTitles] " + message);
    }

    private record CustomMaterial(Provider provider, String pluginName, String itemId) {
        private static CustomMaterial parse(String configuredMaterial) {
            String normalized = configuredMaterial.trim();
            String lowerCase = normalized.toLowerCase(java.util.Locale.ROOT);
            if (lowerCase.startsWith("nexo-") || lowerCase.startsWith("nexo:")) {
                return new CustomMaterial(Provider.NEXO, "Nexo", normalized.substring(5));
            }
            if (lowerCase.startsWith("oraxen-") || lowerCase.startsWith("oraxen:")) {
                return new CustomMaterial(Provider.ORAXEN, "Oraxen", normalized.substring(7));
            }
            if (lowerCase.startsWith("itemsadder-") || lowerCase.startsWith("itemsadder:")) {
                return new CustomMaterial(Provider.ITEMSADDER, "ItemsAdder", normalized.substring(11));
            }
            return null;
        }
    }

    private enum Provider {
        NEXO,
        ORAXEN,
        ITEMSADDER
    }
}
