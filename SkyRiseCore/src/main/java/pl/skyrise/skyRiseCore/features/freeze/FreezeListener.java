package pl.skyrise.skyRiseCore.features.freeze;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

import java.util.UUID;

public class FreezeListener implements Listener {

    private final FreezeModule module;

    public FreezeListener(FreezeModule module) {
        this.module = module;
    }

    // ═══════════════════════════════════════
    // JOIN / QUIT — Natywne zarządzanie widocznością i pozycją
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // 1. Jeśli wchodzący gracz sam jest zamrożony, sprawdź czy nie stał się adminem/OP, jeśli tak - odblokuj, w przeciwnym razie zaaplikuj efekty
        if (module.isFrozen(uuid)) {
            if (player.hasPermission(module.getPermission()) || player.isOp()) {
                module.unfreezePlayer(uuid);
                player.sendMessage(ColorUtil.mini("<#99CCFF>»</#99CCFF> <green>Wykryto uprawnienia administracyjne — zostałeś automatycznie odmrożony."));
            } else {
                module.applyFreezeEffects(player);
            }
        }

        // 2. Ukryj wszystkich aktualnie zamrożonych graczy przed nowo wchodzącym graczem
        for (UUID frozenUuid : module.getFrozenPlayers()) {
            Player frozen = Bukkit.getPlayer(frozenUuid);
            if (frozen != null && frozen.isOnline() && frozen != player) {
                player.hidePlayer(module.getPlugin(), frozen);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Na wypadek wyjścia, ujawnij go wszystkim, by nie zepsuć widoczności po powrocie
        if (module.isFrozen(player.getUniqueId())) {
            module.revealPlayer(player);
        }
    }

    // ═══════════════════════════════════════
    // BLOKADA RUCHU I ROTACJI — Ultra-optymalna (Event-driven)
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (module.isFrozen(player.getUniqueId())) {
            Location freezeLoc = module.getFreezeLocation();
            if (freezeLoc == null) {
                freezeLoc = new Location(player.getWorld(), 0.5, 64, 0.5);
            }

            // Klonujemy punkt freeze i narzucamy mu zablokowany kierunek patrzenia (yaw/pitch)
            Location dest = freezeLoc.clone();
            dest.setYaw(module.getLockedYaw(player.getUniqueId()));
            dest.setPitch(module.getLockedPitch(player.getUniqueId()));

            // Płynnie i natychmiastowo ustawiamy pozycję To, uniemożliwiając jakikolwiek ruch lub obrót kamery
            event.setTo(dest);
        }
    }

    // ═══════════════════════════════════════
    // BLOKADY INTERAKCJI (Czat, komendy, ekwipunek, drop, interakcja)
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (module.isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.mini(module.getFreezeChat()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (module.isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ColorUtil.mini(module.getFreezeChat()));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (module.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (module.isFrozen(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (module.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (module.isFrozen(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSwapItems(PlayerSwapHandItemsEvent event) {
        if (module.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (module.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (module.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
