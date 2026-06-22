package pl.skyrise.skyRiseCore.features.chat;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatModule implements Module {

    private org.bukkit.event.Listener listener;

    private final JavaPlugin plugin;
    private final TabRegistry tabRegistry;
    private final MessageCache messageCache;
    private CustomConfig config;

    // --- Cache ---
    private int radiusSquared;
    private int globalCooldownMs;
    private String globalFormat;
    private String cooldownMessage;
    private Map<String, String> groupColors;
    private String defaultGroupColor;

    public ChatModule(JavaPlugin plugin, TabRegistry tabRegistry, MessageCache messageCache) {
        this.plugin = plugin;
        this.tabRegistry = tabRegistry;
        this.messageCache = messageCache;
    }

    @Override
    public String getName() {
        return "Chat";
    }

    @Override
    public void onEnable() {
        config = new CustomConfig(plugin, "chat.yml");
        config.load();
        cacheConfig();

        this.listener = pl.skyrise.skyRiseCore.core.ModuleSupport.registerListener(plugin, new ChatListener(this));

        GlobalChatCommand command = new GlobalChatCommand(this);
        pl.skyrise.skyRiseCore.core.ModuleSupport.bindExecutor(plugin, command, "globalchat");

        tabRegistry.register("globalchat", (sender, args) -> java.util.List.of());

        plugin.getLogger().info("  → Chat: lokalny " + (int) Math.sqrt(radiusSquared) + " bloków, globalny /g (" + (globalCooldownMs / 1000) + "s cooldown)");
    }

    @Override
    public void onDisable() {
        pl.skyrise.skyRiseCore.core.ModuleSupport.bindDisabled(plugin, getName(), "globalchat");
        pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterListener(this.listener);
        this.listener = null;
        pl.skyrise.skyRiseCore.core.ModuleSupport.unregisterTabs(tabRegistry, "globalchat");
        pl.skyrise.skyRiseCore.core.ModuleSupport.saveConfig(config);
    }

    @Override
    public void onReload() {
        config.reload();
        cacheConfig();
    }

    private void cacheConfig() {
        int radius = config.getConfig().getInt("local-radius", 40);
        this.radiusSquared = radius * radius;
        this.globalCooldownMs = config.getConfig().getInt("global-cooldown", 30) * 1000;

        // Format identyczny jak EssentialsX z [G] z przodu
        // <reset> po każdej sekcji zabija bold/kolory z prefixu rangi
        this.globalFormat = config.getConfig().getString("global-format",
                "<white>[<aqua>G</aqua>]</white><reset> {prefix}<reset> <gray>{player}<reset> <dark_gray>»<reset> {group_color}<!bold>{message}");

        this.cooldownMessage = config.getConfig().getString("cooldown-message",
                "<red>» Musisz odczekać jeszcze {time} sekund przed ponownym użyciem czatu globalnego.");

        // Kolory per grupa — identyczne jak EssentialsX group-formats
        this.groupColors = new HashMap<>();
        ConfigurationSection section = config.getConfig().getConfigurationSection("group-colors");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                groupColors.put(key.toLowerCase(), section.getString(key, "<white>"));
            }
        }
        this.defaultGroupColor = groupColors.remove("default");
        if (defaultGroupColor == null) defaultGroupColor = "<white>";
    }

    // --- Cooldown ---

    public int getRemainingCooldown(UUID uuid) {
        long last = messageCache.getLastSend(uuid, "globalchat");
        if (last == 0) return 0;
        long remaining = globalCooldownMs - (System.currentTimeMillis() - last);
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    public void setCooldown(UUID uuid) {
        messageCache.setSent(uuid, "globalchat");
    }

    // --- Gettery ---

    public int getRadiusSquared() {
        return radiusSquared;
    }

    public String getGlobalFormat() {
        return globalFormat;
    }

    public String getCooldownMessage() {
        return cooldownMessage;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Zwraca kolor MiniMessage dla grupy gracza.
     */
    public String getGroupColor(String group) {
        return groupColors.getOrDefault(group.toLowerCase(), defaultGroupColor);
    }
}
