// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.cobweb;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

// Tracks FF-temporary cobwebs and clears them after lifetime, dropping a
// piece of string at the block centre on expiry.
public class CobwebManager {

    private final Plugin plugin;
    private final long lifetimeTicks;
    private final Map<Location, BukkitTask> tracked = new HashMap<>();

    public CobwebManager(Plugin plugin, long lifetimeSeconds) {

        this.plugin = plugin;
        this.lifetimeTicks = lifetimeSeconds * 20L;

    }

    public void track(Block block) {

        final Location key = block.getLocation();
        cancel(key);

        final BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(key),
                lifetimeTicks);

        tracked.put(key, task);

    }

    public boolean isTracked(Location location) {

        return tracked.containsKey(location);

    }

    public void untrack(Location location) {

        cancel(location);

    }

    public void shutdown() {

        tracked.values().forEach(BukkitTask::cancel);
        tracked.clear();

    }

    private void expire(Location key) {

        final BukkitTask task = tracked.remove(key);
        if (task == null) {

            return;

        }

        final Block block = key.getBlock();
        if (block.getType() != Material.COBWEB) {

            return;

        }

        block.setType(Material.AIR, false);
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.STRING));

    }

    private void cancel(Location key) {

        final BukkitTask task = tracked.remove(key);
        if (task != null) {

            task.cancel();

        }

    }

}
