package com.github.maxos.shitGames;

import com.github.maxos.shitGames.arena.ArenaManager;
import com.github.maxos.shitGames.arena.GameType;
import com.github.maxos.shitGames.command.AdminCommands;
import com.github.maxos.shitGames.command.GameCommands;
import com.github.maxos.shitGames.config.Cfg;
import com.github.maxos.shitGames.config.ConfigManager;
import com.github.maxos.shitGames.config.Message;
import com.github.maxos.shitGames.config.Msg;
import com.github.maxos.shitGames.config.Sfx;
import com.github.maxos.shitGames.game.GameManager;
import com.github.maxos.shitGames.game.GameSession;
import com.github.maxos.shitGames.hook.PapiExpansion;
import com.github.maxos.shitGames.koth.KingManager;
import com.github.maxos.shitGames.listener.ConnectionListener;
import com.github.maxos.shitGames.listener.MovementListener;
import com.github.maxos.shitGames.listener.ProtectionListener;
import com.github.maxos.shitGames.setup.SetupListener;
import com.github.maxos.shitGames.setup.SetupManager;
import com.github.maxos.shitGames.util.Keys;
import com.github.maxos.shitGames.util.SoundSpec;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.util.List;
import java.util.logging.Level;

public final class ShitGames extends JavaPlugin {

	public static final String ADMIN_PERMISSION = "shitgames.admin";
	public static final String PLAY_PERMISSION = "shitgames.play";

	private ConfigManager configManager;
	private ArenaManager arenaManager;
	private GameManager gameManager;
	private com.github.maxos.shitGames.vault.VaultManager vaultManager;
	private KingManager kingManager;
	private SetupManager setupManager;
	private Lamp<BukkitCommandActor> lamp;

	@Override
	public void onEnable() {
		dev.triumphteam.gui.TriumphGui.init(this);
		Keys.init(this);

		configManager = new ConfigManager(this);
		configManager.load();

		vaultManager = new com.github.maxos.shitGames.vault.VaultManager(this);
		vaultManager.loadAll();

		arenaManager = new ArenaManager(this);
		arenaManager.load();

		setupManager = new SetupManager();

		kingManager = new KingManager(this);
		kingManager.load();

		gameManager = new GameManager(this);
		gameManager.start();

		getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
		getServer().getPluginManager().registerEvents(new MovementListener(this), this);
		getServer().getPluginManager().registerEvents(new ConnectionListener(this), this);
		getServer().getPluginManager().registerEvents(new SetupListener(this), this);

		lamp = BukkitLamp.builder(this).build();
		lamp.register(new GameCommands(this));
		lamp.register(new AdminCommands(this));

		if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
			try {
				new PapiExpansion(this).register();
				getLogger().info("PlaceholderAPI подключён: %shitgames_king%");
			} catch (Throwable throwable) {
				getLogger().log(Level.WARNING, "Не удалось зарегистрировать плейсхолдеры", throwable);
			}
		}

		getServer().getScheduler().runTask(this, () -> {
			for (Player player : getServer().getOnlinePlayers()) {
				if (vaultManager.has(player.getUniqueId())) {
					vaultManager.restore(player);
					send(player, Msg.INVENTORY_RESTORED);
				}
			}
			kingManager.rescan();
		});
	}

	@Override
	public void onDisable() {
		if (gameManager != null) {
			gameManager.stop();
		}
		if (kingManager != null) {
			kingManager.shutdown();
			kingManager.save();
		}
		if (arenaManager != null) {
			arenaManager.save();
		}
		if (vaultManager != null) {
			for (Player player : getServer().getOnlinePlayers()) {
				if (vaultManager.has(player.getUniqueId())) {
					vaultManager.restore(player);
				}
			}
		}
	}

	public Cfg cfg() {
		return configManager.cfg();
	}

	public ArenaManager arenas() {
		return arenaManager;
	}

	public GameManager games() {
		return gameManager;
	}

	public com.github.maxos.shitGames.vault.VaultManager vaults() {
		return vaultManager;
	}

	public KingManager king() {
		return kingManager;
	}

	public SetupManager setup() {
		return setupManager;
	}

	public void reloadEverything() {
		configManager.load();
		boolean idle = true;
		for (GameSession session : gameManager.sessions()) {
			if (session.size() > 0) {
				idle = false;
				break;
			}
		}
		if (idle) {
			arenaManager.load();
		}
	}

	public void send(CommandSender target, Msg key, String... replacements) {
		Message message = cfg().msg(key);
		if (message.isEmpty()) {
			return;
		}
		target.sendMessage(message.render(replacements));
	}

	public void playError(CommandSender target) {
		if (target instanceof Player player) {
			SoundSpec spec = cfg().sound(Sfx.ERROR);
			if (spec != null) {
				spec.play(player);
			}
		}
	}

	public void playOk(CommandSender target) {
		if (target instanceof Player player) {
			SoundSpec spec = cfg().sound(Sfx.SETUP_OK);
			if (spec != null) {
				spec.play(player);
			}
		}
	}

	public void runRewards(List<String> commands, Player player, String arena, GameType type) {
		if (commands.isEmpty()) {
			return;
		}
		String game = type == null ? "" : cfg().gameNameRaw(type);
		for (String raw : commands) {
			if (raw == null || raw.isBlank()) {
				continue;
			}
			String command = raw
					.replace("%player%", player.getName())
					.replace("%uuid%", player.getUniqueId().toString())
					.replace("%arena%", arena == null ? "" : arena)
					.replace("%game%", game);
			try {
				getServer().dispatchCommand(getServer().getConsoleSender(), command);
			} catch (RuntimeException exception) {
				getLogger().log(Level.WARNING, "Ошибка выполнения награды: " + command, exception);
			}
		}
	}
}
