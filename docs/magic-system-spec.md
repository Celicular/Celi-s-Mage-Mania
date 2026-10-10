# Celi's Mage Chronicles: Magic System Specification

This is the detailed design and implementation record for Celi's Mage Chronicles. Later revision sections supersede earlier scope notes when the project expands beyond the first iteration.

## 1. Magerald Ore & Resource Progression

Introduce a new ore called **Magerald Ore**, featuring a distinctive light-blue, magical appearance that fits naturally into Minecraft's visual style.

- **Magerald Ore:** A new mineable ore with a light-blue texture and subtle magical detailing.
- **Magerald Shards:** Mining Magerald Ore drops 2–3 Magerald Shards by default.
- **Silk Touch:** Allows players to collect the original Magerald Ore block instead of its usual drops.
- **Fortune:** Fortune enchantments increase the potential shard yield according to standard Minecraft Fortune mechanics.
- **Magerite Cluster:** Crafting 8 Magerald Shards with 1 Refined Ruby in a crafting table produces 1 Magerite Cluster.

### Visual design

- Magerald Ore should have a light-blue, crystalline appearance that distinguishes it from existing Minecraft ores.
- Magerald Shards should resemble small, luminous light-blue crystal fragments.
- Magerite Clusters should combine the light-blue energy of Magerald with the rich ruby-red color of Refined Ruby, creating a distinctive, premium-looking magical crystal.

## 2. New Mana Stat & Mana Bar

Introduce a completely new player stat called **Mana**, which will serve as the foundation for future magic-related mechanics.

- Add a dedicated Mana Bar directly above the existing hunger bar.
- Display the player's current mana and maximum mana numerically alongside the bar, for example, `50 / 50 Mana`.
- Ensure the bar updates dynamically whenever the player's mana capacity or current mana changes.
- The initial base mana capacity should be **50 points**.
- Implement mana as a proper player attribute or stat system that can be extended later with spell costs, regeneration, abilities, and other magical mechanics.

For now, implement the mana stat, its visual indicator, and its interaction with Magerite Cluster armor upgrades. **Do not implement spells or active magic abilities yet.**

## 3. Magerite Cluster Armor Upgrades

Allow players to attach Magerite Clusters to armor pieces using the new **Mage's Altar** block.

- Magerite Clusters can be applied to all armor tiers, including Leather, Chainmail, Iron, Gold, Diamond, Netherite, and any custom Ruby armor included in the mod.
- Each armor piece can hold a maximum of **10 Magerite Clusters**.
- Each attached Magerite Cluster increases the player's maximum mana capacity by **25 points**.
- The mana bonus stacks across all four equipped armor pieces.

### Mana progression

| Armor configuration | Bonus mana | Total capacity |
|---|---:|---:|
| Base player | +0 | 50 |
| 1 cluster | +25 | 75 |
| 10 clusters on one piece | +250 | 300 |
| 10 clusters on two pieces | +500 | 550 |
| 10 clusters on three pieces | +750 | 800 |
| 10 clusters on all four pieces | +1,000 | 1,050 |

The base mana capacity is 50, with additional capacity gained from equipped armor upgrades.

Calculate maximum mana dynamically using the base capacity plus the bonuses from currently equipped armor. When upgraded armor is equipped, removed, or replaced, recalculate the maximum mana correctly. Handle cases where current mana exceeds the newly reduced maximum without introducing bugs or inconsistent player state.

## 4. Mage's Altar: Crafting & Functionality

Introduce a new functional block called the **Mage's Altar**, which serves as the dedicated workstation for upgrading armor with Magerite Clusters.

### Crafting recipe

Craft the Mage's Altar using:

- **6 Magerald** (the Magerald resource used by the mod).
- **3 Refined Rubies**.

Use a crafting-table recipe that consumes all nine ingredients to produce one Mage's Altar. Arrange the ingredients in a logical, symmetrical pattern that reflects the altar's magical purpose.

### Visual design

The Mage's Altar should look like an ancient magical workstation, combining:

- Light-blue crystalline Magerald elements.
- Deep ruby-red inlays and accents.
- A sturdy stone or dark metallic base.
- Gold ornamental detailing and engraved magical patterns.
- A central crystal or energy focal point that communicates its upgrade functionality.

The block should fit naturally into Minecraft's block-based visual style while looking sufficiently elaborate to feel like a specialized magical workstation.

### Upgrade functionality

Implement an interactive interface for the Mage's Altar that allows players to insert armor pieces and Magerite Clusters to apply upgrades.

