package com.github.maxos.shitGames.util;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public record ItemSpec(ItemStack template, int slot) {

	public static ItemSpec read(ConfigurationSection section, Material fallback, int fallbackSlot, String role) {
		Material material = fallback;
		String name = null;
		List<String> lore = List.of();
		int amount = 1;
		int slot = fallbackSlot;
		boolean glow = false;
		boolean unbreakable = true;
		int customModelData = 0;
		int efficiency = 0;

		if (section != null) {
			Material parsed = Material.matchMaterial(section.getString("material", ""));
			if (parsed != null && parsed.isItem()) {
				material = parsed;
			}
			name = section.getString("name");
			lore = section.getStringList("lore");
			amount = Math.max(1, section.getInt("amount", 1));
			slot = section.getInt("slot", fallbackSlot);
			glow = section.getBoolean("glow", false);
			unbreakable = section.getBoolean("unbreakable", true);
			customModelData = section.getInt("custom-model-data", 0);
			efficiency = section.getInt("efficiency-level", 0);
		}

		ItemStack item = new ItemStack(material, amount);
		final String finalName = name;
		final List<String> finalLore = lore;
		final boolean finalGlow = glow;
		final boolean finalUnbreakable = unbreakable;
		final int finalModelData = customModelData;
		final int finalEfficiency = efficiency;

		item.editMeta(meta -> {
			if (finalName != null && !finalName.isEmpty()) {
				meta.displayName(Text.colorize(finalName));
			}
			if (!finalLore.isEmpty()) {
				List<net.kyori.adventure.text.Component> lines = new ArrayList<>(finalLore.size());
				for (String line : finalLore) {
					lines.add(Text.colorize(line));
				}
				meta.lore(lines);
			}
			meta.setUnbreakable(finalUnbreakable);
			if (finalModelData > 0) {
				meta.setCustomModelData(finalModelData);
			}
			if (finalEfficiency > 0) {
				meta.addEnchant(Enchantment.EFFICIENCY, finalEfficiency, true);
			}
			if (finalGlow && finalEfficiency <= 0) {
				meta.addEnchant(Enchantment.UNBREAKING, 1, true);
			}
			meta.addItemFlags(
					ItemFlag.HIDE_ENCHANTS,
					ItemFlag.HIDE_ATTRIBUTES,
					ItemFlag.HIDE_UNBREAKABLE,
					ItemFlag.HIDE_ADDITIONAL_TOOLTIP
			);
		});

		if (role != null) {
			Keys.tag(item, role);
		}
		return new ItemSpec(item, Math.max(0, Math.min(35, slot)));
	}

	public ItemStack build() {
		return template.clone();
	}
}
