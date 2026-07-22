package com.github.maxos.shitGames.config;

import com.github.maxos.shitGames.util.ItemSpec;
import com.github.maxos.shitGames.util.Keys;
import com.github.maxos.shitGames.util.ParticleSpec;
import com.github.maxos.shitGames.util.SoundSpec;
import com.github.maxos.shitGames.util.Text;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;

public final class ConfigManager {

	private final JavaPlugin plugin;
	private volatile Cfg cfg;

	public ConfigManager(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public Cfg cfg() {
		return cfg;
	}

	public void load() {
		plugin.saveDefaultConfig();
		plugin.reloadConfig();
		FileConfiguration raw = plugin.getConfig();
		raw.options().copyDefaults(true);
		this.cfg = build(raw);
	}

	private Cfg build(FileConfiguration raw) {
		String prefix = raw.getString("settings.prefix", "");

		Set<String> allowed = new java.util.HashSet<>();
		for (String command : raw.getStringList("settings.allowed-commands")) {
			allowed.add(normalizeCommand(command));
		}

		Cfg.General general = new Cfg.General(
				prefix,
				allowed,
				raw.getBoolean("settings.block-commands", true),
				raw.getBoolean("settings.deny-teleport", true),
				Math.max(4.0D, raw.getDouble("settings.lobby-radius", 60.0D)),
				Math.max(1000L, raw.getLong("settings.max-selection-volume", 2_000_000L)),
				raw.getBoolean("settings.protect-arenas", true),
				raw.getBoolean("settings.debug", false)
		);

		IntSet announceAt = new IntOpenHashSet();
		for (int value : raw.getIntegerList("game.countdown-announce-at")) {
			announceAt.add(value);
		}
		if (announceAt.isEmpty()) {
			announceAt.addAll(IntSet.of(60, 30, 15, 10, 5, 4, 3, 2, 1));
		}

		int minPlayers = Math.max(2, raw.getInt("game.min-players", 2));
		int maxPlayers = Math.max(minPlayers, raw.getInt("game.max-players", 16));

		Cfg.Game game = new Cfg.Game(
				minPlayers,
				maxPlayers,
				Math.max(3, raw.getInt("game.lobby-countdown", 45)),
				Math.max(1, raw.getInt("game.short-countdown", 10)),
				announceAt,
				Math.max(0, raw.getInt("game.grace-seconds", 3)),
				Math.max(1, raw.getInt("game.end-delay-seconds", 6)),
				Math.max(10, raw.getInt("game.zone-timer-seconds", 300)),
				Math.max(64, raw.getInt("game.reset-blocks-per-tick", 6000)),
				raw.getBoolean("game.eliminate-outside-region", true),
				raw.getBoolean("game.shrink.enabled", true),
				Math.max(1, raw.getInt("game.shrink.blocks-per-second", 12)),
				Math.max(0, raw.getInt("game.shrink.acceleration", 6)),
				Math.max(1, raw.getInt("game.shrink.acceleration-interval", 10)),
				Math.max(1, raw.getInt("game.shrink.max-blocks-per-second", 160))
		);

		Material tntLayer = material(raw.getString("games.tnt-run.layer-material"), Material.TNT);
		Material tntCover = material(raw.getString("games.tnt-run.cover-material"), Material.WHITE_CARPET);
		Cfg.TntRun tntRun = new Cfg.TntRun(
				raw.getBoolean("games.tnt-run.enabled", true),
				tntLayer,
				tntLayer.createBlockData(),
				tntCover,
				tntCover.createBlockData(),
				Math.max(0, raw.getInt("games.tnt-run.fall-delay-ticks", 6)),
				raw.getBoolean("games.tnt-run.falling-block-entity", true),
				Math.max(10, raw.getInt("games.tnt-run.falling-block-lifetime-ticks", 100))
		);

		Material spleefLayer = material(raw.getString("games.spleef.layer-material"), Material.SNOW_BLOCK);
		Cfg.Spleef spleef = new Cfg.Spleef(
				raw.getBoolean("games.spleef.enabled", true),
				spleefLayer,
				spleefLayer.createBlockData(),
				ItemSpec.read(raw.getConfigurationSection("games.spleef.tool"), Material.DIAMOND_SHOVEL, 0, Keys.TOOL)
		);

		Cfg.DoubleJump doubleJump = new Cfg.DoubleJump(
				raw.getBoolean("double-jump.enabled", true),
				Math.max(3, raw.getInt("double-jump.give-interval-seconds", 20)),
				clamp(raw.getDouble("double-jump.chance", 0.4D)),
				Math.max(1, raw.getInt("double-jump.max-amount", 2)),
				raw.getDouble("double-jump.power", 0.95D),
				raw.getDouble("double-jump.forward-boost", 0.35D),
				ItemSpec.read(raw.getConfigurationSection("double-jump.item"), Material.FEATHER, 4, Keys.DOUBLE_JUMP)
		);

		ItemStack filler = null;
		ConfigurationSection fillerSection = raw.getConfigurationSection("vote.menu.filler");
		if (fillerSection != null && fillerSection.getBoolean("enabled", true)) {
			filler = ItemSpec.read(fillerSection, Material.GRAY_STAINED_GLASS_PANE, 0, null).build();
		}

		Cfg.Vote vote = new Cfg.Vote(
				ItemSpec.read(raw.getConfigurationSection("vote.item"), Material.NETHER_STAR, 4, Keys.VOTE),
				Message.of(raw.getString("vote.menu.title", "&8Выбор игры")),
				Math.max(1, Math.min(6, raw.getInt("vote.menu.rows", 3))),
				filler,
				ItemSpec.read(raw.getConfigurationSection("vote.menu.tnt-run"), Material.TNT, 11, null),
				ItemSpec.read(raw.getConfigurationSection("vote.menu.spleef"), Material.SNOW_BLOCK, 15, null),
				List.copyOf(raw.getStringList("vote.menu.vote-lore"))
		);

		Cfg.King king = new Cfg.King(
				raw.getBoolean("king-of-the-hill.enabled", true),
				Math.max(1, raw.getInt("king-of-the-hill.capture-seconds", 20)),
				Math.max(1, raw.getInt("king-of-the-hill.update-ticks", 4)),
				raw.getBoolean("king-of-the-hill.reset-progress-on-leave", false),
				Math.max(0, raw.getInt("king-of-the-hill.progress-loss-per-second", 1)),
				Message.of(raw.getString("king-of-the-hill.bossbar.title", "&eЗахват вершины")),
				barColor(raw.getString("king-of-the-hill.bossbar.color"), BossBar.Color.YELLOW),
				barOverlay(raw.getString("king-of-the-hill.bossbar.style"), BossBar.Overlay.NOTCHED_10),
				raw.getString("king-of-the-hill.no-king", "&7Никого"),
				Math.max(1, raw.getInt("king-of-the-hill.contest-message-cooldown", 4)),
				List.copyOf(raw.getStringList("king-of-the-hill.reward-commands"))
		);

		Cfg.Bars bars = new Cfg.Bars(
				Message.of(raw.getString("bossbar.arena.title", "&fДо сужения зоны: &c%time%")),
				Message.of(raw.getString("bossbar.arena.expired-title", "&cВремя вышло!")),
				barColor(raw.getString("bossbar.arena.color"), BossBar.Color.YELLOW),
				barColor(raw.getString("bossbar.arena.expired-color"), BossBar.Color.RED),
				barOverlay(raw.getString("bossbar.arena.style"), BossBar.Overlay.PROGRESS)
		);

		Cfg.Titles titles = new Cfg.Titles(
				Math.max(0, raw.getInt("titles.fade-in", 6)),
				Math.max(1, raw.getInt("titles.stay", 40)),
				Math.max(0, raw.getInt("titles.fade-out", 8))
		);

		Cfg.Rewards rewards = new Cfg.Rewards(
				List.copyOf(raw.getStringList("rewards.winner-commands")),
				List.copyOf(raw.getStringList("rewards.participation-commands"))
		);

		EnumMap<Msg, Message> messages = new EnumMap<>(Msg.class);
		for (Msg key : Msg.values()) {
			String value = raw.getString("messages." + key.getPath(), "");
			if (value == null) {
				value = "";
			}
			messages.put(key, Message.of(value.replace("%prefix%", prefix)));
		}

		EnumMap<Sfx, SoundSpec> sounds = new EnumMap<>(Sfx.class);
		for (Sfx key : Sfx.values()) {
			String path = "sounds." + key.getPath();
			String value = raw.getString(path);
			SoundSpec spec = SoundSpec.parse(value);
			if (spec != null) {
				sounds.put(key, spec);
			} else if (isBroken(value)) {
				plugin.getLogger().warning("Не удалось разобрать звук в " + path + ": " + value);
			}
		}

		EnumMap<Fx, ParticleSpec> particles = new EnumMap<>(Fx.class);
		for (Fx key : Fx.values()) {
			String path = "particles." + key.getPath();
			String value = raw.getString(path);
			ParticleSpec spec = ParticleSpec.parse(value);
			if (spec != null) {
				particles.put(key, spec);
			} else if (isBroken(value)) {
				plugin.getLogger().warning("Не удалось разобрать частицу в " + path + ": " + value
						+ " (частица должна существовать и не требовать доп. данных)");
			}
		}

		return new Cfg(general, game, tntRun, spleef, doubleJump, vote, king, bars, titles, rewards,
				messages, sounds, particles);
	}

	private boolean isBroken(String value) {
		if (value == null) {
			return false;
		}
		String trimmed = value.trim();
		return !trimmed.isEmpty() && !trimmed.equalsIgnoreCase("none") && !trimmed.equalsIgnoreCase("off");
	}

	private String normalizeCommand(String raw) {
		String value = raw.trim().toLowerCase(Locale.ROOT);
		return value.startsWith("/") ? value.substring(1) : value;
	}

	private double clamp(double value) {
		return Math.max(0.0D, Math.min(1.0D, value));
	}

	private Material material(String name, Material fallback) {
		if (name == null || name.isEmpty()) {
			return fallback;
		}
		Material material = Material.matchMaterial(name);
		if (material == null || !material.isBlock()) {
			plugin.getLogger().log(Level.WARNING, "Неизвестный материал в конфигурации: " + name + ", используется " + fallback);
			return fallback;
		}
		return material;
	}

	private BossBar.Color barColor(String name, BossBar.Color fallback) {
		if (name == null) {
			return fallback;
		}
		try {
			return BossBar.Color.valueOf(name.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	private BossBar.Overlay barOverlay(String name, BossBar.Overlay fallback) {
		if (name == null) {
			return fallback;
		}
		try {
			return BossBar.Overlay.valueOf(name.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return fallback;
		}
	}

	public List<String> colorizedList(List<String> input) {
		List<String> result = new ArrayList<>(input.size());
		for (String line : input) {
			result.add(Text.plain(Text.colorize(line)));
		}
		return result;
	}
}
