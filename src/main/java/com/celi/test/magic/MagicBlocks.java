package com.celi.test.magic;

import com.celi.test.CeliTest;
import com.celi.test.magic.altar.MagesAltarBlock;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.references.BlockItemId;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class MagicBlocks {
	public static final Block MAGERALD_ORE = register(
			"magerald_ore",
			Block::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.STONE)
					.sound(SoundType.STONE)
					.strength(3.0F, 3.0F)
					.requiresCorrectToolForDrops()
	);
	public static final Block MAGES_ALTAR = register(
			"mages_altar",
			MagesAltarBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.COLOR_LIGHT_BLUE)
					.sound(SoundType.STONE)
					.strength(4.0F, 8.0F)
					.requiresCorrectToolForDrops()
					.noOcclusion()
	);

	private MagicBlocks() {
	}

	private static Block register(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
		BlockItemId id = BlockItemId.create(CeliTest.id(path), CeliTest.id(path));
		Block block = factory.apply(properties.setId(id.block()));
		Registry.register(BuiltInRegistries.BLOCK, id.block(), block);
		BlockItem item = new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(id.item()));
		Registry.register(BuiltInRegistries.ITEM, id.item(), item);
		return block;
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS)
				.register(creativeTab -> creativeTab.accept(MAGERALD_ORE.asItem()));
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(creativeTab -> creativeTab.accept(MAGES_ALTAR.asItem()));
	}
}
