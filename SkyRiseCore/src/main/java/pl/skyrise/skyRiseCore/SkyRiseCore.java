package pl.skyrise.skyRiseCore;

import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.ModuleManager;
import pl.skyrise.skyRiseCore.core.CoreTabCompleter;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.core.SkyRiseCoreCommand;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.core.VaultHook;
import pl.skyrise.skyRiseCore.features.adminchat.AdminChatModule;
import pl.skyrise.skyRiseCore.features.armorworld.ArmorWorldModule;
import pl.skyrise.skyRiseCore.features.batspawn.BatSpawnModule;
import pl.skyrise.skyRiseCore.features.chat.ChatModule;

public class SkyRiseCore extends JavaPlugin {

    private static SkyRiseCore instance;
    private ModuleManager moduleManager;
    private TabRegistry tabRegistry;
    private MessageCache messageCache;

    @Override
    public void onEnable() {
        instance = this;
        moduleManager = new ModuleManager(this);
        tabRegistry = new TabRegistry();
        messageCache = new MessageCache(2000);

        VaultHook.setup();
        if (VaultHook.isEnabled()) {
            getLogger().info("Vault połączony — prefixy dostępne.");
        } else {
            getLogger().info("Vault nie znaleziony — prefixy wyłączone.");
        }

        getServer().getPluginManager().registerEvents(messageCache, this);
        getCommand("skyrisecore").setExecutor(new SkyRiseCoreCommand(this, tabRegistry));

        // Rejestruj moduły tutaj:
        moduleManager.register(new AdminChatModule(this, tabRegistry, messageCache));
        moduleManager.register(new BatSpawnModule(this, messageCache));
        moduleManager.register(new ArmorWorldModule(this, tabRegistry, messageCache));
        moduleManager.register(new ChatModule(this, tabRegistry, messageCache));

        CoreTabCompleter completer = new CoreTabCompleter(tabRegistry);
        for (String cmd : tabRegistry.getRegisteredCommands()) {
            getCommand(cmd).setTabCompleter(completer);
        }

        getLogger().info("SkyRiseCore włączony! Załadowano " + moduleManager.getModuleCount() + " modułów.");
    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
        getLogger().info("SkyRiseCore wyłączony.");
    }

    public static SkyRiseCore getInstance() {
        return instance;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public TabRegistry getTabRegistry() {
        return tabRegistry;
    }

    public MessageCache getMessageCache() {
        return messageCache;
    }
}
