// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.fluid;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

// Tracks FF-temporary water/lava (sources, derived flow, waterlogged solids)
// and clears them after lifetime; expiry cascades across connected tracked
// entries of the same fluid so adjacent sources can't refill each other.
public class FluidManager {

    private static final BlockFace[] CARDINALS = { BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST,
            BlockFace.UP, BlockFace.DOWN };

    private final Plugin plugin;
    private final long lifetimeTicks;
    private final Map<Location, Tracked> tracked = new HashMap<>();

    public FluidManager(Plugin plugin, long lifetimeSeconds) {

        this.plugin = plugin;
        this.lifetimeTicks = lifetimeSeconds * 20L;

    }

    public Plugin getPlugin() {

        return plugin;

    }

    public void track(Block block, Material fluid) {

        final Location key = block.getLocation();
        cancel(key);

        final BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> expire(key),
                lifetimeTicks);

        tracked.put(key, new Tracked(fluid, task));

    }

    public boolean isTrackedAs(Location location, Material fluid) {

        final Tracked entry = tracked.get(location);
        return entry != null && entry.fluid == fluid;

    }

    public void untrack(Location location) {

        cancel(location);

    }

    public void shutdown() {

        tracked.values().forEach(entry -> entry.task.cancel());
        tracked.clear();

    }

    // Cascade across same-fluid tracked neighbours so a ping-pong refill can't
    // survive expiry.
    private void expire(Location origin) {

        final Tracked originEntry = tracked.get(origin);
        if (originEntry == null) {

            return;

        }

        final Material fluid = originEntry.fluid;

        final Deque<Location> queue = new ArrayDeque<>();
        final Set<Location> visited = new HashSet<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty()) {

            final Location current = queue.poll();
            final Tracked entry = tracked.remove(current);
            if (entry == null || entry.fluid != fluid) {

                continue;

            }

            entry.task.cancel();
            clearBlock(current.getBlock(), fluid);

            for (BlockFace face : CARDINALS) {

                final Location neighbour = current.getBlock().getRelative(face).getLocation();
                if (visited.add(neighbour) && isTrackedAs(neighbour, fluid)) {

                    queue.add(neighbour);

                }

            }

        }

    }

    private void clearBlock(Block block, Material fluid) {

        final BlockData data = block.getBlockData();

        // Waterloggable host: drain only the water, keep the host block.
        if (data instanceof Waterlogged waterlogged && waterlogged.isWaterlogged() && fluid == Material.WATER) {

            waterlogged.setWaterlogged(false);
            block.setBlockData(waterlogged, false);
            return;

        }

        final Material now = block.getType();

        // Physics off so adjacent tracked sources don't refill before the cascade
        // reaches them.
        if (now == fluid || now == flowingCounterpart(fluid)) {

            block.setType(Material.AIR, false);

        }

    }

    private static Material flowingCounterpart(Material fluid) {

        try {

            if (fluid == Material.WATER) {

                return Material.valueOf("FLOWING_WATER");

            }

            if (fluid == Material.LAVA) {

                return Material.valueOf("FLOWING_LAVA");

            }

        } catch (IllegalArgumentException ignored) {

            // Modern Paper folds flowing variants into WATER / LAVA.

        }

        return null;

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
