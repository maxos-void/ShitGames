package com.github.maxos.shitGames.setup;

import com.github.maxos.shitGames.util.Cuboid;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.bukkit.Location;

import java.util.Map;
import java.util.UUID;

public final class SetupManager {

	private final Map<UUID, Location> first = new Object2ObjectOpenHashMap<>();
	private final Map<UUID, Location> second = new Object2ObjectOpenHashMap<>();
	private final Map<UUID, String> paint = new Object2ObjectOpenHashMap<>();

	public void setFirst(UUID uuid, Location location) {
		first.put(uuid, location);
	}

	public void setSecond(UUID uuid, Location location) {
		second.put(uuid, location);
	}

	public Location first(UUID uuid) {
		return first.get(uuid);
	}

	public Location second(UUID uuid) {
		return second.get(uuid);
	}

	public Cuboid selection(UUID uuid) {
		Location a = first.get(uuid);
		Location b = second.get(uuid);
		if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
			return null;
		}
		if (!a.getWorld().getUID().equals(b.getWorld().getUID())) {
			return null;
		}
		return Cuboid.of(a, b);
	}

	public boolean sameWorld(UUID uuid) {
		Location a = first.get(uuid);
		Location b = second.get(uuid);
		return a != null && b != null && a.getWorld() != null && b.getWorld() != null
				&& a.getWorld().getUID().equals(b.getWorld().getUID());
	}

	public String paintArena(UUID uuid) {
		return paint.get(uuid);
	}

	public void setPaintArena(UUID uuid, String arena) {
		if (arena == null) {
			paint.remove(uuid);
		} else {
			paint.put(uuid, arena);
		}
	}

	public void clear(UUID uuid) {
		first.remove(uuid);
		second.remove(uuid);
		paint.remove(uuid);
	}
}
