package com.celi.test.magic.affinity;

import com.celi.test.CeliTest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record AffinitySyncPayload(String affinityId) implements CustomPacketPayload {
    public static final Type<AffinitySyncPayload> TYPE = new Type<>(CeliTest.id("affinity_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AffinitySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, AffinitySyncPayload::affinityId,
            AffinitySyncPayload::new);

    @Override
    public Type<AffinitySyncPayload> type() {
        return TYPE;
    }
}
