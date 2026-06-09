package pl.skyrise.butelkomat.listener;

import com.nexomc.nexo.api.events.furniture.NexoFurnitureInteractEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import pl.skyrise.butelkomat.Butelkomat;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FurnitureListener implements Listener {

    private final Butelkomat plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public FurnitureListener(Butelkomat plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(NexoFurnitureInteractEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        // Obsługuj tylko główną rękę (ignoruj duplikat z off-handy)
        try {
            if (event.getHand() != EquipmentSlot.HAND) return;
        } catch (Throwable ignored) {
            // Jeśli Nexo nie ma getHand() w tej wersji - cooldown i tak załatwi sprawę
        }

        String furnitureId = event.getMechanic().getItemID();
        if (!plugin.isButelkomat(furnitureId)) return;

        event.setCancelled(true);

        // COOLDOWN - dodatkowa ochrona przed spamem
        long cooldownMs = plugin.getConfig().getLong("interact-cooldown-ms", 500);
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());

        if (last != null && (now - last) < cooldownMs) {
            return; // za szybko - cisza
        }
        cooldowns.put(player.getUniqueId(), now);

        if (!player.hasPermission("butelkomat.use")) {
            plugin.getMessageUtil().send(player, "no-permission");
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();

        // Pusta ręka - cisza
        if (hand.getType() == Material.AIR) {
            return;
        }

        plugin.processReturn(player, hand);
    }
}