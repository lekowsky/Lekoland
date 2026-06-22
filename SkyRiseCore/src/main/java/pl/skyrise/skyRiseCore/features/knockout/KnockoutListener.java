package pl.skyrise.skyRiseCore.features.knockout;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import pl.skyrise.skyRiseCore.SkyRiseCore;
import pl.skyrise.skyRiseCore.features.insurance.InsuranceModule;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

import java.util.UUID;

public class KnockoutListener implements Listener {

    private final KnockoutModule module;

    public KnockoutListener(KnockoutModule module) {
        this.module = module;
    }

    // ═══════════════════════════════════════
    // ANULOWANIE ŚMIERCI / OBSŁUGA POWALENIA
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();

        // 1. Jeśli gracz jest już powalony — nie można go dobić (anulujemy wszelkie obrażenia)
        if (module.isKnocked(uuid)) {
            event.setCancelled(true);
            return;
        }

        // 2. Jeśli gracz miałby zginąć od tych obrażeń — najpierw sprawdzamy ubezpieczenie.
        // Aktywne ubezpieczenie ma pierwszeństwo przed systemem powalania i zużywa polisę.
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            InsuranceModule insurance = SkyRiseCore.getInstance().getModuleManager().getModule("Ubezpieczenie");
            if (insurance != null && !insurance.shouldIgnoreCause(event.getCause())) {
                String effectId = insurance.getSelectedEffectId(uuid);
                if (insurance.consumeInsurance(uuid)) {
                    event.setCancelled(true);
                    insurance.applyProtection(player, event.getCause(), effectId);
                    return;
                }
            }

            // Ignoruj próby powalenia od próby samobójstwa w próżni (void)
            if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;

            event.setCancelled(true);
            module.knockPlayer(player);
        }
    }

    // ═══════════════════════════════════════
    // BLOKADA BICIA INNYCH PRZEZ POWALONEGO
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamageByKnocked(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager) {
            if (module.isKnocked(damager.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    // ═══════════════════════════════════════
    // BLOKADA RUCHU (Dozwolona jedynie rotacja głowy)
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (module.isKnocked(player.getUniqueId())) {
            Location from = event.getFrom();
            Location to = event.getTo();

            // Wymuszaj czołganie na serwerze przy każdym drgnięciu
            player.setSwimming(true);
            player.setPose(org.bukkit.entity.Pose.SWIMMING);

            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                // Pozwalamy na rotację myszką (yaw/pitch), ale cofamy ruch w przestrzeni (X, Y, Z)
                Location dest = from.clone();
                dest.setYaw(to.getYaw());
                dest.setPitch(to.getPitch());
                event.setTo(dest);
            }
        }
    }

    // ═══════════════════════════════════════
    // BLOKADA ZMIANY STANÓW PŁYWANIA (Czołgania)
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onToggleSwim(org.bukkit.event.entity.EntityToggleSwimEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (module.isKnocked(player.getUniqueId())) {
                // Jeśli jest powalony, nie pozwalamy mu przestać czołgać się (pływać na lądzie)
                if (!event.isSwimming()) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // ═══════════════════════════════════════
    // WYKRWAWIANIE (3x Shift)
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (module.isKnocked(player.getUniqueId())) {
            // Liczymy tylko kucnięcie (isSneaking = true)
            if (event.isSneaking()) {
                module.handleKnockedSneak(player);
            }
        }
    }

    // ═══════════════════════════════════════
    // BLOKADY AKCJI DLA POWALONEGO GRACZA
    // ═══════════════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (module.isKnocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ColorUtil.mini("<red>» Nie możesz używać komend podczas powalenia!"));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (module.isKnocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (module.isKnocked(event.getWhoClicked().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (module.isKnocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (module.isKnocked(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSwapItems(PlayerSwapHandItemsEvent event) {
        if (module.isKnocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (module.isKnocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (module.isKnocked(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
