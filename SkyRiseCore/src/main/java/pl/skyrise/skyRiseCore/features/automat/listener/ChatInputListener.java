package pl.skyrise.skyRiseCore.features.automat.listener;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pl.skyrise.skyRiseCore.features.automat.AutomatModule;
import pl.skyrise.skyRiseCore.features.automat.util.ColorUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputListener implements Listener {

    private final AutomatModule plugin;
    private static final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    public ChatInputListener(AutomatModule plugin) {
        this.plugin = plugin;
    }

    public static void requestInput(Player player, String prompt, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
        player.sendMessage(AutomatModule.getInstance().getPrefix() + ColorUtil.color(prompt));
        player.sendMessage(AutomatModule.getInstance().getPrefix() + ColorUtil.color("&fWpisz &ecancel &faby anulować."));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Consumer<String> callback = pendingInputs.remove(event.getPlayer().getUniqueId());
        if (callback == null) return;

        event.setCancelled(true);
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () ->
                    event.getPlayer().sendMessage(plugin.getPrefix() + ColorUtil.color("&cAnulowano.")));
            return;
        }

        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> callback.accept(message));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    public static void clearPendingInputs() {
        pendingInputs.clear();
    }
}