package pl.skyrise.skyRiseCore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Narzędzia do kolorowania tekstu — Paper/Adventure API.
 * MiniMessage: <red>tekst</red>, <gradient:red:blue>tekst</gradient>
 * Legacy: &c, &l, &#RRGGBB
 */
public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Legacy serializer z HEX — &c, &l, &#A30000. */
    private static final LegacyComponentSerializer LEGACY_HEX = LegacyComponentSerializer.builder()
            .hexColors()
            .character('&')
            .build();

    private ColorUtil() {}

    /**
     * Parsuje tekst z MiniMessage <tagami> do Component.
     */
    public static Component mini(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI.deserialize(text);
    }

    /**
     * Escapuje tagi MiniMessage — użyj na input gracza przed wstawieniem do formatu.
     */
    public static String escape(String text) {
        if (text == null || text.isEmpty()) return "";
        return MINI.escapeTags(text);
    }

    /**
     * Konwertuje & kody i &#HEX na MiniMessage <tagi>.
     * Używaj na prefixach z Vault/LuckPerms przed wstawieniem do formatu MiniMessage.
     *
     * Obsługuje: &c, &l, &k, &#A30000, &#E0115F itd.
     * "&4Admin"           → "<dark_red>Admin"
     * "&#A30000Właściciel" → "<color:#A30000>Właściciel"
     */
    public static String legacyToMini(String text) {
        if (text == null || text.isEmpty()) return "";
        Component component = LEGACY_HEX.deserialize(text);
        return MINI.serialize(component);
    }

    /**
     * Component → czysty tekst (bez kolorów).
     */
    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /**
     * String z kolorami → czysty tekst.
     */
    public static String stripColor(String text) {
        return plain(mini(text));
    }
}
