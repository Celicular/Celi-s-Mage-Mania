# Getting Started

## Playing the mod

Celi's Mage Chronicles targets Minecraft 26.3 on Fabric.

### Required runtime

- Minecraft 26.3
- Fabric Loader 0.19.5 or newer
- A Fabric API release compatible with Minecraft 26.3
- Java 25 or newer

Install Fabric Loader and Fabric API first, then place the released mod jar in the profile's `mods` folder. The mod keeps the `celitest` namespace even though its player-facing name is Celi's Mage Chronicles.

## Setting up the development workspace

1. Install Java 25 and make sure `java --version` reports the expected version.
2. Clone or download this repository.
3. Open the project as a Gradle project in your IDE.
4. Let Gradle import the dependencies declared in `gradle.properties` and `build.gradle`.
5. Use the generated run configurations or wrapper tasks for development.

### Build commands

Windows PowerShell:

```powershell
\.\gradlew.bat build
```

macOS or Linux:

```bash
./gradlew build
```

The production jar is written to `build/libs/`.

### Development runs

Use these only when you want to launch a local test instance:

```text
./gradlew runClient
./gradlew runServer
```

The project also contains data-generation support. Check the current Gradle/Loom configuration before running a datagen task because generated output should match the active Minecraft version.

## Before opening a pull request

- Run the repository's Python static checks.
- Inspect resource paths and translation keys.
- Build the mod with the Gradle wrapper.
- Test both a client and a dedicated server when common code changes.
- Test saving, relogging, dimension changes, and multiplayer behavior for persistent player systems.
- Keep generated build output and local `run/` data out of commits.
