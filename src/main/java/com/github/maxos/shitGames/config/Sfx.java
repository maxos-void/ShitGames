package com.github.maxos.shitGames.config;

import lombok.Getter;

@Getter
public enum Sfx {

	ARENA_JOIN("arena-join"),
	ARENA_LEAVE("arena-leave"),
	VOTE_OPEN("vote-open"),
	VOTE_CLICK("vote-click"),
	COUNTDOWN_TICK("countdown-tick"),
	COUNTDOWN_FINAL("countdown-final"),
	GAME_START("game-start"),
	ELIMINATED("eliminated"),
	WIN("win"),
	BLOCK_BREAK("block-break"),
	TNT_FALL("tnt-fall"),
	DOUBLE_JUMP_GIVE("double-jump-give"),
	DOUBLE_JUMP_USE("double-jump-use"),
	ZONE_EXPIRED("zone-expired"),
	FORCED_BREAK("forced-break"),
	SETUP_SELECT("setup-select"),
	SETUP_OK("setup-ok"),
	ERROR("error"),
	KING_CAPTURE_START("king-capture-start"),
	KING_CAPTURE_TICK("king-capture-tick"),
	KING_CAPTURED("king-captured"),
	KING_LOST("king-lost");

	private final String path;

	Sfx(String path) {
		this.path = path;
	}
}
