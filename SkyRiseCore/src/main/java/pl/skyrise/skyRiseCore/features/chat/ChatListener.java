package pl.skyrise.skyRiseCore.features.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final ChatModule module;

    public ChatListener(ChatModule module) {
        this.module = module;
    }

    /**
     * Filtruje odbiorców czatu lokalnego.
     * NIE zmienia formatu — EssentialsX, Vault/LuckPerms formatują normalnie.
     * Usuwa z recipients graczy z innego świata lub poza zasięgiem.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        Location senderLoc = sender.getLocation();
        World senderWorld = senderLoc.getWorld();
        if (senderWorld == null) return;

        double senderX = senderLoc.getX();
        double senderY = senderLoc.getY();
        double senderZ = senderLoc.getZ();
        int maxDistSq = module.getRadiusSquared();

        event.viewers().removeIf(audience -> {
            if (!(audience instanceof Player target)) return false;
            if (target == sender) return false; // Szybki skip dla nadawcy (0 alokacji)
            if (target.getWorld() != senderWorld) return true; // Szybki skip dla innych światów (0 alokacji)

            Location targetLoc = target.getLocation();
            double dx = targetLoc.getX() - senderX;
            double dy = targetLoc.getY() - senderY;
            double dz = targetLoc.getZ() - senderZ;
            return (dx * dx + dy * dy + dz * dz) > maxDistSq;
        });
    }
}
