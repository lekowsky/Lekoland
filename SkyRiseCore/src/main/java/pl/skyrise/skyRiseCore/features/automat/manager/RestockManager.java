package pl.skyrise.skyRiseCore.features.automat.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.features.automat.util.ColorUtil;

import java.util.HashMap;
import java.util.Map;

public class RestockManager {

    private final AutomatModule plugin;
    private final Map<String, BukkitTask> restockTasks;  // templateName -> task
    private BukkitTask lowStockScanTask;

    public RestockManager(AutomatModule plugin) {
        this.plugin = plugin;
        this.restockTasks = new HashMap<>();
    }

    public void startAll() {
        for (MachineTemplate template : plugin.getMachineManager().getAllTemplates()) {
            if (template.isAutoRestockEnabled()) {
                startRestock(template);
            }
        }
        plugin.getLogger().info("[Restock] Started " + restockTasks.size() + " restock timers.");

        // Start okresowego skanowania niskiego stocku
        startLowStockScan();
    }

    public void stopAll() {
        for (BukkitTask task : restockTasks.values()) {
            task.cancel();
        }
        restockTasks.clear();

        if (lowStockScanTask != null) {
            lowStockScanTask.cancel();
            lowStockScanTask = null;
        }
    }

    /**
     * Startuje timer auto-restocku dla danego szablonu.
     * Restock wykonywany jest na KAŻDYM placementcie tego szablonu osobno.
     */
    public void startRestock(MachineTemplate template) {
        stopRestock(template.getName());

        if (!template.isAutoRestockEnabled()) return;

        long intervalTicks = template.getAutoRestockInterval() * 60L * 20L;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin.getPlugin(), () -> {
            int restocked = restockAllPlacementsForTemplate(template);

            if (restocked > 0) {
                plugin.getDataManager().savePlacements();

                if (plugin.getConfig().getBoolean("auto-restock.notify-on-restock", true)) {
                    plugin.getLogger().info("[Restock] Auto-restocked " + restocked +
                            " automat(y) typu '" + template.getName() +
                            "' (+" + template.getAutoRestockAmount() + " szt./item)");
                }

                if (plugin.getConfig().getBoolean("auto-restock.broadcast-restock", false)) {
                    String msg = ColorUtil.color(plugin.getConfig().getString(
                            "auto-restock.broadcast-message",
                            "&#38f28f&lAutomat &7» &fUzupełniono zapasy automatu &e{template}&f."
                    ).replace("{template}", template.getName()));
                    Bukkit.broadcastMessage(msg);
                }

                playRestockSound(template);
            }

        }, intervalTicks, intervalTicks);

