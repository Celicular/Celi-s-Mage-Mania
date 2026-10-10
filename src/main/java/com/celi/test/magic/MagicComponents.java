package com.celi.test.magic;

import com.celi.test.CeliTest;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class MagicComponents {
	private static final Codec<Integer> CLUSTER_CODEC = Codec.intRange(0, MageriteUpgrades.MAX_CLUSTERS);
	public static final DataComponentType<Integer> CLUSTERS = Registry.register(
			BuiltInRegistries.DATA_COMPONENT_TYPE,
			CeliTest.id("magerite_clusters"),
			DataComponentType.<Integer>builder()
					.persistent(CLUSTER_CODEC)
					.networkSynchronized(ByteBufCodecs.fromCodec(CLUSTER_CODEC))
					.build()
	);

	private MagicComponents() {
	}

	public static void initialize() {
	}
}
