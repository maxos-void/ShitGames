package com.github.maxos.shitGames.arena;

import com.github.maxos.shitGames.config.Msg;
import lombok.Getter;

@Getter
public enum ArenaState {

	WAITING(Msg.STATE_WAITING, true),
	COUNTDOWN(Msg.STATE_COUNTDOWN, true),
	PREPARING(Msg.STATE_PREPARING, false),
	RUNNING(Msg.STATE_RUNNING, false),
	ENDING(Msg.STATE_ENDING, false),
	DISABLED(Msg.STATE_DISABLED, false);

	private final Msg displayName;
	private final boolean joinable;

	ArenaState(Msg displayName, boolean joinable) {
		this.displayName = displayName;
		this.joinable = joinable;
	}
}
