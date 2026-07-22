package com.github.maxos.shitGames.listener;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.Arena;
import com.github.maxos.shitGames.arena.ArenaState;
import com.github.maxos.shitGames.arena.GameType;
import com.github.maxos.shitGames.game.GameSession;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

@RequiredArgsConstructor
public final class MovementListener implements Listener {

	private final ShitGames plugin;

	@EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
	public void onMove(PlayerMoveEvent event) {
		Location from = event.getFrom();
		Location to = event.getTo();
		if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
				&& from.getBlockZ() == to.getBlockZ()) {
			return;
		}
		process(event.getPlayer(), to);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onTeleport(PlayerTeleportEvent event) {
		process(event.getPlayer(), event.getTo());
	}

	private void process(Player player, Location to) {
		GameSession session = plugin.games().sessionOf(player);
		if (session != null) {
			handleArena(session, player, to);
		}
		plugin.king().handleMove(player, to);
	}

	private void handleArena(GameSession session, Player player, Location to) {
		Arena arena = session.getArena();
		ArenaState state = session.getState();
		int x = to.getBlockX();
		int y = to.getBlockY();
		int z = to.getBlockZ();

		if (state == ArenaState.RUNNING) {
			if (to.getWorld() == null || !arena.getRegion().world().equals(to.getWorld().getName())) {
				session.eliminate(player);
				return;
			}
			if (arena.isDeadly(x, y, z)) {
				session.eliminate(player);
				return;
			}
			if (plugin.cfg().game().eliminateOutsideRegion()
					&& !arena.getRegion().containsHorizontally(x, z)) {
				session.eliminate(player);
				return;
			}
			if (session.getGameType() == GameType.TNT_RUN && !session.isGraced()) {
				handleTntRun(session, arena, x, y, z);
			}
			return;
		}

		if (state == ArenaState.WAITING || state == ArenaState.COUNTDOWN || state == ArenaState.PREPARING) {
			Location lobby = arena.getLobby();
			if (lobby == null || lobby.getWorld() == null || to.getWorld() == null) {
				return;
			}
			if (!lobby.getWorld().getUID().equals(to.getWorld().getUID())) {
				plugin.games().teleport(player, lobby);
				return;
			}
			double radius = plugin.cfg().general().lobbyRadius();
			if (to.distanceSquared(lobby) > radius * radius) {
				plugin.games().teleport(player, lobby);
			}
		}
	}

	private void handleTntRun(GameSession session, Arena arena, int x, int y, int z) {
		World world = arena.world();
		if (world == null) {
			return;
		}
		Material cover = plugin.cfg().tntRun().cover();
		long tick = plugin.getServer().getCurrentTick();
		if (world.getBlockAt(x, y, z).getType() == cover && arena.isLayerBlock(x, y - 1, z)) {
			session.scheduleFall(x, y, z, tick);
			return;
		}
		int below = y - 1;
		if (world.getBlockAt(x, below, z).getType() == cover && arena.isLayerBlock(x, below - 1, z)) {
			session.scheduleFall(x, below, z, tick);
		}
	}
}
