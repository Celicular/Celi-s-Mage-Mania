package com.celi.test.magic.spell;

import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinitySystem;
import com.celi.test.mixin.LevelWeatherAccessor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.phys.Vec3;

final class ScopedStormWeather {
    private static final int FADE_TICKS = 40;
    private static final Map<MinecraftServer, StormState> STORMS = new IdentityHashMap<>();
    private static boolean initialized;

    private ScopedStormWeather() {
    }

    static void initialize() {
        if (initialized) {
            return;
        }
        ServerTickEvents.START_SERVER_TICK.register(ScopedStormWeather::beforeTick);
        ServerTickEvents.END_SERVER_TICK.register(ScopedStormWeather::afterTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancel(handler.player));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> cancel(player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                cancel(player);
            }
        });
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            StormState state = STORMS.get(server);
            if (state != null) {
                state.resumeAfterSave = state.applied;
                state.restoreBaseline();
            }
        });
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
            StormState state = STORMS.get(server);
            if (state != null && state.resumeAfterSave) {
                state.resumeAfterSave = false;
                state.applyStorm();
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(ScopedStormWeather::stop);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> STORMS.remove(server));
        initialized = true;
    }

    static void beginChannel(ServerPlayer caster, Vec3 center, int channelTicks) {
        MinecraftServer server = caster.level().getServer();
        StormState state = STORMS.computeIfAbsent(server, StormState::new);
        LevelStorm transition = state.transitions.computeIfAbsent(caster.level(),
                level -> new LevelStorm(state.levelBaselines.get(level).raining()));
        Lease lease = new Lease(caster, caster.level(), center, channelTicks + 1, channelTicks,
                transition.strength, server.getTickCount());
        state.leases.computeIfAbsent(caster.level(), level -> new ArrayList<>()).add(lease);
    }

    static void acquire(ServerPlayer caster, Vec3 center, int durationTicks) {
        MinecraftServer server = caster.level().getServer();
        StormState state = STORMS.computeIfAbsent(server, StormState::new);
        LevelStorm transition = state.transitions.computeIfAbsent(caster.level(),
                level -> new LevelStorm(state.levelBaselines.get(level).raining()));
        List<Lease> leases = state.leases.computeIfAbsent(caster.level(), level -> new ArrayList<>());
        for (Lease lease : leases) {
            if (lease.caster == caster && lease.channeling) {
                lease.center = center;
                lease.remainingTicks = durationTicks;
                lease.startedTick = server.getTickCount();
                lease.elapsedTicks = lease.channelTicks;
                lease.channeling = false;
                return;
            }
        }
        leases.add(new Lease(caster, caster.level(), center, durationTicks, 0,
                transition.strength, server.getTickCount()));
    }

    static void cancel(ServerPlayer caster) {
        MinecraftServer server = caster.level().getServer();
        StormState state = STORMS.get(server);
        if (state == null || state.leases.values().stream().flatMap(List::stream)
                .noneMatch(lease -> lease.caster.getUUID().equals(caster.getUUID()))) {
            return;
        }
        boolean applied = state.applied;
        state.restoreBaseline();
        for (List<Lease> leases : state.leases.values()) {
            leases.removeIf(lease -> lease.caster.getUUID().equals(caster.getUUID()));
        }
        state.removeEmptyLevels();
        if (state.leases.isEmpty()) {
            state.syncBaseline();
            STORMS.remove(server);
        } else if (applied) {
            state.applyStorm();
        }
    }

    private static void beforeTick(MinecraftServer server) {
        StormState state = STORMS.get(server);
        if (state != null) {
            state.restoreBaseline();
            state.removeInvalidLeases();
            state.removeEmptyLevels();
            if (state.leases.isEmpty()) {
                state.syncBaseline();
                STORMS.remove(server);
            }
        }
    }

    private static void afterTick(MinecraftServer server) {
        StormState state = STORMS.get(server);
        if (state == null) {
            return;
        }
        state.restoreBaseline();
        state.removeInvalidLeases();
        if (!server.isPaused() && server.tickRateManager().runsNormally()) {
            for (List<Lease> leases : state.leases.values()) {
                for (Lease lease : leases) {
                    if (lease.startedTick != server.getTickCount()) {
                        lease.elapsedTicks++;
                        lease.remainingTicks--;
                        if (lease.channeling) {
                            lease.center = lease.caster.position();
                        }
                        if (lease.remainingTicks > 0 && lease.remainingTicks % 4 == 0) {
                            rainParticles(lease);
                        }
                    }
                }
                leases.removeIf(lease -> lease.remainingTicks <= 0);
            }
        }
        state.updateTransitions(!server.isPaused() && server.tickRateManager().runsNormally());
        state.removeEmptyLevels();
        if (state.leases.isEmpty()) {
            state.syncBaseline();
            STORMS.remove(server);
        } else {
            state.applyStorm();
        }
    }

    private static void stop(MinecraftServer server) {
        StormState state = STORMS.remove(server);
        if (state != null) {
            state.restoreBaseline();
            state.syncBaseline();
        }
    }

    private static void rainParticles(Lease lease) {
        float strength = lease.strength();
        if (strength <= 0.0F) {
            return;
        }
        lease.level.sendParticles(ParticleTypes.RAIN, lease.center.x, lease.center.y + 7.0D, lease.center.z,
                Math.max(1, Math.round(32 * strength)), 7.0D, 1.0D, 7.0D, 0.0D);
        lease.level.sendParticles(ParticleTypes.SPLASH, lease.center.x, lease.center.y + 0.1D, lease.center.z,
                Math.max(1, Math.round(12 * strength)), 7.0D, 0.1D, 7.0D, 0.0D);
    }

    private static final class StormState {
        private final MinecraftServer server;
        private final WeatherBaseline weatherBaseline;
        private final Map<ServerLevel, LevelBaseline> levelBaselines = new IdentityHashMap<>();
        private final Map<ServerLevel, List<Lease>> leases = new IdentityHashMap<>();
        private final Map<ServerLevel, LevelStorm> transitions = new IdentityHashMap<>();
        private boolean applied;
        private boolean resumeAfterSave;

        private StormState(MinecraftServer server) {
            this.server = server;
            weatherBaseline = WeatherBaseline.capture(server.getWeatherData());
            for (ServerLevel level : server.getAllLevels()) {
                levelBaselines.put(level, LevelBaseline.capture(level));
            }
        }

        private void restoreBaseline() {
            weatherBaseline.restore(server.getWeatherData());
            levelBaselines.forEach((level, baseline) -> baseline.restore(level));
            applied = false;
        }

        private void syncBaseline() {
            levelBaselines.forEach((level, baseline) -> baseline.sync(level));
        }

        private void removeInvalidLeases() {
            for (List<Lease> levelLeases : leases.values()) {
                levelLeases.removeIf(lease -> !lease.isValid(server));
            }
        }

        private void updateTransitions(boolean gameTick) {
            for (Map.Entry<ServerLevel, List<Lease>> entry : leases.entrySet()) {
                LevelStorm transition = transitions.get(entry.getKey());
                transition.previousStrength = transition.strength;
                if (!entry.getValue().isEmpty()) {
                    transition.fading = false;
                    transition.strength = 0.0F;
                    for (Lease lease : entry.getValue()) {
                        transition.strength = Math.max(transition.strength, lease.strength());
                    }
                } else {
                    if (!transition.fading) {
                        transition.fading = true;
                        transition.fadeStart = transition.strength;
                        transition.fadeElapsed = 0;
                    }
                    if (gameTick) {
                        transition.fadeElapsed++;
                    }
                    transition.strength = transition.fadeStart * (1.0F - ease(transition.fadeElapsed, FADE_TICKS));
                }
            }
        }

        private void removeEmptyLevels() {
            Iterator<Map.Entry<ServerLevel, List<Lease>>> iterator = leases.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<ServerLevel, List<Lease>> entry = iterator.next();
                if (entry.getValue().isEmpty() && transitions.get(entry.getKey()).strength <= 0.0F) {
                    LevelBaseline baseline = levelBaselines.get(entry.getKey());
                    baseline.restore(entry.getKey());
                    baseline.sync(entry.getKey());
                    transitions.remove(entry.getKey());
                    iterator.remove();
                }
            }
        }

        private void applyStorm() {
            levelBaselines.forEach((level, baseline) -> {
                if (!leases.containsKey(level) || !level.canHaveWeather()) {
                    baseline.sync(level);
                }
            });
            int remainingTicks = 0;
            for (Map.Entry<ServerLevel, List<Lease>> entry : leases.entrySet()) {
                ServerLevel level = entry.getKey();
                if (!level.canHaveWeather()) {
                    continue;
                }
                for (Lease lease : entry.getValue()) {
                    remainingTicks = Math.max(remainingTicks, lease.remainingTicks);
                }
                LevelBaseline baseline = levelBaselines.get(level);
                LevelStorm transition = transitions.get(level);
                remainingTicks = Math.max(remainingTicks, FADE_TICKS - transition.fadeElapsed);
                float rain = mix(baseline.rain(), transition.strength);
                float thunder = mix(baseline.thunder(), transition.strength);
                LevelWeatherAccessor weather = (LevelWeatherAccessor) level;
                weather.celitest$setPreviousRainLevel(mix(baseline.previousRain(), transition.previousStrength));
                weather.celitest$setRainLevel(rain);
                weather.celitest$setPreviousThunderLevel(mix(baseline.previousThunder(), transition.previousStrength));
                weather.celitest$setThunderLevel(thunder);
                level.updateSkyBrightness();
                boolean raining = baseline.raining() || transition.strength > 0.0F;
                syncWeather(level, raining, rain, thunder, transition.lastRaining != raining);
                transition.lastRaining = raining;
            }
            if (remainingTicks > 0) {
                server.setWeatherParameters(0, remainingTicks, true, true);
            }
            applied = true;
        }
    }

    private static void syncWeather(ServerLevel level, boolean raining, float rainLevel, float thunderLevel) {
        syncWeather(level, raining, rainLevel, thunderLevel, true);
    }

    private static void syncWeather(ServerLevel level, boolean raining, float rainLevel, float thunderLevel,
            boolean rainStateChanged) {
        ClientboundGameEventPacket rainState = new ClientboundGameEventPacket(raining
                ? ClientboundGameEventPacket.START_RAINING : ClientboundGameEventPacket.STOP_RAINING, 0.0F);
        ClientboundGameEventPacket rain = new ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, rainLevel);
        ClientboundGameEventPacket thunder = new ClientboundGameEventPacket(
                ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, thunderLevel);
        for (ServerPlayer player : level.players()) {
            if (player.connection != null && !player.hasDisconnected() && !player.isRemoved()) {
                if (rainStateChanged) {
                    player.connection.send(rainState);
                }
                player.connection.send(rain);
                player.connection.send(thunder);
            }
        }
    }

    private static final class Lease {
        private final ServerPlayer caster;
        private final ServerLevel level;
        private final int channelTicks;
        private final float initialStrength;
        private Vec3 center;
        private int startedTick;
        private int elapsedTicks;
        private int remainingTicks;
        private boolean channeling;

        private Lease(ServerPlayer caster, ServerLevel level, Vec3 center, int remainingTicks, int channelTicks,
                float initialStrength, int startedTick) {
            this.caster = caster;
            this.level = level;
            this.center = center;
            this.remainingTicks = remainingTicks;
            this.channelTicks = channelTicks;
            this.initialStrength = initialStrength;
            this.startedTick = startedTick;
            channeling = channelTicks > 0;
        }

        private float strength() {
            return channelTicks == 0 ? 1.0F : mix(initialStrength, ease(elapsedTicks, channelTicks));
        }

        private boolean isValid(MinecraftServer server) {
            return caster.connection != null && !caster.hasDisconnected() && !caster.isRemoved() && caster.isAlive()
                    && caster.level() == level && server.getLevel(level.dimension()) == level
                    && server.getPlayerList().getPlayer(caster.getUUID()) == caster
                    && AffinitySystem.getAffinity(caster) == Affinity.WATER;
        }
    }

    private static final class LevelStorm {
        private float strength;
        private float previousStrength;
        private float fadeStart;
        private int fadeElapsed;
        private boolean fading;
        private boolean lastRaining;

        private LevelStorm(boolean raining) {
            lastRaining = raining;
        }
    }

    private static float ease(int elapsed, int duration) {
        float progress = Math.clamp(elapsed / (float) Math.max(1, duration), 0.0F, 1.0F);
        return progress * progress * (3.0F - 2.0F * progress);
    }

    private static float mix(float baseline, float strength) {
        return baseline + (1.0F - baseline) * strength;
    }

    private record WeatherBaseline(int clearTime, int rainTime, int thunderTime, boolean raining, boolean thundering) {
        private static WeatherBaseline capture(WeatherData weather) {
            return new WeatherBaseline(weather.getClearWeatherTime(), weather.getRainTime(), weather.getThunderTime(),
                    weather.isRaining(), weather.isThundering());
        }

        private void restore(WeatherData weather) {
            weather.setClearWeatherTime(clearTime);
            weather.setRainTime(rainTime);
            weather.setThunderTime(thunderTime);
            weather.setRaining(raining);
            weather.setThundering(thundering);
        }
    }

    private record LevelBaseline(float previousRain, float rain, float previousThunder, float thunder,
            boolean raining) {
        private static LevelBaseline capture(ServerLevel level) {
            LevelWeatherAccessor weather = (LevelWeatherAccessor) level;
            return new LevelBaseline(weather.celitest$getPreviousRainLevel(), weather.celitest$getRainLevel(),
                    weather.celitest$getPreviousThunderLevel(), weather.celitest$getThunderLevel(), level.isRaining());
        }

        private void restore(ServerLevel level) {
            LevelWeatherAccessor weather = (LevelWeatherAccessor) level;
            weather.celitest$setPreviousRainLevel(previousRain);
            weather.celitest$setRainLevel(rain);
            weather.celitest$setPreviousThunderLevel(previousThunder);
            weather.celitest$setThunderLevel(thunder);
            level.updateSkyBrightness();
        }

        private void sync(ServerLevel level) {
            syncWeather(level, raining, rain, thunder);
        }
    }
}
