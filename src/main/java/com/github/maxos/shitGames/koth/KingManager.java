package com.github.maxos.shitGames.koth;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.config.Cfg;
import com.github.maxos.shitGames.config.Fx;
import com.github.maxos.shitGames.config.Message;
import com.github.maxos.shitGames.config.Msg;
import com.github.maxos.shitGames.config.Sfx;
import com.github.maxos.shitGames.util.Effects;
import com.github.maxos.shitGames.util.Locations;
import com.github.maxos.shitGames.util.SoundSpec;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class KingManager {

	private final ShitGames plugin;
	private final File file;

	@Getter
	private Location hill;
	@Getter
	private UUID kingId;
	@Getter
	private String kingName;

	private final Set<UUID> onHill = new ObjectOpenHashSet<>();
	private final Object2LongOpenHashMap<UUID> contestCooldown = new Object2LongOpenHashMap<>();

	private UUID capturing;
	private int progressTicks;
	private long releasedAt;
	private BukkitTask task;
	private BossBar bar;

	public KingManager(ShitGames plugin) {
		this.plugin = plugin;
		this.file = new File(plugin.getDataFolder(), "king.yml");
		this.contestCooldown.defaultReturnValue(0L);
	}

	public void load() {
		if (!file.exists()) {
			return;
		}
		YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
		hill = Locations.deserialize(yaml.getString("hill"));
		String rawUuid = yaml.getString("king-uuid");
		if (rawUuid != null && !rawUuid.isEmpty()) {
			try {
				kingId = UUID.fromString(rawUuid);
				kingName = yaml.getString("king-name");
			} catch (IllegalArgumentException exception) {
				kingId = null;
				kingName = null;
			}
		}
	}

	public void save() {
		YamlConfiguration yaml = new YamlConfiguration();
		yaml.options().setHeader(List.of("Данные мини-игры 'Царь горы'."));
		yaml.set("hill", Locations.serialize(hill));
		yaml.set("king-uuid", kingId == null ? null : kingId.toString());
		yaml.set("king-name", kingName);
		try {
			yaml.save(file);
		} catch (IOException exception) {
			plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить king.yml", exception);
		}
	}

	private void saveAsync() {
		if (plugin.isEnabled()) {
			plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::save);
		} else {
			save();
		}
	}

	public String displayName() {
		Cfg cfg = plugin.cfg();
		if (kingName == null || kingName.isEmpty()) {
			return cfg.king().noKing();
		}
		return kingName;
	}

	public void setHill(Location location) {
		this.hill = location;
		reset();
		saveAsync();
		rescan();
	}

	public void clearHill() {
		this.hill = null;
		reset();
		saveAsync();
	}

	public void setKing(UUID uuid, String name) {
		this.kingId = uuid;
		this.kingName = name;
		reset();
		saveAsync();
		rescan();
	}

	public void clearKing() {
		this.kingId = null;
		this.kingName = null;
		reset();
		saveAsync();
		rescan();
	}

	public void shutdown() {
		stopTask();
		if (bar != null) {
			for (Player player : plugin.getServer().getOnlinePlayers()) {
				player.hideBossBar(bar);
			}
			bar = null;
		}
		onHill.clear();
	}

	private void reset() {
		stopTask();
		capturing = null;
		progressTicks = 0;
		releasedAt = 0L;
		onHill.clear();
	}

	public void rescan() {
		if (hill == null || !plugin.cfg().king().enabled()) {
			return;
		}
		onHill.clear();
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			if (standsOnHill(player.getLocation())) {
				onHill.add(player.getUniqueId());
			}
		}
		evaluate();
	}

	public boolean standsOnHill(Location location) {
		if (hill == null || location == null || location.getWorld() == null || hill.getWorld() == null) {
			return false;
		}
		return location.getWorld().getUID().equals(hill.getWorld().getUID())
				&& location.getBlockX() == hill.getBlockX()
				&& location.getBlockZ() == hill.getBlockZ()
				&& location.getBlockY() == hill.getBlockY() + 1;
	}

	public void handleMove(Player player, Location to) {
		if (hill == null || !plugin.cfg().king().enabled()) {
			return;
		}
		UUID uuid = player.getUniqueId();
		boolean standing = standsOnHill(to) && !player.isDead() && player.getGameMode() != org.bukkit.GameMode.SPECTATOR;
		boolean known = onHill.contains(uuid);
		if (standing == known) {
			return;
		}
		if (standing) {
			onHill.add(uuid);
		} else {
			onHill.remove(uuid);
		}
		evaluate();
	}

	public void handleQuit(Player player) {
		if (onHill.remove(player.getUniqueId())) {
			evaluate();
		}
		contestCooldown.removeLong(player.getUniqueId());
	}

	public void handleJoin(Player player) {
		if (kingId != null && kingId.equals(player.getUniqueId()) && !player.getName().equals(kingName)) {
			kingName = player.getName();
			saveAsync();
		}
	}

	private void evaluate() {
		if (hill == null || !plugin.cfg().king().enabled()) {
			stopCapture();
			return;
		}
		if (onHill.size() != 1) {
			stopCapture();
			if (onHill.size() > 1) {
				notifyContested();
			}
			return;
		}
		UUID uuid = onHill.iterator().next();
		if (uuid.equals(kingId)) {
			stopCapture();
			return;
		}
		Player player = plugin.getServer().getPlayer(uuid);
		if (player == null) {
			stopCapture();
			return;
		}
		Cfg.King settings = plugin.cfg().king();
		if (!uuid.equals(capturing)) {
			capturing = uuid;
			progressTicks = 0;
		} else if (releasedAt > 0L && settings.progressLossPerSecond() > 0) {
			long elapsed = (System.currentTimeMillis() - releasedAt) / 1000L;
			progressTicks = Math.max(0, progressTicks - (int) (elapsed * settings.progressLossPerSecond() * 20L));
		}
		if (settings.resetOnLeave()) {
			progressTicks = 0;
		}
		releasedAt = 0L;
		if (bar == null) {
			bar = BossBar.bossBar(Component.empty(), 0.0F, settings.barColor(), settings.barOverlay());
		}
		player.showBossBar(bar);
		updateBar(player);
		SoundSpec sound = plugin.cfg().sound(Sfx.KING_CAPTURE_START);
		if (sound != null && task == null) {
			sound.play(player);
		}
		if (task == null) {
			int period = settings.updateTicks();
			task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::progress, period, period);
		}
	}

	private void stopCapture() {
		stopTask();
		if (capturing != null) {
			Player player = plugin.getServer().getPlayer(capturing);
			if (player != null && bar != null) {
				player.hideBossBar(bar);
			}
			releasedAt = System.currentTimeMillis();
			if (plugin.cfg().king().resetOnLeave()) {
				progressTicks = 0;
				capturing = null;
			}
		}
	}

	private void stopTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	private void progress() {
		if (capturing == null || hill == null) {
			stopCapture();
			return;
		}
		Player player = plugin.getServer().getPlayer(capturing);
		if (player == null || !player.isOnline() || !standsOnHill(player.getLocation())) {
			UUID stale = capturing;
			stopCapture();
			if (onHill.remove(stale)) {
				evaluate();
			}
			return;
		}
		if (onHill.size() != 1) {
			stopCapture();
			return;
		}
		Cfg.King settings = plugin.cfg().king();
		progressTicks += settings.updateTicks();
		int required = settings.captureSeconds() * 20;
		if (progressTicks >= required) {
			crown(player);
			return;
		}
		updateBar(player);
		SoundSpec sound = plugin.cfg().sound(Sfx.KING_CAPTURE_TICK);
		if (sound != null) {
			sound.play(player);
		}
		Effects.show(player, hill.clone().add(0.5D, 1.2D, 0.5D), plugin.cfg().particle(Fx.KING_PROGRESS));
	}

	private void updateBar(Player player) {
		if (bar == null) {
			return;
		}
		Cfg.King settings = plugin.cfg().king();
		int required = Math.max(1, settings.captureSeconds() * 20);
		float value = Math.max(0.0F, Math.min(1.0F, (float) progressTicks / required));
		int percent = (int) (value * 100.0F);
		bar.progress(value);
		bar.name(settings.barTitle().render(
				"%percent%", String.valueOf(percent),
				"%player%", player.getName(),
				"%king%", displayName()
		));
	}

	private void crown(Player player) {
		stopTask();
		if (bar != null) {
			player.hideBossBar(bar);
		}
		UUID previous = kingId;
		kingId = player.getUniqueId();
		kingName = player.getName();
		capturing = null;
		progressTicks = 0;
		releasedAt = 0L;
		saveAsync();

		plugin.send(player, Msg.KING_CAPTURED);
		Message main = plugin.cfg().msg(Msg.TITLE_KING_MAIN);
		Message sub = plugin.cfg().msg(Msg.TITLE_KING_SUB);
		if (!main.isEmpty() || !sub.isEmpty()) {
			Cfg.Titles titles = plugin.cfg().titles();
			player.showTitle(Title.title(
					main.render("%player%", player.getName()),
					sub.render("%player%", player.getName()),
					Title.Times.times(
							Duration.ofMillis(titles.fadeIn() * 50L),
							Duration.ofMillis(titles.stay() * 50L),
							Duration.ofMillis(titles.fadeOut() * 50L)
					)
			));
		}
		SoundSpec sound = plugin.cfg().sound(Sfx.KING_CAPTURED);
		if (sound != null) {
			sound.play(player);
		}
		Effects.show(player, hill.clone().add(0.5D, 1.2D, 0.5D), plugin.cfg().particle(Fx.KING_CAPTURED));
		Effects.firework(List.of(player), hill.clone().add(0.5D, 1.5D, 0.5D));
		plugin.runRewards(plugin.cfg().king().rewardCommands(), player, "king", null);

		if (previous != null && !previous.equals(kingId)) {
			Player old = plugin.getServer().getPlayer(previous);
			if (old != null && old.isOnline()) {
				plugin.send(old, Msg.KING_LOST, "%player%", player.getName());
				SoundSpec lost = plugin.cfg().sound(Sfx.KING_LOST);
				if (lost != null) {
					lost.play(old);
				}
			}
		}
	}

	private void notifyContested() {
		Message message = plugin.cfg().msg(Msg.KING_CONTESTED);
		if (message.isEmpty()) {
			return;
		}
		long now = System.currentTimeMillis();
		long cooldown = plugin.cfg().king().contestCooldownSeconds() * 1000L;
		Component component = message.render("%count%", String.valueOf(onHill.size()));
		for (UUID uuid : onHill) {
			if (uuid.equals(kingId)) {
				continue;
			}
			if (now - contestCooldown.getLong(uuid) < cooldown) {
				continue;
			}
			Player player = plugin.getServer().getPlayer(uuid);
			if (player != null) {
				contestCooldown.put(uuid, now);
				player.sendActionBar(component);
			}
		}
	}
}
