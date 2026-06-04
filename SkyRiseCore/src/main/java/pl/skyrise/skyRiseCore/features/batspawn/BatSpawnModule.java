package pl.skyrise.skyRiseCore.features.batspawn;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public class BatSpawnModule implements Module {

    private final JavaPlugin plugin;
    private final MessageCache messageCache;
    private CustomConfig config;

    // --- Cache ---
    private int maxY;
    private Set<String> allowedBlocks;   // Set zamiast List — O(1) lookup
    private String denyMessage;

    public BatSpawnModule(JavaPlugin plugin, MessageCache messageCache) {
        this.plugin = plugin;
        this.messageCache = messageCache;
    }

    @Override
    public String getName() {
        return "BatSpawn";
    }

    @Override
    public void onEnable() {
        config = new CustomConfig(plugin, "batspawn.yml");
        config.load();
        cacheConfig();

        plugin.getServer().getPluginManager().registerEvents(new BatSpawnListener(this, messageCache), plugin);

        plugin.getLogger().info("  → BatSpawn: nietoperze zablokowane powyżej Y " + maxY);
    }

    @Override
    public void onDisable() {
        if (config != null) {
            config.save();
        }
    }

    @Override
    public void onReload() {
        config.reload();
        cacheConfig();
    }

    private void cacheConfig() {
        this.maxY = config.getConfig().getInt("max-y", 40);

        Set<String> blocks = new HashSet<>();
        for (String block : config.getConfig().getStringList("allowed-blocks")) {
            blocks.add(block.toUpperCase());
        }
        if (blocks.isEmpty()) {
            blocks = Set.of("STONE", "DEEPSLATE");
        }
        this.allowedBlocks = Collections.unmodifiableSet(blocks);

        this.denyMessage = config.getConfig().getString("deny-message",
                "<red>»</red> <red>Nietoperze mogą spawnować się tylko poniżej Y {max_y} na blokach stone/deepslate.")
                .replace("{max_y}", String.valueOf(maxY));
    }

    /**
     * Sprawdza czy materiał jest na liście dozwolonych — O(1).
     */
    public boolean isAllowed(Material material) {
        return allowedBlocks.contains(material.name());
    }

    public int getMaxY() {
        return maxY;
    }

    public String getDenyMessage() {
        return denyMessage;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
