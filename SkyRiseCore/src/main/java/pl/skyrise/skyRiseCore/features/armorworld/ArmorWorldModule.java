package pl.skyrise.skyRiseCore.features.armorworld;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import pl.skyrise.skyRiseCore.api.Module;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.core.TabRegistry;
import pl.skyrise.skyRiseCore.utils.CustomConfig;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorWorldModule implements Module {

    private org.bukkit.event.Listener listener;

    private final JavaPlugin plugin;
    private final TabRegistry tabRegistry;
    private final MessageCache messageCache;
    private CustomConfig config;

    // --- Cache ---
    private final Set<String> blockedWorlds = ConcurrentHashMap.newKeySet();
    private Set<Material> blockedMaterials;
    private String denyMessage;
    private String bypassPermission;

    /** Pre-cache: Material → czytelna nazwa (np. NETHERITE_HELMET → "Netherite Helmet") */
    private Map<Material, String> materialNames;
    private org.bukkit.scheduler.BukkitTask checkTask;

    public ArmorWorldModule(JavaPlugin plugin, TabRegistry tabRegistry, MessageCache messageCache) {
        this.plugin = plugin;
        this.tabRegistry = tabRegistry;
        this.messageCache = messageCache;
    }

    @Override
    public String getName() {
        return "ArmorWorld";
    }

    @Override
    public void onEnable() {
        config = new CustomConfig(plugin, "armorworld.yml");
        config.load();
        cacheConfig();

        this.listener = new ArmorWorldListener(this, messageCache);
        plugin.getServer().getPluginManager().registerEvents(this.listener, plugin);

        ArmorWorldCommand command = new ArmorWorldCommand(this);
        plugin.getCommand("armorworld").setExecutor(command);

        tabRegistry.register("armorworld", (sender, args) -> {
            if (args.length == 1) {
                return TabRegistry.filter(List.of("add", "remove", "list", "off", "items"), args[0]);
            }
            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                    List<String> worlds = new ArrayList<>();
                    plugin.getServer().getWorlds().forEach(w -> worlds.add(w.getName()));
                    return TabRegistry.filter(worlds, args[1]);
                }
            }
            return List.of();
        });

        plugin.getLogger().info("  → ArmorWorld: " + blockedWorlds.size() + " światów, " + blockedMaterials.size() + " przedmiotów");

        // Bulletproof active protection task
        checkTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                // 1. Szybkie sprawdzenie świata (O(1) HashSet lookup) - chroni przed dalszymi kalkulacjami
                if (!isWorldBlocked(player.getWorld().getName())) continue;

                // 2. Sprawdzenie czy gracz w ogóle nosi zablokowaną zbroję (0 alokacji pamięci - absolutne 0% GC pressure!)
                if (!hasBlockedArmor(player)) continue;

                // 3. Dopiero gdy gracz faktycznie ma zablokowaną zbroję, sprawdzamy permisję bypass
                // Unika to ciągłego, niepotrzebnego odpytywania systemu uprawnień (np. LuckPerms) dla każdego gracza!
                if (player.hasPermission(getBypassPermission())) continue;

                // Pobieramy listę zablokowanych (alokacja następuje TYLKO w rzadkim momencie złamania blokady)
                List<Material> blocked = getBlockedArmor(player);
                if (blocked.isEmpty()) continue;

                // Force take off!
                for (ItemStack item : player.getInventory().getArmorContents()) {
                    if (item != null && item.getType() != Material.AIR && isBlocked(item.getType())) {
                        // Try to add to inventory, or drop it
                        Map<Integer, ItemStack> left = player.getInventory().addItem(item.clone());
                        if (!left.isEmpty()) {
                            for (ItemStack drop : left.values()) {
                                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                            }
                        }
                        // Clear the armor slot
                        if (item.equals(player.getInventory().getHelmet())) player.getInventory().setHelmet(null);
                        else if (item.equals(player.getInventory().getChestplate())) player.getInventory().setChestplate(null);
                        else if (item.equals(player.getInventory().getLeggings())) player.getInventory().setLeggings(null);
                        else if (item.equals(player.getInventory().getBoots())) player.getInventory().setBoots(null);
                    }
                }
                sendDenyMessage(player, blocked.get(0));
            }
        }, 5L, 5L);
    }

    @Override
    public void onDisable() {
        java.util.Optional.ofNullable(plugin.getCommand("armorworld")).ifPresent(c -> c.setExecutor(null));
        if (this.listener != null) {
            org.bukkit.event.HandlerList.unregisterAll(this.listener);
            this.listener = null;
        }
        if (checkTask != null) {
            checkTask.cancel();
        }
        tabRegistry.unregister("armorworld");
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
        blockedWorlds.clear();
        blockedWorlds.addAll(config.getConfig().getStringList("blocked-worlds"));

        // Parsuj String → Material raz (zamiast String.contains() przy każdym evencie)
        Set<Material> materials = EnumSet.noneOf(Material.class);
        Map<Material, String> names = new EnumMap<>(Material.class);

        for (String item : config.getConfig().getStringList("blocked-items")) {
            try {
                Material mat = Material.valueOf(item.toUpperCase());
                materials.add(mat);
                names.put(mat, prettify(item.toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }

        // Default jeśli pusty
        if (materials.isEmpty()) {
            for (Material mat : new Material[]{
                    Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
                    Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
            }) {
                materials.add(mat);
                names.put(mat, prettify(mat.name()));
            }
        }

        this.blockedMaterials = Collections.unmodifiableSet(materials);
        this.materialNames = Collections.unmodifiableMap(names);

        this.denyMessage = config.getConfig().getString("deny-message",
                "<red>» Zbroja jest zakazana w mieście.");
        this.bypassPermission = config.getConfig().getString("bypass-permission", "skyrise.armorworld.bypass");
    }

    /** NETHERITE_HELMET → Netherite Helmet (cache'owane w materialNames) */
    private static String prettify(String raw) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : raw.toCharArray()) {
            if (c == '_') {
                sb.append(' ');
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                upper = false;
            }
        }
        return sb.toString();
    }

    // --- Sprawdzanie zbroi ---

    /**
     * Sprawdza czy przedmiot jest zablokowany — O(1), EnumSet.
     */
    public boolean isBlocked(Material mat) {
        return blockedMaterials.contains(mat);
    }

    /**
     * Sprawdza, czy gracz nosi jakąkolwiek zablokowaną zbroję.
     * Metoda ta alokuje dokładnie 0 bajtów pamięci (0% GC pressure).
     */
    public boolean hasBlockedArmor(Player player) {
        return slotMaterial(player.getInventory().getHelmet()) != null
                || slotMaterial(player.getInventory().getChestplate()) != null
                || slotMaterial(player.getInventory().getLeggings()) != null
                || slotMaterial(player.getInventory().getBoots()) != null;
    }

    /**
     * Zwraca zablokowane przedmioty z noszonych slotów — iteracja tylko RAZ.
     * Zwraca pustą listę jeśli brak (caller sprawdza isEmpty() zamiast hasBlockedArmor()).
     */
    public List<Material> getBlockedArmor(Player player) {
        List<Material> found = null;

        Material m = slotMaterial(player.getInventory().getHelmet());
        if (m != null) { found = new ArrayList<>(); found.add(m); }

        m = slotMaterial(player.getInventory().getChestplate());
        if (m != null) { if (found == null) found = new ArrayList<>(); found.add(m); }

        m = slotMaterial(player.getInventory().getLeggings());
        if (m != null) { if (found == null) found = new ArrayList<>(); found.add(m); }

        m = slotMaterial(player.getInventory().getBoots());
        if (m != null) { if (found == null) found = new ArrayList<>(); found.add(m); }

        return found != null ? found : List.of();
    }

    /** Zwraca Material jeśli zablokowany, null jeśli OK. */
    private Material slotMaterial(ItemStack item) {
        if (item == null) return null;
        Material mat = item.getType();
        return blockedMaterials.contains(mat) ? mat : null;
    }

    /**
     * Formatuje listę zablokowanych materiałów — używa pre-cache'owanych nazw.
     */
    public String formatBlocked(List<Material> blocked) {
        if (blocked.isEmpty()) return "";
        if (blocked.size() == 1) return "<gold>" + materialNames.get(blocked.get(0));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < blocked.size(); i++) {
            if (i > 0) sb.append("<dark_gray>, </dark_gray>");
            sb.append("<gold>").append(materialNames.get(blocked.get(i)));
        }
        return sb.toString();
    }

    /**
     * Formatuje pojedynczy materiał — z pre-cache.
     */
    public String formatMaterial(Material mat) {
        return materialNames.getOrDefault(mat, prettify(mat.name()));
    }

    // --- Zarządzanie światami ---

    public boolean addWorld(String world) {
        if (blockedWorlds.contains(world.toLowerCase())) return false;
        blockedWorlds.add(world.toLowerCase());
        saveWorlds();
        return true;
    }

    public boolean removeWorld(String world) {
        if (!blockedWorlds.contains(world.toLowerCase())) return false;
        blockedWorlds.remove(world.toLowerCase());
        saveWorlds();
        return true;
    }

    public void clearWorlds() {
        blockedWorlds.clear();
        saveWorlds();
    }

    private void saveWorlds() {
        config.getConfig().set("blocked-worlds", new ArrayList<>(blockedWorlds));
        config.save();
    }

    // --- Gettery ---

    public boolean isWorldBlocked(String world) {
        return blockedWorlds.contains(world.toLowerCase());
    }

    public String getDenyMessage() {
        return denyMessage;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public Set<String> getBlockedWorlds() {
        return Collections.unmodifiableSet(blockedWorlds);
    }

    public Set<Material> getBlockedMaterials() {
        return blockedMaterials;
    }

    public void sendDenyMessage(Player player, Material mat) {
        if (!messageCache.canSend(player.getUniqueId(), "armorworld:equip")) return;
        player.sendMessage(pl.skyrise.skyRiseCore.utils.ColorUtil.mini(getDenyMessage()));
        player.sendMessage(pl.skyrise.skyRiseCore.utils.ColorUtil.mini("<red>» Zablokowane: <gold>" + formatMaterial(mat)));
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
