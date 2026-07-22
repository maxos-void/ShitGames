package com.github.maxos.shitGames.vault;

import com.github.maxos.shitGames.util.Locations;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class VaultManager {

	private final JavaPlugin plugin;
	private final File folder;
	private final Map<UUID, PlayerVault> vaults = new ConcurrentHashMap<>();

	public VaultManager(JavaPlugin plugin) {
		this.plugin = plugin;
		this.folder = new File(plugin.getDataFolder(), "vaults");
		if (!folder.exists() && !folder.mkdirs()) {
			plugin.getLogger().warning("Не удалось создать папку vaults");
		}
	}

	public boolean has(UUID uuid) {
		return vaults.containsKey(uuid);
	}

	public int size() {
		return vaults.size();
	}

	public void loadAll() {
		vaults.clear();
		File[] files = folder.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
		if (files == null) {
			return;
		}
		for (File file : files) {
			try {
				PlayerVault vault = read(file);
				if (vault != null) {
					vaults.put(vault.uuid(), vault);
				}
			} catch (RuntimeException exception) {
				plugin.getLogger().log(Level.WARNING, "Повреждённое хранилище инвентаря: " + file.getName(), exception);
			}
		}
		if (!vaults.isEmpty()) {
			plugin.getLogger().info("Восстановлено несохранённых инвентарей: " + vaults.size());
		}
	}

	public void capture(Player player) {
		UUID uuid = player.getUniqueId();
		if (vaults.containsKey(uuid)) {
			return;
		}
		PlayerVault vault = new PlayerVault(
				uuid,
				player.getLocation().clone(),
				player.getGameMode(),
				player.getInventory().getContents().clone(),
				player.getLevel(),
				player.getExp(),
				player.getHealth(),
				player.getFoodLevel(),
				player.getSaturation(),
				new ArrayList<>(player.getActivePotionEffects()),
				player.getAllowFlight(),
				player.isFlying(),
				player.getFireTicks()
		);
		vaults.put(uuid, vault);
		writeAsync(vault);
	}

	public void sanitize(Player player) {
		player.closeInventory();
		player.getInventory().clear();
		player.getInventory().setHeldItemSlot(0);
		player.setGameMode(GameMode.SURVIVAL);
		player.setLevel(0);
		player.setExp(0.0F);
		player.setFoodLevel(20);
		player.setSaturation(20.0F);
		player.setExhaustion(0.0F);
		player.setFireTicks(0);
		player.setFallDistance(0.0F);
		player.setFlying(false);
		player.setAllowFlight(false);
		player.setHealth(maxHealth(player));
		player.setVelocity(new org.bukkit.util.Vector());
		for (PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
	}

	public boolean restore(Player player) {
		PlayerVault vault = vaults.remove(player.getUniqueId());
		if (vault == null) {
			return false;
		}
		deleteAsync(vault.uuid());
		player.closeInventory();
		for (PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
		player.setFireTicks(0);
		player.setFallDistance(0.0F);
		player.setVelocity(new org.bukkit.util.Vector());
		Location location = vault.location();
		if (location != null && location.getWorld() != null) {
			player.teleport(location);
		}
		player.getInventory().setContents(vault.contents());
		player.setGameMode(vault.gameMode());
		player.setLevel(vault.level());
		player.setExp(vault.exp());
		player.setHealth(Math.min(Math.max(vault.health(), 0.5D), maxHealth(player)));
		player.setFoodLevel(vault.food());
		player.setSaturation(vault.saturation());
		player.setAllowFlight(vault.allowFlight());
		player.setFlying(vault.allowFlight() && vault.flying());
		player.setFireTicks(vault.fireTicks());
		Collection<PotionEffect> effects = vault.effects();
		if (effects != null) {
			for (PotionEffect effect : effects) {
				player.addPotionEffect(effect);
			}
		}
		player.updateInventory();
		return true;
	}

	public Location pendingLocation(UUID uuid) {
		PlayerVault vault = vaults.get(uuid);
		return vault == null ? null : vault.location();
	}

	private double maxHealth(Player player) {
		var attribute = player.getAttribute(Attribute.MAX_HEALTH);
		return attribute == null ? 20.0D : attribute.getValue();
	}

	private File fileOf(UUID uuid) {
		return new File(folder, uuid + ".yml");
	}

	private PlayerVault read(File file) {
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		String rawUuid = yaml.getString("uuid");
		if (rawUuid == null) {
			return null;
		}
		UUID uuid;
		try {
			uuid = UUID.fromString(rawUuid);
		} catch (IllegalArgumentException exception) {
			return null;
		}
		List<?> rawContents = yaml.getList("contents", List.of());
		ItemStack[] contents = new ItemStack[Math.max(41, rawContents.size())];
		for (int i = 0; i < rawContents.size(); i++) {
			Object value = rawContents.get(i);
			contents[i] = value instanceof ItemStack stack ? stack : null;
		}
		List<PotionEffect> effects = new ArrayList<>();
		for (Object value : yaml.getList("effects", List.of())) {
			if (value instanceof PotionEffect effect) {
				effects.add(effect);
			}
		}
		GameMode mode;
		try {
			mode = GameMode.valueOf(yaml.getString("gamemode", "SURVIVAL"));
		} catch (IllegalArgumentException exception) {
			mode = GameMode.SURVIVAL;
		}
		return new PlayerVault(
				uuid,
				Locations.deserialize(yaml.getString("location")),
				mode,
				contents,
				yaml.getInt("level"),
				(float) yaml.getDouble("exp"),
				yaml.getDouble("health", 20.0D),
				yaml.getInt("food", 20),
				(float) yaml.getDouble("saturation", 20.0D),
				effects,
				yaml.getBoolean("allow-flight"),
				yaml.getBoolean("flying"),
				yaml.getInt("fire-ticks")
		);
	}

	private void writeAsync(PlayerVault vault) {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.set("uuid", vault.uuid().toString());
		yaml.set("location", Locations.serialize(vault.location()));
		yaml.set("gamemode", vault.gameMode().name());
		yaml.set("contents", Arrays.asList(vault.contents()));
		yaml.set("level", vault.level());
		yaml.set("exp", vault.exp());
		yaml.set("health", vault.health());
		yaml.set("food", vault.food());
		yaml.set("saturation", vault.saturation());
		yaml.set("effects", new ArrayList<>(vault.effects()));
		yaml.set("allow-flight", vault.allowFlight());
		yaml.set("flying", vault.flying());
		yaml.set("fire-ticks", vault.fireTicks());
		File target = fileOf(vault.uuid());
		Runnable task = () -> {
			try {
				yaml.save(target);
			} catch (IOException exception) {
				plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить инвентарь " + vault.uuid(), exception);
			}
		};
		if (plugin.isEnabled()) {
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
		} else {
			task.run();
		}
	}

	private void deleteAsync(UUID uuid) {
		File target = fileOf(uuid);
		Runnable task = () -> {
			if (target.exists() && !target.delete()) {
				plugin.getLogger().warning("Не удалось удалить файл инвентаря " + uuid);
			}
		};
		if (plugin.isEnabled()) {
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task);
		} else {
			task.run();
		}
	}
}
