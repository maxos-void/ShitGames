package com.github.maxos.shitGames.command;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.Arena;
import com.github.maxos.shitGames.config.Msg;
import com.github.maxos.shitGames.game.GameSession;
import com.github.maxos.shitGames.util.Cuboid;
import com.github.maxos.shitGames.util.Keys;
import com.github.maxos.shitGames.util.Locations;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"shitgames", "sg"})
@CommandPermission(ShitGames.ADMIN_PERMISSION)
@RequiredArgsConstructor
public final class AdminCommands {

	private final ShitGames plugin;

	@Subcommand("reload")
	public void reload(CommandSender sender) {
		plugin.reloadEverything();
		plugin.send(sender, Msg.RELOADED);
	}

	@Subcommand("wand")
	public void wand(Player sender) {
		ItemStack wand = new ItemStack(Material.GOLDEN_AXE);
		wand.editMeta(meta -> {
			meta.displayName(com.github.maxos.shitGames.util.Text.colorize(
					plugin.getConfig().getString("setup.wand-name", "&#FFD166✦ &fКисть арены")));
			meta.lore(java.util.List.of(
					com.github.maxos.shitGames.util.Text.colorize("&#8A8A8A▎ &fЛКМ &7— первая точка"),
					com.github.maxos.shitGames.util.Text.colorize("&#8A8A8A▎ &fПКМ &7— вторая точка")
			));
			meta.setUnbreakable(true);
			meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES,
					org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE);
		});
		Keys.tag(wand, Keys.WAND);
		sender.getInventory().addItem(wand);
		plugin.send(sender, Msg.WAND_GIVEN);
		plugin.playOk(sender);
	}

	@Subcommand("create")
	public void create(Player sender, String name) {
		if (plugin.arenas().exists(name)) {
			plugin.send(sender, Msg.ARENA_EXISTS, "%arena%", name);
			plugin.playError(sender);
			return;
		}
		Cuboid selection = plugin.setup().selection(sender.getUniqueId());
		if (selection == null) {
			plugin.send(sender, Msg.NEED_SELECTION);
			plugin.playError(sender);
			return;
		}
		if (selection.volume() > plugin.cfg().general().maxSelectionVolume()) {
			plugin.send(sender, Msg.SELECTION_TOO_BIG, "%limit%",
					String.valueOf(plugin.cfg().general().maxSelectionVolume()));
			plugin.playError(sender);
			return;
		}
		Arena arena = plugin.arenas().create(name);
		arena.setRegion(selection);
		plugin.arenas().save();
		plugin.send(sender, Msg.ARENA_CREATED, "%arena%", arena.getName());
		plugin.playOk(sender);
	}

	@Subcommand("delete")
	public void delete(CommandSender sender, String name) {
		Arena arena = plugin.arenas().get(name);
		if (arena == null) {
			plugin.send(sender, Msg.ARENA_NOT_FOUND, "%arena%", name);
			plugin.playError(sender);
			return;
		}
		plugin.games().dropSession(arena.getName());
		plugin.arenas().delete(arena.getName());
		plugin.arenas().save();
		plugin.send(sender, Msg.ARENA_DELETED, "%arena%", arena.getName());
	}

	@Subcommand("region")
	public void region(Player sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		Cuboid selection = requireSelection(sender);
		if (selection == null) {
			return;
		}
		arena.setRegion(selection);
		plugin.arenas().save();
		plugin.send(sender, Msg.REGION_SET, "%arena%", arena.getName(),
				"%volume%", String.valueOf(selection.volume()));
		plugin.playOk(sender);
	}

	@Subcommand("layer add")
	public void layerAdd(Player sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		Cuboid selection = requireSelection(sender);
		if (selection == null) {
			return;
		}
		if (arena.getRegion() != null && !arena.getRegion().world().equals(selection.world())) {
			plugin.send(sender, Msg.WORLD_MISMATCH);
			plugin.playError(sender);
			return;
		}
		arena.getLayers().add(selection);
		plugin.arenas().save();
		plugin.send(sender, Msg.LAYER_ADDED, "%arena%", arena.getName(),
				"%count%", String.valueOf(arena.getLayers().size()),
				"%volume%", String.valueOf(selection.volume()));
		plugin.playOk(sender);
	}

	@Subcommand("layer remove")
	public void layerRemove(CommandSender sender, String name, int index) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		if (index < 1 || index > arena.getLayers().size()) {
			plugin.send(sender, Msg.LAYER_NOT_FOUND, "%index%", String.valueOf(index));
			return;
		}
		arena.getLayers().remove(index - 1);
		plugin.arenas().save();
		plugin.send(sender, Msg.LAYER_REMOVED, "%arena%", arena.getName(),
				"%count%", String.valueOf(arena.getLayers().size()));
	}

	@Subcommand("layer clear")
	public void layerClear(CommandSender sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		arena.getLayers().clear();
		plugin.arenas().save();
		plugin.send(sender, Msg.LAYERS_CLEARED, "%arena%", arena.getName());
	}

	@Subcommand("deathzone")
	public void deathZone(Player sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		Cuboid selection = requireSelection(sender);
		if (selection == null) {
			return;
		}
		arena.setDeathZone(selection);
		plugin.arenas().save();
		plugin.send(sender, Msg.DEATHZONE_SET, "%arena%", arena.getName());
		plugin.playOk(sender);
	}

	@Subcommand("lobby")
	public void lobby(Player sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		arena.setLobby(sender.getLocation().clone());
		plugin.arenas().save();
		plugin.send(sender, Msg.LOBBY_SET, "%arena%", arena.getName());
		plugin.playOk(sender);
	}

	@Subcommand("spawns")
	public void spawns(Player sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		String current = plugin.setup().paintArena(sender.getUniqueId());
		if (arena.getName().equals(current)) {
			plugin.setup().setPaintArena(sender.getUniqueId(), null);
			plugin.send(sender, Msg.SPAWN_MODE_OFF, "%arena%", arena.getName());
		} else {
			plugin.setup().setPaintArena(sender.getUniqueId(), arena.getName());
			plugin.send(sender, Msg.SPAWN_MODE_ON, "%arena%", arena.getName());
		}
		plugin.playOk(sender);
	}

	@Subcommand("spawn add")
	public void spawnAdd(Player sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		arena.getSpawns().add(sender.getLocation().clone());
		plugin.arenas().save();
		plugin.send(sender, Msg.SPAWN_ADDED, "%count%", String.valueOf(arena.getSpawns().size()),
				"%x%", String.valueOf(sender.getLocation().getBlockX()),
				"%y%", String.valueOf(sender.getLocation().getBlockY()),
				"%z%", String.valueOf(sender.getLocation().getBlockZ()));
		plugin.playOk(sender);
	}

	@Subcommand("spawn clear")
	public void spawnClear(CommandSender sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		arena.getSpawns().clear();
		plugin.arenas().save();
		plugin.send(sender, Msg.SPAWNS_CLEARED, "%arena%", arena.getName());
	}

	@Subcommand("toggle")
	public void toggle(CommandSender sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		arena.setEnabled(!arena.isEnabled());
		plugin.arenas().save();
		if (!arena.isEnabled()) {
			plugin.games().session(arena).forceStop();
		}
		plugin.send(sender, arena.isEnabled() ? Msg.ARENA_ENABLED : Msg.ARENA_DISABLED, "%arena%", arena.getName());
	}

	@Subcommand("limits")
	public void limits(CommandSender sender, String name, int min, int max) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		if (min < 2 || max < min) {
			plugin.send(sender, Msg.INVALID_NUMBER);
			return;
		}
		arena.setMinPlayers(min);
		arena.setMaxPlayers(max);
		plugin.arenas().save();
		plugin.send(sender, Msg.LIMITS_SET, "%arena%", arena.getName(),
				"%min%", String.valueOf(min), "%max%", String.valueOf(max));
	}

	@Subcommand("start")
	public void start(CommandSender sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		if (!arena.isReady()) {
			plugin.send(sender, Msg.ARENA_NOT_READY, "%arena%", arena.getName(), "%missing%", arena.missingParts());
			plugin.playError(sender);
			return;
		}
		GameSession session = plugin.games().session(arena);
		if (session.size() < 1) {
			plugin.send(sender, Msg.ARENA_BUSY, "%arena%", arena.getName());
			return;
		}
		session.forceStart();
		plugin.send(sender, Msg.GAME_FORCE_STARTED, "%arena%", arena.getName());
	}

	@Subcommand("stop")
	public void stop(CommandSender sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		plugin.games().session(arena).forceStop();
		plugin.send(sender, Msg.GAME_FORCE_STOPPED, "%arena%", arena.getName());
	}

	@Subcommand("info")
	public void info(CommandSender sender, String name) {
		Arena arena = requireArena(sender, name);
		if (arena == null) {
			return;
		}
		GameSession session = plugin.games().session(arena);
		plugin.send(sender, Msg.ARENA_INFO,
				"%arena%", arena.getName(),
				"%world%", arena.getRegion() == null ? "-" : arena.getRegion().world(),
				"%state%", plugin.cfg().msg(session.getState().getDisplayName()).raw(),
				"%enabled%", arena.isEnabled() ? "✔" : "✖",
				"%ready%", arena.isReady() ? "✔" : "✖",
				"%missing%", arena.missingParts().isEmpty() ? "-" : arena.missingParts(),
				"%players%", String.valueOf(session.size()),
				"%min%", String.valueOf(session.minPlayers()),
				"%max%", String.valueOf(session.maxPlayers()),
				"%layers%", String.valueOf(arena.getLayers().size()),
				"%spawns%", String.valueOf(arena.getSpawns().size()),
				"%region%", arena.getRegion() == null ? "-" : arena.getRegion().serialize(),
				"%deathzone%", arena.getDeathZone() == null ? "-" : arena.getDeathZone().serialize(),
				"%lobby%", arena.getLobby() == null ? "-" : Locations.serialize(arena.getLobby()));
	}

	@Subcommand("hill set")
	public void hillSet(Player sender) {
		Location block = sender.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();
		plugin.king().setHill(block);
		plugin.send(sender, Msg.HILL_SET,
				"%x%", String.valueOf(block.getBlockX()),
				"%y%", String.valueOf(block.getBlockY()),
				"%z%", String.valueOf(block.getBlockZ()),
				"%world%", block.getWorld().getName());
		plugin.playOk(sender);
	}

	@Subcommand("hill clear")
	public void hillClear(CommandSender sender) {
		plugin.king().clearHill();
		plugin.send(sender, Msg.HILL_CLEARED);
	}

	@Subcommand("king set")
	public void kingSet(CommandSender sender, Player target) {
		plugin.king().setKing(target.getUniqueId(), target.getName());
		plugin.send(sender, Msg.KING_ADMIN_SET, "%player%", target.getName());
	}

	@Subcommand("king clear")
	public void kingClear(CommandSender sender) {
		plugin.king().clearKing();
		plugin.send(sender, Msg.KING_ADMIN_CLEARED);
	}

	@Subcommand("king info")
	public void kingInfo(CommandSender sender) {
		Location hill = plugin.king().getHill();
		if (hill == null) {
			plugin.send(sender, Msg.HILL_NOT_SET);
		}
		plugin.send(sender, Msg.KING_INFO,
				"%king%", plugin.king().displayName(),
				"%hill%", hill == null ? "-" : Locations.serialize(hill));
	}

	private Arena requireArena(CommandSender sender, String name) {
		Arena arena = plugin.arenas().get(name);
		if (arena == null) {
			plugin.send(sender, Msg.ARENA_NOT_FOUND, "%arena%", name);
			if (sender instanceof Player player) {
				plugin.playError(player);
			}
		}
		return arena;
	}

	private Cuboid requireSelection(Player sender) {
		Cuboid selection = plugin.setup().selection(sender.getUniqueId());
		if (selection == null) {
			plugin.send(sender, Msg.NEED_SELECTION);
			plugin.playError(sender);
			return null;
		}
		if (selection.volume() > plugin.cfg().general().maxSelectionVolume()) {
			plugin.send(sender, Msg.SELECTION_TOO_BIG, "%limit%",
					String.valueOf(plugin.cfg().general().maxSelectionVolume()));
			plugin.playError(sender);
			return null;
		}
		return selection;
	}
}
