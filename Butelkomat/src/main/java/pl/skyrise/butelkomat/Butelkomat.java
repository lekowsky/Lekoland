package pl.skyrise.butelkomat;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import pl.skyrise.butelkomat.command.MainCommand;
import pl.skyrise.butelkomat.listener.FurnitureListener;
import pl.skyrise.butelkomat.util.MessageUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class Butelkomat extends JavaPlugin {

    private static Butelkomat instance;
    private MessageUtil messageUtil;
    private Economy economy;

    private final List<AcceptedItem> acceptedItems = new ArrayList<>();
    private List<String> furnitureIds = new ArrayList<>();
    private boolean returnAll;

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("Nexo") == null) {
            getLogger().severe("Nexo nie znaleziony!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy()) {
            getLogger().severe("Vault/Economy nie znaleziony! Plugin wymaga Vault + plugin ekonomii.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        messageUtil = new MessageUtil(this);
        loadItems();

        var cmd = new MainCommand(this);
        getCommand("butelkomat").setExecutor(cmd);
        getCommand("butelkomat").setTabCompleter(cmd);

        Bukkit.getPluginManager().registerEvents(new FurnitureListener(this), this);

        getLogger().info("Butelkomat v" + getDescription().getVersion() + " załadowany!");
    }

    @Override
    public void onDisable() {
        saveItems();
    }

    public void reload() {
        reloadConfig();
        messageUtil.reload();
        loadItems();
    }

    // ==================== VAULT ====================

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);

        if (rsp == null) return false;

        economy = rsp.getProvider();
        return true;
    }

    public Economy getEconomy() {
        return economy;
    }

    // ==================== ŁADOWANIE / ZAPIS ====================

    public void loadItems() {
        acceptedItems.clear();
        furnitureIds = getConfig().getStringList("butelkomat-furniture-ids");
        returnAll = getConfig().getBoolean("return-all-in-hand", true);

        List<Map<?, ?>> itemsList = getConfig().getMapList("items");
        for (Map<?, ?> map : itemsList) {
            try {
                String data = (String) map.get("item-data");
                if (data == null) continue;

                ItemStack stack = deserializeItem(data);
                if (stack == null) continue;

                double price = 0;
                Object priceObj = map.get("price");
                if (priceObj instanceof Number num) {
                    price = num.doubleValue();
                }

                boolean checkName = getBool(map, "check-name", true);
                boolean checkLore = getBool(map, "check-lore", true);
                boolean checkNbt = getBool(map, "check-nbt", true);
                boolean strict = getBool(map, "strict", false);

                acceptedItems.add(new AcceptedItem(
                        stack, price, checkName, checkLore, checkNbt, strict));

            } catch (Exception e) {
                getLogger().warning("Błąd ładowania przedmiotu: " + e.getMessage());
            }
        }

        getLogger().info("Załadowano " + acceptedItems.size() + " przedmiotów.");
    }

    public void saveItems() {
        List<Map<String, Object>> itemsList = new ArrayList<>();

        for (AcceptedItem item : acceptedItems) {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("item-data", serializeItem(item.itemStack));
            map.put("price", item.price);
            map.put("check-name", item.checkName);
            map.put("check-lore", item.checkLore);
            map.put("check-nbt", item.checkNbt);
            map.put("strict", item.strict);
            itemsList.add(map);
        }

        getConfig().set("items", itemsList);
        saveConfig();
    }

    // ==================== LOGIKA BUTELKOMATU ====================

    public boolean isButelkomat(String furnitureId) {
        if (furnitureId == null) return false;
        for (String id : furnitureIds) {
            if (id.equalsIgnoreCase(furnitureId)) return true;
        }
        return false;
    }

    public AcceptedItem findMatch(ItemStack playerItem) {
        if (playerItem == null) return null;
        for (AcceptedItem accepted : acceptedItems) {
            if (matches(accepted, playerItem)) {
                return accepted;
            }
        }
        return null;
    }

    public void processReturn(Player player, ItemStack handItem) {
        AcceptedItem match = findMatch(handItem);
        if (match == null) {
            messageUtil.send(player, "item-not-accepted");
            playSound(player, "error");
            return;
        }

        int amount;
        if (returnAll) {
            amount = handItem.getAmount();
        } else {
            amount = 1;
        }

        double total = match.price * amount;

        // Zabierz przedmioty
        if (returnAll) {
            player.getInventory().setItemInMainHand(null);
        } else {
            if (handItem.getAmount() > 1) {
                handItem.setAmount(handItem.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
        }

        // Daj pieniądze przez Vault
        economy.depositPlayer(player, total);

        // Wiadomość
        messageUtil.send(player, "return-success",
                MessageUtil.placeholders(
                        "{amount}", String.valueOf(amount),
                        "{total}", String.format("%.2f", total)));

        playSound(player, "success");
    }

    // ==================== DODAWANIE / USUWANIE ====================

    public void addItem(ItemStack itemStack, double price) {
        AcceptedItem item = new AcceptedItem(
                itemStack.clone(), price,
                true, true, true, false
        );
        acceptedItems.add(item);
        saveItems();
    }

    public boolean removeItem(int index) {
        if (index < 0 || index >= acceptedItems.size()) return false;
        acceptedItems.remove(index);
        saveItems();
        return true;
    }

    public List<AcceptedItem> getAcceptedItems() {
        return acceptedItems;
    }

    // ==================== PORÓWNYWANIE ====================

    private boolean matches(AcceptedItem accepted, ItemStack other) {
        ItemStack template = accepted.itemStack;

        if (template.getType() != other.getType()) return false;
        if (accepted.strict) return template.isSimilar(other);

        var tMeta = template.getItemMeta();
        var oMeta = other.getItemMeta();

        if (tMeta == null && oMeta == null) return true;
        if (tMeta == null || oMeta == null) return false;

        if (accepted.checkName) {
            if (tMeta.hasDisplayName() != oMeta.hasDisplayName()) return false;
            if (tMeta.hasDisplayName()
                    && !tMeta.displayName().equals(oMeta.displayName())) return false;
        }

        if (accepted.checkLore) {
            if (tMeta.hasLore() != oMeta.hasLore()) return false;
            if (tMeta.hasLore()
                    && !tMeta.lore().equals(oMeta.lore())) return false;
        }

        if (accepted.checkNbt) {
            if (!tMeta.getPersistentDataContainer()
                    .equals(oMeta.getPersistentDataContainer())) {
                return false;
            }
        }

        return true;
    }

    // ==================== POMOCNICZE ====================

    public void playSound(Player player, String type) {
        try {
            String soundName = getConfig().getString(
                    "sounds." + type, "ENTITY_EXPERIENCE_ORB_PICKUP");
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 0.7f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private boolean getBool(Map<?, ?> map, String key, boolean def) {
        Object val = map.get(key);
        if (val instanceof Boolean b) return b;
        return def;
    }

    public static String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOut = new BukkitObjectOutputStream(out);
            dataOut.writeObject(item);
            dataOut.close();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack deserializeItem(String data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(
                    Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataIn = new BukkitObjectInputStream(in);
            ItemStack item = (ItemStack) dataIn.readObject();
            dataIn.close();
            return item;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Butelkomat getInstance() {
        return instance;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    // ==================== MODEL ====================

    public static class AcceptedItem {
        public final ItemStack itemStack;
        public final double price;
        public final boolean checkName;
        public final boolean checkLore;
        public final boolean checkNbt;
        public final boolean strict;

        public AcceptedItem(ItemStack itemStack, double price,
                            boolean checkName, boolean checkLore,
                            boolean checkNbt, boolean strict) {
            this.itemStack = itemStack.clone();
            this.price = price;
            this.checkName = checkName;
            this.checkLore = checkLore;
            this.checkNbt = checkNbt;
            this.strict = strict;
        }
    }
}