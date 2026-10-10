package com.celi.test.magic.mana;

import com.celi.test.CeliTest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record ManaSyncPayload(double current, double maximum) implements CustomPacketPayload {
    public static final Type<ManaSyncPayload> TYPE = new Type<>(CeliTest.id("mana_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ManaSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ManaSyncPayload::current,
            ByteBufCodecs.DOUBLE, ManaSyncPayload::maximum,
            ManaSyncPayload::new);

    @Override
    public Type<ManaSyncPayload> type() {
        return TYPE;
    }
}
