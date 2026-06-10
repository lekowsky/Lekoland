package pl.skyrise.skyRiseCore.utils;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class CustomConfig {

    private final JavaPlugin plugin;
    private final String fileName;
    private final File file;
    private org.bukkit.configuration.file.YamlConfiguration config;

    /**
     * @param plugin   instancja pluginu
     * @param fileName np. "chat.yml" — plik w folderze config/
     */
    public CustomConfig(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), "config" + File.separator + fileName);
    }

    /**
     * Ładuje config — tworzy z zasobów jeśli nie istnieje.
     */
    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                InputStream resource = plugin.getResource("config/" + fileName);
                if (resource != null) {
                    Files.copy(resource, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Nie można utworzyć configu: " + fileName);
                e.printStackTrace();
            }
        }
        config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie można zapisać configu: " + fileName);
            e.printStackTrace();
        }
    }

    public void reload() {
        config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.YamlConfiguration getConfig() {
        return config;
    }

    public String getFileName() {
        return fileName;
    }
}
