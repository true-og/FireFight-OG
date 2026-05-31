// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

import net.trueog.firefightog.FireFightOG;
import net.trueog.firefightog.fluid.FluidManager;

// Lets players deploy water/lava in temporary-fluids regions without permanent terrain changes.
public class FluidListener implements Listener {

    private final FluidManager fluids;

    public FluidListener(FluidManager fluids) {

        this.fluids = fluids;

    }

    // HIGH runs after WorldGuard so we can re-allow placement inside
    // temporary-fluids regions.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {

        final Material fluid;
        if (event.getBucket() == Material.WATER_BUCKET) {

            fluid = Material.WATER;

        } else if (event.getBucket() == Material.LAVA_BUCKET) {

            fluid = Material.LAVA;

        } else {

            return;

        }

        final Block affected = affectedBlock(event);
        if (!FireFightOG.allows(affected.getLocation(), FireFightOG.temporaryFluids())) {

            return;

        }

        // Override WorldGuard's build-deny for non-members.
        event.setCancelled(false);

        // Auto-remove the source after its configured lifetime.
        fluids.track(affected, fluid);

    }

    // Allow picking fluids back up in temporary-fluids regions and cancel their
    // timer.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBucketFill(PlayerBucketFillEvent event) {

        final Block source = event.getBlockClicked();
        if (!FireFightOG.allows(source.getLocation(), FireFightOG.temporaryFluids())) {

            return;

        }

        event.setCancelled(false);
        fluids.untrack(source.getLocation());

    }

    // Stop water/lava from generating obsidian, cobblestone, stone or basalt.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {

        final Material type = event.getNewState().getType();
        if (type != Material.OBSIDIAN && type != Material.COBBLESTONE && type != Material.STONE
                && type != Material.BASALT)
        {

            return;

        }

        if (!FireFightOG.allows(event.getBlock().getLocation(), FireFightOG.temporaryFluids())) {

            return;

        }

        event.setCancelled(true);

    }

    // Mirror WorldGuard's calculation of which block a bucket fills.
    private Block affectedBlock(PlayerBucketEmptyEvent event) {

        final Block clicked = event.getBlockClicked();
        if (clicked.getBlockData() instanceof Waterlogged) {

            return clicked;

        }

        return clicked.getRelative(event.getBlockFace());

    }

}
