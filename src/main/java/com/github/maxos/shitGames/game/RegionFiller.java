package com.github.maxos.shitGames.game;

import com.github.maxos.shitGames.util.Cuboid;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public final class RegionFiller {

	private static final BlockData AIR = Material.AIR.createBlockData();

	private final Plugin plugin;
	private final World world;
	private final List<Cuboid> layers;
	private final BlockData layerData;
	private final BlockData coverData;
	private final int perTick;
	private final Runnable onDone;

	private int layerIndex;
	private long cursor;
	private BukkitTask task;
	private boolean finished;

	public RegionFiller(Plugin plugin, World world, List<Cuboid> layers, BlockData layerData, BlockData coverData,
	                    int perTick, Runnable onDone) {
		this.plugin = plugin;
		this.world = world;
		this.layers = layers;
		this.layerData = layerData;
		this.coverData = coverData;
		this.perTick = perTick;
		this.onDone = onDone;
	}

	public void start() {
		if (world == null || layers.isEmpty()) {
			finish();
			return;
		}
		task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::process, 0L, 1L);
	}

	public void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}
		finished = true;
	}

	private void process() {
		int budget = perTick;
		while (budget > 0) {
			if (layerIndex >= layers.size()) {
				finish();
				return;
			}
			Cuboid cuboid = layers.get(layerIndex);
			long volume = cuboid.volume();
			if (cursor >= volume) {
				layerIndex++;
				cursor = 0L;
				continue;
			}
			int lengthX = cuboid.lengthX();
			int lengthZ = cuboid.lengthZ();
			int area = lengthX * lengthZ;
			int index = (int) cursor;
			int y = cuboid.minY() + index / area;
			int rest = index % area;
			int x = cuboid.minX() + rest % lengthX;
			int z = cuboid.minZ() + rest / lengthX;

			world.getBlockAt(x, y, z).setBlockData(layerData, false);

			if (y == cuboid.maxY()) {
				int above = y + 1;
				if (!insideAnyLayer(x, above, z)) {
					world.getBlockAt(x, above, z).setBlockData(coverData == null ? AIR : coverData, false);
				}
			}

			cursor++;
			budget--;
		}
	}

	private boolean insideAnyLayer(int x, int y, int z) {
		for (int i = 0, size = layers.size(); i < size; i++) {
			if (layers.get(i).contains(x, y, z)) {
				return true;
			}
		}
		return false;
	}

	private void finish() {
		if (finished) {
			return;
		}
		finished = true;
		if (task != null) {
			task.cancel();
			task = null;
		}
		if (onDone != null) {
			onDone.run();
		}
	}
}
