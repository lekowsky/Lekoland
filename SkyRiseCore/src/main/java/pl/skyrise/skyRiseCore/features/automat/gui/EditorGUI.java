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

public class EditorGUI implements InventoryHolder {

    private final AutomatModule plugin;
    private final MachineTemplate template;
    private final Player player;
    private Inventory inventory;
    private EditorMode mode;

    public enum EditorMode { MAIN_MENU, ITEMS_LIST, GUI_SETTINGS }

    public EditorGUI(AutomatModule plugin, MachineTemplate template, Player player) {
        this.plugin = plugin;
        this.template = template;
        this.player = player;
        this.mode = EditorMode.MAIN_MENU;
    }

    public void open() { openMainMenu(); }

    public void openMainMenu() {
        this.mode = EditorMode.MAIN_MENU;
        inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&8Edytor: &f" + template.getName()));

        ItemStack bg = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

        int placements = plugin.getPlacementManager().getPlacementCount(template.getName());

        // Rząd 1: Ustawienia szablonu (19-25)
        inventory.setItem(19, new ItemBuilder(Material.NAME_TAG)
                .name("&eTytuł GUI")
                .lore("&7Obecny: &f" + template.getTitle(), "", "&eKliknij aby zmienić").build());

        inventory.setItem(20, new ItemBuilder(Material.CHEST)
                .name("&eRozmiar")
                .lore("&7Rzędy: &f" + template.getRows(), "", "&aLPM +1", "&cPPM -1").build());

        inventory.setItem(21, new ItemBuilder(Material.DIAMOND)
                .name("&bPrzedmioty")
                .lore("&7Ilość: &f" + template.getItems().size(), "", "&eKliknij aby edytować").build());

        inventory.setItem(22, new ItemBuilder(Material.PAINTING)
                .name("&dWygląd GUI")
                .lore("&7Tło, ramki, kolory", "", "&eKliknij").build());

        inventory.setItem(23, new ItemBuilder(template.isEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name(template.isEnabled() ? "&aWłączony" : "&cWyłączony")
                .lore("&7Status automatu", "", "&eKliknij aby przełączyć").build());

        inventory.setItem(24, new ItemBuilder(Material.IRON_BARS)
                .name("&6Uprawnienie")
                .lore("&7Obecne: &f" + (template.getPermission().isEmpty() ? "Brak" : template.getPermission()), "",
                        "&eLPM zmień", "&cPPM usuń").build());

        inventory.setItem(25, new ItemBuilder(Material.ARMOR_STAND)
                .name("&6Nexo ID")
                .lore("&7Obecne: &f" + (template.getNexoFurnitureId() != null ? template.getNexoFurnitureId() : "Brak"), "",
                        "&eLPM zmień", "&cPPM usuń").build());

        // Rząd 2: Akcje i restock (28-34)
        inventory.setItem(28, new ItemBuilder(Material.COMPASS)
                .name("&aPostaw automat")
                .lore("&7Instancje: &f" + placements, "", "&7Włącza tryb stawiania", "", "&eKliknij").build());

        inventory.setItem(29, new ItemBuilder(template.isCloseButtonEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&cPrzycisk Zamknij")
                .lore("&7Status: " + (template.isCloseButtonEnabled() ? "&aWł" : "&cWył"), "", "&eKliknij").build());

        inventory.setItem(30, new ItemBuilder(Material.ENDER_EYE)
                .name("&aPodgląd")
                .lore("&7Otwórz automat jako gracz", "", "&eKliknij").build());

        // NOWE - Auto-restock opcje
        inventory.setItem(32, new ItemBuilder(template.isAutoRestockEnabled() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&6Auto-restock")
                .lore(
                        "&7Status: " + (template.isAutoRestockEnabled() ? "&aWłączony" : "&cWyłączony"),
                        "",
                        "&eKliknij aby przełączyć"
                ).build());

        inventory.setItem(33, new ItemBuilder(Material.CLOCK)
                .name("&6Interwał restocku")
                .lore(
                        "&7Co: &e" + template.getAutoRestockInterval() + " &7minut",
                        "",
                        "&aLPM +5 min",
                        "&cPPM -5 min",
                        "&6Shift+LPM wpisz ręcznie"
                ).build());

        inventory.setItem(34, new ItemBuilder(Material.HOPPER)
                .name("&6Ilość restocku")
                .lore(
                        "&7Dodaje: &e+" + template.getAutoRestockAmount() + " &7szt.",
                        "",
                        "&aLPM +5",
                        "&cPPM -5",
                        "&6Shift+LPM wpisz ręcznie"
                ).build());

        // Zamknij
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).name("&cZamknij edytor").build());

        player.openInventory(inventory);
    }

    public void openItemsList() {
        this.mode = EditorMode.ITEMS_LIST;
        inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&8Przedmioty: &f" + template.getName()));

        ItemStack bg = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

        int slot = 0;
        for (VendingItem item : template.getItems().values()) {
            if (slot >= 45) break;
            inventory.setItem(slot, item.buildEditorItem());
            slot++;
        }

        inventory.setItem(48, new ItemBuilder(Material.EMERALD)
                .name("&aDodaj z ręki")
                .lore("&7Trzymaj item w ręce", "", "&eKliknij aby dodać").build());

        inventory.setItem(50, new ItemBuilder(Material.CRAFTING_TABLE)
                .name("&eDodaj pusty")
                .lore("&7Dodaje przedmiot do edycji", "", "&eKliknij").build());

        inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&cPowrót").build());

        player.openInventory(inventory);
    }

    public void openGUISettings() {
        this.mode = EditorMode.GUI_SETTINGS;
        inventory = Bukkit.createInventory(this, 54, ColorUtil.color("&8Wygląd: &f" + template.getName()));

        ItemStack bg = new ItemBuilder(Material.WHITE_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 54; i++) inventory.setItem(i, bg);

        inventory.setItem(20, new ItemBuilder(template.isFillEmpty() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&eWypełnianie tła")
                .lore("&7Status: " + (template.isFillEmpty() ? "&aWł" : "&cWył"), "", "&eKliknij").build());

        inventory.setItem(21, new ItemBuilder(template.getFillerMaterial())
                .name("&eMateriał tła")
                .lore("&7Obecny: &f" + template.getFillerMaterial().name(), "", "&7Trzymaj blok i kliknij").build());

        inventory.setItem(23, new ItemBuilder(template.isBorder() ? Material.LIME_DYE : Material.GRAY_DYE)
                .name("&eRamka")
                .lore("&7Status: " + (template.isBorder() ? "&aWł" : "&cWył"), "", "&eKliknij").build());

        inventory.setItem(24, new ItemBuilder(template.getBorderMaterial())
                .name("&eMateriał ramki")
                .lore("&7Obecny: &f" + template.getBorderMaterial().name(), "", "&7Trzymaj blok i kliknij").build());

        inventory.setItem(45, new ItemBuilder(Material.ARROW).name("&cPowrót").build());

        player.openInventory(inventory);
    }

    public MachineTemplate getTemplate() { return template; }
    public EditorMode getMode() { return mode; }
    public Player getPlayer() { return player; }

    @Override
    public Inventory getInventory() { return inventory; }
}