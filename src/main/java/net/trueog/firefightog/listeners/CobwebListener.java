// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.sk89q.worldguard.bukkit.event.block.BreakBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;

import net.trueog.firefightog.FireFightOG;
import net.trueog.firefightog.cobweb.CobwebManager;

public class CobwebListener implements Listener {

    private final CobwebManager cobwebs;

    public CobwebListener(CobwebManager cobwebs) {

        this.cobwebs = cobwebs;

    }

    // Pre-allow WG's synthetic PlaceBlockEvent for survival-mode cobweb placement.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onWgPlaceBlock(PlaceBlockEvent event) {

        if (event.getExplicitResult() == Result.DENY) {

            return;

        }

        if (event.getEffectiveMaterial() != Material.COBWEB) {

            return;

        }

        if (!(event.getCause().getRootCause() instanceof Player player)) {

            return;

        }

        if (player.getGameMode() == GameMode.CREATIVE) {

            return;

        }

        for (Block block : event.getBlocks()) {

            if (!FireFightOG.allows(block.getLocation(), FireFightOG.temporaryCobwebs())) {

                return;

            }

        }

        event.setAllowed(true);

    }

    // Pre-allow WG's synthetic BreakBlockEvent on FF-tracked cobwebs so survival
    // players can clear their own placements while builder cobwebs stay immutable.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onWgBreakBlock(BreakBlockEvent event) {

        if (event.getExplicitResult() == Result.DENY) {

            return;

        }

        if (!(event.getCause().getRootCause() instanceof Player)) {

            return;

        }

        for (Block block : event.getBlocks()) {

            if (block.getType() != Material.COBWEB) {

                return;

            }

            if (!FireFightOG.allows(block.getLocation(), FireFightOG.temporaryCobwebs())) {

                return;

            }

            if (!cobwebs.isTracked(block.getLocation())) {

                return;

            }

        }

        event.setAllowed(true);

    }

    // Schedule expiry for survival-placed cobwebs; creative placements stay
    // permanent.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {

        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {

            return;

        }

        final Block block = event.getBlock();
        if (block.getType() != Material.COBWEB) {

            return;

        }

        if (!FireFightOG.allows(block.getLocation(), FireFightOG.temporaryCobwebs())) {

            return;

        }

        cobwebs.track(block);

    }

    // Drop the timer when a tracked cobweb is broken naturally; vanilla drops
    // apply.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        if (event.getBlock().getType() != Material.COBWEB) {

            return;

        }

        cobwebs.untrack(event.getBlock().getLocation());

    }

}
