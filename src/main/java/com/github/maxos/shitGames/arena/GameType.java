package com.github.maxos.shitGames.arena;

import com.github.maxos.shitGames.config.Msg;
import lombok.Getter;

import java.util.Locale;

@Getter
public enum GameType {

	TNT_RUN(Msg.GAME_NAME_TNT_RUN),
	SPLEEF(Msg.GAME_NAME_SPLEEF);

	private static final GameType[] VALUES = values();

	private final Msg displayName;

	GameType(Msg displayName) {
		this.displayName = displayName;
	}

	public static GameType[] cached() {
		return VALUES;
	}

	public static GameType parse(String raw) {
		if (raw == null) {
			return null;
		}
		String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
		for (GameType type : VALUES) {
			if (type.name().equals(normalized)) {
				return type;
			}
		}
		return null;
	}
}
