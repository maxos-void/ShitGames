package com.github.maxos.shitGames.config;

import lombok.Getter;

@Getter
public enum Msg {

	NO_PERMISSION("no-permission"),
	PLAYERS_ONLY("players-only"),
	RELOADED("reloaded"),
	INVALID_NUMBER("invalid-number"),
	WORLD_MISMATCH("world-mismatch"),
	SELECTION_TOO_BIG("selection-too-big"),

	ARENA_NOT_FOUND("arena-not-found"),
	ARENA_EXISTS("arena-exists"),
	ARENA_CREATED("arena-created"),
	ARENA_DELETED("arena-deleted"),
	ARENA_LIST_HEADER("arena-list-header"),
	ARENA_LIST_ENTRY("arena-list-entry"),
	ARENA_LIST_EMPTY("arena-list-empty"),
	ARENA_INFO("arena-info"),
	ARENA_NOT_READY("arena-not-ready"),
	ARENA_ENABLED("arena-enabled"),
	ARENA_DISABLED("arena-disabled"),
	ARENA_BUSY("arena-busy"),

	WAND_GIVEN("wand-given"),
	POS_FIRST("pos-first"),
	POS_SECOND("pos-second"),
	NEED_SELECTION("need-selection"),
	REGION_SET("region-set"),
	LAYER_ADDED("layer-added"),
	LAYER_REMOVED("layer-removed"),
	LAYERS_CLEARED("layers-cleared"),
	LAYER_NOT_FOUND("layer-not-found"),
	DEATHZONE_SET("deathzone-set"),
	LOBBY_SET("lobby-set"),
	SPAWN_MODE_ON("spawn-mode-on"),
	SPAWN_MODE_OFF("spawn-mode-off"),
	SPAWN_ADDED("spawn-added"),
	SPAWN_REMOVED("spawn-removed"),
	SPAWNS_CLEARED("spawns-cleared"),
	LIMITS_SET("limits-set"),

	JOIN_SUCCESS("join-success"),
	JOIN_BROADCAST("join-broadcast"),
	JOIN_ALREADY("join-already"),
	JOIN_RUNNING("join-running"),
	JOIN_FULL("join-full"),
	JOIN_DISABLED("join-disabled"),
	JOIN_NOT_READY("join-not-ready"),
	JOIN_NO_ARENA("join-no-arena"),
	LEAVE_SUCCESS("leave-success"),
	LEAVE_BROADCAST("leave-broadcast"),
	NOT_IN_GAME("not-in-game"),

	WAITING_FOR_PLAYERS("waiting-for-players"),
	COUNTDOWN("countdown"),
	COUNTDOWN_CANCELLED("countdown-cancelled"),
	PREPARING("preparing"),
	GAME_STARTED("game-started"),
	GAME_FORCE_STOPPED("game-force-stopped"),
	GAME_FORCE_STARTED("game-force-started"),
	GRACE_ACTIVE("grace-active"),

	VOTE_SET("vote-set"),
	VOTE_SAME("vote-same"),
	VOTE_CLOSED("vote-closed"),

	ELIMINATED_SELF("eliminated-self"),
	ELIMINATED_BROADCAST("eliminated-broadcast"),
	WINNER_SELF("winner-self"),
	WINNER_BROADCAST("winner-broadcast"),
	NO_WINNER("no-winner"),

	DOUBLE_JUMP_RECEIVED("double-jump-received"),
	DOUBLE_JUMP_USED("double-jump-used"),
	ZONE_EXPIRED("zone-expired"),

	ACTION_BLOCKED("action-blocked"),
	COMMAND_BLOCKED("command-blocked"),
	TELEPORT_BLOCKED("teleport-blocked"),
	INVENTORY_RESTORED("inventory-restored"),

	HILL_SET("hill-set"),
	HILL_CLEARED("hill-cleared"),
	HILL_NOT_SET("hill-not-set"),
	KING_CAPTURED("king-captured"),
	KING_LOST("king-lost"),
	KING_CONTESTED("king-contested"),
	KING_ADMIN_SET("king-admin-set"),
	KING_ADMIN_CLEARED("king-admin-cleared"),
	KING_INFO("king-info"),

	TITLE_START_MAIN("titles.start-main"),
	TITLE_START_SUB("titles.start-sub"),
	TITLE_COUNTDOWN_MAIN("titles.countdown-main"),
	TITLE_COUNTDOWN_SUB("titles.countdown-sub"),
	TITLE_ELIMINATED_MAIN("titles.eliminated-main"),
	TITLE_ELIMINATED_SUB("titles.eliminated-sub"),
	TITLE_WINNER_MAIN("titles.winner-main"),
	TITLE_WINNER_SUB("titles.winner-sub"),
	TITLE_KING_MAIN("titles.king-main"),
	TITLE_KING_SUB("titles.king-sub"),

	GAME_NAME_TNT_RUN("game-names.tnt-run"),
	GAME_NAME_SPLEEF("game-names.spleef"),

	STATE_WAITING("states.waiting"),
	STATE_COUNTDOWN("states.countdown"),
	STATE_PREPARING("states.preparing"),
	STATE_RUNNING("states.running"),
	STATE_ENDING("states.ending"),
	STATE_DISABLED("states.disabled");

	private final String path;

	Msg(String path) {
		this.path = path;
	}
}
