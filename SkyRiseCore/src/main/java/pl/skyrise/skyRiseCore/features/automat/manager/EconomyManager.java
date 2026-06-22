package pl.skyrise.skyRiseCore.features.automat.manager;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;

public class EconomyManager {

    private final AutomatModule plugin;
    private Economy economy;

    public EconomyManager(AutomatModule plugin) {
        this.plugin = plugin;
    }

    public boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean isReady() {
        return economy != null;
    }

    public double getBalance(Player player) {
        if (economy == null || player == null) return 0.0;
        return economy.getBalance(player);
    }

    public boolean hasEnough(Player player, double amount) {
        if (amount <= 0.0) return true;
        return economy != null && player != null && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (amount <= 0.0) return true;
        return economy != null && player != null && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(Player player, double amount) {
        if (amount <= 0.0) return true;
        return economy != null && player != null && economy.depositPlayer(player, amount).transactionSuccess();
    }

    public String getCurrencySymbol() {
        return plugin.getConfig().getString("currency-symbol", "$");
    }
}
