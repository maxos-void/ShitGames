package com.github.maxos.shitGames.arena;

import com.github.maxos.shitGames.util.Cuboid;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.List;

@Getter
@Setter
public final class Arena {

	private final String name;
	private Cuboid region;
	private Cuboid deathZone;
	private Location lobby;
	private final List<Cuboid> layers = new ObjectArrayList<>();
	private final List<Location> spawns = new ObjectArrayList<>();
	private int minPlayers = -1;
	private int maxPlayers = -1;
	private boolean enabled = true;

	public Arena(String name) {
		this.name = name;
	}

	public boolean isReady() {
		return region != null
				&& deathZone != null
				&& lobby != null
				&& !layers.isEmpty()
				&& !spawns.isEmpty()
				&& region.bukkitWorld() != null;
	}

	public World world() {
		return region == null ? null : region.bukkitWorld();
	}

	public int resolvedMin(int globalMin) {
		return minPlayers > 0 ? minPlayers : globalMin;
	}

	public int resolvedMax(int globalMax) {
		return maxPlayers > 0 ? maxPlayers : globalMax;
	}

	public boolean isLayerBlock(int x, int y, int z) {
		for (int i = 0, size = layers.size(); i < size; i++) {
			if (layers.get(i).contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	public boolean isDeadly(int x, int y, int z) {
		if (deathZone == null) {
			return false;
		}
		return y <= deathZone.maxY()
				&& (deathZone.containsHorizontally(x, z) || region == null || region.containsHorizontally(x, z));
	}

	public String missingParts() {
		StringBuilder builder = new StringBuilder();
		if (region == null) {
			builder.append("region ");
		}
		if (layers.isEmpty()) {
			builder.append("layers ");
		}
		if (deathZone == null) {
			builder.append("death-zone ");
		}
		if (lobby == null) {
			builder.append("lobby ");
		}
		if (spawns.isEmpty()) {
			builder.append("spawns ");
		}
		if (region != null && region.bukkitWorld() == null) {
			builder.append("world(").append(region.world()).append(") ");
		}
		return builder.toString().trim();
	}
}