- Provide an armor input slot and a Magerite Cluster input slot.
- Display the selected armor piece and its current number of attached clusters.
- Show the mana bonus that will be granted by the next upgrade.
- Clearly display the remaining upgrade capacity, such as `6 / 10 Clusters`.
- Allow players to apply one cluster at a time, consuming one cluster per successful upgrade.
- Update the armor's stored upgrade count, tooltip information, and visual appearance immediately after each successful upgrade.
- Prevent upgrades when the armor has already reached the 10-cluster limit.
- Prevent invalid items from being upgraded.
- Ensure the interface accurately displays the resulting mana bonus before the player commits to an upgrade.
- Return the upgraded armor safely to the player through the interface, without losing items when slots are changed or the interface is closed.

The altar should be the primary and intended method for applying Magerite Cluster upgrades. Do not require an anvil for this process.

Ensure armor upgrade data persists correctly through saving, loading, relogging, and world transitions. The system must not duplicate upgrades or mana bonuses when armor is moved between inventories or equipment slots.

## 5. Detailed Tooltips & Upgrade Information

Implement clear, polished tooltips for all relevant items and upgraded armor pieces.

- **Magerald Ore:** Explain its role as the source of Magerald Shards.
- **Magerald Shard:** Explain its role in crafting Magerite Clusters.
- **Magerite Cluster:** Display its purpose as an armor mana upgrade and its crafting ingredients.
- **Mage's Altar:** Explain that it is used to attach Magerite Clusters to armor.
- **Upgraded Armor:** Show the number of attached clusters, the maximum capacity of 10, the total mana bonus provided by that piece, and its upgrade contribution.

Example upgraded armor tooltip:

`Magerite Upgrades: 6 / 10`

`Bonus Mana: +150`

`Each cluster grants +25 maximum mana.`

Tooltips should be consistent, easy to read, and visually appropriate for a magical progression system. Use Minecraft's familiar tooltip conventions with subtle color accents where suitable.

## 6. Progressive Armor Visual Trims

Create a visual system that makes Magerite Cluster upgrades visible directly on armor models.

- Each armor piece should visibly display the Magerite Clusters attached to it.
- The visual appearance must progressively evolve with every upgrade level, from 1 through 10.
- At low upgrade levels, show a small number of distinct light-blue and ruby-red magical crystals or embedded accents.
- At higher levels, progressively add more crystals, decorative inlays, glowing details, and intricate magical patterns.
- Fully upgraded armor should look significantly more elaborate than unupgraded armor while remaining recognizable as its original armor tier.
- Support every compatible armor material, including custom Ruby armor.
- Ensure the visuals work consistently across supported armor models and rendering systems.

The number and placement of visible upgrades must accurately represent the number of clusters attached to each individual armor piece. Do not show the same fully upgraded appearance at every upgrade level.

The final design should feel like a native Minecraft armor enhancement system, rather than a completely separate armor set.

## 7. Mage's Staff: New Item & 3D Model

Introduce a new item called the **Mage's Staff**, designed as a majestic, premium magical weapon that will eventually serve as the player's primary tool for casting spells.

For this phase, implement the item, registration, inventory appearance, custom 3D model, textures, and rendering. Actual spellcasting functionality will be added later.

### Visual direction

The staff should feel ancient, powerful, elegant, and magical.

- **Primary colors:** Gold, light blue, and the distinctive ruby-red and light-blue combination associated with Magerite.
- **Silhouette:** A tall, elegant staff with a carefully shaped shaft, ornate headpiece, and a prominent magical crystal at its top.
- **Headpiece:** An elaborate gold frame surrounding a luminous light-blue Magerald crystal or a mixed ruby-red and light-blue Magerite centerpiece.
- **Detailing:** Intricate golden engravings, decorative metalwork, crystal inlays, fine ornamental rings, and other details that communicate craftsmanship and magical power.
- **Material contrast:** Combine rich metallic gold with crystalline surfaces, subtle highlights, and darker accent materials to create depth.
- **Overall aesthetic:** Majestic and visually striking without becoming excessively cluttered. The design should feel believable within Minecraft's blocky, stylized world.

### Model and texture requirements

- Create a proper custom 3D model rather than relying solely on a flat item texture.
- Design cohesive textures for the shaft, gold components, and magical crystals.
- Ensure the staff looks attractive in first-person view, third-person view, inventory slots, and item displays.
- Use appropriate model geometry and UV mapping, paying attention to proportions and visual readability at Minecraft's normal rendering scale.
- Where supported by the existing modding setup, prepare the model for future animation and magical visual effects.

The Mage's Staff should be one of the mod's signature items, with a recognizable silhouette and a visual identity consistent with the Magerald and Magerite resource progression.

