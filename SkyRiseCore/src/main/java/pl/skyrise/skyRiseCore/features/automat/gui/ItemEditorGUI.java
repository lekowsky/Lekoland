package pl.skyrise.skyRiseCore.features.automat.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.features.automat.util.ColorUtil;
import pl.skyrise.skyRiseCore.features.automat.util.ItemBuilder;

public class ItemEditorGUI implements InventoryHolder {

    private final AutomatModule plugin;
    private final MachineTemplate template;
    private final VendingItem item;
    private final Player player;
    private Inventory inventory;

    public ItemEditorGUI(AutomatModule plugin, MachineTemplate template, VendingItem item, Player player) {
        this.plugin = plugin;
        this.template = template;
        this.item = item;
        this.player = player;
    }

    public void open() {
        inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&8Item: &f" + item.getId()));
        populate();
        player.openInventory(inventory);
    }

    private void populate() {
        ItemStack bg = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

        String curr = plugin.getEconomyManager().getCurrencySymbol();

        // Podgląd
        inventory.setItem(4, item.buildDisplayItem(curr));

        // Rząd 1: Podstawowe opcje (19-25)
        inventory.setItem(19, new ItemBuilder(Material.CRAFTING_TABLE)
                .name("&eMateriał")
                .lore("&7Obecny: &f" + item.getMaterial().name(), "", "&7Trzymaj item i kliknij").build());

        inventory.setItem(20, new ItemBuilder(Material.NAME_TAG)
                .name("&eNazwa")
                .lore("&7Obecna: &f" + item.getDisplayName(), "", "&eKliknij aby zmienić").build());

        inventory.setItem(21, new ItemBuilder(Material.WRITABLE_BOOK)
                .name("&eOpis")
                .lore("&7Linie: &f" + item.getLore().size(), "", "&eLPM edytuj", "&cPPM wyczyść").build());

        inventory.setItem(22, new ItemBuilder(Material.GOLD_INGOT)
                .name("&eCena")
                .lore("&7Obecna: &a" + String.format("%.2f", item.getPrice()) + curr, "", "&eKliknij").build());

        inventory.setItem(23, new ItemBuilder(Material.CHEST)
                .name("&eIlość")
                .lore("&7Obecna: &f" + item.getAmount(), "",
                        "&aLPM +1 / Shift+LPM +10",
                        "&cPPM -1 / Shift+PPM -10").build());

        inventory.setItem(24, new ItemBuilder(Material.ITEM_FRAME)
                .name("&eSlot w GUI")
                .lore("&7Obecny: &f" + item.getSlot(), "", "&eKliknij").build());

        inventory.setItem(25, new ItemBuilder(item.isGlowing() ? Material.GLOWSTONE : Material.COAL)
                .name("&eŚwiecenie")
                .lore("&7Status: " + (item.isGlowing() ? "&aWł" : "&cWył"), "", "&eKliknij").build());

        // Rząd 2: Zaawansowane opcje (28-34)
        inventory.setItem(28, new ItemBuilder(Material.ARMOR_STAND)
                .name("&eCustom Model Data")
                .lore("&7Obecne: &f" + (item.getCustomModelData() > 0 ? item.getCustomModelData() : "Brak"), "", "&eKliknij").build());

        inventory.setItem(29, new ItemBuilder(Material.IRON_BARS)
                .name("&eUprawnienie")
                .lore("&7Obecne: &f" + (item.getPermission().isEmpty() ? "Brak" : item.getPermission()), "",
                        "&eLPM zmień", "&cPPM usuń").build());

        inventory.setItem(30, new ItemBuilder(Material.HOPPER)
                .name("&eLimit zakupów")
                .lore("&7Obecny: &f" + (item.getPurchaseLimit() > 0 ? item.getPurchaseLimit() : "Brak"), "",
                        "&aLPM +1", "&cPPM -1", "&6Shift+LPM ręcznie").build());

        inventory.setItem(31, new ItemBuilder(Material.COMMAND_BLOCK)
                .name("&eKomendy po zakupie")
                .lore("&7Ilość: &f" + item.getCommandsOnPurchase().size(),
                        "&7{player}, {amount}", "",
                        "&eLPM dodaj", "&cPPM wyczyść").build());

        // NOWE - Stock opcje (rząd 3: 37-43)
        inventory.setItem(37, new ItemBuilder(item.isUnlimitedStock() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&eNieograniczony stock")
                .lore(
                        "&7Status: " + (item.isUnlimitedStock() ? "&a∞ Nieograniczony" : "&cOgraniczony"),
                        "",
                        "&eKliknij aby przełączyć"
                ).build());

        inventory.setItem(38, new ItemBuilder(Material.BARREL)
                .name("&eObecny stock")
                .lore(
                        "&7Stan: &e" + item.getStock() + "&7/&e" + item.getMaxStock(),
                        "",
                        "&aLPM +10",
                        "&cPPM -10",
                        "&6Shift+LPM wpisz ręcznie"
                ).build());

        inventory.setItem(39, new ItemBuilder(Material.ENDER_CHEST)
                .name("&eMaksymalny stock")
                .lore(
                        "&7Obecny max: &e" + item.getMaxStock(),
                        "",
                        "&aLPM +10",
                        "&cPPM -10",
                        "&6Shift+LPM wpisz ręcznie"
                ).build());

        // Dolny pasek
        inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&cPowrót").build());
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("&cUsuń przedmiot").build());
    }

    public MachineTemplate getTemplate() { return template; }
    public VendingItem getVendingItem() { return item; }
    public Player getPlayer() { return player; }

    @Override
    public Inventory getInventory() { return inventory; }
}