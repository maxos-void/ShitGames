package com.github.maxos.shitGames.listener;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.config.Msg;
import com.github.maxos.shitGames.game.GameSession;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@RequiredArgsConstructor
public final class ConnectionListener implements Listener {

	private final ShitGames plugin;

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		plugin.king().handleJoin(player);
		if (plugin.vaults().has(player.getUniqueId())) {
			plugin.getServer().getScheduler().runTask(plugin, () -> {
				if (player.isOnline() && plugin.vaults().restore(player)) {
					plugin.send(player, Msg.INVENTORY_RESTORED);
				}
			});
		}
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			if (player.isOnline()) {
				plugin.king().handleMove(player, player.getLocation());
			}
		});
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		GameSession session = plugin.games().sessionOf(player);
		if (session != null) {
			session.handleQuit(player);
		}
		plugin.king().handleQuit(player);
		plugin.setup().clear(player.getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		Location pending = plugin.vaults().pendingLocation(player.getUniqueId());
		if (pending == null || pending.getWorld() == null) {
			return;
		}
		event.setRespawnLocation(pending);
		plugin.getServer().getScheduler().runTask(plugin, () -> {
			if (player.isOnline()) {
				plugin.vaults().restore(player);
			}
		});
	}
}
