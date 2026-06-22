package pl.skyrise.skyRiseCore.features.automat.model;

import org.bukkit.Location;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MachinePlacement {

    private UUID id;
    private String templateName;
    private Location location;
    private String nexoItemId;
    private String nexoEntityUUID;
    private UUID placedBy;

    // NOWE - stock per placement (itemId -> ilość w magazynie)
    private final Map<String, Integer> stock = new HashMap<>();

    public MachinePlacement(String templateName, Location location) {
        this.id = UUID.randomUUID();
        this.templateName = templateName;
        this.location = location;
        this.nexoItemId = null;
        this.nexoEntityUUID = null;
        this.placedBy = null;
    }

    public MachinePlacement(UUID id, String templateName, Location location) {
        this.id = id;
        this.templateName = templateName;
        this.location = location;
    }

    public String getLocationKey() { return toLocationKey(location); }

    public static String toLocationKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "null";
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    // ==================== STOCK PER PLACEMENT ====================

    /**
     * Inicjalizuje stock dla wszystkich itemów z szablonu (na podstawie defaultów)
     */
    public void initStockFromTemplate(MachineTemplate template) {
        if (template == null) return;
        for (VendingItem item : template.getItems().values()) {
            if (item.isUnlimitedStock()) continue;
            // domyślnie zaczynamy od pełnego stocku (max-stock) — biznes zaczyna z towarem
            if (!stock.containsKey(item.getId())) {
                stock.put(item.getId(), item.getMaxStock());
            }
        }
    }

    /**
     * Synchronizuje stock z templatem — dodaje nowe itemy z 0 stockiem,
     * usuwa stock dla itemów których już nie ma w templacie
     */
    public void syncWithTemplate(MachineTemplate template) {
        if (template == null) return;

        // Dodaj nowe itemy (z pełnym stockiem na start)
        for (VendingItem item : template.getItems().values()) {
            if (!item.isUnlimitedStock() && !stock.containsKey(item.getId())) {
                stock.put(item.getId(), item.getMaxStock());
            }
        }

        // Usuń stock dla itemów których nie ma w templacie
        stock.keySet().removeIf(itemId -> template.getItem(itemId) == null);
    }

    public int getStock(String itemId) {
        return stock.getOrDefault(itemId, 0);
    }

    public void setStock(String itemId, int amount) {
        stock.put(itemId, Math.max(0, amount));
    }

    /**
     * Próbuje pobrać stock przy zakupie. Zwraca true jeśli się udało.
     */
    public boolean withdrawStock(String itemId, int amount) {
        int current = getStock(itemId);
        if (current < amount) return false;
        stock.put(itemId, current - amount);
        return true;
    }

    /**
     * Dodaje do stocku z poszanowaniem max. Zwraca ile faktycznie dodano.
     */
    public int addStock(String itemId, int amount, int max) {
        int current = getStock(itemId);
        int newStock = Math.min(current + amount, max);
        int added = newStock - current;
        stock.put(itemId, newStock);
        return added;
    }

    /**
     * Usuwa ze stocku. Zwraca ile faktycznie usunięto.
     */
    public int removeStock(String itemId, int amount) {
        int current = getStock(itemId);
        int toRemove = Math.min(amount, current);
        stock.put(itemId, current - toRemove);
        return toRemove;
    }

    public Map<String, Integer> getStockMap() {
        return stock;
    }

    public void setStockMap(Map<String, Integer> stockMap) {
        stock.clear();
        if (stockMap != null) stock.putAll(stockMap);
    }

    // ==================== GETTERY / SETTERY ====================

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String t) { this.templateName = t; }
    public Location getLocation() { return location; }
    public void setLocation(Location l) { this.location = l; }
    public String getNexoItemId() { return nexoItemId; }
    public void setNexoItemId(String id) { this.nexoItemId = id; }
    public String getNexoEntityUUID() { return nexoEntityUUID; }
    public void setNexoEntityUUID(String uuid) { this.nexoEntityUUID = uuid; }
    public UUID getPlacedBy() { return placedBy; }
    public void setPlacedBy(UUID p) { this.placedBy = p; }
}