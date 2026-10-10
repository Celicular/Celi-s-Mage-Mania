package com.celi.test.magic.spell;

import com.celi.test.magic.MagicItems;
import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinityData;
import com.celi.test.magic.affinity.AffinitySystem;
import com.celi.test.magic.affinity.StaffAbility;
import com.celi.test.magic.mana.ManaMath;
import com.celi.test.magic.mana.ManaSystem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class StaffCasting {
    private static final int REJECTION_FEEDBACK_INTERVAL_TICKS = 10;
    private static final Map<UUID, ActiveCast> ACTIVE_CASTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_REQUEST_TICKS = new HashMap<>();
    private static final Map<UUID, Long> LAST_REJECTION_TICKS = new HashMap<>();
    private static final Set<UUID> STOPPING_USE = new HashSet<>();
    private static boolean initialized;

    private StaffCasting() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        PayloadTypeRegistry.serverboundPlay().register(StaffCastPayload.TYPE, StaffCastPayload.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CastFeedbackPayload.TYPE, CastFeedbackPayload.STREAM_CODEC);
        ServerPlayNetworking.registerGlobalReceiver(StaffCastPayload.TYPE, (payload, context) -> {
            if (context.server().isSameThread()) {
                receive(context.player(), payload);
            } else {
                context.server().execute(() -> receive(context.player(), payload));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean gameTick = !server.isPaused() && server.tickRateManager().runsNormally();
            for (ActiveCast cast : List.copyOf(ACTIVE_CASTS.values())) {
                if (cast.level().getServer() == server) {
                    advance(cast, gameTick);
                }
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                cancel(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> forget(oldPlayer));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> cancel(player));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ActiveCast cast : List.copyOf(ACTIVE_CASTS.values())) {
                if (cast.level().getServer() == server) {
                    cancel(cast.player());
                }
            }
            clear();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
        ThornvinePrison.initialize();
        initialized = true;
    }

    public static boolean request(ServerPlayer player, boolean secondary, InteractionHand hand) {
        requireServerThread(player);
        if (!claimIngress(player)) {
            return false;
        }
        return begin(player, secondary, hand);
    }

    public static void cancel(ServerPlayer player) {
        requireServerThread(player);
        UUID id = player.getUUID();
        if (STOPPING_USE.contains(id)) {
            return;
        }
        ThunderstormSpells.cancel(player);
        ActiveCast cast = ACTIVE_CASTS.get(id);
        if (cast == null || cast.player() != player) {
            return;
        }
        ACTIVE_CASTS.remove(id);
        stopUse(cast);
        feedback(player, cast.ability(), "interrupted", 0, false, false);
    }

    private static void receive(ServerPlayer player, StaffCastPayload payload) {
        requireServerThread(player);
        if (!claimIngress(player)) {
            return;
        }
        if (payload.secondary()) {
            feedback(player, null, "unsafe", 0, false, true);
            return;
        }
        begin(player, false, InteractionHand.MAIN_HAND);
    }

    private static boolean begin(ServerPlayer player, boolean secondary, InteractionHand hand) {
        ActiveCast current = ACTIVE_CASTS.get(player.getUUID());
        if (current != null) {
            MinecraftServer server = player.level().getServer();
            advance(current, !server.isPaused() && server.tickRateManager().runsNormally());
        }
        if (hand != InteractionHand.MAIN_HAND || !isSafe(player)
                || !player.getMainHandItem().is(MagicItems.MAGES_STAFF)) {
            feedback(player, null, "unsafe", 0, false, true);
            return false;
        }
        AffinityData data = AffinitySystem.dataFor(player);
        Affinity affinity = data.affinity();
        StaffAbility ability = StaffAbility.forAffinity(affinity, secondary);
        if (affinity == Affinity.NONE || ability == null) {
            feedback(player, null, "no_affinity", 0, false, true);
            return false;
        }
        if (ACTIVE_CASTS.containsKey(player.getUUID()) || player.isUsingItem()) {
            feedback(player, ability, "busy", 0, false, true);
            return false;
        }
        if (data.getCooldown(ability) > 0) {
            feedback(player, ability, "cooldown", 0, false, true);
            return false;
        }
        if (ManaSystem.getCurrentMana(player) < ability.manaCost()) {
            feedback(player, ability, "insufficient_mana", 0, false, true);
            return false;
        }
        ItemStack stack = player.getMainHandItem();
        player.startUsingItem(hand);
        if (!player.isUsingItem() || player.getUsedItemHand() != hand || player.getUseItem() != stack) {
            feedback(player, ability, "unsafe", 0, false, true);
            return false;
        }
        ActiveCast cast = new ActiveCast(player, player.level(), stack, player.getInventory().getSelectedSlot(),
                ability, gameTime(player));
        if (!ManaSystem.trySpend(player, ability.manaCost())) {
            stopUse(cast);
            feedback(player, ability, "insufficient_mana", 0, false, true);
            return false;
        }
        ACTIVE_CASTS.put(player.getUUID(), cast);
        data.startCooldown(ability);
        if (ability == StaffAbility.THUNDERSTORM) {
            ThunderstormSpells.beginChannel(player);
        }
        if (!secondary) {
            StaffProjectiles.launch(player, affinity);
        }
        feedback(player, ability, "accepted", ability.castTicks(), true, false);
        return true;
    }

    private static void advance(ActiveCast cast, boolean gameTick) {
        ServerPlayer player = cast.player();
        if (ACTIVE_CASTS.get(player.getUUID()) != cast) {
            return;
        }
        if (!isMaintained(cast)) {
            cancel(player);
            return;
        }
        if (!gameTick || remainingTicks(cast) > 0) {
            return;
        }
        ACTIVE_CASTS.remove(player.getUUID());
        stopUse(cast);
        if (!cast.ability().secondary()) {
            sendFeedback(player, cast.ability(), 0, true);
            return;
        }
        boolean completed = switch (cast.ability()) {
            case INFERNO -> FireSpells.castInferno(player);
            case THUNDERSTORM -> ThunderstormSpells.cast(player);
            case SKYWARD_ASCENT -> WindSpells.castSkywardAscent(player);
            case THORNVINE_PRISON -> ThornvinePrison.cast(player);
            default -> false;
        };
        feedback(player, cast.ability(), completed ? "accepted" : "unsafe", 0, completed, false);
    }

    private static boolean isMaintained(ActiveCast cast) {
        ServerPlayer player = cast.player();
        return isSafe(player) && player.level() == cast.level()
                && player.getInventory().getSelectedSlot() == cast.slot()
                && player.getMainHandItem() == cast.stack()
                && cast.stack().is(MagicItems.MAGES_STAFF)
                && AffinitySystem.getAffinity(player) == cast.ability().affinity()
                && player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND
                && player.getUseItem() == cast.stack();
    }

    private static int remainingTicks(ActiveCast cast) {
        long elapsed = Math.max(0L, gameTime(cast.player()) - cast.startedAt());
        return (int) Math.max(0L, cast.ability().castTicks() - elapsed);
    }

    private static boolean claimIngress(ServerPlayer player) {
        if (!isConnected(player)) {
            return false;
        }
        long tick = gameTime(player);
        Long previous = LAST_REQUEST_TICKS.put(player.getUUID(), tick);
        return previous == null || previous.longValue() != tick;
    }

    private static void feedback(ServerPlayer player, StaffAbility ability, String reason, int castTicks,
                                 boolean accepted, boolean throttle) {
        if (!isConnected(player)) {
            return;
        }
        boolean showMessage = true;
        if (throttle) {
            long tick = gameTime(player);
            Long previous = LAST_REJECTION_TICKS.get(player.getUUID());
            showMessage = previous == null || tick - previous >= REJECTION_FEEDBACK_INTERVAL_TICKS;
            if (showMessage) {
                LAST_REJECTION_TICKS.put(player.getUUID(), tick);
            }
        }
        if (showMessage) {
            int cooldown = ability == null ? 0 : AffinitySystem.dataFor(player).getCooldown(ability);
            Component name = Component.translatable(ability == null ? "item.celitest.mages_staff" : ability.translationKey());
            if ("insufficient_mana".equals(reason) && ability != null) {
                player.sendOverlayMessage(Component.translatable("message.celitest.staff.insufficient_mana",
                        name, ability.manaCost(), ManaMath.format(ManaSystem.getCurrentMana(player))));
            } else {
                player.sendOverlayMessage(Component.translatable("message.celitest.staff." + reason,
                        name, String.format(Locale.ROOT, "%.1f", cooldown / 20.0D)));
            }
        }
        ActiveCast active = ACTIVE_CASTS.get(player.getUUID());
        if (!accepted && active != null && active.player() == player) {
            sendFeedback(player, active.ability(), remainingTicks(active), false);
        } else {
            sendFeedback(player, ability, castTicks, accepted);
        }
    }

    private static void sendFeedback(ServerPlayer player, StaffAbility ability, int castTicks, boolean accepted) {
        if (isConnected(player) && ServerPlayNetworking.canSend(player, CastFeedbackPayload.TYPE)) {
            int cooldown = ability == null ? 0 : AffinitySystem.dataFor(player).getCooldown(ability);
            ServerPlayNetworking.send(player, new CastFeedbackPayload(ability == null ? "" : ability.id(),
                    cooldown, Math.max(0, castTicks), accepted));
        }
    }

    private static void stopUse(ActiveCast cast) {
        ServerPlayer player = cast.player();
        if (!player.isUsingItem() || player.getUsedItemHand() != InteractionHand.MAIN_HAND
                || player.getUseItem() != cast.stack() || !STOPPING_USE.add(player.getUUID())) {
            return;
        }
        try {
            player.stopUsingItem();
        } finally {
            STOPPING_USE.remove(player.getUUID());
        }
    }

    private static boolean isSafe(ServerPlayer player) {
        return isConnected(player) && player.isAlive() && !player.isSpectator()
                && !player.isSleeping() && !player.isChangingDimension();
    }

    private static boolean isConnected(ServerPlayer player) {
        return player.connection != null && !player.hasDisconnected() && !player.isRemoved();
    }

    private static long gameTime(ServerPlayer player) {
        return player.level().getServer().getWorldData().overworldData().getGameTime();
    }

    private static void forget(ServerPlayer player) {
        cancel(player);
        LAST_REQUEST_TICKS.remove(player.getUUID());
        LAST_REJECTION_TICKS.remove(player.getUUID());
        STOPPING_USE.remove(player.getUUID());
    }

    private static void clear() {
        ACTIVE_CASTS.clear();
        LAST_REQUEST_TICKS.clear();
        LAST_REJECTION_TICKS.clear();
        STOPPING_USE.clear();
    }

    private static void requireServerThread(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Staff casting must run on the server thread");
        }
    }

    private record ActiveCast(ServerPlayer player, ServerLevel level, ItemStack stack, int slot,
                              StaffAbility ability, long startedAt) {
    }
}
