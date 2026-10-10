package com.celi.test.magic.affinity;

import java.util.Objects;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class AffinityPassives {
    private static boolean initialized;

    private AffinityPassives() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> restore(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> AffinityOwnedEffects.clear(handler.player));
        ServerPlayerEvents.LEAVE.register(AffinityOwnedEffects::clear);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                AffinityOwnedEffects.clear(player);
            }
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            AffinityOwnedEffects.clear(oldPlayer);
            restore(newPlayer);
        });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> restore(player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean gameTick = !server.isPaused() && server.tickRateManager().runsNormally();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isActive(player)) {
                    AffinityOwnedEffects.refresh(player, AffinitySystem.getAffinity(player), gameTick);
                } else {
                    AffinityOwnedEffects.clear(player);
                }
            }
            AffinityOwnedEffects.clearAbsent(server.getPlayerList().getPlayers());
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                AffinityOwnedEffects.clear(player);
            }
            AffinityOwnedEffects.clearAll();
        });
        initialized = true;
    }

    public static void onAffinityChanged(ServerPlayer player) {
        requireServerThread(player);
        restore(player);
    }

    public static float staffDamageMultiplier(ServerPlayer player) {
        return AffinitySystem.getAffinity(player) == Affinity.WIND && isAirborne(player) ? 1.5F : 1.0F;
    }

    public static void grantSlowFalling(ServerPlayer player, int ticks) {
        requireServerThread(player);
        if (ticks > 0 && isActive(player) && AffinitySystem.getAffinity(player) == Affinity.WIND) {
            AffinityOwnedEffects.grantSlowFalling(player, ticks);
        }
    }

    private static boolean isAirborne(ServerPlayer player) {
        return !player.onGround() && !player.isInWater() && !player.isInLava()
                && !player.isPassenger() && !player.getAbilities().flying;
    }

    private static boolean isActive(ServerPlayer player) {
        return player.isAlive() && !player.isRemoved() && player.connection != null && !player.hasDisconnected();
    }

    private static void restore(ServerPlayer player) {
        AffinityOwnedEffects.clear(player);
        if (isActive(player)) {
            AffinityOwnedEffects.refresh(player, AffinitySystem.getAffinity(player), false);
        }
    }

    private static void requireServerThread(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Affinity passives must be changed on the server thread");
        }
    }
}
