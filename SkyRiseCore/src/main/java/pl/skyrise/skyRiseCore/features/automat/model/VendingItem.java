package pl.skyrise.skyRiseCore.features.automat.model;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import pl.skyrise.skyRiseCore.features.automat.util.ColorUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

public class VendingItem {

    private String id;
    private Material material;
    private String displayName;
    private List<String> lore;
    private int amount;
    private double price;
    private int slot;
    private boolean glowing;
    private int customModelData;
    private Map<Enchantment, Integer> enchantments;
    private String permission;
    private int purchaseLimit;
    private Map<UUID, Integer> purchaseCounts;
    private List<String> commandsOnPurchase;

    private String serializedItem;
    private boolean useSerializedItem;

    // NOWE - System stockowania
    private int stock;           // Obecna ilość w magazynie
    private int maxStock;        // Maksymalna pojemność magazynu
    private boolean unlimitedStock; // Czy nieograniczony stock (domyślnie true dla kompatybilności)

    public VendingItem(String id) {
        this.id = id;
        this.material = Material.STONE;
        this.displayName = "&fItem";
        this.lore = new ArrayList<>();
        this.amount = 1;
        this.price = 0.0;
        this.slot = 0;
        this.glowing = false;
        this.customModelData = -1;
        this.enchantments = new HashMap<>();
        this.permission = "";
        this.purchaseLimit = 0;
        this.purchaseCounts = new HashMap<>();
        this.commandsOnPurchase = new ArrayList<>();
        this.serializedItem = null;
        this.useSerializedItem = false;

        // Stock defaults
        this.stock = 0;
        this.maxStock = 64;
        this.unlimitedStock = true; // Domyślnie bez limitu stocku
    }

