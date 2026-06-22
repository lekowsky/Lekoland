package pl.skyrise.skyRiseCore.features.insurance.util;

import net.kyori.adventure.text.Component;
import pl.skyrise.skyRiseCore.utils.ColorUtil;

/**
 * Kompatybilny adapter modułu Insurance do centralnego ColorUtil.
 * Zostawiony, aby nie rozlewać importów po module.
 */
public final class InsuranceText {

    private InsuranceText() {}

    public static Component component(String text) {
        return ColorUtil.legacy(text);
    }

    public static String color(String text) {
        return ColorUtil.legacyColor(text);
    }
}
