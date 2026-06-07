// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.bukkit.event.block.BreakBlockEvent;
import com.sk89q.worldguard.protection.flags.StateFlag;

import net.trueog.firefightog.FireFightOG;
import net.trueog.firefightog.fire.FireManager;

public class FireExtinguishListener implements Listener {

    private final FireManager fires;
    private final Plugin plugin;

    public FireExtinguishListener(Plugin plugin, FireManager fires) {

        this.plugin = plugin;
        this.fires = fires;

    }

    // Track survival-mode flint/fireball ignitions as temporary FireFight fire.
    // BlockIgniteEvent fires before the block becomes FIRE; defer tracking by
    // one tick so we record the actual fire variant the world settled on.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {

        final Player player = event.getPlayer();
        if (player == null) {

            return;

        }

        if (player.getGameMode() == GameMode.CREATIVE) {

            return;

        }

        final IgniteCause cause = event.getCause();
        if (cause != IgniteCause.FLINT_AND_STEEL && cause != IgniteCause.FIREBALL) {

            return;

        }

        final StateFlag flag = FireFightOG.fireExtinguish();
        if (flag == null) {

            return;

        }

        final Block block = event.getBlock();
        if (!FireFightOG.allows(block.getLocation(), flag)) {

            return;

        }

        Bukkit.getScheduler().runTask(plugin, () -> {

            final Material type = block.getType();
            if (type == Material.FIRE || type == Material.SOUL_FIRE) {

                fires.track(block, type);

            }

        });

    }

    // Pre-allow WG's BreakBlockEvent for FF-tracked fire so the extinguish path is
    // not denied.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onWgBreakBlock(BreakBlockEvent event) {

        if (event.getExplicitResult() == Result.DENY) {

            return;

        }

        if (!(event.getCause().getRootCause() instanceof Player)) {

            return;

        }

        final StateFlag flag = FireFightOG.fireExtinguish();
        if (flag == null) {

            return;

        }

        for (Block block : event.getBlocks()) {

            final Material type = block.getType();
            if (type != Material.FIRE && type != Material.SOUL_FIRE) {

                return;

            }

            if (!FireFightOG.allows(block.getLocation(), flag)) {

                return;

            }

            if (!fires.isTrackedAs(block.getLocation(), type)) {

                return;

            }

        }

        event.setAllowed(true);

    }

    // Override useInteractedBlock when the fuel block click would extinguish
    // FF-tracked fire.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {

            return;

        }

        final Block clicked = event.getClickedBlock();
        if (clicked == null) {

            return;

        }

        final Block fire = clicked.getRelative(event.getBlockFace());

        final Material type = fire.getType();
        if (type != Material.FIRE && type != Material.SOUL_FIRE) {

            return;

        }

        final StateFlag flag = FireFightOG.fireExtinguish();
        if (!FireFightOG.allows(fire.getLocation(), flag)) {

            return;

        }

        if (!fires.isTrackedAs(fire.getLocation(), type)) {

            return;

        }

        event.setUseInteractedBlock(Result.ALLOW);
        fires.untrack(fire.getLocation());

    }

}
