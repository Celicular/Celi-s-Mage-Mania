package com.celi.test.item;

import com.celi.test.CeliTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

public final class ModToolMaterials {
	public static final TagKey<Item> RUBY_TOOL_MATERIALS = TagKey.create(Registries.ITEM, CeliTest.id("ruby_tool_materials"));
	public static final ToolMaterial RUBY = new ToolMaterial(
			BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
			2500,
			9.0F,
			4.0F,
			18,
			RUBY_TOOL_MATERIALS
	);

	private ModToolMaterials() {
	}
}
