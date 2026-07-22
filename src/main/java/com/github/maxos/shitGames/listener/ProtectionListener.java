package com.github.maxos.shitGames.listener;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.ArenaState;
import com.github.maxos.shitGames.config.Fx;
import com.github.maxos.shitGames.config.Message;
import com.github.maxos.shitGames.config.Msg;
import com.github.maxos.shitGames.config.Sfx;
import com.github.maxos.shitGames.game.GameSession;
import com.github.maxos.shitGames.gui.VoteMenu;
import com.github.maxos.shitGames.util.Effects;
import com.github.maxos.shitGames.util.Keys;
import com.github.maxos.shitGames.util.SoundSpec;
import dev.triumphteam.gui.guis.BaseGui;
import lombok.RequiredArgsConstructor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Locale;

@RequiredArgsConstructor
public final class ProtectionListener implements Listener {

	private final ShitGames plugin;

	private GameSession session(Player player) {
		return plugin.games().sessionOf(player);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		GameSession session = session(player);
		if (session != null) {
			if (session.allowBreak(event.getBlock())) {
				event.setDropItems(false);
				event.setExpToDrop(0);
				session.onBreak(event.getBlock());
				return;
			}
			event.setCancelled(true);
			notify(player, session.isGraced() ? Msg.GRACE_ACTIVE : Msg.ACTION_BLOCKED);
			return;
		}
		if (plugin.cfg().general().protectArenas()
				&& !player.hasPermission(ShitGames.ADMIN_PERMISSION)
				&& plugin.arenas().arenaAt(event.getBlock()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		if (session(player) != null) {
			event.setCancelled(true);
			deny(player);
			return;
		}
		if (plugin.cfg().general().protectArenas()
				&& !player.hasPermission(ShitGames.ADMIN_PERMISSION)
				&& plugin.arenas().arenaAt(event.getBlock()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onDrop(PlayerDropItemEvent event) {
		if (session(event.getPlayer()) != null) {
			event.setCancelled(true);
			deny(event.getPlayer());
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPickup(EntityPickupItemEvent event) {
		if (event.getEntity() instanceof Player player && session(player) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player && session(player) != null) {
			event.setCancelled(true);
			event.setResult(org.bukkit.event.Event.Result.DENY);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onDrag(InventoryDragEvent event) {
		if (event.getWhoClicked() instanceof Player player && session(player) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onOpen(InventoryOpenEvent event) {
		if (!(event.getPlayer() instanceof Player player) || session(player) == null) {
			return;
		}
		if (event.getInventory().getHolder() instanceof BaseGui) {
			return;
		}
		event.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onSwap(PlayerSwapHandItemsEvent event) {
		if (session(event.getPlayer()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onConsume(PlayerItemConsumeEvent event) {
		if (session(event.getPlayer()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onItemDamage(PlayerItemDamageEvent event) {
		if (session(event.getPlayer()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		if (session(event.getPlayer()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBucketFill(PlayerBucketFillEvent event) {
		if (session(event.getPlayer()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onVehicle(VehicleEnterEvent event) {
		if (event.getEntered() instanceof Player player && session(player) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onGameMode(PlayerGameModeChangeEvent event) {
		if (session(event.getPlayer()) != null && event.getNewGameMode() != GameMode.SURVIVAL) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onFlight(PlayerToggleFlightEvent event) {
		Player player = event.getPlayer();
		if (session(player) != null) {
			event.setCancelled(true);
			player.setAllowFlight(false);
			player.setFlying(false);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player player && session(player) != null) {
			event.setCancelled(true);
			player.setFireTicks(0);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onFood(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player player && session(player) != null) {
			event.setCancelled(true);
			player.setFoodLevel(20);
			player.setSaturation(20.0F);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		GameSession session = session(player);
		if (session == null) {
			return;
		}
		event.getDrops().clear();
		event.setKeepInventory(true);
		event.setKeepLevel(true);
		event.setDroppedExp(0);
		event.deathMessage(null);
		session.handleQuit(player);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onTeleport(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		GameSession session = session(player);
		if (session == null || !plugin.cfg().general().denyTeleport()) {
			return;
		}
		if (plugin.games().isInternalTeleport(player.getUniqueId())) {
			return;
		}
		event.setCancelled(true);
		plugin.send(player, Msg.TELEPORT_BLOCKED);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (session(player) == null || !plugin.cfg().general().blockCommands()) {
			return;
		}
		if (player.hasPermission(ShitGames.ADMIN_PERMISSION)) {
			return;
		}
		String raw = event.getMessage().toLowerCase(Locale.ROOT);
		int space = raw.indexOf(' ');
		String label = (space > 0 ? raw.substring(1, space) : raw.substring(1));
		int colon = label.indexOf(':');
		if (colon >= 0) {
			label = label.substring(colon + 1);
		}
		if (plugin.cfg().general().allowedCommands().contains(label)) {
			return;
		}
		event.setCancelled(true);
		plugin.send(player, Msg.COMMAND_BLOCKED);
		deny(player);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		GameSession session = session(player);
		if (session == null) {
			return;
		}
		ItemStack item = event.getItem();
		Action action = event.getAction();
		boolean rightClick = action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;

		if (Keys.is(item, Keys.VOTE)) {
			event.setCancelled(true);
			if (rightClick) {
				VoteMenu.open(plugin, player, session);
			}
			return;
		}
		if (Keys.is(item, Keys.DOUBLE_JUMP)) {
			event.setCancelled(true);
			if (rightClick) {
				useDoubleJump(player, session, event.getHand());
			}
			return;
		}
		if (action == Action.RIGHT_CLICK_BLOCK || action == Action.PHYSICAL) {
			event.setCancelled(true);
		}
	}

	private void useDoubleJump(Player player, GameSession session, EquipmentSlot hand) {
		if (session.getState() != ArenaState.RUNNING || !plugin.cfg().doubleJump().enabled()) {
			return;
		}
		var settings = plugin.cfg().doubleJump();
		Vector direction = player.getLocation().getDirection();
		direction.setY(0.0D);
		if (direction.lengthSquared() > 0.0D) {
			direction.normalize().multiply(settings.forwardBoost());
		}
		direction.setY(settings.power());
		player.setVelocity(direction);
		player.setFallDistance(0.0F);

		EquipmentSlot slot = hand == null ? EquipmentSlot.HAND : hand;
		ItemStack held = player.getInventory().getItem(slot);
		if (Keys.is(held, Keys.DOUBLE_JUMP)) {
			int amount = held.getAmount();
			if (amount <= 1) {
				player.getInventory().setItem(slot, null);
			} else {
				held.setAmount(amount - 1);
				player.getInventory().setItem(slot, held);
			}
		}
		plugin.send(player, Msg.DOUBLE_JUMP_USED);
		SoundSpec sound = plugin.cfg().sound(Sfx.DOUBLE_JUMP_USE);
		if (sound != null) {
			sound.playAll(session.viewers(), player.getLocation());
		}
		Effects.show(session.viewers(), player.getLocation(), plugin.cfg().particle(Fx.DOUBLE_JUMP_USE));
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		if (!(event.getEntity() instanceof FallingBlock falling)) {
			return;
		}
		if (plugin.arenas().arenaAt(event.getBlock()) == null) {
			return;
		}
		event.setCancelled(true);
		falling.remove();
	}

	@EventHandler(ignoreCancelled = true)
	public void onIgnite(BlockIgniteEvent event) {
		if (plugin.arenas().arenaAt(event.getBlock()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBurn(BlockBurnEvent event) {
		if (plugin.arenas().arenaAt(event.getBlock()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (plugin.arenas().arenaAt(event.getLocation()) != null) {
			event.setCancelled(true);
			event.blockList().clear();
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		if (plugin.arenas().arenaAt(event.getBlock()) != null) {
			event.setCancelled(true);
			event.blockList().clear();
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onFlow(BlockFromToEvent event) {
		if (plugin.arenas().arenaAt(event.getToBlock()) != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPhysics(BlockPhysicsEvent event) {
		Material cover = plugin.cfg().tntRun().cover();
		Material layer = plugin.cfg().spleef().layer();
		Material changed = event.getChangedType();
		Material current = event.getBlock().getType();
		if (changed != cover && current != cover && changed != layer && current != layer) {
			return;
		}
		if (plugin.arenas().arenaAt(event.getBlock()) != null) {
			event.setCancelled(true);
		}
	}

	private void deny(Player player) {
		notify(player, Msg.ACTION_BLOCKED);
	}

	private void notify(Player player, Msg key) {
		plugin.playError(player);
		Message message = plugin.cfg().msg(key);
		if (!message.isEmpty()) {
			player.sendActionBar(message.render());
		}
	}
}
