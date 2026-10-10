# Systems and Code Map

## Project layout

```text
src/main/java/                 Common and server-authoritative code
src/client/java/               Client UI, HUD, tooltips, and rendering
src/main/resources/            Fabric metadata, data, models, textures, and language
tests/                         Static Python checks and focused Java regression sources
docs/                          Project documentation and visual references
```

## Main systems

### Mod bootstrap

`com.celi.test.CeliTest` initializes items, blocks, entities, mana, affinities, altar menus, staff casting, projectiles, and spell systems.

### Affinity system

- `magic/affinity/Affinity.java` defines the available affinities.
- `magic/affinity/AffinityData.java` stores the selected affinity and per-ability cooldowns.
- `magic/affinity/AffinitySystem.java` validates and synchronizes affinity changes.
- `magic/affinity/AffinityPassives.java` owns passive affinity effects.
- `magic/affinity/StaffAbility.java` maps one primary and one secondary ability to each affinity.

Affinity changes are server-authoritative. The player has only one active affinity, and changing affinity cancels active staff channels and Earth prison effects.

### Mana system

- `magic/mana/ManaSystem.java` owns server-side mana, regeneration, equipment refreshes, and synchronization.
- `magic/mana/ManaData.java` stores current mana, equipment snapshots, and sync state.
- `magic/mana/ManaAttributes.java` registers maximum-mana and regeneration attributes.
- `magic/mana/ManaMath.java` contains shared, side-effect-free calculations.
- `mixin/ServerPlayerManaMixin.java` persists current mana with the player.

### Magerite armor

- `magic/MageriteUpgrades.java` validates armor and updates the cluster component.
- `magic/MagicComponents.java` registers the stored cluster component.
- `magic/altar/MagesAltarMenu.java` validates input slots and applies one upgrade at a time.
- `client/magic/MageriteArmorRendering.java` renders the progressive vein overlays.

### Staff casting

- `magic/spell/StaffCasting.java` validates input, checks the active ability, spends mana once, starts cooldowns, and controls the channel lifecycle.
- `magic/spell/StaffProjectiles.java` manages the server-side staff projectile simulation.
- `magic/spell/StaffCombat.java` centralizes valid-target checks, damage, and safe knockback.
- `magic/spell/ThornvinePrison.java` owns Earth capture, immobilization, thorn damage, lifesteal, cleanup, and temporary particles.

### Client integration

- `client/magic/ManaHud.java` displays authoritative mana values.
- `client/magic/MagicTooltips.java` adds system-specific item descriptions.
- `client/magic/MagesAltarScreen.java` displays altar tabs, affinity descriptions, and upgrade previews.
- `client/magic/StaffClient.java` handles client input and cast feedback without becoming authoritative for gameplay.

## Extension guidelines

- Keep gameplay decisions on the server thread.
- Use existing mana and cooldown APIs rather than creating parallel state.
- Add new affinities to the enum, ability mapping, altar stone mapping, client translation keys, and resource checks together.
- Keep temporary visuals bounded and avoid world-block mutation for spell effects.
- Preserve stable item ids, component ids, and saved-data keys once worlds can contain them.
- Do not import `net.minecraft.client` or `com.celi.test.client` from common sources.
