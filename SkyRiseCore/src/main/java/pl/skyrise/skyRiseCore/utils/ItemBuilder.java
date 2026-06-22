package pl.skyrise.skyRiseCore.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import pl.skyrise.skyRiseCore.SkyRiseCore;


/**
 * Builder ItemStack — Paper/Adventure API.
 * Używa Component dla nazw i opisów.
 */
public class ItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder name(Component name) {
        if (meta != null) {
            meta.displayName(name);
        }
        return this;
    }

    public ItemBuilder name(String miniText) {
        return name(ColorUtil.mini(miniText));
    }

    public ItemBuilder lore(Component line) {
        if (meta != null) {
            List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
            lore.add(line);
            meta.lore(lore);
        }
        return this;
    }

    public ItemBuilder lore(List<Component> lines) {
        if (meta != null) {
            meta.lore(new ArrayList<>(lines));
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        item.setAmount(amount);
        return this;
    }


    /**
     * Zapisuje ukryte dane w PersistentDataContainer.
     * Niezbędne do weryfikacji przedmiotów (np. narkotyków).
     */
    public ItemBuilder setStringData(String keyString, String value) {
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(SkyRiseCore.getInstance(), keyString);
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        }
        return this;
    }

    /**
     * Odczytuje ukryte dane z PersistentDataContainer bez tworzenia obiektu Buildera.
     */
    public static String getStringData(ItemStack item, String keyString) {
        if (item == null || !item.hasItemMeta()) return null;
        NamespacedKey key = new NamespacedKey(SkyRiseCore.getInstance(), keyString);
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
