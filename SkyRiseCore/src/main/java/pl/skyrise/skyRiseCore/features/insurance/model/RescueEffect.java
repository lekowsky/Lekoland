package pl.skyrise.skyRiseCore.features.insurance.model;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.List;

public record RescueEffect(
        String id,
        int slot,
        Material material,
        String name,
        List<String> lore,
        Particle particle,
        int particleCount,
        Sound sound
) {}
