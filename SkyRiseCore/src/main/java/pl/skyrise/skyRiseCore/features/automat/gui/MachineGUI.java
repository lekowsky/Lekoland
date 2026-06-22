package pl.skyrise.skyRiseCore.features.automat.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.model.MachinePlacement;
import pl.skyrise.skyRiseCore.features.automat.model.MachineTemplate;
import pl.skyrise.skyRiseCore.features.automat.model.VendingItem;
import pl.skyrise.skyRiseCore.features.automat.util.ColorUtil;
import pl.skyrise.skyRiseCore.features.automat.util.ItemBuilder;

import java.util.HashSet;
import java.util.Set;

public class MachineGUI implements InventoryHolder {

    private final AutomatModule plugin;
    private final MachineTemplate template;
    private final MachinePlacement placement; // może być null - tryb preview/admin
    private final Player player;
    private Inventory inventory;

    /**
     * @param placement konkretny postawiony automat (z własnym stockiem). Może być null dla podglądu.
     */
    public MachineGUI(AutomatModule plugin, MachineTemplate template, Player player, MachinePlacement placement) {
        this.plugin = plugin;
        this.template = template;
        this.player = player;
        this.placement = placement;
    }

    /**
     * Stary konstruktor (zachowany dla wstecznej kompatybilności - bez placement = preview)
     */
    public MachineGUI(AutomatModule plugin, MachineTemplate template, Player player) {
        this(plugin, template, player, null);
    }

    public void open() {
        inventory = Bukkit.createInventory(this, template.getSize(), ColorUtil.color(template.getTitle()));
        populate();
        player.openInventory(inventory);
    }

    private void populate() {
        String currency = plugin.getEconomyManager().getCurrencySymbol();

        // Ramka
        Set<Integer> borderSlots = new HashSet<>();
        if (template.isBorder()) {
            int size = template.getSize();
            int rows = template.getRows();
            for (int i = 0; i < size; i++) {
                int row = i / 9;
                int col = i % 9;
                if (row == 0 || row == rows - 1 || col == 0 || col == 8) borderSlots.add(i);
            }
            ItemStack borderItem = new ItemBuilder(template.getBorderMaterial()).name(template.getBorderName()).build();
            for (int slot : borderSlots) inventory.setItem(slot, borderItem);
        }

        // Filler
        if (template.isFillEmpty()) {
            ItemStack filler = new ItemBuilder(template.getFillerMaterial()).name(template.getFillerName()).build();
            for (int i = 0; i < template.getSize(); i++) {
                if (inventory.getItem(i) == null) inventory.setItem(i, filler);
            }
        }

        // Itemy - z uwzględnieniem stocku per placement
        for (VendingItem item : template.getItems().values()) {
            if (item.getSlot() >= 0 && item.getSlot() < template.getSize()) {
                ItemStack displayItem;
                if (placement != null && !item.isUnlimitedStock()) {
                    // Tymczasowo nadpisujemy stock z placementu na czas budowy wyświetlanego itemu
                    int origStock = item.getStock();
                    item.setStock(placement.getStock(item.getId()));
                    displayItem = item.buildDisplayItem(currency);
                    item.setStock(origStock); // przywracamy
                } else {
                    displayItem = item.buildDisplayItem(currency);
                }
                inventory.setItem(item.getSlot(), displayItem);
            }
        }

        // Close button
        if (template.isCloseButtonEnabled()) {
            int closeSlot = template.getResolvedCloseSlot();
            if (closeSlot >= 0 && closeSlot < template.getSize()) {
                inventory.setItem(closeSlot, new ItemBuilder(template.getCloseButtonMaterial())
                        .name(template.getCloseButtonName())
                        .lore(template.getCloseButtonLore()).build());
            }
        }
    }

    public MachineTemplate getTemplate() { return template; }
    public MachinePlacement getPlacement() { return placement; }

    @Override
    public Inventory getInventory() { return inventory; }
}