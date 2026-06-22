package pl.skyrise.skyRiseCore.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public class CustomConfig {

    private final JavaPlugin plugin;
    private final String fileName;
    private final String moduleFolderName;
    private final File file;
    private org.bukkit.configuration.file.YamlConfiguration config;

    /**
     * @param plugin   instancja pluginu
     * @param fileName np. "chat.yml" — zapis runtime: plugins/SkyRiseCore/chat/config.yml
     */
    public CustomConfig(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.moduleFolderName = moduleFolderName(fileName);
        this.file = new File(plugin.getDataFolder(), moduleFolderName + File.separator + "config.yml");
    }

    /**
     * Ładuje config — migruje stary układ /config/*.yml i tworzy z zasobów jeśli nie istnieje.
     */
    public void load() {
        migrateLegacyConfigIfNeeded();
        if (!file.exists()) {
            try {
                File parent = file.getParentFile();
                if (parent != null) parent.mkdirs();

                try (InputStream resource = findResource()) {
                    if (resource != null) {
                        Files.copy(resource, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        file.createNewFile();
                    }
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Nie można utworzyć configu modułu " + moduleFolderName + ": " + fileName);
                e.printStackTrace();
            }
        }
        config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        if (config == null) return;
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać configu modułu " + moduleFolderName + ": " + fileName);
            e.printStackTrace();
        }
    }

    public void reload() {
        if (!file.exists()) {
            load();
            return;
        }
        config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.YamlConfiguration getConfig() {
        return config;
    }

    public String getFileName() {
        return fileName;
    }

    public String getModuleFolderName() {
        return moduleFolderName;
    }

    public File getFile() {
        return file;
    }

    private InputStream findResource() {
        InputStream resource = plugin.getResource("config/" + fileName);
        if (resource != null) return resource;
        return plugin.getResource(fileName);
    }

    private void migrateLegacyConfigIfNeeded() {
        if (file.exists()) return;

        File oldConfigFile = new File(plugin.getDataFolder(), "config" + File.separator + fileName);
        if (oldConfigFile.exists()) {
            moveLegacy(oldConfigFile);
            cleanupEmptyDirectory(oldConfigFile.getParentFile());
            return;
        }

        File oldRootFile = new File(plugin.getDataFolder(), fileName);
        if (oldRootFile.exists()) {
            moveLegacy(oldRootFile);
        }
    }

    private void moveLegacy(File legacyFile) {
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Files.move(legacyFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            plugin.getLogger().info("Przeniesiono config modułu " + moduleFolderName + " do "
                    + moduleFolderName + File.separator + "config.yml");
        } catch (IOException e) {
            plugin.getLogger().warning("Nie udało się przenieść starego configu " + legacyFile.getPath()
                    + ": " + e.getMessage());
        }
    }

    private void cleanupEmptyDirectory(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        String[] children = dir.list();
        if (children == null || children.length == 0) {
            dir.delete();
        }
    }

    private static String moduleFolderName(String fileName) {
        String name = fileName == null ? "module" : fileName.toLowerCase(Locale.ROOT).trim();
        if (name.endsWith(".yml")) name = name.substring(0, name.length() - 4);
        if (name.endsWith(".yaml")) name = name.substring(0, name.length() - 5);
        name = name.replaceAll("[^a-z0-9_-]", "");
        return name.isBlank() ? "module" : name;
    }
}
