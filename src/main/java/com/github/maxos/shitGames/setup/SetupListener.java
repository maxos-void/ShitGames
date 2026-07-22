package com.github.maxos.shitGames.setup;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.Arena;
import com.github.maxos.shitGames.config.Fx;
import com.github.maxos.shitGames.config.Msg;
import com.github.maxos.shitGames.config.Sfx;
import com.github.maxos.shitGames.util.Effects;
import com.github.maxos.shitGames.util.Keys;
import com.github.maxos.shitGames.util.Locations;
import com.github.maxos.shitGames.util.SoundSpec;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Iterator;

@RequiredArgsConstructor
public final class SetupListener implements Listener {

	private final ShitGames plugin;

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBreak(BlockBreakEvent event) {
		if (Keys.is(event.getPlayer().getInventory().getItemInMainHand(), Keys.WAND)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (!Keys.is(event.getItem(), Keys.WAND)) {
			return;
		}
		event.setCancelled(true);
		if (!player.hasPermission(ShitGames.ADMIN_PERMISSION)) {
			return;
		}
		Block block = event.getClickedBlock();
		if (block == null) {
			return;
		}
		Action action = event.getAction();
		if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
			return;
		}

		String paintArena = plugin.setup().paintArena(player.getUniqueId());
		if (paintArena != null) {
			handlePaint(player, paintArena, block, action == Action.RIGHT_CLICK_BLOCK);
			return;
		}

		Location location = block.getLocation();
		if (action == Action.LEFT_CLICK_BLOCK) {
			plugin.setup().setFirst(player.getUniqueId(), location);
			plugin.send(player, Msg.POS_FIRST, "%x%", String.valueOf(block.getX()),
					"%y%", String.valueOf(block.getY()), "%z%", String.valueOf(block.getZ()));
		} else {
			plugin.setup().setSecond(player.getUniqueId(), location);
			plugin.send(player, Msg.POS_SECOND, "%x%", String.valueOf(block.getX()),
					"%y%", String.valueOf(block.getY()), "%z%", String.valueOf(block.getZ()));
		}
		feedback(player, location);
	}

	private void handlePaint(Player player, String arenaName, Block block, boolean add) {
		Arena arena = plugin.arenas().get(arenaName);
		if (arena == null) {
			plugin.setup().setPaintArena(player.getUniqueId(), null);
			plugin.send(player, Msg.ARENA_NOT_FOUND, "%arena%", arenaName);
			return;
		}
		Location spawn = Locations.blockCenter(block.getLocation().add(0.0D, 1.0D, 0.0D));
		spawn.setYaw(player.getLocation().getYaw());
		spawn.setPitch(0.0F);
		if (add) {
			for (Location existing : arena.getSpawns()) {
				if (existing.getBlockX() == spawn.getBlockX() && existing.getBlockY() == spawn.getBlockY()
						&& existing.getBlockZ() == spawn.getBlockZ()) {
					plugin.send(player, Msg.SPAWN_REMOVED, "%count%", String.valueOf(arena.getSpawns().size() - 1));
					arena.getSpawns().remove(existing);
					plugin.arenas().save();
					feedback(player, spawn);
					return;
				}
			}
			arena.getSpawns().add(spawn);
			plugin.send(player, Msg.SPAWN_ADDED, "%count%", String.valueOf(arena.getSpawns().size()),
					"%x%", String.valueOf(block.getX()), "%y%", String.valueOf(block.getY() + 1),
					"%z%", String.valueOf(block.getZ()));
		} else {
			Iterator<Location> iterator = arena.getSpawns().iterator();
			boolean removed = false;
			while (iterator.hasNext()) {
				Location existing = iterator.next();
				if (existing.getBlockX() == spawn.getBlockX() && existing.getBlockY() == spawn.getBlockY()
						&& existing.getBlockZ() == spawn.getBlockZ()) {
					iterator.remove();
					removed = true;
					break;
				}
			}
			if (!removed) {
				return;
			}
			plugin.send(player, Msg.SPAWN_REMOVED, "%count%", String.valueOf(arena.getSpawns().size()));
		}
		plugin.arenas().save();
		feedback(player, spawn);
	}

	private void feedback(Player player, Location location) {
		SoundSpec sound = plugin.cfg().sound(Sfx.SETUP_SELECT);
		if (sound != null) {
			sound.play(player);
		}
		Effects.show(player, location.clone().add(0.5D, 1.0D, 0.5D), plugin.cfg().particle(Fx.SETUP_SELECT));
	}
}
