package com.github.maxos.shitGames.gui;

import com.github.maxos.shitGames.ShitGames;
import com.github.maxos.shitGames.arena.GameType;
import com.github.maxos.shitGames.config.Cfg;
import com.github.maxos.shitGames.config.Sfx;
import com.github.maxos.shitGames.game.GameSession;
import com.github.maxos.shitGames.util.ItemSpec;
import com.github.maxos.shitGames.util.SoundSpec;
import com.github.maxos.shitGames.util.Text;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class VoteMenu {

	private VoteMenu() {
	}

	public static void open(ShitGames plugin, Player player, GameSession session) {
		Cfg cfg = plugin.cfg();
		Cfg.Vote settings = cfg.vote();

		Gui gui = Gui.gui()
				.title(settings.menuTitle().render())
				.rows(settings.rows())
				.disableAllInteractions()
				.create();

		if (settings.filler() != null) {
			gui.getFiller().fill(new GuiItem(settings.filler()));
		}

		add(plugin, gui, session, player, GameType.TNT_RUN, settings.tntRun(), settings);
		add(plugin, gui, session, player, GameType.SPLEEF, settings.spleef(), settings);

		gui.open(player);
		SoundSpec sound = cfg.sound(Sfx.VOTE_OPEN);
		if (sound != null) {
			sound.play(player);
		}
	}

	private static void add(ShitGames plugin, Gui gui, GameSession session, Player player,
	                        GameType type, ItemSpec spec, Cfg.Vote settings) {
		if (spec == null || !plugin.cfg().isEnabled(type)) {
			return;
		}
		int votes = session.voteCount(type);
		int total = Math.max(1, session.size());
		boolean chosen = session.voteOf(player.getUniqueId()) == type;

		ItemStack item = spec.build();
		if (!settings.voteLore().isEmpty()) {
			List<Component> lore = new ArrayList<>();
			ItemStack template = spec.template();
			if (template.hasItemMeta() && template.getItemMeta().hasLore()) {
				lore.addAll(template.getItemMeta().lore());
			}
			for (String line : settings.voteLore()) {
				lore.add(Text.colorize(line
						.replace("%votes%", String.valueOf(votes))
						.replace("%percent%", String.valueOf(votes * 100 / total))
						.replace("%chosen%", chosen ? "✔" : "✖")));
			}
			item.editMeta(meta -> meta.lore(lore));
		}

		gui.setItem(spec.slot(), new GuiItem(item, event -> {
			event.setCancelled(true);
			Player clicker = (Player) event.getWhoClicked();
			session.vote(clicker, type);
			plugin.getServer().getScheduler().runTask(plugin, (Runnable) clicker::closeInventory);
		}));
	}
}
