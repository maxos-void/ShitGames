package com.github.maxos.shitGames.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Locale;

public record ParticleSpec(Particle particle, int count, double offsetX, double offsetY, double offsetZ, double speed) {

	public static ParticleSpec parse(String raw) {
		if (raw == null) {
			return null;
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("none") || trimmed.equalsIgnoreCase("off")) {
			return null;
		}
		String[] parts = trimmed.split("\\s+");
		Particle particle;
		try {
			particle = Particle.valueOf(parts[0].toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return null;
		}
		if (particle.getDataType() != Void.class) {
			return null;
		}
		int count = parts.length > 1 ? parseInt(parts[1], 8) : 8;
		double offsetX = parts.length > 2 ? parseDouble(parts[2], 0.3D) : 0.3D;
		double offsetY = parts.length > 3 ? parseDouble(parts[3], 0.3D) : 0.3D;
		double offsetZ = parts.length > 4 ? parseDouble(parts[4], 0.3D) : 0.3D;
		double speed = parts.length > 5 ? parseDouble(parts[5], 0.02D) : 0.02D;
		return new ParticleSpec(particle, count, offsetX, offsetY, offsetZ, speed);
	}

	private static int parseInt(String raw, int fallback) {
		try {
			return Integer.parseInt(raw);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	private static double parseDouble(String raw, double fallback) {
		try {
			return Double.parseDouble(raw);
		} catch (NumberFormatException exception) {
			return fallback;
		}
	}

	public void show(Player viewer, Location location) {
		viewer.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
	}

	public void show(Collection<? extends Player> viewers, Location location) {
		for (Player viewer : viewers) {
			viewer.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
		}
	}
}
