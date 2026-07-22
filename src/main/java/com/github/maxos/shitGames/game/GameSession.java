package com.github.maxos.shitGames.game;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.Arena;
import com.github.maxos.shitGames.arena.ArenaState;
import com.github.maxos.shitGames.arena.GameType;
import com.github.maxos.shitGames.config.Cfg;
import com.github.maxos.shitGames.config.Fx;
import com.github.maxos.shitGames.config.Message;
import com.github.maxos.shitGames.config.Msg;
import com.github.maxos.shitGames.config.Sfx;
import com.github.maxos.shitGames.util.Cuboid;
import com.github.maxos.shitGames.util.Effects;
import com.github.maxos.shitGames.util.ItemSpec;
import com.github.maxos.shitGames.util.Keys;
import com.github.maxos.shitGames.util.SoundSpec;
import com.github.maxos.shitGames.util.Text;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GameSession {

	private static final BlockData AIR = Material.AIR.createBlockData();

	private final ShitGames plugin;
	@Getter
	private final Arena arena;

	@Getter
	private ArenaState state = ArenaState.WAITING;
	@Getter
	private GameType gameType;

	private final LinkedHashSet<UUID> players = new LinkedHashSet<>();
	private final List<Player> online = new ObjectArrayList<>();
	private final Map<UUID, GameType> votes = new Object2ObjectOpenHashMap<>();

	private final LongArrayFIFOQueue fallPositions = new LongArrayFIFOQueue();
	private final LongArrayFIFOQueue fallDeadlines = new LongArrayFIFOQueue();
	private final LongOpenHashSet pendingFalls = new LongOpenHashSet();
	private final ArrayDeque<FallingBlock> spawnedBlocks = new ArrayDeque<>();
	private final LongArrayFIFOQueue spawnedDeadlines = new LongArrayFIFOQueue();

	private BossBar bossBar;
	private RegionFiller filler;

	private int countdown;
	private int timeLeft;
	private int graceLeft;
	private int endDelay;
	private int shrinkSeconds;
	private int doubleJumpTimer;
	private boolean expired;
	private boolean closing;
	private Player winner;

	public GameSession(ShitGames plugin, Arena arena) {
		this.plugin = plugin;
		this.arena = arena;
	}

	public static long pack(int x, int y, int z) {
		return (x & 0x3FFFFFFL) | ((z & 0x3FFFFFFL) << 26) | ((y & 0xFFFL) << 52);
	}

	private static int unpackX(long value) {
		return (int) (value << 38 >> 38);
	}

	private static int unpackZ(long value) {
		return (int) (value << 12 >> 38);
	}

	private static int unpackY(long value) {
		return (int) (value >> 52);
	}

	public List<Player> viewers() {
		return online;
	}

	public int size() {
		return players.size();
	}

	public boolean contains(UUID uuid) {
		return players.contains(uuid);
	}

	public boolean isRunning() {
		return state == ArenaState.RUNNING;
	}

	public boolean isGraced() {
		return graceLeft > 0;
	}

	public int minPlayers() {
		return arena.resolvedMin(cfg().game().minPlayers());
	}

	public int maxPlayers() {
		return arena.resolvedMax(cfg().game().maxPlayers());
	}

	private Cfg cfg() {
		return plugin.cfg();
	}

	private void refresh() {
		online.clear();
		for (UUID uuid : players) {
			Player player = plugin.getServer().getPlayer(uuid);
			if (player != null && player.isOnline()) {
				online.add(player);
			}
		}
	}

	public void broadcast(Msg key, String... replacements) {
		Message message = cfg().msg(key);
		if (message.isEmpty()) {
			return;
		}
		Component component = message.render(replacements);
		for (int i = 0, size = online.size(); i < size; i++) {
			online.get(i).sendMessage(component);
		}
	}

	public void sound(Sfx key) {
		SoundSpec spec = cfg().sound(key);
		if (spec != null) {
			spec.playAll(online);
		}
	}

	public void particles(Fx key, Location location) {
		Effects.show(online, location, cfg().particle(key));
	}

	private void title(Msg main, Msg sub, String... replacements) {
		Message mainMessage = cfg().msg(main);
		Message subMessage = cfg().msg(sub);
		if (mainMessage.isEmpty() && subMessage.isEmpty()) {
			return;
		}
		Title title = buildTitle(mainMessage.render(replacements), subMessage.render(replacements));
		for (int i = 0, size = online.size(); i < size; i++) {
			online.get(i).showTitle(title);
		}
	}

	private Title buildTitle(Component main, Component sub) {
		Cfg.Titles titles = cfg().titles();
		return Title.title(main, sub, Title.Times.times(
				Duration.ofMillis(titles.fadeIn() * 50L),
				Duration.ofMillis(titles.stay() * 50L),
				Duration.ofMillis(titles.fadeOut() * 50L)
		));
	}

	public boolean join(Player player) {
		UUID uuid = player.getUniqueId();
		if (!players.add(uuid)) {
			return false;
		}
		plugin.games().bind(uuid, this);
		plugin.vaults().capture(player);
		plugin.vaults().sanitize(player);
		plugin.games().teleport(player, arena.getLobby());
		giveLobbyItems(player);
		refresh();
		String name = player.getName();
		plugin.send(player, Msg.JOIN_SUCCESS, "%arena%", arena.getName(), "%players%",
				String.valueOf(players.size()), "%max%", String.valueOf(maxPlayers()));
		broadcastExcept(player, Msg.JOIN_BROADCAST, "%player%", name, "%arena%", arena.getName(),
				"%players%", String.valueOf(players.size()), "%max%", String.valueOf(maxPlayers()));
		sound(Sfx.ARENA_JOIN);
		return true;
	}

	private void broadcastExcept(Player except, Msg key, String... replacements) {
		Message message = cfg().msg(key);
		if (message.isEmpty()) {
			return;
		}
		Component component = message.render(replacements);
		for (int i = 0, size = online.size(); i < size; i++) {
			Player player = online.get(i);
			if (!player.equals(except)) {
				player.sendMessage(component);
			}
		}
	}

	private void giveLobbyItems(Player player) {
		ItemSpec spec = cfg().vote().item();
		if (spec != null) {
			player.getInventory().setItem(spec.slot(), spec.build());
			player.getInventory().setHeldItemSlot(Math.min(8, Math.max(0, spec.slot())));
		}
	}

	public void remove(Player player, LeaveReason reason) {
		UUID uuid = player.getUniqueId();
		if (!players.remove(uuid)) {
			return;
		}
		Location lastSpot = player.getLocation().clone();
		votes.remove(uuid);
		plugin.games().unbind(uuid);
		if (bossBar != null) {
			player.hideBossBar(bossBar);
		}
		refresh();

		if (reason.restore()) {
			plugin.vaults().restore(player);
		}

		switch (reason) {
			case ELIMINATED -> {
				particles(Fx.ELIMINATED, lastSpot);
				plugin.send(player, Msg.ELIMINATED_SELF, "%arena%", arena.getName());
				Message main = cfg().msg(Msg.TITLE_ELIMINATED_MAIN);
				Message sub = cfg().msg(Msg.TITLE_ELIMINATED_SUB);
				if (!main.isEmpty() || !sub.isEmpty()) {
					player.showTitle(buildTitle(main.render(), sub.render()));
				}
				SoundSpec spec = cfg().sound(Sfx.ELIMINATED);
				if (spec != null) {
					spec.play(player);
				}
				broadcast(Msg.ELIMINATED_BROADCAST, "%player%", player.getName(), "%left%",
						String.valueOf(players.size()));
			}
			case COMMAND -> {
				plugin.send(player, Msg.LEAVE_SUCCESS, "%arena%", arena.getName());
				broadcast(Msg.LEAVE_BROADCAST, "%player%", player.getName(), "%players%",
						String.valueOf(players.size()), "%max%", String.valueOf(maxPlayers()));
				sound(Sfx.ARENA_LEAVE);
			}
			case ADMIN -> plugin.send(player, Msg.GAME_FORCE_STOPPED, "%arena%", arena.getName());
			default -> {
			}
		}

		checkState();
	}

	public void handleQuit(Player player) {
		UUID uuid = player.getUniqueId();
		if (!players.remove(uuid)) {
			return;
		}
		votes.remove(uuid);
		plugin.games().unbind(uuid);
		if (bossBar != null) {
			player.hideBossBar(bossBar);
		}
		refresh();
		broadcast(Msg.ELIMINATED_BROADCAST, "%player%", player.getName(), "%left%", String.valueOf(players.size()));
		checkState();
	}

	private void checkState() {
		if (closing) {
			return;
		}
		switch (state) {
			case COUNTDOWN -> {
				if (players.size() < minPlayers()) {
					state = ArenaState.WAITING;
					countdown = 0;
					broadcast(Msg.COUNTDOWN_CANCELLED);
				}
			}
			case PREPARING, RUNNING -> {
				if (players.size() <= 1) {
					finish(players.isEmpty() ? null : online.isEmpty() ? null : online.get(0));
				}
			}
			case ENDING -> {
				if (players.isEmpty()) {
					closeRound();
				}
			}
			default -> {
			}
		}
	}

	public void tick() {
		switch (state) {
			case WAITING -> tickWaiting();
			case COUNTDOWN -> tickCountdown();
			case RUNNING -> tickRunning();
			case ENDING -> tickEnding();
			default -> {
			}
		}
	}

	private void tickWaiting() {
		if (players.isEmpty()) {
			return;
		}
		int min = minPlayers();
		if (players.size() >= min) {
			state = ArenaState.COUNTDOWN;
			countdown = cfg().game().lobbyCountdown();
			return;
		}
		Message message = cfg().msg(Msg.WAITING_FOR_PLAYERS);
		if (message.isEmpty()) {
			return;
		}
		Component component = message.render("%players%", String.valueOf(players.size()),
				"%min%", String.valueOf(min), "%need%", String.valueOf(min - players.size()));
		for (int i = 0, size = online.size(); i < size; i++) {
			online.get(i).sendActionBar(component);
		}
	}

	private void tickCountdown() {
		if (players.size() < minPlayers()) {
			state = ArenaState.WAITING;
			broadcast(Msg.COUNTDOWN_CANCELLED);
			return;
		}
		if (players.size() >= maxPlayers() && countdown > cfg().game().shortCountdown()) {
			countdown = cfg().game().shortCountdown();
		}
		if (cfg().game().announceAt().contains(countdown)) {
			broadcast(Msg.COUNTDOWN, "%time%", String.valueOf(countdown));
			if (countdown <= 5) {
				sound(Sfx.COUNTDOWN_FINAL);
				title(Msg.TITLE_COUNTDOWN_MAIN, Msg.TITLE_COUNTDOWN_SUB, "%time%", String.valueOf(countdown));
			} else {
				sound(Sfx.COUNTDOWN_TICK);
			}
		}
		countdown--;
		if (countdown <= 0) {
			prepare();
		}
	}

	private void tickRunning() {
		if (graceLeft > 0) {
			graceLeft--;
		}
		if (!expired) {
			timeLeft--;
			updateBossBar();
			if (timeLeft <= 0) {
				expired = true;
				updateBossBar();
				broadcast(Msg.ZONE_EXPIRED);
				sound(Sfx.ZONE_EXPIRED);
			}
		} else if (cfg().game().shrinkEnabled()) {
			shrinkSeconds++;
			Cfg.Game game = cfg().game();
			int amount = game.shrinkBlocksPerSecond()
					+ game.shrinkAcceleration() * (shrinkSeconds / game.shrinkAccelerationInterval());
			forceBreak(Math.min(amount, game.shrinkMaxPerSecond()));
		}

		if (cfg().doubleJump().enabled()) {
			doubleJumpTimer++;
			if (doubleJumpTimer >= cfg().doubleJump().intervalSeconds()) {
				doubleJumpTimer = 0;
				giveDoubleJumps();
			}
		}
		checkState();
	}

	private void tickEnding() {
		endDelay--;
		if (winner != null && winner.isOnline()) {
			Effects.firework(online, winner.getLocation().add(0.0D, 1.0D, 0.0D));
		}
		if (endDelay <= 0) {
			closeRound();
		}
	}

	private void updateBossBar() {
		if (bossBar == null) {
			return;
		}
		Cfg.Bars bars = cfg().bars();
		if (expired) {
			bossBar.name(bars.expiredTitle().render("%time%", "00:00"));
			bossBar.color(bars.expiredColor());
			bossBar.progress(1.0F);
			return;
		}
		int total = Math.max(1, cfg().game().zoneTimerSeconds());
		bossBar.name(bars.arenaTitle().render("%time%", Text.time(timeLeft)));
		bossBar.progress(Math.max(0.0F, Math.min(1.0F, (float) timeLeft / total)));
	}

	private void prepare() {
		state = ArenaState.PREPARING;
		gameType = tallyVotes();
		broadcast(Msg.PREPARING, "%game%", cfg().msg(gameType.getDisplayName()).raw());
		resetBlocks(this::begin);
	}

	private GameType tallyVotes() {
		int[] counts = new int[GameType.cached().length];
		for (Map.Entry<UUID, GameType> entry : votes.entrySet()) {
			if (players.contains(entry.getKey()) && cfg().isEnabled(entry.getValue())) {
				counts[entry.getValue().ordinal()]++;
			}
		}
		List<GameType> best = new ArrayList<>(2);
		int max = 0;
		for (GameType type : GameType.cached()) {
			if (!cfg().isEnabled(type)) {
				continue;
			}
			int value = counts[type.ordinal()];
			if (value > max) {
				max = value;
				best.clear();
				best.add(type);
			} else if (value == max) {
				best.add(type);
			}
		}
		if (best.isEmpty()) {
			return GameType.TNT_RUN;
		}
		return best.get(ThreadLocalRandom.current().nextInt(best.size()));
	}

	private void resetBlocks(Runnable onDone) {
		if (filler != null) {
			filler.cancel();
		}
		clearSpawnedBlocks();
		pendingFalls.clear();
		fallPositions.clear();
		fallDeadlines.clear();

		World world = arena.world();
		if (world == null || arena.getRegion() == null || arena.getLayers().isEmpty()) {
			if (onDone != null) {
				onDone.run();
			}
			return;
		}
		world.getNearbyEntities(arena.getRegion().box(), entity -> entity instanceof FallingBlock)
				.forEach(org.bukkit.entity.Entity::remove);

		GameType type = gameType == null ? GameType.TNT_RUN : gameType;
		BlockData layerData = type == GameType.TNT_RUN ? cfg().tntRun().layerData() : cfg().spleef().layerData();
		BlockData coverData = type == GameType.TNT_RUN ? cfg().tntRun().coverData() : null;

		filler = new RegionFiller(plugin, world, new ArrayList<>(arena.getLayers()), layerData, coverData,
				cfg().game().resetBlocksPerTick(), onDone);
		filler.start();
	}

	private void begin() {
		filler = null;
		if (state != ArenaState.PREPARING) {
			return;
		}
		if (players.isEmpty() || online.isEmpty()) {
			closeRound();
			return;
		}
		state = ArenaState.RUNNING;
		expired = false;
		shrinkSeconds = 0;
		doubleJumpTimer = 0;
		timeLeft = cfg().game().zoneTimerSeconds();
		graceLeft = cfg().game().graceSeconds();
		winner = null;

		List<Location> spawns = new ArrayList<>(arena.getSpawns());
		Collections.shuffle(spawns, ThreadLocalRandom.current());
		List<Player> shuffled = new ArrayList<>(online);
		Collections.shuffle(shuffled, ThreadLocalRandom.current());

		bossBar = BossBar.bossBar(Component.empty(), 1.0F, cfg().bars().color(), cfg().bars().overlay());

		for (int i = 0; i < shuffled.size(); i++) {
			Player player = shuffled.get(i);
			plugin.vaults().sanitize(player);
			plugin.games().teleport(player, spawns.get(i % spawns.size()));
			giveKit(player);
			player.showBossBar(bossBar);
		}
		updateBossBar();

		String name = cfg().msg(gameType.getDisplayName()).raw();
		broadcast(Msg.GAME_STARTED, "%game%", name);
		title(Msg.TITLE_START_MAIN, Msg.TITLE_START_SUB, "%game%", name);
		sound(Sfx.GAME_START);
		Location center = arena.getRegion().center();
		if (center != null) {
			particles(Fx.GAME_START, center);
		}
		plugin.games().refreshFastTask();
		if (cfg().general().debug()) {
			plugin.getLogger().info("Арена " + arena.getName() + ": старт " + gameType
					+ ", игроков " + players.size());
		}
	}

	private void giveKit(Player player) {
		if (gameType == GameType.SPLEEF) {
			ItemSpec tool = cfg().spleef().tool();
			if (tool != null) {
				player.getInventory().setItem(tool.slot(), tool.build());
				player.getInventory().setHeldItemSlot(Math.min(8, Math.max(0, tool.slot())));
			}
		}
	}

	private void giveDoubleJumps() {
		Cfg.DoubleJump settings = cfg().doubleJump();
		ItemSpec spec = settings.item();
		if (spec == null) {
			return;
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0, size = online.size(); i < size; i++) {
			if (random.nextDouble() > settings.chance()) {
				continue;
			}
			Player player = online.get(i);
			PlayerInventory inventory = player.getInventory();
			int slot = spec.slot();
			ItemStack current = inventory.getItem(slot);
			int amount = Keys.is(current, Keys.DOUBLE_JUMP) ? current.getAmount() : 0;
			if (amount >= settings.maxAmount()) {
				continue;
			}
			ItemStack item = spec.build();
			item.setAmount(amount + 1);
			inventory.setItem(slot, item);
			plugin.send(player, Msg.DOUBLE_JUMP_RECEIVED);
			SoundSpec sound = cfg().sound(Sfx.DOUBLE_JUMP_GIVE);
			if (sound != null) {
				sound.play(player);
			}
		}
	}

	public void scheduleFall(int x, int y, int z, long currentTick) {
		long packed = pack(x, y, z);
		if (!pendingFalls.add(packed)) {
			return;
		}
		fallPositions.enqueue(packed);
		fallDeadlines.enqueue(currentTick + cfg().tntRun().fallDelayTicks());
	}

	public void fastTick(long currentTick) {
		while (!fallDeadlines.isEmpty() && fallDeadlines.firstLong() <= currentTick) {
			fallDeadlines.dequeueLong();
			long packed = fallPositions.dequeueLong();
			pendingFalls.remove(packed);
			collapse(unpackX(packed), unpackY(packed), unpackZ(packed));
		}
		while (!spawnedDeadlines.isEmpty() && spawnedDeadlines.firstLong() <= currentTick) {
			spawnedDeadlines.dequeueLong();
			FallingBlock block = spawnedBlocks.pollFirst();
			if (block != null && block.isValid()) {
				block.remove();
			}
		}
	}

	public boolean hasFallWork() {
		return !fallDeadlines.isEmpty() || !spawnedDeadlines.isEmpty();
	}

	private void collapse(int x, int coverY, int z) {
		World world = arena.world();
		if (world == null || state != ArenaState.RUNNING) {
			return;
		}
		Block cover = world.getBlockAt(x, coverY, z);
		if (cover.getType() == cfg().tntRun().cover()) {
			cover.setBlockData(AIR, false);
		}
		Block layer = world.getBlockAt(x, coverY - 1, z);
		Location location = layer.getLocation().add(0.5D, 0.0D, 0.5D);
		if (layer.getType() == cfg().tntRun().layer()) {
			BlockData data = layer.getBlockData();
			layer.setBlockData(AIR, false);
			if (cfg().tntRun().fallingEntity() && spawnedBlocks.size() < 400) {
				FallingBlock entity = world.spawnFallingBlock(location, data);
				entity.setDropItem(false);
				entity.setCancelDrop(true);
				entity.setHurtEntities(false);
				entity.setPersistent(false);
				spawnedBlocks.addLast(entity);
				spawnedDeadlines.enqueue(plugin.getServer().getCurrentTick() + cfg().tntRun().fallingLifetimeTicks());
			}
		}
		particles(Fx.TNT_FALL, location);
		SoundSpec sound = cfg().sound(Sfx.TNT_FALL);
		if (sound != null) {
			sound.playAll(online, location);
		}
	}

	public boolean allowBreak(Block block) {
		if (state != ArenaState.RUNNING || gameType != GameType.SPLEEF || graceLeft > 0) {
			return false;
		}
		if (block.getType() != cfg().spleef().layer()) {
			return false;
		}
		return arena.isLayerBlock(block.getX(), block.getY(), block.getZ());
	}

	public void onBreak(Block block) {
		Location location = block.getLocation().add(0.5D, 0.5D, 0.5D);
		particles(Fx.BLOCK_BREAK, location);
		SoundSpec sound = cfg().sound(Sfx.BLOCK_BREAK);
		if (sound != null) {
			sound.playAll(online, location);
		}
	}

	private void forceBreak(int amount) {
		World world = arena.world();
		List<Cuboid> layers = arena.getLayers();
		if (world == null || layers.isEmpty()) {
			return;
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		Material layerMaterial = cfg().layerMaterial(gameType);
		Material cover = gameType == GameType.TNT_RUN ? cfg().tntRun().cover() : null;
		int played = 0;
		for (int i = 0; i < amount; i++) {
			Cuboid cuboid = layers.get(random.nextInt(layers.size()));
			int x = cuboid.minX() + random.nextInt(cuboid.lengthX());
			int y = cuboid.minY() + random.nextInt(cuboid.lengthY());
			int z = cuboid.minZ() + random.nextInt(cuboid.lengthZ());
			Block block = world.getBlockAt(x, y, z);
			if (block.getType() != layerMaterial) {
				continue;
			}
			block.setBlockData(AIR, false);
			if (cover != null && y == cuboid.maxY()) {
				Block above = world.getBlockAt(x, y + 1, z);
				if (above.getType() == cover) {
					above.setBlockData(AIR, false);
				}
			}
			Location location = block.getLocation().add(0.5D, 0.5D, 0.5D);
			particles(Fx.FORCED_BREAK, location);
			if (played < 3) {
				played++;
				SoundSpec sound = cfg().sound(Sfx.FORCED_BREAK);
				if (sound != null) {
					sound.playAll(online, location);
				}
			}
		}
	}

	public void eliminate(Player player) {
		remove(player, LeaveReason.ELIMINATED);
	}

	public void finish(Player champion) {
		if (state == ArenaState.ENDING) {
			return;
		}
		state = ArenaState.ENDING;
		endDelay = cfg().game().endDelaySeconds();
		winner = champion;
		if (bossBar != null) {
			for (int i = 0, size = online.size(); i < size; i++) {
				online.get(i).hideBossBar(bossBar);
			}
		}
		if (champion != null) {
			plugin.send(champion, Msg.WINNER_SELF, "%arena%", arena.getName());
			Message main = cfg().msg(Msg.TITLE_WINNER_MAIN);
			Message sub = cfg().msg(Msg.TITLE_WINNER_SUB);
			if (!main.isEmpty() || !sub.isEmpty()) {
				champion.showTitle(buildTitle(main.render("%player%", champion.getName()),
						sub.render("%player%", champion.getName())));
			}
			broadcastExcept(champion, Msg.WINNER_BROADCAST, "%player%", champion.getName(),
					"%arena%", arena.getName());
			sound(Sfx.WIN);
			Effects.firework(online, champion.getLocation().add(0.0D, 1.0D, 0.0D));
			plugin.runRewards(cfg().rewards().winnerCommands(), champion, arena.getName(), gameType);
		} else {
			broadcast(Msg.NO_WINNER);
		}
		for (int i = 0, size = online.size(); i < size; i++) {
			plugin.runRewards(cfg().rewards().participationCommands(), online.get(i), arena.getName(), gameType);
		}
	}

	private void closeRound() {
		if (closing) {
			return;
		}
		closing = true;
		state = ArenaState.ENDING;
		List<Player> copy = new ArrayList<>(online);
		for (Player player : copy) {
			remove(player, LeaveReason.GAME_END);
		}
		players.clear();
		votes.clear();
		online.clear();
		winner = null;
		bossBar = null;
		clearSpawnedBlocks();
		pendingFalls.clear();
		fallPositions.clear();
		fallDeadlines.clear();
		state = ArenaState.WAITING;
		countdown = 0;
		expired = false;
		closing = false;
		resetBlocks(null);
		gameType = null;
	}

	public void shutdown() {
		if (filler != null) {
			filler.cancel();
			filler = null;
		}
		clearSpawnedBlocks();
		List<Player> copy = new ArrayList<>(online);
		for (Player player : copy) {
			if (bossBar != null) {
				player.hideBossBar(bossBar);
			}
			players.remove(player.getUniqueId());
			plugin.games().unbind(player.getUniqueId());
			plugin.vaults().restore(player);
		}
		players.clear();
		online.clear();
		votes.clear();
		state = ArenaState.WAITING;
	}

	public void forceStop() {
		if (state == ArenaState.WAITING) {
			return;
		}
		finish(null);
		closeRound();
	}

	public void forceStart() {
		if (state == ArenaState.WAITING || state == ArenaState.COUNTDOWN) {
			countdown = 0;
			prepare();
		}
	}

	private void clearSpawnedBlocks() {
		for (FallingBlock block : spawnedBlocks) {
			if (block.isValid()) {
				block.remove();
			}
		}
		spawnedBlocks.clear();
		spawnedDeadlines.clear();
	}

	public void vote(Player player, GameType type) {
		if (state != ArenaState.WAITING && state != ArenaState.COUNTDOWN) {
			plugin.send(player, Msg.VOTE_CLOSED);
			return;
		}
		if (!players.contains(player.getUniqueId())) {
			return;
		}
		if (!cfg().isEnabled(type)) {
			plugin.send(player, Msg.VOTE_CLOSED);
			return;
		}
		GameType previous = votes.get(player.getUniqueId());
		if (previous == type) {
			plugin.send(player, Msg.VOTE_SAME, "%game%", cfg().msg(type.getDisplayName()).raw());
			return;
		}
		votes.put(player.getUniqueId(), type);
		plugin.send(player, Msg.VOTE_SET, "%game%", cfg().msg(type.getDisplayName()).raw());
		SoundSpec sound = cfg().sound(Sfx.VOTE_CLICK);
		if (sound != null) {
			sound.play(player);
		}
	}

	public int voteCount(GameType type) {
		int count = 0;
		for (Map.Entry<UUID, GameType> entry : votes.entrySet()) {
			if (entry.getValue() == type && players.contains(entry.getKey())) {
				count++;
			}
		}
		return count;
	}

	public GameType voteOf(UUID uuid) {
		return votes.get(uuid);
	}
}
