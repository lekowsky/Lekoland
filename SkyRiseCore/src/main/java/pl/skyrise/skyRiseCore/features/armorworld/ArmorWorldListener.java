package pl.skyrise.skyRiseCore.features.armorworld;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.skyRiseCore.core.MessageCache;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

import java.util.List;

public class ArmorWorldListener implements Listener {

    private final ArmorWorldModule module;
    private final MessageCache messageCache;

    public ArmorWorldListener(ArmorWorldModule module, MessageCache messageCache) {
        this.module = module;
        this.messageCache = messageCache;
    }

    // ═══════════════════════════════════════
    // TELEPORT / PORTAL — blokuje wejście z założoną zbroją
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        if (event.getFrom().getWorld() != null && event.getFrom().getWorld().equals(event.getTo().getWorld())) return;

        Player player = event.getPlayer();
        if (player.hasPermission(module.getBypassPermission())) return;
        if (!module.isWorldBlocked(event.getTo().getWorld().getName())) return;

        List<Material> blocked = module.getBlockedArmor(player);
        if (blocked.isEmpty()) return;

        event.setCancelled(true);
        sendBlockedInfo(player, blocked);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getTo() == null) return;

        Player player = event.getPlayer();
        if (player.hasPermission(module.getBypassPermission())) return;
        if (!module.isWorldBlocked(event.getTo().getWorld().getName())) return;

        List<Material> blocked = module.getBlockedArmor(player);
        if (blocked.isEmpty()) return;

        event.setCancelled(true);
        sendBlockedInfo(player, blocked);
    }

    // ═══════════════════════════════════════
    // INVENTORY CLICK — blokuje ZAKŁADANIE, pozwala ZDEJMOWANIE
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.hasPermission(module.getBypassPermission())) return;
        if (!module.isWorldBlocked(player.getWorld().getName())) return;

        // --- Interakcje poza slotem zbroi (w zwykłym ekwipunku) ---
        // Shift-click lub Prawy klik na zablokowaną zbroję w ekwipunku próbuje ją automatycznie założyć/podmienić (1.20+)
        // Ograniczenie to dotyczy tylko własnego ekwipunku gracza (CRAFTING / PLAYER / CREATIVE).
        // W skrzyniach lub innych pojemnikach (gdzie top inventory to np. CHEST) pozwalamy na swobodne przenoszenie i shift-click!
        if (event.getSlotType() != InventoryType.SlotType.ARMOR) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() != Material.AIR && module.isBlocked(clicked.getType())) {
                InventoryType topType = event.getView().getTopInventory().getType();
                if (topType == InventoryType.CRAFTING || topType == InventoryType.PLAYER || topType == InventoryType.CREATIVE) {
                    if (event.isShiftClick() || event.getClick().isRightClick()) {
                        event.setCancelled(true);
                        sendSingleDeny(player, clicked.getType());
                        return;
                    }
                }
            }
        }

        // --- Interakcje bezpośrednio ze slotem zbroi (ARMOR) ---
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {
            // 1. Kładzenie przedmiotu z kursora do slotu zbroi (również przy trzymaniu Shift)
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR && module.isBlocked(cursor.getType())) {
                event.setCancelled(true);
                sendSingleDeny(player, cursor.getType());
                return;
            }

            // 2. Zamiana z hotbarem (klawisze 1-9)
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (hotbarItem != null && module.isBlocked(hotbarItem.getType())) {
                    event.setCancelled(true);
                    sendSingleDeny(player, hotbarItem.getType());
                    return;
                }
            }

            // 3. Zamiana z drugą ręką (klawisz F)
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand != null && offhand.getType() != Material.AIR && module.isBlocked(offhand.getType())) {
                    event.setCancelled(true);
                    sendSingleDeny(player, offhand.getType());
                }
            }
        }
    }

    // ═══════════════════════════════════════
    // INVENTORY DRAG — blokuje przeciąganie na slot zbroi
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (player.hasPermission(module.getBypassPermission())) return;
        if (!module.isWorldBlocked(player.getWorld().getName())) return;

        ItemStack dragged = event.getOldCursor();
        if (dragged == null || dragged.getType() == Material.AIR || !module.isBlocked(dragged.getType())) return;

        // Sloty zbroi w ekwipunku gracza (CRAFTING / PLAYER / CREATIVE type): raw 5=helmet, 6=chestplate, 7=leggings, 8=boots
        InventoryView view = event.getView();
        InventoryType topType = view.getTopInventory().getType();
        if (topType == InventoryType.CRAFTING || topType == InventoryType.PLAYER || topType == InventoryType.CREATIVE) {
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot >= 5 && rawSlot <= 8) {
                    event.setCancelled(true);
                    sendSingleDeny(player, dragged.getType());
                    return;
                }
            }
        }
    }

    // ═══════════════════════════════════════
    // RIGHT-CLICK — blokuje zakładanie przez kliknięcie
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        if (player.hasPermission(module.getBypassPermission())) return;
        if (!module.isWorldBlocked(player.getWorld().getName())) return;

        ItemStack item = event.getItem();
        if (item == null) return;
        if (!module.isBlocked(item.getType())) return;

        event.setCancelled(true);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        sendSingleDeny(player, item.getType());
    }

    // ═══════════════════════════════════════
    // DISPENSER — blokuje zakładanie z dystrybutora
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        ItemStack item = event.getItem();
        if (item == null) return;
        if (!module.isBlocked(item.getType())) return;

        String typeName = item.getType().name();
        boolean isHelmet = typeName.endsWith("_HELMET");
        boolean isChestplate = typeName.endsWith("_CHESTPLATE");
        boolean isLeggings = typeName.endsWith("_LEGGINGS");
        boolean isBoots = typeName.endsWith("_BOOTS");
        if (!isHelmet && !isChestplate && !isLeggings && !isBoots) return;

        for (org.bukkit.entity.Entity entity : event.getBlock().getWorld().getNearbyEntities(
                event.getBlock().getLocation().add(event.getVelocity().normalize()), 1.5, 1.5, 1.5)) {
            if (!(entity instanceof Player player)) continue;
            if (player.hasPermission(module.getBypassPermission())) continue;
            if (!module.isWorldBlocked(player.getWorld().getName())) continue;

            ItemStack slot = isHelmet ? player.getInventory().getHelmet()
                    : isChestplate ? player.getInventory().getChestplate()
                    : isLeggings ? player.getInventory().getLeggings()
                    : player.getInventory().getBoots();

            if (slot == null || slot.getType() == Material.AIR) {
                event.setCancelled(true);
                sendSingleDeny(player, item.getType());
                return;
            }
        }
    }

    // ═══════════════════════════════════════
    // JOIN — powiadomienie
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission(module.getBypassPermission())) return;
        if (!module.isWorldBlocked(player.getWorld().getName())) return;

        List<Material> blocked = module.getBlockedArmor(player);
        if (blocked.isEmpty()) return;

        sendBlockedInfo(player, blocked);
    }

    // ═══════════════════════════════════════
    // WIADOMOŚCI z cooldown
    // ═══════════════════════════════════════

    private void sendBlockedInfo(Player player, List<Material> blocked) {
        if (!messageCache.canSend(player.getUniqueId(), "armorworld:blocked")) return;
        player.sendMessage(ColorUtil.mini(module.getDenyMessage()));
        player.sendMessage(ColorUtil.mini("<red>» Zablokowane: " + module.formatBlocked(blocked)));
    }

    private void sendSingleDeny(Player player, Material mat) {
        if (!messageCache.canSend(player.getUniqueId(), "armorworld:equip")) return;
        player.sendMessage(ColorUtil.mini(module.getDenyMessage()));
        player.sendMessage(ColorUtil.mini("<red>» Zablokowane: <gold>" + module.formatMaterial(mat)));
    }
}
