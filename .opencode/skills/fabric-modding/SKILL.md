---
name: fabric-modding
description: Fabric Minecraft mod development guidance based on Fabric
  Loader,   Fabric API, Fabric Loom, Yarn mappings, and official Fabric docs.
  Use when   working on Fabric mods, fabric.mod.json, ModInitializer,
  ClientModInitializer,   Fabric API events, registries, items, blocks, data
  generation, networking   payloads, mixins, access wideners, Loom Gradle setup,
  client/server   environment safety, or porting between Fabric and
  Forge/NeoForge.
---
# Fabric Modding

Use this skill to implement or review Minecraft mods targeting Fabric. Prefer the project's declared Minecraft, Fabric Loader, Fabric API, Loom, Yarn, and Java versions over generic examples. When behavior depends on a specific Minecraft version, verify against the matching Fabric docs and the project's generated sources before coding.

## First Checks

- Confirm versions from `gradle.properties`, `build.gradle`, `settings.gradle`, and `fabric.mod.json`.
- Confirm the mod id, Maven group, base package, entrypoints, mixin configs, and resource namespace all match.
- Expect the Gradle plugin `net.fabricmc.fabric-loom`; do not replace it with ForgeGradle, NeoGradle, or ModDevGradle.
- Use the Java version required by the target Minecraft version. Modern Fabric templates may target Java 21 or newer; older versions can differ.
- Prefer generated Gradle tasks: `./gradlew build`, `./gradlew runClient`, `./gradlew runServer`, `./gradlew runDatagen` when configured.
- Use Fabric/Yarn names. Do not mix in Forge/NeoForge imports such as `net.minecraftforge.*` or `net.neoforged.*`.

## Build Tooling

- Keep version values in `gradle.properties`: `minecraft_version`, `loader_version`, `loom_version`, `fabric_api_version`, `mod_version`, and `maven_group`.
- Declare dependencies with Loom configurations:
  - `minecraft "com.mojang:minecraft:${project.minecraft_version}"`
  - `mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"` when the project declares Yarn explicitly
  - `modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"`
  - `modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"`
- Use `modImplementation`, `modCompileOnly`, `modRuntimeOnly`, `include`, or `modApi` for mod dependencies so Loom can remap them.
- Use `loom { splitEnvironmentSourceSets() }` when the project has a separate client source set; bind both `main` and `client` source sets under `loom.mods`.
- Expand `${version}` or other metadata in `processResources`, but keep `fabric.mod.json` valid JSON after expansion.
- Publish or distribute the remapped production jar from `build/libs`, not an unremapped development artifact.

## Project Structure

- Put common Java code in `src/main/java`.
- Put client-only Java code in `src/client/java` when `splitEnvironmentSourceSets()` is enabled, or isolate it behind client entrypoints/classes when not.
- Put runtime resources in `src/main/resources`.
- Use `src/main/resources/assets/<modid>` for client assets and `src/main/resources/data/<modid>` for server data.
- Keep generated datagen output in a dedicated generated resources directory if the project is configured for datagen.
- Put `fabric.mod.json` at `src/main/resources/fabric.mod.json`.
- Use `accessWidener` in `fabric.mod.json` only when the project has a matching access widener file.

## Metadata And Entrypoints

- Use `fabric.mod.json` `schemaVersion: 1`, a lowercase mod id, version, name, authors, license, environment, entrypoints, mixins, and dependency ranges.
- Use `"environment": "*"` for mods that load on both physical client and dedicated server. Use `"client"` only for client-only mods.
- Register common initialization through a `main` entrypoint implementing `net.fabricmc.api.ModInitializer`.
- Register physical-client initialization through a `client` entrypoint implementing `net.fabricmc.api.ClientModInitializer`.
- Register datagen through a `fabric-datagen` entrypoint implementing `DataGeneratorEntrypoint` when the project uses Fabric datagen.
- Put client mixin configs in a separate config with `"environment": "client"` when they reference client classes.
- Keep dependency ids as Fabric ids: `fabricloader`, `minecraft`, `java`, and `fabric-api` when Fabric API is required.

Typical entrypoint shape:

```java
public final class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.initialize();
        ModBlocks.initialize();
    }
}
```

## Registries

- Use vanilla registries through `net.minecraft.registry.Registries` and `net.minecraft.registry.Registry`.
- Create identifiers with the API used by the target version, usually `Identifier.of(MOD_ID, "path")` or `Identifier.ofVanilla("path")` in newer Yarn versions.
- Register objects during mod initialization, before registry freeze.
- Keep registered objects as static final singleton instances.
- Keep registry paths lowercase snake_case.
- For items and blocks in newer versions, set the registry key on settings/properties when the target API requires it. Check project mappings before copying examples.

Typical item registration shape:

```java
public final class ModItems {
    public static final Item EXAMPLE_ITEM = register("example_item", new Item.Settings());

    private static Item register(String path, Item.Settings settings) {
        Identifier id = Identifier.of(ExampleMod.MOD_ID, path);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        Item item = new Item(settings.registryKey(key));
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void initialize() {
    }
}
```

## Items, Blocks, And Components

