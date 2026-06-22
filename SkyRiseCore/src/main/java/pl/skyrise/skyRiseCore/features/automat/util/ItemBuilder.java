package pl.skyrise.skyRiseCore.features.automat.util;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) { this.item = new ItemStack(material); this.meta = item.getItemMeta(); }
    public ItemBuilder(Material material, int amount) { this.item = new ItemStack(material, amount); this.meta = item.getItemMeta(); }

    public ItemBuilder name(String name) { meta.setDisplayName(ColorUtil.color(name)); return this; }

    public ItemBuilder lore(String... lines) {
        List<String> lore = new ArrayList<>();
        for (String l : lines) lore.add(ColorUtil.color(l));
        meta.setLore(lore); return this;
    }

    public ItemBuilder lore(List<String> lines) {
        List<String> lore = new ArrayList<>();
        for (String l : lines) lore.add(ColorUtil.color(l));
        meta.setLore(lore); return this;
    }

    public ItemBuilder glow() {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS); return this;
    }

    public ItemStack build() { item.setItemMeta(meta); return item; }
}