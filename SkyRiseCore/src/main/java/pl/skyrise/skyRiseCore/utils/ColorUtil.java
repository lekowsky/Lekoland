package pl.skyrise.skyRiseCore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralne narzędzia do kolorowania tekstu — Paper/Adventure API.
 * MiniMessage: <red>tekst</red>, <gradient:red:blue>tekst</gradient>
 * Legacy: &c, &l, &#RRGGBB
 */
public final class ColorUtil {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    /** Legacy serializer z HEX — &c, &l, &#A30000. */
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.builder()
            .hexColors()
            .character('&')
            .build();

    /** Legacy serializer pod stare Bukkit String API — § + HEX jako §x§R§R... */
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.builder()
            .hexColors()
            .character('§')
            .build();

    private ColorUtil() {}

    /** Parsuje tekst z MiniMessage <tagami> do Component. */
    public static Component mini(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return MINI.deserialize(text);
    }

    /** Parsuje legacy (& + &#HEX) do Component i wymusza brak kursywy. */
    public static Component legacy(String text) {
        if (text == null || text.isEmpty()) return Component.empty().decoration(TextDecoration.ITALIC, false);
        return LEGACY_AMPERSAND.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    /** Legacy (& + &#HEX) → String z sekcjami § dla API wymagających Stringa. */
    public static String legacyColor(String text) {
        if (text == null || text.isEmpty()) return "";
        return LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(text));
    }

    public static List<String> legacyColor(List<String> lines) {
        List<String> colored = new ArrayList<>();
        if (lines == null) return colored;
        for (String line : lines) colored.add(legacyColor(line));
        return colored;
    }

    /** Czysty tekst z legacy (& + &#HEX). */
    public static String legacyStrip(String text) {
        return plain(legacy(text));
    }

    /** Escapuje tagi MiniMessage — użyj na input gracza przed wstawieniem do formatu. */
    public static String escape(String text) {
        if (text == null || text.isEmpty()) return "";
        return MINI.escapeTags(text);
    }

    /**
     * Konwertuje & kody i &#HEX na MiniMessage <tagi>.
     * Używaj na prefixach z Vault/LuckPerms przed wstawieniem do formatu MiniMessage.
     */
    public static String legacyToMini(String text) {
        if (text == null || text.isEmpty()) return "";
        Component component = LEGACY_AMPERSAND.deserialize(text);
        return MINI.serialize(component);
    }

    /** Component → czysty tekst (bez kolorów). */
    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    /** String MiniMessage → czysty tekst. */
    public static String stripColor(String text) {
        return plain(mini(text));
    }
}
