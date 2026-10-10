package com.celi.test.magic.spell;

import com.celi.test.CeliTest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StaffCastPayload(boolean secondary) implements CustomPacketPayload {
    public static final Type<StaffCastPayload> TYPE = new Type<>(CeliTest.id("staff_cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StaffCastPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffCastPayload::secondary,
            StaffCastPayload::new);

    @Override
    public Type<StaffCastPayload> type() {
        return TYPE;
    }
}
