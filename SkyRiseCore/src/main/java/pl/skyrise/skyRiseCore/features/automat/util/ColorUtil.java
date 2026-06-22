package pl.skyrise.skyRiseCore.features.automat.util;

import java.util.List;

/**
 * Adapter legacy dla przeniesionego modułu Automat.
 * Implementacja jest centralnie w pl.skyrise.skyRiseCore.utils.ColorUtil.
 */
public final class ColorUtil {

    private ColorUtil() {}

    public static String color(String message) {
        return pl.skyrise.skyRiseCore.utils.ColorUtil.legacyColor(message);
    }

    public static List<String> color(List<String> lines) {
        return pl.skyrise.skyRiseCore.utils.ColorUtil.legacyColor(lines);
    }

    public static String strip(String message) {
        return pl.skyrise.skyRiseCore.utils.ColorUtil.legacyStrip(message);
    }
}
