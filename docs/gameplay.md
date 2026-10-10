# Gameplay Guide

## Resource progression

### Magerald

Magerald Ore provides Magerald Shards. Magerald Shards are used in the mod's magical crafting recipes, including affinity stones and Magerite Clusters.

### Refined Ruby and Magerite

Refined Ruby combines with Magerald Shards to create Magerite Clusters. Magerite Clusters are applied to compatible armor at the Mage's Altar.

Each armor piece can hold up to ten clusters. A cluster contributes 25 base maximum mana while the armor is equipped.

## Mana

Players begin with 50 maximum mana. Equipped armor contributes additional capacity, and the server recalculates the value when equipment changes.

Mana regenerates passively every ten game ticks. Mana Store and Mana Regen enchantments modify different parts of the system:

| Enchantment | Levels | Effect |
| --- | ---: | --- |
| Mana Store | I–III | Adds 15% per level to that armor piece's Magerite contribution |
| Mana Regen | I–V | Adds 50% per equipped level to regeneration |

Mana Store is calculated independently for each armor piece. Mana Regen counts only armor currently worn in the four armor slots.

## Mage's Altar

The Mage's Altar has two tabs:

- **Armor Upgrades** accepts compatible armor and Magerite Clusters.
- **Affinity** accepts one Fire, Water, Wind, or Earth Affinity Stone.

Selecting a new affinity replaces the previous one and consumes one matching stone. An active affinity is stored with the player and restored when the player rejoins or changes dimensions.

## Mage's Staff

The Mage's Staff uses the active affinity to select its two abilities. Left-click abilities launch quickly, while right-click abilities use the existing staff-raising channel.

| Affinity | Left-click | Cost / cooldown | Right-click | Cost / cooldown |
| --- | --- | --- | --- | --- |
| Fire | Fireball | 50 mana / 0.2 s | Inferno | 200 mana / 15 s |
| Water | Water Orb | 50 mana / 0.2 s | Thunderstorm | 200 mana / 45 s |
| Wind | Wind Burst | 50 mana / 0.2 s | Skyward Ascent | 200 mana / 15 s |
| Earth | Earth Shard | 50 mana / 1 s | Thornvine Prison | 200 mana / 15 s |

### Earth Shard

Earth Shard fires a fast rocky magical projectile toward the player's look direction. It travels up to 45 blocks, deals 3 hearts to a valid target, and creates earthy dust and stone-fragment particles during flight and impact.

### Thornvine Prison

Thornvine Prison checks for valid targets within 6 blocks when it activates. Captured targets are held for up to 10 seconds, take 1 heart once per second, and cause the caster to recover 25% of actual thorn damage dealt. If no target is captured, the cast grants no healing.

The effect uses temporary server-sent particles and does not place vine blocks or alter terrain. It ends safely when the target dies, unloads, changes validity, or the caster disconnects, dies, changes dimension, or changes affinity.

## Ruby equipment

Ruby tools and armor provide a high-tier equipment progression alongside the magical Magerite system. Ruby armor is compatible with Magerite upgrades and the mod's mana enchantments.

The Ruby Axe also supports its charged throwing attack and impact area damage system.
