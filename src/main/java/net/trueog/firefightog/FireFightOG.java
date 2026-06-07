// This is free and unencumbered software released into the public domain.
// Author: NotAlexNoyle.
package net.trueog.firefightog;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;

import net.trueog.firefightog.fire.FireManager;
import net.trueog.firefightog.fluid.FluidManager;
import net.trueog.firefightog.listeners.FireExtinguishListener;
import net.trueog.firefightog.listeners.FluidListener;
import net.trueog.utilitiesog.UtilitiesOG;
import net.trueog.utilitiesog.misc.FlagRegistrationException;

public class FireFightOG extends JavaPlugin {

    private static final String PREFIX = "[FireFight-OG]";

    public static final String FIRE_EXTINGUISH_NAME = "fire-extinguish";

    public static final String TEMPORARY_FLUIDS_NAME = "temporary-fluids";

    private static StateFlag fireExtinguish;

    private static StateFlag temporaryFluids;

    private FluidManager fluidManager;

    private FireManager fireManager;

    // WorldGuard only accepts flag registration during onLoad.
    @Override
    public void onLoad() {

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {

            getLogger().severe("WorldGuard is not installed! FireFight-OG flags will not register.");
            return;

        }

        try {

            fireExtinguish = registerStateFlag(FIRE_EXTINGUISH_NAME);
            temporaryFluids = registerStateFlag(TEMPORARY_FLUIDS_NAME);

        } catch (FlagRegistrationException flagregistrationException) {

            getLogger().severe(
                    "Failed to register FireFight-OG WorldGuard flags. " + flagregistrationException.getMessage());

        }

    }

    @Override
    public void onEnable() {

        saveDefaultConfig();
        final FileConfiguration config = getConfig();

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") == null) {

            UtilitiesOG.logToConsole(PREFIX, "WorldGuard not found, disabling FireFight-OG.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;

        }

        final PluginManager pluginManager = getServer().getPluginManager();

        if (config.getBoolean("fire-extinguish")) {

            final long fireLifetimeSeconds = config.getLong("fluid-lifetime-seconds");
            fireManager = new FireManager(this, fireLifetimeSeconds);
            pluginManager.registerEvents(new FireExtinguishListener(this, fireManager), this);
            UtilitiesOG.logToConsole(PREFIX, "Enabled fire-extinguish module (lifetime " + fireLifetimeSeconds + "s).");

        }

        if (!config.getBoolean("temporary-fluids")) {

            return;

        }

        final long lifetimeSeconds = config.getLong("fluid-lifetime-seconds");
        fluidManager = new FluidManager(this, lifetimeSeconds);

        pluginManager.registerEvents(new FluidListener(fluidManager), this);
        UtilitiesOG.logToConsole(PREFIX, "Enabled temporary-fluids module (lifetime " + lifetimeSeconds + "s).");

    }

    @Override
    public void onDisable() {

        if (fluidManager != null) {

            fluidManager.shutdown();

        }

        if (fireManager != null) {

            fireManager.shutdown();

        }

    }

    public static StateFlag fireExtinguish() {

        return fireExtinguish;

    }

    public static StateFlag temporaryFluids() {

        return temporaryFluids;

    }

    // True when the applicable regions resolve the given StateFlag to ALLOW.
    public static boolean allows(Location location, StateFlag flag) {

        if (flag == null) {

            return false;

        }

        final RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        final RegionQuery query = container.createQuery();
        final ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));

        return set.testState(null, flag);

    }

    private static StateFlag registerStateFlag(String name) throws FlagRegistrationException {

        final FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

        try {

            // Default false: each feature is off until a region explicitly allows it.
            final StateFlag flag = new StateFlag(name, false);

            registry.register(flag);

            return flag;

        } catch (FlagConflictException error) {

            // Reuse the already-registered flag when it is a StateFlag.
            final Flag<?> existing = registry.get(name);
            if (existing instanceof StateFlag stateFlag) {

                return stateFlag;

            }

            throw new FlagRegistrationException("Failed to register the " + name + " flag due to a conflict.", error);

        }

    }

}
