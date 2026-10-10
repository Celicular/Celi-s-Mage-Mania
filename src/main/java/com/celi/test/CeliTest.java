package com.celi.test;

import com.celi.test.block.ModBlocks;
import com.celi.test.entity.ModEntities;
import com.celi.test.gameplay.RubyGameplay;
import com.celi.test.item.ModItems;
import com.celi.test.magic.MagicBlocks;
import com.celi.test.magic.MagicComponents;
import com.celi.test.magic.MagicItems;
import com.celi.test.magic.altar.MagicMenus;
import com.celi.test.magic.affinity.AffinityPassives;
import com.celi.test.magic.affinity.AffinitySystem;
import com.celi.test.magic.mana.ManaSystem;
import com.celi.test.magic.spell.FireSpells;
import com.celi.test.magic.spell.StaffCasting;
import com.celi.test.magic.spell.StaffProjectiles;
import com.celi.test.magic.spell.ThunderstormSpells;
import com.celi.test.magic.spell.WindSpells;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CeliTest implements ModInitializer {
	public static final String MOD_ID = "celitest";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.initialize();
		ModBlocks.initialize();
		ModEntities.initialize();
		RubyGameplay.initialize();
		MagicComponents.initialize();
		MagicItems.initialize();
		MagicBlocks.initialize();
		MagicMenus.initialize();
		FireSpells.initialize();
		ManaSystem.initialize();
		AffinitySystem.initialize();
		AffinityPassives.initialize();
		StaffProjectiles.initialize();
		WindSpells.initialize();
		ThunderstormSpells.initialize();
		StaffCasting.initialize();
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
