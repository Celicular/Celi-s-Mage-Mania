package com.celi.test.magic.affinity;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class AffinityOwnedEffects {
    private static final Map<ServerPlayer, State> STATES = new IdentityHashMap<>();

    private AffinityOwnedEffects() {
    }

    static void refresh(ServerPlayer player, Affinity affinity, boolean gameTick) {
        State state = stateFor(player, affinity == Affinity.WATER);
        if (state == null) {
            return;
        }
        reconcile(player, state.waterBreathing);
        reconcile(player, state.slowFalling);
        if (gameTick && state.slowFallingTicks > 0
                && state.lastGrantTick != player.level().getServer().getTickCount()) {
            state.slowFallingTicks--;
        }
        if (affinity == Affinity.WATER) {
            apply(player, state.waterBreathing, MobEffectInstance.INFINITE_DURATION);
        } else {
            removeOwned(player, state.waterBreathing);
        }
        if (affinity == Affinity.WIND && state.slowFallingTicks > 0) {
            apply(player, state.slowFalling, state.slowFallingTicks);
        } else {
            state.slowFallingTicks = 0;
            removeOwned(player, state.slowFalling);
        }
        if (affinity != Affinity.WATER && state.slowFallingTicks == 0) {
            STATES.remove(player);
        }
    }

    static void grantSlowFalling(ServerPlayer player, int ticks) {
        State state = stateFor(player, true);
        state.slowFallingTicks = Math.max(state.slowFallingTicks, ticks);
        state.lastGrantTick = player.level().getServer().getTickCount();
        apply(player, state.slowFalling, state.slowFallingTicks);
    }

    public static boolean beforeAdd(ServerPlayer player, MobEffectInstance incoming) {
        State state = STATES.get(player);
        Slot slot = state == null ? null : state.slot(incoming.getEffect());
        if (slot == null) {
            return true;
        }
        if (slot.applying == incoming) {
            MobEffectInstance current = player.getEffect(slot.effect);
            if (current != null && current != slot.owned) {
                return false;
            }
            if (current == null) {
                slot.owned = incoming;
            }
        } else {
            removeOwned(player, slot);
        }
        return true;
    }

    public static void afterRemoval(ServerPlayer player, Holder<MobEffect> effect, MobEffectInstance removed) {
        State state = STATES.get(player);
        Slot slot = state == null ? null : state.slot(effect);
        if (slot == null || removed == null || removed == slot.removing) {
            return;
        }
        slot.owned = null;
        if (slot == state.slowFalling) {
            state.slowFallingTicks = 0;
        }
    }

    public static void afterRemoveAll(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null) {
            return;
        }
        reconcile(player, state.waterBreathing);
        reconcile(player, state.slowFalling);
        state.slowFallingTicks = 0;
    }

    public static Collection<MobEffectInstance> effectsToSave(ServerPlayer player,
                                                              Collection<MobEffectInstance> effects) {
        State state = STATES.get(player);
        if (state == null) {
            return effects;
        }
        return effects.stream().filter(effect -> effect != state.waterBreathing.owned
                && effect != state.slowFalling.owned).toList();
    }

    public static void clear(ServerPlayer player) {
        State state = STATES.get(player);
        if (state == null) {
            return;
        }
        state.slowFallingTicks = 0;
        removeOwned(player, state.waterBreathing);
        removeOwned(player, state.slowFalling);
        STATES.remove(player);
    }

    static void clearAbsent(Collection<ServerPlayer> onlinePlayers) {
        for (ServerPlayer player : List.copyOf(STATES.keySet())) {
            if (!onlinePlayers.contains(player)) {
                clear(player);
            }
        }
    }

    static void clearAll() {
        for (ServerPlayer player : List.copyOf(STATES.keySet())) {
            clear(player);
        }
    }

    private static State stateFor(ServerPlayer player, boolean create) {
        State state = STATES.get(player);
        if (state != null && state.level != player.level()) {
            clear(player);
            state = null;
        }
        if (state == null && create) {
            state = new State(player.level());
            STATES.put(player, state);
        }
        return state;
    }

    private static void apply(ServerPlayer player, Slot slot, int duration) {
        reconcile(player, slot);
        MobEffectInstance current = player.getEffect(slot.effect);
        if (slot.applying != null || (current != null && current != slot.owned)
                || (current != null && (current.isInfiniteDuration() || current.getDuration() >= duration))) {
            return;
        }
        MobEffectInstance incoming = new MobEffectInstance(slot.effect, duration, 0, true, false, true);
        slot.applying = incoming;
        try {
            player.addEffect(incoming, player);
        } finally {
            slot.applying = null;
            reconcile(player, slot);
        }
    }

    private static void reconcile(ServerPlayer player, Slot slot) {
        if (player.getEffect(slot.effect) != slot.owned) {
            slot.owned = null;
        }
    }

    private static void removeOwned(ServerPlayer player, Slot slot) {
        MobEffectInstance owned = slot.owned;
        slot.owned = null;
        if (owned == null || player.getEffect(slot.effect) != owned) {
            return;
        }
        MobEffectInstance previousRemoval = slot.removing;
        slot.removing = owned;
        try {
            MobEffectInstance removed = player.removeEffectNoUpdate(slot.effect);
            if (removed != null) {
                ((AffinityEffectRemovalAccess) player).celitest$notifyAffinityEffectRemoval(List.of(removed));
            }
        } finally {
            slot.removing = previousRemoval;
        }
    }

    private static final class State {
        private final ServerLevel level;
        private final Slot waterBreathing = new Slot(MobEffects.WATER_BREATHING);
        private final Slot slowFalling = new Slot(MobEffects.SLOW_FALLING);
        private int slowFallingTicks;
        private int lastGrantTick = -1;

        private State(ServerLevel level) {
            this.level = level;
        }

        private Slot slot(Holder<MobEffect> effect) {
            if (effect.equals(MobEffects.WATER_BREATHING)) {
                return waterBreathing;
            }
            return effect.equals(MobEffects.SLOW_FALLING) ? slowFalling : null;
        }
    }

    private static final class Slot {
        private final Holder<MobEffect> effect;
        private MobEffectInstance owned;
        private MobEffectInstance applying;
        private MobEffectInstance removing;

        private Slot(Holder<MobEffect> effect) {
            this.effect = effect;
        }
    }
}
