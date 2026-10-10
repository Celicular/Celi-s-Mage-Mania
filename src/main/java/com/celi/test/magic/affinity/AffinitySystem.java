package com.celi.test.magic.affinity;

import com.celi.test.magic.spell.StaffCasting;
import com.celi.test.magic.spell.ThornvinePrison;
import java.util.Objects;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class AffinitySystem {
    private static boolean initialized;

    private AffinitySystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        PayloadTypeRegistry.clientboundPlay().register(AffinitySyncPayload.TYPE, AffinitySyncPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
        ServerPlayerEvents.COPY_FROM.register(
                (oldPlayer, newPlayer, alive) -> dataFor(newPlayer).copyFrom(dataFor(oldPlayer)));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> sync(player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.isPaused() || !server.tickRateManager().runsNormally()) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (isConnected(player)) {
                    dataFor(player).tickCooldowns();
                }
            }
        });
        initialized = true;
    }

    public static Affinity getAffinity(Player player) {
        return player instanceof AffinityPlayer affinityPlayer
                ? affinityPlayer.celitest$getAffinityData().affinity() : Affinity.NONE;
    }

    public static AffinityData dataFor(ServerPlayer player) {
        return ((AffinityPlayer) Objects.requireNonNull(player, "player")).celitest$getAffinityData();
    }

    public static boolean setAffinity(ServerPlayer player, Affinity affinity) {
        Objects.requireNonNull(player, "player");
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Affinity must be changed on the server thread");
        }
        AffinityData data = dataFor(player);
        if (affinity == null || affinity == Affinity.NONE || affinity == data.affinity()) {
            return false;
        }
        StaffCasting.cancel(player);
        ThornvinePrison.cancel(player);
        data.setAffinity(affinity);
        AffinityPassives.onAffinityChanged(player);
        sync(player);
        return true;
    }

    private static void sync(ServerPlayer player) {
        if (isConnected(player) && ServerPlayNetworking.canSend(player, AffinitySyncPayload.TYPE)) {
            ServerPlayNetworking.send(player, new AffinitySyncPayload(dataFor(player).affinity().id()));
        }
    }

    private static boolean isConnected(ServerPlayer player) {
        return player.connection != null && !player.hasDisconnected() && !player.isRemoved();
    }
}
