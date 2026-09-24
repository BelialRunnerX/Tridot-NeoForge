# Changelog — Tridot NeoForge 1.21.1 port

All notable changes to the port are listed here, newest first. The mod version stays `1.21.1-1.0.169` (upstream Tridot 1.0.169); entries are identified by date and commit. Every code-level change also carries a `// PORT NOTE:` comment and is described in [PORTING.md](PORTING.md). Open problems live in [KNOWN_ISSUES.md](KNOWN_ISSUES.md).

## 2026-09-23 — fixes from the first in-game play test

- `7e92bd1` **Fixed** client crash "Rendering screen … `particle.sprite` is null" when a GUI particle was drawn (first hit when opening Valoria's Codex, whose entries show items with wisp particles). `TridotLibClient.registerParticleFactory` ran before `TridotParticles` had registered the world particles, so every screen-particle factory captured a null sprite set. The listener now runs at `LOWEST` priority, `TridotScreenParticleType.Factory` resolves its sprite set lazily by id, and a particle without a sprite is skipped instead of crashing.
- `7e92bd1` **Fixed** `NullPointerException` in `AttributeUtilMixin` while JEI indexed curio tooltips: Curios 9 passes its slot attributes as `Holder.direct(...)` (no registry key) and the mixin copied them into `AttributeUtil.sortedMap()`, whose comparator sorts by key. The rebuilt map now keeps the shape of the incoming one (`LinkedHashMultimap` unless the input was already sorted).

## 2026-09-23 — first client/server test fixes

- `c957952` **Fixed** server tick crash "Unable to calculate boundingbox without pieces": `MusicModifier.DungeonMusic.isPlayerInStructure` now returns false when the player is outside a matching structure instead of asking `StructureStart.INVALID_START` for a bounding box.
- `a60f926` **Changed** targeted loot conditions (`tridot:mob_effect`) to accept the 1.20.1 target names `killer`, `direct_killer`, `killer_player` as aliases of the 1.21 names `attacker`, `direct_attacker`, `attacking_player`, so existing data packs keep loading.
- `fb7f1d1` **Fixed** all Tridot core shaders failing to compile on 1.21.1 (`fog_distance` lost its matrix parameter), which left the game stuck on the Mojang loading screen. `include/common.glsl` keeps the old `fogDistance(mat4, vec3, int)` wrapper for dependents.
- `774c83c` **Fixed** "Side-loaded models must use the 'standalone' variant" on first launch: skin, bow, crossbow and `_in_hand` models are registered and looked up with `ModelResourceLocation.standalone(<mod>:item/<name>)`; the base item model is no longer passed to `RegisterAdditional`.
- `6578718` **Fixed** mod construction failure on NeoForge's strict event bus: removed `forgeBus.register(this)` (no instance listeners) and made `Events.onServerTick` an instance method. Note: that handler (dungeon music packets) never ran on 1.20.1 and is now active.

## 2026-09-23 — initial port (`dcb1df6`)

- Forge 1.20.1 / Java 17 → NeoForge 21.1.251 / Java 21, ModDevGradle 2.0.147, Parchment 2024.11.17.
- Registries, events, networking (`CustomPacketPayload`), capabilities → data attachments, enchantments → data-driven with `tridot:enchantable/*` tags, rendering (`Matrix4fStack`, new vertex API), mixins retargeted, access transformer rewritten to Mojang names.
- Published to Maven local as `pro.komaru:Tridot:1.21.1-1.0.169` (jar, `api`, `sources`).
