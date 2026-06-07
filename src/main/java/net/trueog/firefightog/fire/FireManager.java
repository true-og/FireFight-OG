// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.fire;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

// Tracks FF-temporary fire / soul-fire cells and clears them after lifetime.
public class FireManager {

    private final Plugin plugin;
    private final long lifetimeTicks;
    private final Map<Location, Tracked> tracked = new HashMap<>();

    public FireManager(Plugin plugin, long lifetimeSeconds) {

        this.plugin = plugin;
        this.lifetimeTicks = lifetimeSeconds * 20L;

    }

    public void track(Block block, Material fire) {

        final Location key = block.getLocation();
        cancel(key);

        final BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(key),
                lifetimeTicks);

        tracked.put(key, new Tracked(fire, task));

    }

    public boolean isTrackedAs(Location location, Material fire) {

        final Tracked entry = tracked.get(location);
        return entry != null && entry.fire == fire;

    }

    public void untrack(Location location) {

        cancel(location);

    }

    public void shutdown() {

        tracked.values().forEach(entry -> entry.task.cancel());
        tracked.clear();

    }

    private void expire(Location key) {

        final Tracked entry = tracked.remove(key);
        if (entry == null) {

            return;

        }

        final Block block = key.getBlock();
        if (block.getType() == entry.fire) {

            block.setType(Material.AIR, false);

        }

    }

    private void cancel(Location key) {

        final Tracked entry = tracked.remove(key);
        if (entry != null) {

            entry.task.cancel();

        }

    }

    private record Tracked(Material fire, BukkitTask task) {
    }

}
