package org.lirdev.lirtitles.fonts;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import org.bukkit.plugin.java.JavaPlugin;

public final class FontRegistry {
    private static final String FONTS_DIRECTORY = "fonts";
    private static final String FONT_RESOURCE_PREFIX = FONTS_DIRECTORY + "/";
    private static final String FONT_FILE_EXTENSION = ".json";
    private static final String DEFAULT_FONT_ID = "normal";
    private static final String INSTALLED_MARKER_FILE_NAME = ".installed";
    private static final String INSTALLED_VERSION_PREFIX = "version=";

    private static List<String> bundledFontResources;

    private final Map<String, TitleFont> fonts;
    private final List<TitleFont> fontList;
    private final Map<String, String> sourceByFile;
    private final Map<String, TitleFont> fontByFile;

    private FontRegistry(Map<String, TitleFont> fonts, Map<String, String> sourceByFile, Map<String, TitleFont> fontByFile) {
        this.fonts = Collections.unmodifiableMap(defaultFontFirst(fonts));
        this.fontList = List.copyOf(this.fonts.values());
        this.sourceByFile = Map.copyOf(sourceByFile);
        this.fontByFile = Map.copyOf(fontByFile);
    }

    public static void install(JavaPlugin plugin) {
        File directory = new File(plugin.getDataFolder(), FONTS_DIRECTORY);
        directory.mkdirs();
        File markerFile = new File(directory, INSTALLED_MARKER_FILE_NAME);
        String pluginVersion = plugin.getPluginMeta().getVersion();
        InstalledMarker marker = readMarker(markerFile);
        if (pluginVersion.equals(marker.version())) {
            return;
        }
        Set<String> alreadyOffered = marker.resources();
        Set<String> updatedMarker = new LinkedHashSet<>(alreadyOffered);
        for (String resource : bundledFontResources(plugin)) {
            if (alreadyOffered.contains(resource)) {
                continue;
            }
            File target = new File(plugin.getDataFolder(), resource);
            if (!target.exists()) {
                plugin.saveResource(resource, false);
            }
            updatedMarker.add(resource);
        }
        writeMarker(markerFile, pluginVersion, updatedMarker);
    }

    public static FontRegistry load(JavaPlugin plugin) {
        File directory = new File(plugin.getDataFolder(), FONTS_DIRECTORY);
        Map<String, String> sourceByFile = new LinkedHashMap<>();
        Map<String, TitleFont> fontByFile = new LinkedHashMap<>();
        Map<String, TitleFont> fonts = readAll(directory, plugin.getLogger(), sourceByFile, fontByFile, null);
        return new FontRegistry(fonts, sourceByFile, fontByFile);
    }

    public static ReloadResult reload(JavaPlugin plugin, FontRegistry previous) {
        File directory = new File(plugin.getDataFolder(), FONTS_DIRECTORY);
        Map<String, String> sourceByFile = new LinkedHashMap<>();
        Map<String, TitleFont> fontByFile = new LinkedHashMap<>();
        Map<String, TitleFont> fonts = readAll(directory, plugin.getLogger(), sourceByFile, fontByFile, previous);
        if (sourceByFile.equals(previous.sourceByFile)) {
            return new ReloadResult(previous, false);
        }
        return new ReloadResult(new FontRegistry(fonts, sourceByFile, fontByFile), true);
    }

    public record ReloadResult(FontRegistry registry, boolean changed) {
    }

    public Optional<TitleFont> find(String id) {
        return Optional.ofNullable(fonts.get(id));
    }

    public Optional<TitleFont> findOrDefault(String id) {
        Optional<TitleFont> font = find(id);
        return font.isPresent() ? font : fontList.isEmpty() ? Optional.empty() : Optional.of(fontList.get(0));
    }

    public Component render(String fontId, Component text) {
        TitleFont font = fonts.get(fontId);
        return font == null ? text : FontTextRenderer.render(font, text);
    }

    public Map<String, TitleFont> fonts() {
        return fonts;
    }

    public List<TitleFont> fontList() {
        return fontList;
    }

