// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.fluid;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

// Tracks player-placed fluid sources and removes them after their lifetime expires.
public class FluidManager {

    private final Plugin plugin;
    private final long lifetimeTicks;
    private final Map<Location, Tracked> tracked = new HashMap<>();

    public FluidManager(Plugin plugin, long lifetimeSeconds) {

        this.plugin = plugin;
        this.lifetimeTicks = lifetimeSeconds * 20L;

    }

    // Record a placed fluid source and schedule its removal.
    public void track(Block block, Material fluid) {

        final Location key = block.getLocation();

        // Replace any existing timer for the fluid block.
        cancel(key);

        final BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> remove(key),
                lifetimeTicks);

        tracked.put(key, new Tracked(fluid, task));

    }

    // Stop tracking a source without removing it (e.g. picked up with a bucket).
    public void untrack(Location location) {

        cancel(location);

    }

    // Cancel all pending removals. Called on plugin disable.
    public void shutdown() {

        tracked.values().forEach(entry -> entry.task.cancel());
        tracked.clear();

    }

    private void remove(Location key) {

        final Tracked entry = tracked.remove(key);
        if (entry == null) {

            return;

        }

        final Block block = key.getBlock();

        // Only clear the block if it is still the fluid we placed.
        if (block.getType() == entry.fluid) {

            block.setType(Material.AIR, true);

        }

    }

    private void cancel(Location key) {

        final Tracked entry = tracked.remove(key);
        if (entry != null) {

            entry.task.cancel();

        }

    }

    private record Tracked(Material fluid, BukkitTask task) {
    }

}