package com.github.maxos.shitGames.game;

public enum LeaveReason {

	COMMAND(true, true),
	QUIT(false, false),
	ELIMINATED(true, true),
	GAME_END(true, false),
	ADMIN(true, true);

	private final boolean restore;
	private final boolean announce;

	LeaveReason(boolean restore, boolean announce) {
		this.restore = restore;
		this.announce = announce;
	}

	public boolean restore() {
		return restore;
	}

	public boolean announce() {
		return announce;
	}
}
