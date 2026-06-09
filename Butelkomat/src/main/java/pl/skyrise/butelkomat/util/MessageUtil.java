package pl.skyrise.butelkomat.util;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import pl.skyrise.butelkomat.Butelkomat;

import java.util.HashMap;
import java.util.Map;

public class MessageUtil {

    private final Butelkomat plugin;
    private String prefix;

    public MessageUtil(Butelkomat plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        prefix = plugin.getConfig().getString("messages.prefix", "&8[&6Butelkomat&8] ");
    }

    public String getRaw(String key) {
        return plugin.getConfig().getString("messages." + key, "&cBrak: " + key);
    }

    public String get(String key) {
        return colorize(prefix + getRaw(key));
    }

    public String get(String key, Map<String, String> placeholders) {
        String msg = prefix + getRaw(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace(entry.getKey(), entry.getValue());
        }
        return colorize(msg);
    }

    public String getNoPrefix(String key) {
        return getRaw(key);
    }

    public void send(Player player, String key) {
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(get(key)));
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(get(key, placeholders)));
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static Map<String, String> placeholders(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length - 1; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}