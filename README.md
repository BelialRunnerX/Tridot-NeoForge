# Tridot — NeoForge 1.21.1 port

> 🇷🇺 Русская версия документации: [README.ru.md](README.ru.md) · [PORTING.ru.md](PORTING.ru.md) · [CHANGELOG.ru.md](CHANGELOG.ru.md) · [KNOWN_ISSUES.ru.md](KNOWN_ISSUES.ru.md). The English documentation is the authoritative version.

This repository is a **community port of [Tridot](https://github.com/KomaruPRO/Tridot)** (by IriDark / the Komaru org) from Forge 1.20.1 to **NeoForge 1.21.1**.

## Please read before opening an issue

- This port was made **with the permission of the original author**.
- **Do not contact the original author with concerns about this port.** The maintainer of *this* repository is responsible for it. Bugs, crashes, or questions about the NeoForge 1.21.1 build belong in this repository's issue tracker, not in the upstream Tridot repository.
- You may use this port on the same terms as the original mod: whatever **licensing and permissions the original Tridot requires still apply here**. The repository ships the original `LICENSE` file unchanged (GNU GPL). If you redistribute or build on this port, you must keep complying with that license and with any conditions the original author has set.

## Status

**Working build, not fully validated.** Together with the Valoria port, the library loads, passes resource reload and shader compilation, and runs in a live world (client and dedicated server). Individual features (music modifiers, text effects, post-processing, percent armour, skins) still need targeted play-testing — see [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## What changed

Every non-trivial rewrite (registration, events, networking, data components, rendering, mixins) is documented in [PORTING.md](PORTING.md), and each code site carries a `// PORT NOTE:` comment explaining the change. The library API was ported in full — nothing was stubbed out or removed.

Fixes made after the initial port are listed by commit in **[CHANGELOG.md](CHANGELOG.md)**; open, inherited and not-yet-verified problems are tracked in **[KNOWN_ISSUES.md](KNOWN_ISSUES.md)**. Please check that list before opening an issue.

Notable things dependent mods need to know:

- Enchantments are data-driven in 1.21. Tridot's enchantments now apply through item tags (`tridot:enchantable/dash_weapon`, `radius_weapon`, `overdrive`, `shield`) — tag your items.
- Capabilities became NeoForge data attachments; `INBTSerializable` now receives a `HolderLookup.Provider`.
- `SimpleChannel` became `CustomPacketPayload` + `StreamCodec` (`PacketHandler.addRegistration` still lets other mods register on Tridot's channel).
- Attribute modifier ids are `ResourceLocation`s; `Tridot.BASE_PROJECTILE_DAMAGE_ID` replaces the old UUID.

## For developers

Build locally and publish to your Maven local repository:

```bash
./gradlew build publishToMavenLocal
```

Then in your mod's `build.gradle`:

```groovy
repositories {
    mavenLocal()
}

dependencies {
    implementation "pro.komaru:Tridot:1.21.1-1.0.169"
    // or, to compile against the API only:
    compileOnly "pro.komaru:Tridot:1.21.1-1.0.169:api"
}
```

### Requirements and tested versions

| Component | Version | Where to get it |
|---|---|---|
| Minecraft | 1.21.1 | — |
| Java | 21 (Temurin 21.0.12 used) | https://adoptium.net/ |
| **NeoForge** | **21.1.251** (accepted range `[21.1,)`) | https://neoforged.net/ · https://projects.neoforged.net/neoforged/neoforge |
| Curios API (optional, range `[9,)`) | 9.5.1+1.21.1 | https://modrinth.com/mod/curios · https://www.curseforge.com/minecraft/mc-mods/curios |
| Iris (optional, replaces Oculus) | 1.8.12+1.21.1-neoforge | https://modrinth.com/mod/iris |

Build tooling: Gradle 8.14.5, ModDevGradle 2.0.147, Parchment 2024.11.17 (see `gradle.properties`).

## What Tridot offers

> **Tridot** offers utilities in almost every way needed for a developer;
> - Simplifying your calculations and data storing with custom structures
> - Providing useful rendering methods for "le beauty"
> - Fixing the modding experience for more robust additions
> ...and more!

### Screenshakes, even as an earthquake
> Uses trigonometry to make it natural and nice-looking
### Component `Style` replacement
> `DotStyle` adds new `DotStyleEffect` class which helps to modify text rendering
- Custom character effects, and has built-in ones.
### `SplashHandler` for adding custom splashes
> Add new customizable splashes to title screen using this class
- Language-specific splashes
- Translatable splashes
- Weight for controlling chance of a splash appear
### `Item` and `Armor` skins system
> Change Items' and Armor' models with a modifiable skin system
### Music Modifiers
> Changes music depending on some factors
- Biome-specific music
- Dungeon(structure)-specific music
### Attribute names modifiers
### Armor with percent-based protection
> Adds a new defense property for armor, which reduces taken damage in percents
- Removes vanilla 80% cap so armor can provide up to 100% protection
### Flexible, builder-based systems
> Robust modding with new systems will be even easier
- Armor Builders
- Command Builder
- Particle Builders
- Rendering Builders
- - cubes, beams, vertices, etc.

---

Original project: https://github.com/KomaruPRO/Tridot — all credit for Tridot itself goes to its authors (IriDark, Auriny, Skoow).
