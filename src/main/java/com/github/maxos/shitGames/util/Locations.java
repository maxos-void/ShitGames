package com.github.maxos.shitGames.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public final class Locations {

	private Locations() {
	}

	public static String serialize(Location location) {
		if (location == null || location.getWorld() == null) {
			return null;
		}
		return location.getWorld().getName() + ';'
				+ round(location.getX()) + ';'
				+ round(location.getY()) + ';'
				+ round(location.getZ()) + ';'
				+ round(location.getYaw()) + ';'
				+ round(location.getPitch());
	}

	public static Location deserialize(String raw) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}
		String[] parts = raw.split(";");
		if (parts.length < 4) {
			return null;
		}
		World world = Bukkit.getWorld(parts[0]);
		if (world == null) {
			return null;
		}
		try {
			double x = Double.parseDouble(parts[1]);
			double y = Double.parseDouble(parts[2]);
			double z = Double.parseDouble(parts[3]);
			float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0.0F;
			float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0.0F;
			return new Location(world, x, y, z, yaw, pitch);
		} catch (NumberFormatException exception) {
			return null;
		}
	}

	public static Location blockCenter(Location location) {
		return new Location(
				location.getWorld(),
				location.getBlockX() + 0.5D,
				location.getBlockY(),
				location.getBlockZ() + 0.5D,
				location.getYaw(),
				location.getPitch()
		);
	}

	private static double round(double value) {
		return Math.round(value * 1000.0D) / 1000.0D;
	}

	private static float round(float value) {
		return Math.round(value * 10.0F) / 10.0F;
	}
}
