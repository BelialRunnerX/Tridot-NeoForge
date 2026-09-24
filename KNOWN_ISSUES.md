# Known issues — Tridot NeoForge 1.21.1 port

Status legend: **Open** (needs a fix), **Upstream** (present in the original 1.20.1 mod too; kept as-is unless it breaks the port), **Unverified** (ported by the book, not yet exercised in game). Fixed items move to [CHANGELOG.md](CHANGELOG.md). Report new ones in this repository's issue tracker, not to the original author.

## Open

_None known at the moment. The library has passed mod loading, resource reload and a dedicated-server world generation smoke test together with Valoria; gameplay features are still being play-tested._

## Upstream (inherited from Tridot 1.0.169)

- `tridot:item/test` has no model or texture (`Unable to load model: 'tridot:item/test'`). It is the library's built-in test item; harmless.
- `tridot:particle/skull` is 5×5 px, which forces the particle atlas mip level to 0 (`SpriteLoader` warning). Cosmetic.
- `tridot:translucent` shader declares `Sampler0` but does not use it, so the driver reports "could not find sampler named Sampler0". Cosmetic.
- `mods.toml` declares `GPL-2.0` while the shipped `LICENSE` is GPL-3.0; both were preserved unchanged.

## Unverified in game

- Delayed render buffers / post-processing (`glow` post shader) under Iris.
- `DotStyle` text effects (codec swap through `StyleSerializerMixin`) and `StringRenderOutputMixin` glyph hooks.
- Percent-armor damage reduction now runs in `LivingIncomingDamageEvent` (earlier than the old `LivingHurtEvent`); interaction with other damage mods untested.
- `sendToTracking(chunk)` uses a 64-block player radius instead of exact chunk tracking.
- Dungeon music (`Events.onServerTick`) is active for the first time (it was silently dead on 1.20.1); music start/stop when entering and leaving structures needs a listen.
- Boat passenger attachment points, shield-disable chance and projectile enchantment application follow vanilla 1.21.1 semantics and were not compared frame by frame with 1.20.1.
