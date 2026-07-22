package com.github.maxos.shitGames.vault;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.Collection;
import java.util.UUID;

public record PlayerVault(UUID uuid,
                          Location location,
                          GameMode gameMode,
                          ItemStack[] contents,
                          int level,
                          float exp,
                          double health,
                          int food,
                          float saturation,
                          Collection<PotionEffect> effects,
                          boolean allowFlight,
                          boolean flying,
                          int fireTicks) {
}
