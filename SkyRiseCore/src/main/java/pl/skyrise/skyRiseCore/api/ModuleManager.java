package pl.skyrise.skyRiseCore.api;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ModuleManager {

    private final JavaPlugin plugin;
    private final Map<String, Module> modules;

    public ModuleManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.modules = new LinkedHashMap<>();
    }

    /**
     * Rejestruje i włącza moduł.
     */
    public void register(Module module) {
        try {
            module.onEnable();
            modules.put(module.getName().toLowerCase(), module);
            plugin.getLogger().info("  ✔ Moduł " + module.getName() + " załadowany.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "  ✘ Błąd podczas ładowania modułu " + module.getName(), e);
        }
    }

    /**
     * Wyłącza i wyrejestrowuje pojedynczy moduł.
     */
    public void unregister(String name) {
        Module module = modules.remove(name.toLowerCase());
        if (module != null) {
            try {
                module.onDisable();
                plugin.getLogger().info("  ✘ Moduł " + module.getName() + " wyładowany.");
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas wyłączania modułu " + module.getName(), e);
            }
        }
    }

    /**
     * Wyłącza wszystkie moduły — wywoływane przy onDisable().
     */
    public void disableAll() {
        for (Module module : modules.values()) {
            try {
                module.onDisable();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas wyłączania modułu " + module.getName(), e);
            }
        }
        modules.clear();
    }

    /**
     * Przeładowuje konkretny moduł po nazwie.
     * @return true jeśli moduł istnieje i został przeładowany
     */
    public boolean reload(String name) {
        Module module = modules.get(name.toLowerCase());
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

    /**
     * Przeładowuje wszystkie zarejestrowane moduły.
     */
    public void reloadAll() {
        for (Module module : modules.values()) {
            try {
                module.onReload();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Błąd podczas przeładowania modułu " + module.getName(), e);
            }
        }
        plugin.getLogger().info("  ↻ Przeładowano wszystkie moduły (" + modules.size() + ").");
    }

    /**
     * Zwraca moduł po nazwie (lub null).
     */
    @SuppressWarnings("unchecked")
    public <T extends Module> T getModule(String name) {
        return (T) modules.get(name.toLowerCase());
    }

    public int getModuleCount() {
        return modules.size();
    }

    public Set<String> getModuleNames() {
        return Collections.unmodifiableSet(modules.keySet());
    }

    public Map<String, Module> getModules() {
        return Collections.unmodifiableMap(modules);
    }
}
