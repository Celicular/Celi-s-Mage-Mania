package com.celi.test.client.magic;

import com.celi.test.client.mixin.MultiPlayerGameModeStaffAccessor;
import com.celi.test.magic.MagesStaffItem;
import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinitySyncPayload;
import com.celi.test.magic.affinity.StaffAbility;
import com.celi.test.magic.spell.CastFeedbackPayload;
import com.celi.test.magic.spell.StaffCastPayload;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class StaffClient {
	private static final int LEFT_INTERVAL_TICKS = 4;
	private static final int MESSAGE_INTERVAL_TICKS = 20;
	private static final AtomicLong lifecycleGeneration = new AtomicLong();
	private static final Map<StaffAbility, Long> cooldownEnds = new EnumMap<>(StaffAbility.class);
	private static LocalPlayer trackedPlayer;
	private static ClientLevel trackedLevel;
	private static Affinity affinity = Affinity.NONE;
	private static ItemStack raisedStack;
	private static long clientTick;
	private static long lastLeftRequestTick = -LEFT_INTERVAL_TICKS;
	private static long lastMessageTick = -MESSAGE_INTERVAL_TICKS;
	private static String lastMessageKey = "";
	private static long raiseEndTick;
	private static boolean vanillaUseStarted;

	private StaffClient() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(AffinitySyncPayload.TYPE, (payload, context) -> {
			var client = context.client();
			var player = client.player;
			var level = client.level;
			var sender = context.responseSender();
			long generation = lifecycleGeneration.get();
			client.execute(() -> {
				if (isCurrent(client, player, level, sender, generation) && ensureContext(client)) {
					Affinity newAffinity = Affinity.fromId(payload.affinityId());
					if (newAffinity != affinity) {
						stopRaise(client);
					}
					affinity = newAffinity;
				}
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(CastFeedbackPayload.TYPE, (payload, context) -> {
			var client = context.client();
			var player = client.player;
			var level = client.level;
			var sender = context.responseSender();
			long generation = lifecycleGeneration.get();
			client.execute(() -> {
				if (isCurrent(client, player, level, sender, generation) && ensureContext(client)) {
					applyFeedback(client, payload);
				}
			});
		});
		ClientPlayConnectionEvents.INIT.register((handler, client) -> invalidate(client));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> invalidate(client));
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> invalidate(client));
		ClientTickEvents.START_CLIENT_TICK.register(StaffClient::tick);
	}

	public static Affinity getAffinity() {
		return ensureContext(Minecraft.getInstance()) ? affinity : Affinity.NONE;
	}

	public static boolean isHoldingStaff(Minecraft client) {
		return client.player != null && client.level != null && !client.player.isSpectator()
				&& client.player.getMainHandItem().getItem() instanceof MagesStaffItem;
	}

	public static boolean handleAttack(Minecraft client, boolean attacking) {
		if (!isHoldingStaff(client)) {
			return false;
		}
		if (client.gameMode != null && client.gameMode.isDestroying()) {
			client.gameMode.stopDestroyBlock();
		}
		if (attacking && ensureContext(client)) {
			requestLeft(client);
		}
		return true;
	}

	public static boolean isUsingItemForInput(LocalPlayer player) {
		var client = Minecraft.getInstance();
		return player.isUsingItem() && !(player == client.player && hasRaise(client));
	}

	public static void beginVanillaUse(Minecraft client) {
		if (isHoldingStaff(client) && ensureContext(client)) {
			stopRaise(client);
			vanillaUseStarted = true;
		}
	}

	private static void requestLeft(Minecraft client) {
		var player = client.player;
		if (client.gameMode == null || !player.isAlive() || player.isHandsBusy() || client.isPaused()
				|| client.gui.screen() != null || client.gui.overlay() != null
				|| !client.mouseHandler.isMouseGrabbed() || client.options.keyUse.isDown()
				|| isUsingItemForInput(player) || clientTick - lastLeftRequestTick < LEFT_INTERVAL_TICKS) {
			return;
		}
		StaffAbility ability = StaffAbility.forAffinity(affinity, false);
		int remaining = getCooldown(ability);
		if (remaining > 0) {
			showCooldown(client, ability, remaining);
			return;
		}
		if (ClientPlayNetworking.canSend(StaffCastPayload.TYPE)) {
			((MultiPlayerGameModeStaffAccessor) client.gameMode).celitest$ensureStaffCarriedItem();
			lastLeftRequestTick = clientTick;
			ClientPlayNetworking.send(new StaffCastPayload(false));
		}
	}

	private static void applyFeedback(Minecraft client, CastFeedbackPayload payload) {
		StaffAbility ability = abilityFromId(payload.abilityId());
		if (ability == null) {
			if (!payload.accepted() && isHoldingStaff(client)) {
				stopPredictedUse(client);
				showMessage(client, affinity == Affinity.NONE
						? "message.celitest.staff.no_affinity" : "message.celitest.staff.unavailable");
			}
			return;
		}
		long cooldownBase = payload.accepted() && !ability.secondary()
				? lastLeftRequestTick : clientTick;
		cooldownEnds.put(ability, Math.max(clientTick, cooldownBase + Math.max(0, payload.cooldownTicks())));
		if (!isHoldingStaff(client) || !client.player.isAlive() || ability.affinity() != affinity) {
			return;
		}
		if (!payload.accepted()) {
			if (ability.secondary() && payload.castTicks() == 0) {
				stopPredictedUse(client);
			}
			return;
		}
		if (ability.secondary()) {
			if (payload.castTicks() == 0) {
				stopPredictedUse(client);
				return;
			}
			if (client.options.keyUse.isDown() && client.player.isUsingItem()) {
				showMessage(client, "message.celitest.staff.casting", Component.translatable(ability.translationKey()));
			}
			return;
		}
		if (!vanillaUseStarted && !client.options.keyUse.isDown() && client.gui.screen() == null
				&& (!client.player.isUsingItem() || client.player.getUsedItemHand() == InteractionHand.MAIN_HAND)) {
			raisedStack = client.player.getMainHandItem();
			raiseEndTick = clientTick + Math.clamp(payload.castTicks(), 0, LEFT_INTERVAL_TICKS);
			if (raiseEndTick > clientTick) {
				client.player.startUsingItem(InteractionHand.MAIN_HAND);
			}
			showMessage(client, "message.celitest.staff.cast", Component.translatable(ability.translationKey()));
		}
	}

	private static void stopPredictedUse(Minecraft client) {
		if (vanillaUseStarted && client.player.isUsingItem()
				&& client.player.getUsedItemHand() == InteractionHand.MAIN_HAND
				&& client.player.getUseItem().getItem() instanceof MagesStaffItem) {
			client.player.stopUsingItem();
			vanillaUseStarted = false;
		}
	}

	private static StaffAbility abilityFromId(String id) {
		for (StaffAbility ability : StaffAbility.values()) {
			if (ability.id().equals(id)) {
				return ability;
			}
		}
		return null;
	}

	private static int getCooldown(StaffAbility ability) {
		return ability == null ? 0 : (int) Math.clamp(cooldownEnds.getOrDefault(ability, clientTick) - clientTick,
				0L, Integer.MAX_VALUE);
	}

	private static void showCooldown(Minecraft client, StaffAbility ability, int remaining) {
		showMessage(client, "message.celitest.staff.cooldown", Component.translatable(ability.translationKey()),
				String.format(Locale.ROOT, "%.1f", remaining / 20.0));
	}

	private static void showMessage(Minecraft client, String key, Object... arguments) {
		if (!key.equals(lastMessageKey) || clientTick - lastMessageTick >= MESSAGE_INTERVAL_TICKS) {
			client.player.sendOverlayMessage(Component.translatable(key, arguments));
			lastMessageTick = clientTick;
			lastMessageKey = key;
		}
	}

	private static void tick(Minecraft client) {
		if (!ensureContext(client) || client.isPaused() || !client.level.tickRateManager().runsNormally()) {
			return;
		}
		clientTick++;
		cooldownEnds.values().removeIf(end -> end <= clientTick);
		if (!client.player.isUsingItem()) {
			vanillaUseStarted = false;
		}
		if (raisedStack == null) {
			return;
		}
		if (!hasRaise(client) || !client.player.isAlive() || client.gui.screen() != null || vanillaUseStarted) {
			stopRaise(client);
		} else if (!client.player.isUsingItem()) {
			client.player.startUsingItem(InteractionHand.MAIN_HAND);
		}
	}

	private static boolean hasRaise(Minecraft client) {
		return raisedStack != null && client.player == trackedPlayer && client.level == trackedLevel
				&& raiseEndTick > clientTick && client.player.getMainHandItem() == raisedStack
				&& !vanillaUseStarted;
	}

	private static void stopRaise(Minecraft client) {
		if (raisedStack != null && client.player == trackedPlayer && client.level == trackedLevel
				&& client.player.isUsingItem() && client.player.getUsedItemHand() == InteractionHand.MAIN_HAND
				&& client.player.getUseItem() == raisedStack && !vanillaUseStarted) {
			client.player.stopUsingItem();
		}
		raisedStack = null;
		raiseEndTick = 0;
	}

	private static boolean isCurrent(Minecraft client, LocalPlayer player, ClientLevel level, PacketSender sender,
			long generation) {
		return generation == lifecycleGeneration.get() && player != null && level != null
				&& client.player == player && client.level == level && player.level() == level
				&& client.getConnection() == player.connection && ClientPlayNetworking.getSender() == sender;
	}

	private static boolean ensureContext(Minecraft client) {
		if (client.player == null || client.level == null || client.player.level() != client.level
				|| client.getConnection() != client.player.connection) {
			reset(client);
			return false;
		}
		if (client.player != trackedPlayer || client.level != trackedLevel) {
			reset(client);
			trackedPlayer = client.player;
			trackedLevel = client.level;
		}
		return true;
	}

	private static void invalidate(Minecraft client) {
		long generation = lifecycleGeneration.incrementAndGet();
		client.execute(() -> {
			if (generation == lifecycleGeneration.get()) {
				reset(client);
			}
		});
	}

	private static void reset(Minecraft client) {
		stopRaise(client);
		trackedPlayer = null;
		trackedLevel = null;
		affinity = Affinity.NONE;
		cooldownEnds.clear();
		clientTick = 0;
		lastLeftRequestTick = -LEFT_INTERVAL_TICKS;
		lastMessageTick = -MESSAGE_INTERVAL_TICKS;
		lastMessageKey = "";
		vanillaUseStarted = false;
	}
}
