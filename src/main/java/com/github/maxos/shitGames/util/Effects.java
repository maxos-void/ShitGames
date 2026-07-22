package com.github.maxos.shitGames.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Collection;

public final class Effects {

	private Effects() {
	}

	public static void show(Collection<? extends Player> viewers, Location location, ParticleSpec spec) {
		if (spec == null || location == null || viewers.isEmpty()) {
			return;
		}
		spec.show(viewers, location);
	}

	public static void show(Player viewer, Location location, ParticleSpec spec) {
		if (spec == null || location == null || viewer == null) {
			return;
		}
		spec.show(viewer, location);
	}

	public static void firework(Collection<? extends Player> viewers, Location location) {
		if (location == null || viewers.isEmpty()) {
			return;
		}
		for (Player viewer : viewers) {
			viewer.spawnParticle(Particle.FIREWORK, location, 60, 0.6D, 0.6D, 0.6D, 0.25D);
			viewer.spawnParticle(Particle.FLASH, location, 2, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}
}
