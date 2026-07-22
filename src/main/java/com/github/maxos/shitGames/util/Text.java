package com.github.maxos.shitGames.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class Text {

	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
			.character('&')
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

	private static final MiniMessage MINI = MiniMessage.miniMessage();

	private Text() {
	}

	public static Component colorize(String raw) {
		if (raw == null || raw.isEmpty()) {
			return Component.empty();
		}
		Component component = raw.indexOf('<') >= 0 ? MINI.deserialize(raw) : LEGACY.deserialize(raw);
		return component.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
	}

	public static String plain(Component component) {
		return PlainTextComponentSerializer.plainText().serialize(component);
	}

	public static String time(int totalSeconds) {
		if (totalSeconds < 0) {
			totalSeconds = 0;
		}
		int minutes = totalSeconds / 60;
		int seconds = totalSeconds % 60;
		StringBuilder builder = new StringBuilder(5);
		if (minutes < 10) {
			builder.append('0');
		}
		builder.append(minutes).append(':');
		if (seconds < 10) {
			builder.append('0');
		}
		return builder.append(seconds).toString();
	}
}