    public static String serializeItemStack(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception ignored) {
            return null;
        }
    }

    public static ItemStack deserializeItemStack(String data) {
        if (data == null || data.isEmpty()) return null;
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception ignored) {
            return null;
        }
    }

    public void setFromItemStack(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;

        this.serializedItem = serializeItemStack(item);
        this.useSerializedItem = true;

        this.material = item.getType();
        this.amount = item.getAmount();
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasDisplayName()) this.displayName = meta.getDisplayName();
            if (meta.hasLore()) this.lore = new ArrayList<>(meta.getLore());
            if (meta.hasCustomModelData()) this.customModelData = meta.getCustomModelData();
            if (meta.hasEnchants()) this.enchantments = new HashMap<>(meta.getEnchants());
        }
    }

    /**
     * Sprawdza czy item jest dostępny do kupienia (ma stock lub unlimited)
     */
    public boolean isInStock() {
        if (unlimitedStock) return true;
        return stock >= amount;
    }

    /**
     * Pobiera z magazynu przy zakupie
     */
    public boolean withdrawStock() {
        if (unlimitedStock) return true;
        if (stock < amount) return false;
        stock -= amount;
        return true;
    }

    /**
     * Dodaje do magazynu
     */
    public int addStock(int qty) {
        int newStock = stock + qty;
        if (newStock > maxStock) newStock = maxStock;
        int added = newStock - stock;
        stock = newStock;
        return added;
    }

    /**
     * Usuwa z magazynu
     */
    public int removeStock(int qty) {
        int toRemove = Math.min(qty, stock);
        stock -= toRemove;
        return toRemove;
    }

    public ItemStack buildDisplayItem(String currencySymbol) {
        ItemStack item;

        if (useSerializedItem && serializedItem != null) {
            item = deserializeItemStack(serializedItem);
            if (item == null) {
                item = new ItemStack(material, amount);
            } else {
                item.setAmount(amount);
            }
        } else {
            item = new ItemStack(material, amount);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (displayName != null && !displayName.equals("&fItem")) {
            meta.setDisplayName(ColorUtil.color(displayName));
        }

        List<String> fullLore = new ArrayList<>();

        if (useSerializedItem && meta.hasLore()) {
            fullLore.addAll(meta.getLore());
        } else {
            for (String line : lore) {
                fullLore.add(ColorUtil.color(line));
            }
        }

        fullLore.add("");
        fullLore.add(ColorUtil.color("&7Cena: &a" + String.format("%.2f", price) + currencySymbol));

        // Stock info
        if (unlimitedStock) {
            fullLore.add(ColorUtil.color("&7Dostępność: &a∞"));
        } else {
            int available = stock / amount;
            if (available > 0) {
                fullLore.add(ColorUtil.color("&7Dostępność: &a" + available + " szt."));
            } else {
                fullLore.add(ColorUtil.color("&7Dostępność: &cBrak w magazynie"));
            }
        }

        if (purchaseLimit > 0) {
            fullLore.add(ColorUtil.color("&7Limit: &e" + purchaseLimit));
        }

        if (isInStock()) {
            fullLore.add(ColorUtil.color("&8(Kliknij aby kupić)"));
        }

        meta.setLore(fullLore);

        if (customModelData > 0 && !useSerializedItem) meta.setCustomModelData(customModelData);

        if (!useSerializedItem) {
            for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
                meta.addEnchant(e.getKey(), e.getValue(), true);
            }
            if (glowing && enchantments.isEmpty()) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack buildPurchaseItem() {
        ItemStack item;

        if (useSerializedItem && serializedItem != null) {
            item = deserializeItemStack(serializedItem);
            if (item == null) {
                item = new ItemStack(material, amount);
            } else {
                item.setAmount(amount);
            }
            return item;
        }

        item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(ColorUtil.color(displayName));
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) coloredLore.add(ColorUtil.color(line));
        meta.setLore(coloredLore);

        if (customModelData > 0) meta.setCustomModelData(customModelData);
        for (Map.Entry<Enchantment, Integer> e : enchantments.entrySet()) {
            meta.addEnchant(e.getKey(), e.getValue(), true);
        }
        if (glowing && enchantments.isEmpty()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        return item;
    }

    public ItemStack buildEditorItem() {
        ItemStack item;
        if (useSerializedItem && serializedItem != null) {
            item = deserializeItemStack(serializedItem);
            if (item == null) item = new ItemStack(material, amount);
        } else {
            item = new ItemStack(material, amount);
        }
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayedName = meta.hasDisplayName() ? meta.getDisplayName() : ColorUtil.color(displayName);
        meta.setDisplayName(ColorUtil.color("&b" + displayedName));

        List<String> editorLore = new ArrayList<>();
        editorLore.add(ColorUtil.color("&7ID: &f" + id));
        editorLore.add(ColorUtil.color("&7Cena: &a" + String.format("%.2f", price)));
        editorLore.add(ColorUtil.color("&7Ilość: &e" + amount));
        editorLore.add(ColorUtil.color("&7Slot: &e" + slot));

        // Stock info w edytorze
        if (unlimitedStock) {
            editorLore.add(ColorUtil.color("&7Stock: &a∞ (nieograniczony)"));
        } else {
            editorLore.add(ColorUtil.color("&7Stock: &e" + stock + "&7/&e" + maxStock));
        }

        editorLore.add("");
        editorLore.add(ColorUtil.color("&e▶ LPM - Edytuj"));
        editorLore.add(ColorUtil.color("&c▶ PPM - Usuń"));
        meta.setLore(editorLore);

        item.setItemMeta(meta);
        return item;
    }

    public boolean canPurchase(UUID playerUUID) {
        if (purchaseLimit <= 0) return true;
        return purchaseCounts.getOrDefault(playerUUID, 0) < purchaseLimit;
    }

    public void recordPurchase(UUID playerUUID) {
        purchaseCounts.put(playerUUID, purchaseCounts.getOrDefault(playerUUID, 0) + 1);
    }

    // Gettery i Settery
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Material getMaterial() { return material; }
    public void setMaterial(Material material) { this.material = material; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public List<String> getLore() { return lore; }
    public void setLore(List<String> lore) { this.lore = lore; }
    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = Math.max(1, Math.min(64, amount)); }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = Math.max(0, price); }
    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }
    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }
    public int getCustomModelData() { return customModelData; }
    public void setCustomModelData(int cmd) { this.customModelData = cmd; }
    public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
    public void setEnchantments(Map<Enchantment, Integer> e) { this.enchantments = e; }
    public String getPermission() { return permission; }
    public void setPermission(String p) { this.permission = p; }
    public int getPurchaseLimit() { return purchaseLimit; }
    public void setPurchaseLimit(int l) { this.purchaseLimit = l; }
    public Map<UUID, Integer> getPurchaseCounts() { return purchaseCounts; }
    public void setPurchaseCounts(Map<UUID, Integer> p) { this.purchaseCounts = p; }
    public List<String> getCommandsOnPurchase() { return commandsOnPurchase; }
    public void setCommandsOnPurchase(List<String> c) { this.commandsOnPurchase = c; }

    public String getSerializedItem() { return serializedItem; }
    public void setSerializedItem(String s) { this.serializedItem = s; }
    public boolean isUseSerializedItem() { return useSerializedItem; }
    public void setUseSerializedItem(boolean b) { this.useSerializedItem = b; }

    // Stock gettery/settery
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = Math.max(0, stock); }
    public int getMaxStock() { return maxStock; }
    public void setMaxStock(int maxStock) { this.maxStock = Math.max(1, maxStock); }
    public boolean isUnlimitedStock() { return unlimitedStock; }
    public void setUnlimitedStock(boolean unlimitedStock) { this.unlimitedStock = unlimitedStock; }
}