## 8. Implementation Requirements

Implement these features within the existing mod project, following its current architecture, Minecraft version, mod loader, and coding conventions.

- Register all new blocks, items, recipes, attributes, and related components correctly.
- Implement the mana system in a modular way so future spellcasting mechanics can integrate cleanly.
- Ensure armor upgrades persist correctly and that mana bonuses are calculated from equipped armor only.
- Prevent duplicated bonuses, invalid upgrades, and exceeding the 10-cluster limit per armor piece.
- Implement the Mage's Altar interface with reliable item handling and upgrade feedback.
- Keep client-side rendering and server-side gameplay logic consistent, including multiplayer behavior.
- Add all necessary textures, models, item definitions, recipes, and resource files.
- Organize the code and assets cleanly to support future expansion.

### Scope restriction

This phase focuses exclusively on:

1. Magerald Ore, Magerald Shards, and Magerite Clusters.
2. Crafting recipes and resource progression.
3. The Mana stat and Mana Bar.
4. Armor upgrades through the Mage's Altar.
5. Armor tooltips and progressive visual trims.
6. The Mage's Altar block, interface, recipe, and functionality.
7. The Mage's Staff item, custom 3D model, textures, and rendering.

**Do not implement spells, mana-consuming abilities, or other active magic functionality yet.**

The goal is to establish a polished foundation for a larger magical progression system, with cohesive visuals, meaningful resource progression, clear upgrade feedback, and a distinctive Mage's Staff ready for future expansion.

## First-iteration decisions

- Magerite Cluster recipe: exactly 8 Magerald Shards surrounding 1 Refined Ruby.
- The altar's six Magerald ingredients mean six Magerald Shards; no separate Magerald item.
- No natural ore generation settings were specified; register the mineable ore and creative access without choosing a distribution.
- Initial mana is 50. Increased capacity does not refill current mana. Reduced capacity clamps current mana.
- No mana regeneration, spells, active abilities, or staff casting.
- User handles builds and in-game tests. Static integration review is not a runtime pass.

## Revised armor visual direction

This revision supersedes the isolated crystal studs and gold-bordered armor panels described in the initial visual design.

- Magerite should appear as branching cracks and mineral veins embedded in the original armor material.
- Use dark recessed fracture edges, predominantly light-blue filling, sparse ruby-red pockets, and restrained highlights.
- Each upgrade level from 1 through 10 extends the existing vein network with additional length and branches; the pattern reflects that individual armor piece's stored level.
- Preserve most of the original armor surface. Remove gold rectangular borders and regular rows of mounted crystals.
- Maintain distinct helmet, chestplate, leggings, and boots layouts, including adult and baby equipment UVs.
- Higher levels may reveal small emissive vein facets, not a uniformly glowing armor surface.
- Mana, upgrade counts, altar behavior, and the original armor material remain unchanged.

The texture contact sheet at `docs/previews/magerite-veins.png` shows chest-front levels 1, 5, and 10 on neutral swatches; it is not an in-game render.

## 9. Passive mana regeneration extension

This extension supersedes the first iteration's exclusion of regeneration and adds the two enchantments below. Spells, mana-consuming abilities, and staff casting remain out of scope.

- Living, connected players recover **10 mana per 10 game ticks** before enchantment bonuses.
- Recovery pauses at full mana and resumes when current mana is below the effective maximum.
- Frozen or paused game time and offline time do not generate recovery. Regeneration progress is session-local, not persisted as elapsed wall time.
- Server logic alone awards mana and clamps it to maximum. The client only displays authoritative current/maximum values.
- Armor bonuses are recomputed from copied equipment/component snapshots when equipment changes. Transient modifiers are replaced by stable IDs, not accumulated.
- End-of-tick synchronization coalesces changes into at most one custom mana payload per player per server tick. Unchanged state does not send another payload, except required lifecycle resynchronization.
- `celitest:mana_regeneration` is a player attribute measured in mana per 10 ticks, allowing future independent modifiers without adding active mechanics now.

## 10. Mana Store and Mana Regen

Both enchantments support Leather, Chainmail, Iron, Gold, Diamond, Netherite, and Ruby armor. They can coexist with one another and vanilla armor enchantments. They use the normal vanilla non-treasure acquisition pool, including enchanting tables, enchanted books, and anvil application; no custom acquisition workstation was added.

### Mana Store I-III

`Armor contribution = clusters × 25 × (1 + 0.15 × Mana Store level)`

Each piece is calculated independently from its original cluster contribution. Player base mana stays **50**, and an armor piece with no clusters gains no capacity from Mana Store.

