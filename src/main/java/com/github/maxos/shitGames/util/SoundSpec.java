package com.github.maxos.shitGames.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;

import java.util.Collection;
import java.util.Locale;

public record SoundSpec(Sound sound) {

	public static SoundSpec parse(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("none") || trimmed.equalsIgnoreCase("off")) {
			return null;
		}
		String[] parts = trimmed.split("\\s+");
		Key key;
		try {
			key = Key.key(parts[0].toLowerCase(Locale.ROOT).replace(' ', '_'));
		} catch (RuntimeException exception) {
			return null;
		}
		float volume = parts.length > 1 ? parseFloat(parts[1], 1.0F) : 1.0F;
		float pitch = parts.length > 2 ? parseFloat(parts[2], 1.0F) : 1.0F;
		return new SoundSpec(Sound.sound(key, Sound.Source.MASTER, volume, pitch));
	}

	private static float parseFloat(String raw, float fallback) {
		try {
			return Float.parseFloat(raw);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	public void play(Audience audience) {
		if (audience != null) {
			audience.playSound(sound);
		}
	}

	public void play(Audience audience, Location location) {
		if (audience != null && location != null) {
			audience.playSound(sound, location.getX(), location.getY(), location.getZ());
		}
	}

	public void playAll(Collection<? extends Audience> audiences) {
		for (Audience audience : audiences) {
			audience.playSound(sound);
		}
	}

	public void playAll(Collection<? extends Audience> audiences, Location location) {
		double x = location.getX();
		double y = location.getY();
		double z = location.getZ();
		for (Audience audience : audiences) {
			audience.playSound(sound, x, y, z);
		}
	}
}
