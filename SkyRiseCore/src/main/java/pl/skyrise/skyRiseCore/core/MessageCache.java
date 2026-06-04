package pl.skyrise.skyRiseCore.core;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Globalny cooldown na wiadomości wysyłane graczom przez moduły.
 * Klucz: "UUID:messageKey" → timestamp.
 * Thread-safe, samo-czyszczący się przy wyjściu gracza.
 */
public class MessageCache implements Listener {

    private final ConcurrentHashMap<String, Long> cache = new ConcurrentHashMap<>();
    private long cooldownMs;

    public MessageCache(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    /**
     * Sprawdza czy wiadomość może być wysłana (cooldown minął).
     * Jeśli tak — automatycznie zapisuje timestamp.
     */
    public boolean canSend(UUID uuid, String messageKey) {
        String key = uuid.toString() + ":" + messageKey;
        long now = System.currentTimeMillis();
        Long last = cache.get(key);
        if (last != null && (now - last) < cooldownMs) return false;
        cache.put(key, now);
        return true;
    }

    /**
     * Zwraca timestamp ostatniego wysłania (lub 0 jeśli nigdy).
     * Używane przez moduły z własnym czasem cooldown (np. ChatModule 30s).
     */
    public long getLastSend(UUID uuid, String messageKey) {
        Long last = cache.get(uuid.toString() + ":" + messageKey);
        return last != null ? last : 0;
    }

    /**
     * Zapisuje timestamp bez sprawdzania cooldownu.
     */
    public void setSent(UUID uuid, String messageKey) {
        cache.put(uuid.toString() + ":" + messageKey, System.currentTimeMillis());
    }

    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public void clean(UUID uuid) {
        String prefix = uuid.toString() + ":";
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clean(event.getPlayer().getUniqueId());
    }
}