    private static Map<String, TitleFont> readAll(File directory, Logger logger, Map<String, String> sourceByFile,
                                                   Map<String, TitleFont> fontByFile, FontRegistry previous) {
        Map<String, TitleFont> fonts = new LinkedHashMap<>();
        File[] files = directory.listFiles((ignoredDirectory, name) -> name.endsWith(FONT_FILE_EXTENSION));
        if (files == null) {
            return fonts;
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            readFont(file, logger, sourceByFile, fontByFile, previous).ifPresent(font -> putIfAbsent(fonts, font, file, logger));
        }
        return fonts;
    }

    private static InstalledMarker readMarker(File markerFile) {
        if (!markerFile.isFile()) {
            return new InstalledMarker("", Set.of());
        }
        try {
            Set<String> entries = new LinkedHashSet<>();
            String version = "";
            for (String line : Files.readAllLines(markerFile.toPath(), StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.startsWith(INSTALLED_VERSION_PREFIX)) {
                    version = trimmed.substring(INSTALLED_VERSION_PREFIX.length());
                } else if (!trimmed.isEmpty()) {
                    entries.add(trimmed);
                }
            }
            return new InstalledMarker(version, Set.copyOf(entries));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read '" + markerFile.getName() + "'", exception);
        }
    }

    private static void writeMarker(File markerFile, String version, Set<String> resources) {
        try {
            List<String> contents = new java.util.ArrayList<>(resources.size() + 1);
            contents.add(INSTALLED_VERSION_PREFIX + version);
            contents.addAll(resources);
            Files.write(markerFile.toPath(), contents, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not write '" + markerFile.getName() + "'", exception);
        }
    }

    private record InstalledMarker(String version, Set<String> resources) {
    }

    private static synchronized List<String> bundledFontResources(JavaPlugin plugin) {
        if (bundledFontResources != null) {
            return bundledFontResources;
        }
        URL location = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        try {
            bundledFontResources = resourcesIn(Path.of(location.toURI()));
            return bundledFontResources;
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Could not locate bundled fonts", exception);
        }
    }

    private static List<String> resourcesIn(Path pluginFile) {
        try (JarFile archive = new JarFile(pluginFile.toFile())) {
            TreeSet<String> resources = new TreeSet<>();
            archive.stream().map(JarEntry::getName).filter(FontRegistry::isFontResource).forEach(resources::add);
            return List.copyOf(resources);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read bundled fonts", exception);
        }
    }

    private static boolean isFontResource(String path) {
        return path.startsWith(FONT_RESOURCE_PREFIX) && path.endsWith(FONT_FILE_EXTENSION);
    }

    private static Optional<TitleFont> readFont(File file, Logger logger, Map<String, String> sourceByFile,
                                                 Map<String, TitleFont> fontByFile, FontRegistry previous) {
        try {
            String source = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            sourceByFile.put(file.getName(), source);
            String previousSource = previous == null ? null : previous.sourceByFile.get(file.getName());
            TitleFont font = source.equals(previousSource) ? previous.fontByFile.get(file.getName()) : FontFileReader.read(file, source);
            if (font != null) {
                fontByFile.put(file.getName(), font);
            }
            return Optional.ofNullable(font);
        } catch (IOException | IllegalStateException exception) {
            logger.warning(exception.getMessage());
            return Optional.empty();
        }
    }

    private static void putIfAbsent(Map<String, TitleFont> fonts, TitleFont font, File file, Logger logger) {
        if (fonts.putIfAbsent(font.id(), font) != null) {
            logger.warning("Duplicate font id '" + font.id() + "' in file '" + file.getName() + "' was ignored");
        }
    }

    private static Map<String, TitleFont> defaultFontFirst(Map<String, TitleFont> fonts) {
        Map<String, TitleFont> orderedFonts = new LinkedHashMap<>();
        TitleFont defaultFont = fonts.get(DEFAULT_FONT_ID);
        if (defaultFont != null) {
            orderedFonts.put(defaultFont.id(), defaultFont);
        }
        fonts.forEach((id, font) -> {
            if (!DEFAULT_FONT_ID.equals(id)) {
                orderedFonts.put(id, font);
            }
        });
        return orderedFonts;
    }
}