        restockTasks.put(template.getName().toLowerCase(), task);
        plugin.getLogger().info("[Restock] Timer for '" + template.getName() +
                "' - every " + template.getAutoRestockInterval() + " min, +" +
                template.getAutoRestockAmount() + " items per placement");
    }

    /**
     * Wykonuje restock na wszystkich placementach danego szablonu.
     * Zwraca liczbę placementów na których faktycznie coś dodano.
     */
    public int restockAllPlacementsForTemplate(MachineTemplate template) {
        int count = 0;
        int amount = template.getAutoRestockAmount();

        for (MachinePlacement placement : plugin.getPlacementManager().getAllPlacements()) {
            if (!placement.getTemplateName().equalsIgnoreCase(template.getName())) continue;

            boolean added = false;
            for (VendingItem item : template.getItems().values()) {
                if (item.isUnlimitedStock()) continue;
                int actuallyAdded = placement.addStock(item.getId(), amount, item.getMaxStock());
                if (actuallyAdded > 0) added = true;
            }
            if (added) count++;
        }
        return count;
    }

    /**
     * Natychmiastowy restock pojedynczego placementu (do max).
     */
    public void fillPlacement(MachinePlacement placement) {
        MachineTemplate template = plugin.getMachineManager().getTemplate(placement.getTemplateName());
        if (template == null) return;

        for (VendingItem item : template.getItems().values()) {
            if (item.isUnlimitedStock()) continue;
            placement.setStock(item.getId(), item.getMaxStock());
        }
        plugin.getDataManager().savePlacements();
    }

    public void stopRestock(String templateName) {
        BukkitTask task = restockTasks.remove(templateName.toLowerCase());
        if (task != null) {
            task.cancel();
        }
    }

    public void restartRestock(MachineTemplate template) {
        stopRestock(template.getName());
        if (template.isAutoRestockEnabled()) {
            startRestock(template);
        }
    }

    // ==================== POWIADOMIENIA O NISKIM STOCKU ====================

    /**
     * Cykliczne skanowanie automatów pod kątem niskiego stocku.
     * Powiadamia graczy z permisją vendingmachine.restock.notify.
     */
    private void startLowStockScan() {
        if (!plugin.getConfig().getBoolean("stock.low-stock-warning.enabled", true)) return;

        // Skanowanie co X minut (domyślnie 5 min)
        int scanIntervalMin = plugin.getConfig().getInt("stock.low-stock-warning.scan-interval", 5);
        long ticks = scanIntervalMin * 60L * 20L;

        lowStockScanTask = Bukkit.getScheduler().runTaskTimer(plugin.getPlugin(), this::scanAllForLowStock, ticks, ticks);
        plugin.getLogger().info("[Stock] Low-stock scanner started (co " + scanIntervalMin + " min)");
    }

    /**
     * Skanuje wszystkie placementy pod kątem niskiego stocku
     * i wysyła powiadomienie zbiorcze.
     */
    public void scanAllForLowStock() {
        int threshold = plugin.getConfig().getInt("stock.low-stock-warning.threshold", 5);

        int emptyCount = 0;
        int lowCount = 0;

        for (MachinePlacement placement : plugin.getPlacementManager().getAllPlacements()) {
            MachineTemplate template = plugin.getMachineManager().getTemplate(placement.getTemplateName());
            if (template == null) continue;

            boolean hasEmpty = false;
            boolean hasLow = false;

            for (VendingItem item : template.getItems().values()) {
                if (item.isUnlimitedStock()) continue;
                int stock = placement.getStock(item.getId());
                if (stock == 0) hasEmpty = true;
                else if (stock <= threshold) hasLow = true;
            }

            if (hasEmpty) emptyCount++;
            else if (hasLow) lowCount++;
        }

        if (emptyCount == 0 && lowCount == 0) return;

        String summary = plugin.getPrefix() + ColorUtil.color(
                "&c⚠ Automaty wymagają uzupełnienia! &cWyprzedane: &f" + emptyCount +
                        " &7| &eNiski stock: &f" + lowCount +
                        " &8(/automat restocklist)"
        );

        boolean logConsole = plugin.getConfig().getBoolean("stock.low-stock-warning.log-to-console", true);
        if (logConsole) plugin.getLogger().info(ColorUtil.strip(summary));

        notifyRestockers(summary);
    }

    /**
     * Powiadomienie wysyłane natychmiast gdy item zostanie wyprzedany przy zakupie.
     * Wywoływane z MachineGUI po zakupie.
     */
    public void notifyItemSoldOut(MachinePlacement placement, VendingItem item) {
        MachineTemplate template = plugin.getMachineManager().getTemplate(placement.getTemplateName());
        if (template == null) return;

        Location loc = placement.getLocation();
        String locStr = loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

        String msg = plugin.getPrefix() + ColorUtil.color(
                plugin.getConfig().getString("messages.stock-empty",
                                "&c⚠ Wyprzedano &f{item} &cw automacie &6{template}&c!")
                        .replace("{item}", ColorUtil.color(item.getDisplayName()))
                        .replace("{template}", template.getName())
                        .replace("{location}", locStr)
        );

        if (plugin.getConfig().getBoolean("stock.low-stock-warning.log-to-console", true)) {
            plugin.getLogger().warning("[Stock] " + ColorUtil.strip(msg) + " @ " + locStr);
        }

        notifyRestockers(msg);
    }

    /**
     * Powiadomienie gdy stock spadnie poniżej threshold po zakupie.
     */
    public void notifyItemLowStock(MachinePlacement placement, VendingItem item, int currentStock) {
        MachineTemplate template = plugin.getMachineManager().getTemplate(placement.getTemplateName());
        if (template == null) return;

        Location loc = placement.getLocation();
        String locStr = loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

        String msg = plugin.getPrefix() + ColorUtil.color(
                plugin.getConfig().getString("messages.stock-low-warning",
                                "&e⚠ Niski stock w automacie &6{template}&e: &f{item} &7(&e{stock}&7/&e{max}&7)")
                        .replace("{item}", ColorUtil.color(item.getDisplayName()))
                        .replace("{template}", template.getName())
                        .replace("{stock}", String.valueOf(currentStock))
                        .replace("{max}", String.valueOf(item.getMaxStock()))
                        .replace("{location}", locStr)
        );

        notifyRestockers(msg);
    }

    /**
     * Wysyła wiadomość do wszystkich graczy online z permisją vendingmachine.restock.notify
     */
    private void notifyRestockers(String message) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("vendingmachine.restock.notify")) {
                p.sendMessage(message);
            }
        }
    }

    private void playRestockSound(MachineTemplate template) {
        try {
            String soundName = plugin.getConfig().getString("sounds.restock", "BLOCK_NOTE_BLOCK_PLING");
            Sound sound = Sound.valueOf(soundName);

            plugin.getPlacementManager().getAllPlacements().stream()
                    .filter(p -> p.getTemplateName().equalsIgnoreCase(template.getName()))
                    .forEach(placement -> {
                        Location loc = placement.getLocation();
                        if (loc.getWorld() == null) return;
                        for (Player p : loc.getWorld().getPlayers()) {
                            if (p.getLocation().distanceSquared(loc) <= 100) {
                                p.playSound(loc, sound, 0.3f, 1.2f);
                            }
                        }
                    });
        } catch (Exception ignored) {}
    }
}