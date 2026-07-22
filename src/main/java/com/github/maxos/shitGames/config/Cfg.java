package com.github.maxos.shitGames.config;

import com.github.maxos.shitGames.arena.GameType;
import com.github.maxos.shitGames.util.ItemSpec;
import com.github.maxos.shitGames.util.ParticleSpec;
import com.github.maxos.shitGames.util.SoundSpec;
import it.unimi.dsi.fastutil.ints.IntSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Set;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
public final class Cfg {

	private final General general;
	private final Game game;
	private final TntRun tntRun;
	private final Spleef spleef;
	private final DoubleJump doubleJump;
	private final Vote vote;
	private final King king;
	private final Bars bars;
	private final Titles titles;
	private final Rewards rewards;
	private final EnumMap<Msg, Message> messages;
	private final EnumMap<Sfx, SoundSpec> sounds;
	private final EnumMap<Fx, ParticleSpec> particles;

	public Message msg(Msg key) {
		return messages.getOrDefault(key, Message.EMPTY);
	}

	public SoundSpec sound(Sfx key) {
		return sounds.get(key);
	}

	public ParticleSpec particle(Fx key) {
		return particles.get(key);
	}

	public Component gameName(GameType type) {
		return msg(type.getDisplayName()).render();
	}

	public String gameNameRaw(GameType type) {
		return msg(type.getDisplayName()).raw();
	}

	public boolean isEnabled(GameType type) {
		return type == GameType.TNT_RUN ? tntRun.enabled() : spleef.enabled();
	}

	public Material layerMaterial(GameType type) {
		return type == GameType.TNT_RUN ? tntRun.layer() : spleef.layer();
	}

	public record General(String prefix,
	                      Set<String> allowedCommands,
	                      boolean blockCommands,
	                      boolean denyTeleport,
	                      double lobbyRadius,
	                      long maxSelectionVolume,
	                      boolean protectArenas,
	                      boolean debug) {
	}

	public record Game(int minPlayers,
	                   int maxPlayers,
	                   int lobbyCountdown,
	                   int shortCountdown,
	                   IntSet announceAt,
	                   int graceSeconds,
	                   int endDelaySeconds,
	                   int zoneTimerSeconds,
	                   int resetBlocksPerTick,
	                   boolean eliminateOutsideRegion,
	                   boolean shrinkEnabled,
	                   int shrinkBlocksPerSecond,
	                   int shrinkAcceleration,
	                   int shrinkAccelerationInterval,
	                   int shrinkMaxPerSecond) {
	}

	public record TntRun(boolean enabled,
	                     Material layer,
	                     BlockData layerData,
	                     Material cover,
	                     BlockData coverData,
	                     int fallDelayTicks,
	                     boolean fallingEntity,
	                     int fallingLifetimeTicks) {
	}

	public record Spleef(boolean enabled,
	                     Material layer,
	                     BlockData layerData,
	                     ItemSpec tool) {
	}

	public record DoubleJump(boolean enabled,
	                         int intervalSeconds,
	                         double chance,
	                         int maxAmount,
	                         double power,
	                         double forwardBoost,
	                         ItemSpec item) {
	}

	public record Vote(ItemSpec item,
	                   Message menuTitle,
	                   int rows,
	                   ItemStack filler,
	                   ItemSpec tntRun,
	                   ItemSpec spleef,
	                   List<String> voteLore) {
	}

	public record King(boolean enabled,
	                   int captureSeconds,
	                   int updateTicks,
	                   boolean resetOnLeave,
	                   int progressLossPerSecond,
	                   Message barTitle,
	                   BossBar.Color barColor,
	                   BossBar.Overlay barOverlay,
	                   String noKing,
	                   int contestCooldownSeconds,
	                   List<String> rewardCommands) {
	}

	public record Bars(Message arenaTitle,
	                   Message expiredTitle,
	                   BossBar.Color color,
	                   BossBar.Color expiredColor,
	                   BossBar.Overlay overlay) {
	}

	public record Titles(int fadeIn,
	                     int stay,
	                     int fadeOut) {
	}

	public record Rewards(List<String> winnerCommands,
	                      List<String> participationCommands) {
	}
}
