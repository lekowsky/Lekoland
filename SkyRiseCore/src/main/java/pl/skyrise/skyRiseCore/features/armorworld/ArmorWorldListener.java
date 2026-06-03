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

        // --- Shift-click ---
        if (event.isShiftClick()) {
            // Shift-click ZE slotu zbroi (zdejmowanie) → POZWALAMY
            if (event.getSlotType() == InventoryType.SlotType.ARMOR) return;

            // Shift-click z innego slotu z zablokowanym przedmiotem → próbuje założyć → BLOKUJEMY
            ItemStack current = event.getCurrentItem();
            if (current != null && current.getType() != Material.AIR && module.isBlocked(current.getType())) {
                event.setCancelled(true);
                sendSingleDeny(player, current.getType());
            }
            return;
        }

        // --- Normalny klik na slot zbroi ---
        if (event.getSlotType() == InventoryType.SlotType.ARMOR) {

            // Przedmiot na kursorze → slot zbroi
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() != Material.AIR && module.isBlocked(cursor.getType())) {
                event.setCancelled(true);
                sendSingleDeny(player, cursor.getType());
                return;
            }

            // Number key (1-9) → zamiana z hotbarem
            int hotbarButton = event.getHotbarButton();
            if (hotbarButton >= 0) {
                ItemStack hotbarItem = player.getInventory().getItem(hotbarButton);
                if (hotbarItem != null && module.isBlocked(hotbarItem.getType())) {
                    event.setCancelled(true);
                    sendSingleDeny(player, hotbarItem.getType());
                    return;
                }
            }

            // F key → zamiana z offhand
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand.getType() != Material.AIR && module.isBlocked(offhand.getType())) {
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

        // Sloty zbroi w ekwipunku gracza (CRAFTING type): raw 5=helmet, 6=chestplate, 7=leggings, 8=boots
        InventoryView view = event.getView();
        if (view.getTopInventory().getType() == InventoryType.CRAFTING) {
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
        player.sendMessage(ColorUtil.mini("<red>»</red> <red>Zablokowane: " + module.formatBlocked(blocked)));
    }

    private void sendSingleDeny(Player player, Material mat) {
        if (!messageCache.canSend(player.getUniqueId(), "armorworld:equip")) return;
        player.sendMessage(ColorUtil.mini(module.getDenyMessage()));
        player.sendMessage(ColorUtil.mini("<red>»</red> <red>Zablokowane: <gold>" + module.formatMaterial(mat)));
    }
}