| Clusters | No enchantment | Store I | Store II | Store III |
|---:|---:|---:|---:|---:|
| 4 | 100 | 115 | 130 | 145 |
| 10 | 250 | 287.5 | 325 | 362.5 |

Four fully upgraded Store III pieces yield **1,500 maximum mana**, including the player's base 50. Raising capacity does not immediately refill current mana; lowering it clamps current mana.

### Mana Regen I-V

`Mana per 10 ticks = 10 × (1 + 0.50 × total equipped Mana Regen levels)`

Only compatible armor in the four actual equipment slots contributes. An inventory item does not contribute, and regeneration enchantments work even without cluster upgrades. Mana Regen does not affect capacity.

| Equipped enchantments | Combined bonus | Mana per 10 ticks |
|---|---:|---:|
| None | 0% | 10 |
| One I | 50% | 15 |
| One V | 250% | 35 |
| Four I | 200% | 30 |
| Four II | 400% | 50 |
| Four III | 600% | 70 |
| Four IV | 800% | 90 |
| Four V | 1,000% | 110 |

## 11. Integration and verification status

- Mana values remain doubles internally. HUD, armor tooltips, and altar previews share formatting with at most two decimal places, no trailing zeros, and no mutation of internal values for display.
- Tooltips show base cluster mana, Mana Store level/percentage, final armor contribution, and Mana Regen level/percentage. Regeneration tooltips also show the local player's currently equipped total and resulting enchantment-based rate.
- Altar current/next previews include Mana Store. An upgrade changes only the existing cluster component; enchantments, durability, custom names, trims, and other armor properties are not replaced.
- Existing cluster-count-based armor rendering remains unchanged.
- Current mana still persists through the existing player save data. Cluster and enchantment data remain in their existing ItemStack components.
- `python -B "scripts/check_mana_resources.py"` passed **12 static checks**, covering resource definitions, Ruby armor tags, acquisition tags, translations, project imports, and common/client import separation.
- `tests/java/com/celi/test/magic/mana/ManaMathRegression.java` contains assertions against the actual shared calculation methods for all requested examples, additive stacking, non-compounding calculations, fractional clamping, and formatting. It has **not been compiled or run**.
- No Gradle build, compilation, client/server launch, or datagen was run. Live registry decoding, mixin application, multiplayer timing, saving/relogging, enchanting/anvil behavior, and in-game UI layout still require validation with explicit user permission.

## 12. Staff mana costs and Thunderstorm revision

This revision supersedes the earlier exclusions of active casting and mana-consuming spells.

- All affinities share the same costs: left-click spells cost **50 mana**, and right-click spells cost **200 mana**.
- The server validates and deducts mana exactly once when accepting a cast. Insufficient mana does not begin a channel, launch a projectile, or start a cooldown. Interrupted accepted channels do not refund mana or their cooldown.
- Mana still regenerates through the existing system and synchronizes through its existing coalesced mana payload. Base capacity remains **50**; right-click spells require capacity upgrades and at least 200 current mana.
- Water's Thunderstorm fades rain and thunder in over its **40-tick / 2-second channel**, then holds the storm for **200 ticks / 10 seconds**. Expiry or interruption fades back to the original weather over **40 ticks / 2 seconds**.
- Overlapping weather leases share the highest active intensity. A new channel during a fade starts from the current intensity rather than snapping to clear skies. Original weather flags, timers, and rain/thunder interpolation are restored; temporary spell weather is not persisted during saves or shutdown.
- At channel completion, Thunderstorm strikes eligible living entities in a **10-block sphere around the caster**, including hostile and passive mobs. Each target receives **16 damage / 8 hearts before normal damage mitigation** and **100 ticks / 5 seconds of Nausea** on a successful hit. The caster is excluded. Existing ally, owned-pet, and PvP protections remain active.
- Lightning is visual-only on the server; spell damage is applied once through the existing staff combat helper, without extra vanilla lightning damage or block ignition.
- Stone and staff tooltips display mana costs and updated Thunderstorm information. Rejection feedback now retains the server's specific reason instead of replacing it with a generic client error.
- `python -B -m unittest discover -s "tests" -p "test_*.py" -v` passed **27 checks** covering resource/source contracts and the easing expressions extracted from Java. These checks are not Java compilation or an in-game regression pass.

In-game follow-up: test exact and insufficient mana balances, interrupted channels, hostile and passive mobs just inside/outside the radius, overlapping casters, original clear/rain/thunder weather, and saving or changing dimensions during the transition. No Gradle build, compilation, Minecraft launch, or datagen was run for this revision.
