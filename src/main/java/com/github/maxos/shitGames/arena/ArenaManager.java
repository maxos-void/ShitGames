package com.github.maxos.shitGames.arena;

import com.github.maxos.shitGames.util.Cuboid;
import com.github.maxos.shitGames.util.Locations;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class ArenaManager {

	private final JavaPlugin plugin;
	private final File file;
	private final Object2ObjectLinkedOpenHashMap<String, Arena> arenas = new Object2ObjectLinkedOpenHashMap<>();

	public ArenaManager(JavaPlugin plugin) {
		this.plugin = plugin;
		this.file = new File(plugin.getDataFolder(), "arenas.yml");
	}

	public Collection<Arena> all() {
		return arenas.values();
	}

	public List<String> names() {
		return new ArrayList<>(arenas.keySet());
	}

	public Arena get(String name) {
		return name == null ? null : arenas.get(name.toLowerCase(Locale.ROOT));
	}

	public boolean exists(String name) {
		return get(name) != null;
	}

	public Arena create(String name) {
		Arena arena = new Arena(name.toLowerCase(Locale.ROOT));
		arenas.put(arena.getName(), arena);
		return arena;
	}

	public boolean delete(String name) {
		return arenas.remove(name.toLowerCase(Locale.ROOT)) != null;
	}

	public Arena arenaAt(org.bukkit.Location location) {
		if (location == null || location.getWorld() == null) {
			return null;
		}
		String worldName = location.getWorld().getName();
		int x = location.getBlockX();
		int y = location.getBlockY();
		int z = location.getBlockZ();
		for (Arena arena : arenas.values()) {
			Cuboid region = arena.getRegion();
			if (region != null && region.world().equals(worldName) && region.contains(x, y, z)) {
				return arena;
			}
		}
		return null;
	}

	public Arena arenaAt(org.bukkit.block.Block block) {
		String worldName = block.getWorld().getName();
		int x = block.getX();
		int y = block.getY();
		int z = block.getZ();
		for (Arena arena : arenas.values()) {
			Cuboid region = arena.getRegion();
			if (region != null && region.world().equals(worldName) && region.contains(x, y, z)) {
				return arena;
			}
		}
		return null;
	}

	public void load() {
		arenas.clear();
		if (!file.exists()) {
			return;
		}
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		ConfigurationSection root = yaml.getConfigurationSection("arenas");
		if (root == null) {
			return;
		}
		for (String key : root.getKeys(false)) {
			ConfigurationSection section = root.getConfigurationSection(key);
			if (section == null) {
				continue;
			}
			Arena arena = new Arena(key.toLowerCase(Locale.ROOT));
			arena.setRegion(Cuboid.deserialize(section.getString("region")));
			arena.setDeathZone(Cuboid.deserialize(section.getString("death-zone")));
			arena.setLobby(Locations.deserialize(section.getString("lobby")));
			arena.setEnabled(section.getBoolean("enabled", true));
			arena.setMinPlayers(section.getInt("min-players", -1));
			arena.setMaxPlayers(section.getInt("max-players", -1));
			for (String layer : section.getStringList("layers")) {
				Cuboid cuboid = Cuboid.deserialize(layer);
				if (cuboid != null) {
					arena.getLayers().add(cuboid);
				}
			}
			for (String spawn : section.getStringList("spawns")) {
				Location location = Locations.deserialize(spawn);
				if (location != null) {
					arena.getSpawns().add(location);
				}
			}
			arenas.put(arena.getName(), arena);
		}
		plugin.getLogger().info("Загружено арен: " + arenas.size());
	}

	public void save() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.options().setHeader(List.of(
				"Файл арен ShitGames. Редактируйте только при выключенном сервере.",
				"Формат кубоида: мир;minX;minY;minZ;maxX;maxY;maxZ",
				"Формат точки: мир;x;y;z;yaw;pitch"
		));
		for (Arena arena : arenas.values()) {
			String base = "arenas." + arena.getName() + '.';
			yaml.set(base + "enabled", arena.isEnabled());
			yaml.set(base + "min-players", arena.getMinPlayers());
			yaml.set(base + "max-players", arena.getMaxPlayers());
			yaml.set(base + "region", arena.getRegion() == null ? null : arena.getRegion().serialize());
			yaml.set(base + "death-zone", arena.getDeathZone() == null ? null : arena.getDeathZone().serialize());
			yaml.set(base + "lobby", Locations.serialize(arena.getLobby()));
			List<String> layers = new ArrayList<>(arena.getLayers().size());
			for (Cuboid cuboid : arena.getLayers()) {
				layers.add(cuboid.serialize());
			}
			yaml.set(base + "layers", layers);
			List<String> spawns = new ArrayList<>(arena.getSpawns().size());
			for (Location location : arena.getSpawns()) {
				String serialized = Locations.serialize(location);
				if (serialized != null) {
					spawns.add(serialized);
				}
			}
			yaml.set(base + "spawns", spawns);
		}
		try {
			File parent = file.getParentFile();
			if (parent != null && !parent.exists() && !parent.mkdirs()) {
				plugin.getLogger().warning("Не удалось создать папку плагина");
			}
			yaml.save(file);
		} catch (IOException exception) {
			plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить arenas.yml", exception);
		}
	}
}
