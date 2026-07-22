package com.github.maxos.shitGames.config;

import lombok.Getter;

@Getter
public enum Fx {

	TNT_FALL("tnt-fall"),
	BLOCK_BREAK("block-break"),
	GAME_START("game-start"),
	ELIMINATED("eliminated"),
	DOUBLE_JUMP_USE("double-jump-use"),
	FORCED_BREAK("forced-break"),
	KING_PROGRESS("king-progress"),
	KING_CAPTURED("king-captured"),
	SETUP_SELECT("setup-select");

	private final String path;

	Fx(String path) {
		this.path = path;
	}
}