- Register blocks before matching block items.
- Register a `BlockItem` if a block should appear in inventories or be placeable from an item stack.
- Add items to creative tabs with Fabric API item group events such as `ItemGroupEvents.modifyEntriesEvent(...)`.
- Put models, blockstates, lang, textures, loot tables, recipes, and tags in resources or datagen providers, not hardcoded Java.
- Treat `ItemStack` as mutable. Copy stacks before mutating values that may be shared.
- Use data components for modern item state when available; avoid legacy NBT patterns unless targeting older Minecraft versions.

## Events

- Prefer Fabric API callback events over mixins when a callback exists.
- Register event handlers during common or client initialization, depending on side.
- Keep server-authoritative gameplay on server-side callbacks such as server tick, use-block/use-item, entity, command, or lifecycle events.
- Use `ServerLifecycleEvents`, `ServerTickEvents`, `PlayerBlockBreakEvents`, `UseItemCallback`, `UseBlockCallback`, `CommandRegistrationCallback`, and similar Fabric API events according to the behavior needed.
- Check callback return contracts carefully. `ActionResult`, `TypedActionResult`, and pass/fail/success semantics vary by event and Minecraft version.
- Use mixins only when no Fabric API hook or vanilla extension point covers the behavior.

## Client And Server Environments

- Use `ClientModInitializer` for key bindings, screens, renderers, model predicates, color providers, particles, and client networking handlers.
- Never load `net.minecraft.client.*` classes from common entrypoints or common static initializers.
- With split source sets, put client-only code under `src/client/java` and common code under `src/main/java`.
- Use `world.isClient()` for logical-side checks. Run authoritative gameplay only when it is `false`.
- Remember an integrated singleplayer game has both logical client and logical server inside a physical client.
- Always run `runServer` after touching common code to catch accidental client-only class loading.

## Networking

- Use Fabric API networking payload APIs that match the target Minecraft/Fabric API version.
- Prefer custom payload types/codecs in modern versions; use older `PacketByteBuf` channel APIs only in projects already targeting older Fabric APIs.
- Register serverbound receivers in common/server initialization and clientbound receivers in client initialization.
- Keep packet identifiers namespaced under the mod id.
- Validate all client-provided data on the server. Check chunk/block entity availability before acting on positions from a client packet.
- Execute gameplay mutations on the appropriate game thread through the networking context or server/client executor supplied by the API.
- Keep packet schemas stable and versioned when they cross multiplayer boundaries.

## Mixins And Access Wideners

- Prefer Fabric API events, registries, and vanilla extension points before adding mixins.
- Keep mixins narrow, named for the target class, and scoped to the smallest injection needed.
- Use `@Inject`, `@Redirect`, `@ModifyVariable`, or accessor/invoker mixins only when the target and failure mode are clear.
- Mark client-only mixins with a client-only mixin config in `fabric.mod.json`.
- Use access wideners for access changes that are stable and explicit; declare the access widener file in `fabric.mod.json`.
- Re-run client and server after mixin or access widener changes; failures often appear only at launch.

## Data Generation And Resources

- Use Fabric datagen for repetitive or version-sensitive JSON: models, blockstates, lang, tags, recipes, loot tables, advancements, and dynamic registry data.
- Add a `fabric-datagen` entrypoint and configure the Loom datagen task when the project does not already have one.
- Keep generated resources separate from hand-written resources unless the project has an established pattern.
- Verify generated JSON paths, namespaces, and pack formats against the target Minecraft version.
- Put shared tags under the correct conventional namespace when interoperability matters, commonly `c` tags in modern Fabric ecosystems.

## Porting Notes

- Fabric registration is direct `Registry.register`, not Forge/NeoForge `DeferredRegister`.
- Fabric entrypoints come from `fabric.mod.json`, not `@Mod` annotations.
- Fabric events are callback objects, not Forge event bus subscribers.
- Fabric networking APIs differ from Forge `SimpleChannel` and modern NeoForge payload registration; translate concepts, not method names.
- Yarn names and Mojang/Forge names differ. Confirm mapping names in the target project before porting examples.
- Fabric API is modular but usually consumed as `fabric-api`; a loader-only mod may not have Fabric API callbacks available.

## Validation Checklist
- Do not run these commands without asking the user explicitly.
- Run `./gradlew build` after code or metadata changes.
- Run `./gradlew runClient` for assets, models, screens, rendering, key bindings, particles, client events, and client networking.
- Run `./gradlew runServer` for common initialization, registries, networking, commands, resources, mixins, and classloading safety.
- Run `./gradlew runDatagen` after datagen changes and inspect generated output.
- Check logs for missing models/textures/translations, registry errors, invalid `fabric.mod.json`, mixin apply failures, access widener errors, packet registration errors, and client-only class loading on server.

## Official References

- Fabric docs: `https://docs.fabricmc.net/`
- Fabric developer versions: `https://fabricmc.net/develop/`
- Fabric example mod: `https://github.com/FabricMC/fabric-example-mod`
- Fabric Loader: `https://github.com/FabricMC/fabric-loader`
- Fabric Loom: `https://github.com/FabricMC/fabric-loom`
- Fabric API: `https://github.com/FabricMC/fabric`
- Yarn mappings: `https://github.com/FabricMC/yarn`
