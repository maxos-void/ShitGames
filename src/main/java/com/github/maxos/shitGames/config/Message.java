package com.github.maxos.shitGames.config;

import com.github.maxos.shitGames.util.Text;
import net.kyori.adventure.text.Component;

public record Message(String raw, Component cached) {

	public static final Message EMPTY = new Message("", null);

	public static Message of(String raw) {
		if (raw == null || raw.isEmpty()) {
			return EMPTY;
		}
		return new Message(raw, raw.indexOf('%') < 0 ? Text.colorize(raw) : null);
	}

	public boolean isEmpty() {
		return raw == null || raw.isEmpty();
	}

	public Component render() {
		return cached != null ? cached : Text.colorize(raw);
	}

	public Component render(String key, String value) {
		if (cached != null) {
			return cached;
		}
		return Text.colorize(raw.replace(key, value));
	}

	public Component render(String... replacements) {
		if (cached != null) {
			return cached;
		}
		String result = raw;
		for (int i = 0; i + 1 < replacements.length; i += 2) {
			result = result.replace(replacements[i], replacements[i + 1]);
		}
		return Text.colorize(result);
	}

	public String rendered(String... replacements) {
		String result = raw;
		for (int i = 0; i + 1 < replacements.length; i += 2) {
			result = result.replace(replacements[i], replacements[i + 1]);
		}
		return result;
	}
}
