package com.celi.test.magic.spell;

import com.celi.test.CeliTest;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CastFeedbackPayload(String abilityId, int cooldownTicks, int castTicks, boolean accepted)
        implements CustomPacketPayload {
    public static final Type<CastFeedbackPayload> TYPE = new Type<>(CeliTest.id("cast_feedback"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastFeedbackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), CastFeedbackPayload::abilityId,
            ByteBufCodecs.VAR_INT, CastFeedbackPayload::cooldownTicks,
            ByteBufCodecs.VAR_INT, CastFeedbackPayload::castTicks,
            ByteBufCodecs.BOOL, CastFeedbackPayload::accepted,
            CastFeedbackPayload::new);

    @Override
    public Type<CastFeedbackPayload> type() {
        return TYPE;
    }
}
