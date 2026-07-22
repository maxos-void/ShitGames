package com.github.maxos.shitGames.game;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.Arena;
import com.github.maxos.shitGames.arena.ArenaState;
import com.github.maxos.shitGames.arena.GameType;
import com.github.maxos.shitGames.config.Msg;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class GameManager {

	private final ShitGames plugin;
	private final Object2ObjectOpenHashMap<String, GameSession> sessions = new Object2ObjectOpenHashMap<>();
	private final Object2ObjectOpenHashMap<UUID, GameSession> byPlayer = new Object2ObjectOpenHashMap<>();
	private final Set<UUID> internalTeleports = new ObjectOpenHashSet<>();

	private BukkitTask secondTask;
	private BukkitTask fastTask;

	public GameManager(ShitGames plugin) {
		this.plugin = plugin;
	}

	public void start() {
		secondTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
	}

	public void stop() {
		if (secondTask != null) {
			secondTask.cancel();
			secondTask = null;
		}
		if (fastTask != null) {
			fastTask.cancel();
			fastTask = null;
		}
		for (GameSession session : new ArrayList<>(sessions.values())) {
			session.shutdown();
		}
		sessions.clear();
		byPlayer.clear();
	}

	public Collection<GameSession> sessions() {
		return sessions.values();
	}

	public GameSession session(Arena arena) {
		return sessions.computeIfAbsent(arena.getName(), key -> new GameSession(plugin, arena));
	}

	public GameSession sessionOf(UUID uuid) {
		return byPlayer.get(uuid);
	}

	public GameSession sessionOf(Player player) {
		return byPlayer.get(player.getUniqueId());
	}

	public boolean isPlaying(UUID uuid) {
		return byPlayer.containsKey(uuid);
	}

	public void bind(UUID uuid, GameSession session) {
		byPlayer.put(uuid, session);
	}

	public void unbind(UUID uuid) {
		byPlayer.remove(uuid);
	}

	public void dropSession(String arenaName) {
		GameSession session = sessions.remove(arenaName.toLowerCase(Locale.ROOT));
		if (session != null) {
			session.shutdown();
		}
	}

	public boolean isInternalTeleport(UUID uuid) {
		return internalTeleports.contains(uuid);
	}

	public void teleport(Player player, Location location) {
		if (location == null || location.getWorld() == null) {
			return;
		}
		UUID uuid = player.getUniqueId();
		internalTeleports.add(uuid);
		try {
			player.teleport(location);
			player.setFallDistance(0.0F);
		} finally {
			internalTeleports.remove(uuid);
		}
	}

	public void refreshFastTask() {
		if (fastTask != null) {
			return;
		}
		fastTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::fastTick, 1L, 1L);
	}

	private void fastTick() {
		long currentTick = plugin.getServer().getCurrentTick();
		boolean needed = false;
		for (GameSession session : sessions.values()) {
			if (session.isRunning() && session.getGameType() == GameType.TNT_RUN) {
				needed = true;
			}
			session.fastTick(currentTick);
			if (session.hasFallWork()) {
				needed = true;
			}
		}
		if (!needed && fastTask != null) {
			fastTask.cancel();
			fastTask = null;
		}
	}

	private void tick() {
		if (sessions.isEmpty()) {
			return;
		}
		for (GameSession session : new ArrayList<>(sessions.values())) {
			try {
				session.tick();
			} catch (RuntimeException exception) {
				plugin.getLogger().severe("Ошибка в цикле арены " + session.getArena().getName() + ": " + exception);
				session.forceStop();
			}
		}
	}

	public void join(Player player, Arena arena) {
		if (byPlayer.containsKey(player.getUniqueId())) {
			plugin.send(player, Msg.JOIN_ALREADY);
			plugin.playError(player);
			return;
		}
		if (!arena.isEnabled()) {
			plugin.send(player, Msg.JOIN_DISABLED, "%arena%", arena.getName());
			plugin.playError(player);
			return;
		}
		if (!arena.isReady()) {
			plugin.send(player, Msg.JOIN_NOT_READY, "%arena%", arena.getName(), "%missing%", arena.missingParts());
			plugin.playError(player);
			return;
		}
		GameSession session = session(arena);
		if (!session.getState().isJoinable()) {
			plugin.send(player, Msg.JOIN_RUNNING, "%arena%", arena.getName());
			plugin.playError(player);
			return;
		}
		if (session.size() >= session.maxPlayers()) {
			plugin.send(player, Msg.JOIN_FULL, "%arena%", arena.getName());
			plugin.playError(player);
			return;
		}
		session.join(player);
	}

	public void joinBest(Player player) {
		GameSession best = null;
		for (Arena arena : plugin.arenas().all()) {
			if (!arena.isEnabled() || !arena.isReady()) {
				continue;
			}
			GameSession session = session(arena);
			if (!session.getState().isJoinable() || session.size() >= session.maxPlayers()) {
				continue;
			}
			if (best == null || session.size() > best.size()) {
				best = session;
			}
		}
		if (best == null) {
			plugin.send(player, Msg.JOIN_NO_ARENA);
			plugin.playError(player);
			return;
		}
		join(player, best.getArena());
	}

	public void leave(Player player) {
		GameSession session = byPlayer.get(player.getUniqueId());
		if (session == null) {
			plugin.send(player, Msg.NOT_IN_GAME);
			plugin.playError(player);
			return;
		}
		if (session.getState() == ArenaState.RUNNING) {
			session.eliminate(player);
		} else {
			session.remove(player, LeaveReason.COMMAND);
		}
	}

	public List<GameSession> activeSessions() {
		List<GameSession> result = new ArrayList<>();
		for (GameSession session : sessions.values()) {
			if (session.size() > 0) {
				result.add(session);
			}
		}
		return result;
	}
}
