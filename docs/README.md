# Celi's Mage Chronicles Documentation

Welcome to the documentation for **Celi's Mage Chronicles**, a Fabric magic expansion for Minecraft 26.3.

## Player documentation

- [Getting started](getting-started.md) explains the required tools, development setup, and basic installation workflow.
- [Gameplay guide](gameplay.md) explains resources, mana, armor upgrades, affinities, staff abilities, and Ruby equipment.
- [Visual previews](visuals.md) collects the available texture and design references.

## Developer documentation

- [Systems and code map](architecture.md) describes the main packages, server/client split, and extension points.
- [Detailed magic-system specification](magic-system-spec.md) contains the longer design and implementation record.
- [Credits](credits.md) records the project authorship and contact details.

## Project conventions

- The mod id is `celitest`. Keep it stable for saved worlds and existing item identifiers.
- Common gameplay code belongs in `src/main/java`.
- Client-only rendering and UI code belongs in `src/client/java`.
- Runtime resources belong in `src/main/resources`.
- Do not place secrets, local saves, build output, or private configuration in the repository.
