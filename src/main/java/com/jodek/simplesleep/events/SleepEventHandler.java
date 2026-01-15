package com.jodek.simplesleep.events;

import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSleep;
import com.hypixel.hytale.builtin.beds.sleep.components.PlayerSomnolence;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSleep;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSlumber;
import com.hypixel.hytale.builtin.beds.sleep.resources.WorldSomnolence;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.gameplay.SleepConfig;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.jodek.simplesleep.SimpleSleep;
import com.jodek.simplesleep.util.MessageUtil;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Handles sleep game events.
 * Tracks sleeping players and triggers night skip
 */
public class SleepEventHandler {

    private final com.jodek.simplesleep.config.SleepConfig config;

    // Tracks the last sleeping player count per world to avoid spam
    private final Map<String, Integer> lastSleepingCount = new HashMap<>();

    public SleepEventHandler(SimpleSleep plugin, com.jodek.simplesleep.config.SleepConfig config) {
        this.config = config;
    }


    public void checkAllWorlds() {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }

        for (World world : universe.getWorlds().values()) {
            world.execute(() -> checkWorldSleep(world));
        }
    }

    private void checkWorldSleep(World world) {
        if (world == null) {
            return;
        }

        EntityStore entityStore = world.getEntityStore();

        Store<EntityStore> store = entityStore.getStore();
        if (store == null) {
            return;
        }

        WorldSomnolence worldSomnolence = store.getResource(WorldSomnolence.getResourceType());

        WorldSleep worldSleep = worldSomnolence.getState();
        if (worldSleep instanceof WorldSlumber) {
            return;
        }

        Collection<PlayerRef> playerRefs = world.getPlayerRefs();
        if (playerRefs.isEmpty()) {
            return;
        }

        int totalPlayers = playerRefs.size();

        // Count for display (includes NoddingOff)
        int displaySleepingPlayers = countSleepingPlayersForDisplay(store, playerRefs);

        // Count for night skip (only Slumber)
        int readySleepingPlayers = countReadySleepingPlayers(store, playerRefs);

        int required = config.getRequiredSleepingPlayers(totalPlayers);

        // Show message only if the count changed
        if (config.isShowSleepingPlayers()) {
            String worldName = world.getName();
            Integer lastCount = lastSleepingCount.get(worldName);

            // Show message if count changed
            if (lastCount == null || lastCount != displaySleepingPlayers) {
                if (displaySleepingPlayers > 0) {
                    String message = displaySleepingPlayers + "/" + required + " players sleeping";
                    MessageUtil.broadcastActionBar(Universe.get(), message);
                }
                lastSleepingCount.put(worldName, displaySleepingPlayers);
            }
        }

        // Trigger night skip only when enough players are in Slumber
        if (readySleepingPlayers >= required) {
            triggerSlumber(store, world, worldSomnolence);
        }
    }

    /**
     * Counts sleeping players for display
     * Includes both NoddingOff and Slumber states
     */
    private int countSleepingPlayersForDisplay(Store<EntityStore> store, Collection<PlayerRef> playerRefs) {
        int sleepingCount = 0;

        for (PlayerRef playerRef : playerRefs) {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null) {
                continue;
            }

            PlayerSomnolence somnolence = store.getComponent(entityRef, PlayerSomnolence.getComponentType());
            if (somnolence == null) {
                continue;
            }

            PlayerSleep sleepState = somnolence.getSleepState();

            // Count NoddingOff and Slumber for display
            if (sleepState instanceof PlayerSleep.Slumber || sleepState instanceof PlayerSleep.NoddingOff) {
                sleepingCount++;
            }
        }

        return sleepingCount;
    }

    /**
     * Counts players ready for night skip
     * Only includes Slumber state
     */
    private int countReadySleepingPlayers(Store<EntityStore> store, Collection<PlayerRef> playerRefs) {
        int sleepingCount = 0;

        for (PlayerRef playerRef : playerRefs) {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null) continue;

            PlayerSomnolence somnolence = store.getComponent(entityRef, PlayerSomnolence.getComponentType());
            if (somnolence == null) continue;

            PlayerSleep sleepState = somnolence.getSleepState();

            if (sleepState instanceof PlayerSleep.Slumber) {
                sleepingCount++;
            }

            else if (sleepState instanceof PlayerSleep.NoddingOff noddingOff) {
                if (Instant.now().isAfter(noddingOff.realTimeStart().plusMillis(3150L))) {
                    sleepingCount++;
                }
            }
        }
        return sleepingCount;
    }

    // Triggers night skip
    private void triggerSlumber(Store<EntityStore> store, World world, WorldSomnolence worldSomnolence) {
        // Check to not trigger twice
        if (worldSomnolence.getState() instanceof WorldSlumber) {
            return;
        }

        // Gets WorldTimeResource to set time
        WorldTimeResource timeResource = store.getResource(WorldTimeResource.getResourceType());

        SleepConfig sleepConfig = world.getGameplayConfig().getWorldConfig().getSleepConfig();
        float wakeUpHour = sleepConfig.getWakeUpHour();

        // Calculate wakeup time
        Instant now = timeResource.getGameTime();
        Instant wakeUp = computeWakeupInstant(now, wakeUpHour);

        // Sets game time to morning (this triggers the night skip)
        timeResource.setGameTime(wakeUp, world, store);

        // Wake up all sleeping players
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            Ref<EntityStore> entityRef = playerRef.getReference();
            if (entityRef == null) {
                continue;
            }

            PlayerSomnolence somnolence = store.getComponent(entityRef, PlayerSomnolence.getComponentType());
            if (somnolence == null) {
                continue;
            }

            PlayerSleep sleepState = somnolence.getSleepState();

            // Set sleeping players to MorningWakeUp state
            if (sleepState instanceof PlayerSleep.NoddingOff || sleepState instanceof PlayerSleep.Slumber) {
                PlayerSomnolence wakeUpState = new PlayerSomnolence(new PlayerSleep.MorningWakeUp(wakeUp));
                store.putComponent(entityRef, PlayerSomnolence.getComponentType(), wakeUpState);
            }
        }
    }

    // Calculates wakeup time based on wakeup hour
    private Instant computeWakeupInstant(Instant now, float wakeUpHour) {
        LocalDateTime nowDateTime = LocalDateTime.ofInstant(now, ZoneOffset.UTC);

        int hour = (int) wakeUpHour;
        float minuteFraction = wakeUpHour - hour;
        int minutes = (int) (minuteFraction * 60.0F);

        LocalDateTime wakeUpDateTime = nowDateTime.toLocalDate().atTime(hour, minutes);

        if (!nowDateTime.isBefore(wakeUpDateTime)) {
            wakeUpDateTime = wakeUpDateTime.plusDays(1L);
        }

        return wakeUpDateTime.toInstant(ZoneOffset.UTC);
    }
}

