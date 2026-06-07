// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.listeners;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.sk89q.worldguard.bukkit.event.block.BreakBlockEvent;
import com.sk89q.worldguard.bukkit.event.block.PlaceBlockEvent;

import net.trueog.firefightog.FireFightOG;
import net.trueog.firefightog.fluid.FluidManager;

public class FluidListener implements Listener {

    private final FluidManager fluids;

    public FluidListener(FluidManager fluids) {

        this.fluids = fluids;

    }

    // Pre-allow WG's synthetic PlaceBlockEvent for player bucket-empty and for
    // tracked liquid flow before WG's NORMAL handler can deny it.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onWgPlaceBlock(PlaceBlockEvent event) {

        if (event.getExplicitResult() == Result.DENY) {

            return;

        }

        final Object root = event.getCause().getRootCause();

        if (root instanceof Player) {

            handlePlayerBucketEmpty(event);
            return;

        }

        if (root instanceof Block sourceBlock) {

            handleLiquidFlow(event, sourceBlock);
            return;

        }

    }

    // Pre-allow water/lava bucket placement on both the PlayerInteractEvent
    // and PlayerBucketEmptyEvent paths so the interact never cancels first.
    private void handlePlayerBucketEmpty(PlaceBlockEvent event) {

        final Material effective = event.getEffectiveMaterial();
        if (effective != Material.WATER && effective != Material.LAVA) {

            return;

        }

        final Event original = event.getOriginalEvent();
        final Player player;

        if (original instanceof PlayerBucketEmptyEvent emptyEvent) {

            final Material bucket = emptyEvent.getBucket();
            if (bucket != Material.WATER_BUCKET && bucket != Material.LAVA_BUCKET) {

                return;

            }

            player = emptyEvent.getPlayer();

        } else if (original instanceof PlayerInteractEvent interactEvent) {

            final ItemStack item = interactEvent.getItem();
            if (item == null || (item.getType() != Material.WATER_BUCKET && item.getType() != Material.LAVA_BUCKET)) {

                return;

            }

            player = interactEvent.getPlayer();

        } else {

            return;

        }

        // Creative-mode placements are builder/world fluid; leave them alone.
        if (player.getGameMode() == GameMode.CREATIVE) {

            return;

        }

        for (Block block : event.getBlocks()) {

            if (!FireFightOG.allows(block.getLocation(), FireFightOG.temporaryFluids())) {

                return;

            }

        }

        event.setAllowed(true);

    }

    // Allow flow only from FF-tracked sources so natural pools cannot leak in.
    private void handleLiquidFlow(PlaceBlockEvent event, Block sourceBlock) {

        final Material sourceType = sourceBlock.getType();
        if (sourceType != Material.WATER && sourceType != Material.LAVA) {

            return;

        }

        final Material effective = event.getEffectiveMaterial();
        if (effective != Material.WATER && effective != Material.LAVA) {

            return;

        }

        if (!fluids.isTrackedAs(sourceBlock.getLocation(), sourceType)) {

            return;

        }

        for (Block block : event.getBlocks()) {

            if (!FireFightOG.allows(block.getLocation(), FireFightOG.temporaryFluids())) {

                return;

            }

        }

        event.setAllowed(true);

    }

    // Bucket-fill on FF-tracked source; resolves the real fluid block from
    // the original event since WG's blocks list points at the adjacent air.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onWgBreakBlock(BreakBlockEvent event) {

        if (event.getExplicitResult() == Result.DENY) {

            return;

        }

        if (!(event.getCause().getRootCause() instanceof Player)) {

            return;

        }

        final Event original = event.getOriginalEvent();
        if (!(original instanceof PlayerBucketFillEvent fillEvent)) {

            return;

        }

        final Block source = fillEvent.getBlock();
        final Material type = source.getType();
        if (type != Material.WATER && type != Material.LAVA) {

            return;

        }

        if (!FireFightOG.allows(source.getLocation(), FireFightOG.temporaryFluids())) {

            return;

        }

        if (!fluids.isTrackedAs(source.getLocation(), type)) {

            return;

        }

        event.setAllowed(true);

    }

    // Schedule removal of survival-placed sources; creative placements stay
    // permanent.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {

        if (event.getPlayer().getGameMode() == GameMode.CREATIVE) {

            return;

        }

        final Material fluid;
        if (event.getBucket() == Material.WATER_BUCKET) {

            fluid = Material.WATER;

        } else if (event.getBucket() == Material.LAVA_BUCKET) {

            fluid = Material.LAVA;

        } else {

            return;

        }

        final Block affected = event.getBlock();
        if (!FireFightOG.allows(affected.getLocation(), FireFightOG.temporaryFluids())) {

            return;

        }

        fluids.track(affected, fluid);

    }

    // Cancel the removal timer when the player picks the source back up.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {

        final Block source = event.getBlock();
        if (!FireFightOG.allows(source.getLocation(), FireFightOG.temporaryFluids())) {

            return;

        }

        fluids.untrack(source.getLocation());

    }

    // Propagate tracking along flow so derived flow cells / merged sources
    // also expire; the post-tick check rejects phantom destinations.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {

        final Block from = event.getBlock();
        final Material fromType = from.getType();
        if (fromType != Material.WATER && fromType != Material.LAVA) {

            return;

        }

        final Location fromLocation = from.getLocation();
        if (!fluids.isTrackedAs(fromLocation, fromType)) {

            return;

        }

        final Block to = event.getToBlock();
        final Location toLocation = to.getLocation();
        final Material toType = to.getType();

        // Same-fluid destination: don't infect natural pools.
        if (toType == Material.WATER || toType == Material.LAVA) {

            return;

        }

        // Pure solid (non-waterloggable): WG skips and no fluid actually lands.
        if (toType.isSolid() && !(to.getBlockData() instanceof Waterlogged)) {

            return;

        }

        if (!FireFightOG.allows(toLocation, FireFightOG.temporaryFluids())) {

            return;

        }

        // Defer tracking until the world has actually mutated.
        Bukkit.getScheduler().runTask(fluids.getPlugin(), () -> {

            if (!fluids.isTrackedAs(fromLocation, fromType)) {

                return;

            }

            final Material currentType = to.getType();
            if (currentType == fromType) {

                fluids.track(to, fromType);
                return;

            }

            if (fromType == Material.WATER && to.getBlockData() instanceof Waterlogged waterlogged
                    && waterlogged.isWaterlogged())
            {

                fluids.track(to, fromType);

            }

        });

    }

    // Block obsidian / cobble / stone / basalt formation inside temp-fluids
    // regions.
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

}
