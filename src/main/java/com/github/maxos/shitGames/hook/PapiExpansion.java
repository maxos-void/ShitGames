package com.github.maxos.shitGames.hook;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.Arena;
import com.github.maxos.shitGames.game.GameSession;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@RequiredArgsConstructor
public final class PapiExpansion extends PlaceholderExpansion {

	private final ShitGames plugin;

	@Override
	public @NotNull String getIdentifier() {
		return "shitgames";
	}

	@Override
	public @NotNull String getAuthor() {
		return "Maxos";
	}

	@Override
	public @NotNull String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onRequest(OfflinePlayer player, @NotNull String params) {
		String key = params.toLowerCase(Locale.ROOT);
		switch (key) {
			case "king" -> {
				return plugin.king().displayName();
			}
			case "king_uuid" -> {
				return plugin.king().getKingId() == null ? "" : plugin.king().getKingId().toString();
			}
			case "arenas" -> {
				return String.valueOf(plugin.arenas().all().size());
			}
			case "playing" -> {
				int total = 0;
				for (GameSession session : plugin.games().sessions()) {
					total += session.size();
				}
				return String.valueOf(total);
			}
			default -> {
			}
		}
		if (key.startsWith("arena_")) {
			int last = key.lastIndexOf('_');
			if (last <= 6) {
				return null;
			}
			String name = key.substring(6, last);
			String field = key.substring(last + 1);
			Arena arena = plugin.arenas().get(name);
			if (arena == null) {
				return null;
			}
			GameSession session = plugin.games().session(arena);
			return switch (field) {
				case "players" -> String.valueOf(session.size());
				case "max" -> String.valueOf(session.maxPlayers());
				case "min" -> String.valueOf(session.minPlayers());
				case "state" -> plugin.cfg().msg(session.getState().getDisplayName()).raw();
				case "game" -> session.getGameType() == null ? "" : plugin.cfg().gameNameRaw(session.getGameType());
				default -> null;
			};
		}
		return null;
	}
}
