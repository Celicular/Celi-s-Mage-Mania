package com.celi.test.magic.mana;

import com.celi.test.CeliTest;
import com.celi.test.magic.MagicEnchantments;
import com.celi.test.magic.MageriteUpgrades;
import java.util.Objects;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public final class ManaSystem {
    private static final Identifier ARMOR_MODIFIER_ID = CeliTest.id("magerite_armor_mana");
    private static final Identifier ARMOR_REGENERATION_MODIFIER_ID = CeliTest.id("armor_mana_regeneration");
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private static boolean initialized;

    private ManaSystem() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        ManaAttributes.initialize();
        PayloadTypeRegistry.clientboundPlay().register(ManaSyncPayload.TYPE, ManaSyncPayload.STREAM_CODEC);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            dataFor(handler.player).resetSession();
            refreshEquipment(handler.player);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> dataFor(handler.player).resetRegeneration());
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            dataFor(newPlayer).copyFrom(dataFor(oldPlayer));
            snapshot(newPlayer);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> requestSnapshot(newPlayer));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> requestSnapshot(player));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean gameTick = !server.isPaused() && server.tickRateManager().runsNormally();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                tick(player, gameTick);
            }
        });
        initialized = true;
    }

    public static double getCurrentMana(ServerPlayer player) {
        return snapshot(player).current();
    }

    public static double getMaximumMana(ServerPlayer player) {
        return snapshot(player).maximum();
    }

    public static double setCurrentMana(ServerPlayer player, double amount) {
        requireServerThread(player);
        if (!Double.isFinite(amount)) {
            throw new IllegalArgumentException("Mana must be finite");
        }
        double maximum = snapshot(player).maximum();
        double current = Math.clamp(amount, 0.0D, maximum);
        ManaData data = dataFor(player);
        data.setCurrent(current);
        if (current >= maximum) {
            data.resetRegeneration();
        }
        return current;
    }

    public static void refreshEquipment(ServerPlayer player) {
        snapshot(player);
    }

    public static boolean trySpend(ServerPlayer player, double amount) {
        requireServerThread(player);
        if (!Double.isFinite(amount) || amount < 0.0D) {
            throw new IllegalArgumentException("Mana cost must be finite and nonnegative");
        }
        double current = snapshot(player).current();
        if (current < amount) {
            return false;
        }
        dataFor(player).setCurrent(current - amount);
        return true;
    }

    private static void tick(ServerPlayer player, boolean gameTick) {
        ManaSyncPayload payload = snapshot(player);
        ManaData data = dataFor(player);
        if (!isConnected(player) || !player.isAlive() || payload.current() >= payload.maximum()) {
            data.resetRegeneration();
        } else if (gameTick && data.advanceRegeneration()) {
            double current = ManaMath.regenerate(payload.current(), payload.maximum(),
                    player.getAttributeValue(ManaAttributes.MANA_REGENERATION));
            data.setCurrent(current);
            payload = new ManaSyncPayload(current, payload.maximum());
            if (current >= payload.maximum()) {
                data.resetRegeneration();
            }
        }
        sync(player, payload);
    }

    private static void requestSnapshot(ServerPlayer player) {
        snapshot(player);
        dataFor(player).setLastSynced(null);
    }

    private static ManaSyncPayload snapshot(ServerPlayer player) {
        requireServerThread(player);
        double maximum = updateArmorModifiers(player);
        ManaData data = dataFor(player);
        double current = Math.clamp(data.current(), 0.0D, maximum);
        data.setCurrent(current);
        if (current >= maximum) {
            data.resetRegeneration();
        }
        return new ManaSyncPayload(current, maximum);
    }

    private static double updateArmorModifiers(ServerPlayer player) {
        ManaData data = dataFor(player);
        boolean changed = false;
        for (int index = 0; index < ARMOR_SLOTS.length; index++) {
            changed |= data.updateArmor(index, player.getItemBySlot(ARMOR_SLOTS[index]));
        }
        if (changed) {
            double manaBonus = 0.0D;
            int regenerationLevel = 0;
            for (int index = 0; index < ARMOR_SLOTS.length; index++) {
                ItemStack stack = data.armor(index);
                Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
                if (equippable != null && equippable.slot() == ARMOR_SLOTS[index]) {
                    manaBonus += MageriteUpgrades.getManaBonus(stack);
                }
                if (MagicEnchantments.isCompatibleArmor(stack, ARMOR_SLOTS[index])) {
                    regenerationLevel += MagicEnchantments.getManaRegenLevel(stack);
                }
            }
            data.setArmorBonuses(manaBonus, ManaMath.regeneration(regenerationLevel) - ManaMath.BASE_REGENERATION);
        }
        AttributeInstance maximum = Objects.requireNonNull(player.getAttribute(ManaAttributes.MAX_MANA),
                "Player maximum mana attribute is missing");
        AttributeInstance regeneration = Objects.requireNonNull(player.getAttribute(ManaAttributes.MANA_REGENERATION),
                "Player mana regeneration attribute is missing");
        updateArmorModifier(maximum, ARMOR_MODIFIER_ID, data.armorManaBonus());
        updateArmorModifier(regeneration, ARMOR_REGENERATION_MODIFIER_ID, data.armorRegenerationBonus());
        return maximum.getValue();
    }

    private static void updateArmorModifier(AttributeInstance attribute, Identifier id, double bonus) {
        AttributeModifier current = attribute.getModifier(id);
        if (bonus == 0.0D) {
            if (current != null) {
                attribute.removeModifier(id);
            }
        } else if (current == null || current.amount() != bonus
                || current.operation() != AttributeModifier.Operation.ADD_VALUE
                || attribute.getPermanentModifiers().contains(current)) {
            attribute.removeModifier(id);
            attribute.addTransientModifier(new AttributeModifier(id, bonus,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void sync(ServerPlayer player, ManaSyncPayload payload) {
        ManaData data = dataFor(player);
        if (payload.equals(data.lastSynced()) || !isConnected(player)
                || !ServerPlayNetworking.canSend(player, ManaSyncPayload.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, payload);
        data.setLastSynced(payload);
    }

    private static boolean isConnected(ServerPlayer player) {
        return player.connection != null && !player.hasDisconnected() && !player.isRemoved();
    }

    private static ManaData dataFor(ServerPlayer player) {
        return ((ManaPlayer) player).celitest$getManaData();
    }

    private static void requireServerThread(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Mana must be accessed on the server thread");
        }
    }
}
