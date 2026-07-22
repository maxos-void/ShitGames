package com.github.maxos.shitGames.command;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.Arena;
import com.github.maxos.shitGames.config.Msg;
import com.github.maxos.shitGames.game.GameSession;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;

@Command({"shitgames", "sg"})
@RequiredArgsConstructor
public final class GameCommands {

	private final ShitGames plugin;

	@Subcommand("join")
	@CommandPermission(ShitGames.PLAY_PERMISSION)
	public void join(Player sender, @Optional String arenaName) {
		if (arenaName == null || arenaName.isEmpty()) {
			plugin.games().joinBest(sender);
			return;
		}
		Arena arena = plugin.arenas().get(arenaName);
		if (arena == null) {
			plugin.send(sender, Msg.ARENA_NOT_FOUND, "%arena%", arenaName);
			plugin.playError(sender);
			return;
		}
		plugin.games().join(sender, arena);
	}

	@Subcommand("leave")
	@CommandPermission(ShitGames.PLAY_PERMISSION)
	public void leave(Player sender) {
		plugin.games().leave(sender);
	}

	@Subcommand("list")
	@CommandPermission(ShitGames.PLAY_PERMISSION)
	public void list(Player sender) {
		if (plugin.arenas().all().isEmpty()) {
			plugin.send(sender, Msg.ARENA_LIST_EMPTY);
			return;
		}
		plugin.send(sender, Msg.ARENA_LIST_HEADER, "%count%", String.valueOf(plugin.arenas().all().size()));
		for (Arena arena : plugin.arenas().all()) {
			GameSession session = plugin.games().session(arena);
			plugin.send(sender, Msg.ARENA_LIST_ENTRY,
					"%arena%", arena.getName(),
					"%state%", plugin.cfg().msg(session.getState().getDisplayName()).raw(),
					"%players%", String.valueOf(session.size()),
					"%min%", String.valueOf(session.minPlayers()),
					"%max%", String.valueOf(session.maxPlayers()),
					"%ready%", arena.isReady() ? "✔" : "✖");
		}
	}
}
