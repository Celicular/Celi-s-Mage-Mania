package com.celi.test.block;

import com.celi.test.CeliTest;
import net.minecraft.references.BlockItemId;
import net.minecraft.resources.Identifier;

public final class ModBlockItemIds {
	public static final BlockItemId RUBY_BLOCK = create("ruby_block");

	private ModBlockItemIds() {
	}

	private static BlockItemId create(String name) {
		Identifier id = CeliTest.id(name);
		return BlockItemId.create(id, id);
	}
}
