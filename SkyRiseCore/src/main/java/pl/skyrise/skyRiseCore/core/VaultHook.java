package pl.skyrise.skyRiseCore.core;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Opcjonalna integracja z Vault.
 * Jeśli Vault nie jest na serwerze — zwraca puste stringi, plugin działa normalnie.
 */
public final class VaultHook {

    private static Chat chat = null;
    private static Economy eco = null;
    private static boolean enabled = false;

    private VaultHook() {}

    /**
     * Łączy z Vault — wywołaj raz w onEnable().
     */
    public static void setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;

        try {
            RegisteredServiceProvider<Chat> rsp = Bukkit.getServicesManager().getRegistration(Chat.class);
            if (rsp != null) {
                chat = rsp.getProvider();
                enabled = true;
            }
            RegisteredServiceProvider<Economy> rspEco = Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rspEco != null) {
                eco = rspEco.getProvider();
            }
        } catch (Exception ignored) {
        }
    }

        public static Economy getEconomy() {
        return eco;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /**
     * Pobiera prefix gracza z Vault (np. z LuckPerms).
     * Zwraca pusty string jeśli Vault nie jest dostępny.
     */
    public static String getPrefix(Player player) {
        if (!enabled) return "";
        try {
            String prefix = chat.getPlayerPrefix(player);
            return prefix != null ? prefix : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Pobiera suffix gracza z Vault.
     */
    public static String getSuffix(Player player) {
        if (!enabled) return "";
        try {
            String suffix = chat.getPlayerSuffix(player);
            return suffix != null ? suffix : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Pobiera główną grupę gracza z Vault (np. "wlasciciel", "admin").
     * Zwraca "default" jeśli Vault nie jest dostępny.
     */
    public static String getPrimaryGroup(Player player) {
        if (!enabled) return "default";
        try {
            String group = chat.getPrimaryGroup(player);
            return group != null ? group : "default";
        } catch (Exception e) {
            return "default";
        }
    }
}
