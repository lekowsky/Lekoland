package pl.skyrise.skyRiseCore.features.adminchat;

import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AdminChatModule implements Module {

    private final JavaPlugin plugin;
    private final TabRegistry tabRegistry;
    private final MessageCache messageCache;
    private CustomConfig acConfig;

    /** Thread-safe zbiór UUID graczy z włączonym toggle. */
    private final Set<UUID> toggledPlayers = ConcurrentHashMap.newKeySet();

    // --- Cache wartości z configa ---
    private String permission;
    private String format;
    private String toggleOnMsg;
    private String toggleOffMsg;

    public AdminChatModule(JavaPlugin plugin, TabRegistry tabRegistry, MessageCache messageCache) {
        this.plugin = plugin;
        this.tabRegistry = tabRegistry;
        this.messageCache = messageCache;
    }

    @Override
    public String getName() {
        return "AdminChat";
    }

    @Override
    public void onEnable() {
        acConfig = new CustomConfig(plugin, "adminchat.yml");
        acConfig.load();
        cacheConfig();

        // Komenda
        plugin.getCommand("adminchat").setExecutor(new AdminChatCommand(this));

        // Tab completion — rejestracja w centralnym TabRegistry
        tabRegistry.register("adminchat", (sender, args) -> TabRegistry.filter(
                java.util.List.of(), args.length > 0 ? args[args.length - 1] : ""
        ));

        // Listener
        plugin.getServer().getPluginManager().registerEvents(new AdminChatListener(this), plugin);

        plugin.getLogger().info("  → AdminChat: /ac, /adminchat");
    }

    @Override
    public void onDisable() {
        toggledPlayers.clear();
        tabRegistry.unregister("adminchat");
        if (acConfig != null) {
            acConfig.save();
        }
    }

    @Override
    public void onReload() {
        acConfig.reload();
        cacheConfig();
    }

    private void cacheConfig() {
        this.permission = acConfig.getConfig().getString("permission", "skyrise.adminchat");
        this.format = acConfig.getConfig().getString("messages.format",
                "<dark_gray>[<dark_red>ADMIN</dark_red>]</dark_gray> <red>{player} »</red> <white>{message}");
        this.toggleOnMsg = acConfig.getConfig().getString("messages.toggle-on",
                "<#99CCFF>»</#99CCFF> <green>AdminChat włączony.");
        this.toggleOffMsg = acConfig.getConfig().getString("messages.toggle-off",
                "<#99CCFF>»</#99CCFF> <red>AdminChat wyłączony.");
    }

    // --- Gettery — zwracają cached wartości, O(1) ---

    public String getPermission() {
        return permission;
    }

    public String getFormat() {
        return format;
    }

    public String getToggleOnMsg() {
        return toggleOnMsg;
    }

    public String getToggleOffMsg() {
        return toggleOffMsg;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    // --- Toggle ---

    public boolean isToggled(UUID uuid) {
        return toggledPlayers.contains(uuid);
    }

    public void toggleOn(UUID uuid) {
        toggledPlayers.add(uuid);
    }

    public void toggleOff(UUID uuid) {
        toggledPlayers.remove(uuid);
    }
}
