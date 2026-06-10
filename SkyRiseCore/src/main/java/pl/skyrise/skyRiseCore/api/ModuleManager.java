package pl.skyrise.skyRiseCore.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ModuleManager {

    private final JavaPlugin plugin;
    private final Map<String, Module> allModules;
    private final Set<String> enabledModules;

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.allModules = new LinkedHashMap<>();
        this.enabledModules = new HashSet<>();
    }

    public void register(Module module) {
        allModules.put(module.getName().toLowerCase(), module);
        enable(module.getName());
    }

    public boolean enable(String name) {
        String key = name.toLowerCase();
        Module module = allModules.get(key);
        if (module == null) return false;
        if (enabledModules.contains(key)) return false;

        try {
            module.onEnable();
            enabledModules.add(key);
            plugin.getLogger().info("  ✔ Moduł " + module.getName() + " włączony.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "  ✘ Błąd podczas włączania modułu " + module.getName(), e);
            return false;
        }
    }

    public boolean disable(String name) {
        String key = name.toLowerCase();
        Module module = allModules.get(key);
        if (module == null) return false;
        if (!enabledModules.contains(key)) return false;

        try {
            module.onDisable();
            enabledModules.remove(key);
            plugin.getLogger().info("  ✘ Moduł " + module.getName() + " wyłączony.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas wyłączania modułu " + module.getName(), e);
            return false;
        }
    }

    public void unregister(String name) {
        disable(name);
        allModules.remove(name.toLowerCase());
    }

    public void disableAll() {
        for (String key : new HashSet<>(enabledModules)) {
            disable(key);
        }
    }

    public boolean reload(String name) {
        String key = name.toLowerCase();
        if (!enabledModules.contains(key)) return false;
        Module module = allModules.get(key);
        if (module == null) return false;

        try {
            module.onReload();
            plugin.getLogger().info("  ↻ Moduł " + module.getName() + " przeładowany.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Błąd podczas przeładowania modułu " + module.getName(), e);
            return false;
        }
    }

    public void reloadAll() {
        for (String key : enabledModules) {
            reload(key);
        }
        plugin.getLogger().info("  ↻ Przeładowano włączone moduły (" + enabledModules.size() + ").");
    }

    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(String name) {
        return (T) allModules.get(name.toLowerCase());
    }
    
    public boolean isEnabled(String name) {
        return enabledModules.contains(name.toLowerCase());
    }

    public int getModuleCount() {
        return allModules.size();
    }
    
    public int getEnabledModuleCount() {
        return enabledModules.size();
    }

    public Set<String> getModuleNames() {
        return Collections.unmodifiableSet(allModules.keySet());
    }

    public Map<String, Module> getModules() {
        return Collections.unmodifiableMap(allModules);
    }
}
