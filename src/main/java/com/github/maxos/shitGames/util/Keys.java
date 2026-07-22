package com.github.maxos.shitGames.util;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

public final class Keys {

	public static final String WAND = "wand";
	public static final String VOTE = "vote";
	public static final String DOUBLE_JUMP = "double_jump";
	public static final String TOOL = "tool";

	private static NamespacedKey marker;

	private Keys() {
	}

	public static void init(Plugin plugin) {
		marker = new NamespacedKey(plugin, "item_role");
	}

	public static ItemStack tag(ItemStack item, String role) {
		item.editMeta(meta -> meta.getPersistentDataContainer().set(marker, PersistentDataType.STRING, role));
		return item;
	}

	public static boolean is(ItemStack item, String role) {
		if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return false;
		}
		return role.equals(meta.getPersistentDataContainer().get(marker, PersistentDataType.STRING));
	}

	public static boolean isAny(ItemStack item) {
		if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		return meta != null && meta.getPersistentDataContainer().has(marker, PersistentDataType.STRING);
	}
}
