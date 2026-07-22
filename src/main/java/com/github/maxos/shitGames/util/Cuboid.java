package com.github.maxos.shitGames.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;

public record Cuboid(String world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

	public static Cuboid of(Location first, Location second) {
		return new Cuboid(
				first.getWorld().getName(),
				Math.min(first.getBlockX(), second.getBlockX()),
				Math.min(first.getBlockY(), second.getBlockY()),
				Math.min(first.getBlockZ(), second.getBlockZ()),
				Math.max(first.getBlockX(), second.getBlockX()),
				Math.max(first.getBlockY(), second.getBlockY()),
				Math.max(first.getBlockZ(), second.getBlockZ())
		);
	}

	public boolean contains(int x, int y, int z) {
		return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
	}

	public boolean contains(Location location) {
		return location != null
				&& location.getWorld() != null
				&& location.getWorld().getName().equals(world)
				&& contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	public boolean containsHorizontally(int x, int z) {
		return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
	}

	public int lengthX() {
		return maxX - minX + 1;
	}

	public int lengthY() {
		return maxY - minY + 1;
	}

	public int lengthZ() {
		return maxZ - minZ + 1;
	}

	public long volume() {
		return (long) lengthX() * lengthY() * lengthZ();
	}

	public World bukkitWorld() {
		return Bukkit.getWorld(world);
	}

	public Location center() {
		World bukkit = bukkitWorld();
		if (bukkit == null) {
			return null;
		}
		return new Location(bukkit, (minX + maxX) / 2.0D + 0.5D, (minY + maxY) / 2.0D, (minZ + maxZ) / 2.0D + 0.5D);
	}

	public BoundingBox box() {
		return new BoundingBox(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
	}

	public String serialize() {
		return world + ';' + minX + ';' + minY + ';' + minZ + ';' + maxX + ';' + maxY + ';' + maxZ;
	}

	public static Cuboid deserialize(String raw) {
		if (raw == null || raw.isEmpty()) {
			return null;
		}
		String[] parts = raw.split(";");
		if (parts.length != 7) {
			return null;
		}
		try {
			return new Cuboid(
					parts[0],
					Integer.parseInt(parts[1]),
					Integer.parseInt(parts[2]),
					Integer.parseInt(parts[3]),
					Integer.parseInt(parts[4]),
					Integer.parseInt(parts[5]),
					Integer.parseInt(parts[6])
			);
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
