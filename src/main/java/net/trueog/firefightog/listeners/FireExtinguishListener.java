// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.sk89q.worldguard.protection.flags.StateFlag;

import net.trueog.firefightog.FireFightOG;

// Lets players punch out fire in regions flagged fire-extinguish, even where building is denied.
public class FireExtinguishListener implements Listener {

    // HIGH runs after WorldGuard's NORMAL priority interact handler.
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {

            return;

        }

        final Block clicked = event.getClickedBlock();
        if (clicked == null) {

            return;

        }

        // WorldGuard treats the fire as the clicked block's relative face.
        final Block fire = clicked.getRelative(event.getBlockFace());

        final Material type = fire.getType();
        if (type != Material.FIRE && type != Material.SOUL_FIRE) {

            return;

        }

        final StateFlag flag = FireFightOG.fireExtinguish();
        if (!FireFightOG.allows(fire.getLocation(), flag)) {

            return;

        }

        // Override WorldGuard's DENY so players can extinguish the fire.
        event.setUseInteractedBlock(Result.ALLOW);

    }

}