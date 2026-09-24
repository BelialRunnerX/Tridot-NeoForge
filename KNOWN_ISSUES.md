# Known issues — Tridot NeoForge 1.21.1 port

Status legend: **Open** (needs a fix), **Upstream** (present in the original 1.20.1 mod too; kept as-is unless it breaks the port), **Changed** (intentional difference from 1.20.1 that dependent mods must know about, documented in [PORTING.md](PORTING.md)), **Unverified** (ported by the book, not yet exercised in game). Fixed items move to [CHANGELOG.md](CHANGELOG.md). Report new ones in this repository's issue tracker, not to the original author.

## Open

_None known at the moment._

**Test status (2026-09-23):** working build, not fully validated. Loads and runs in a live world (client and dedicated server) together with Valoria; individual library features have not been play-tested one by one — see "Unverified in game".

## Upstream (inherited from Tridot 1.0.169)

- `tridot:item/test` has no model or texture (`Unable to load model: 'tridot:item/test'`). It is the library's built-in test item; harmless.
- `tridot:particle/skull` is 5×5 px, which forces the particle atlas mip level to 0 (`SpriteLoader` warning). Cosmetic.
- `tridot:translucent` shader declares `Sampler0` but does not use it, so the driver reports "could not find sampler named Sampler0". Cosmetic.
- `mods.toml` declares `GPL-2.0` while the shipped `LICENSE` is GPL-3.0; both were preserved unchanged.
- Iris replaces Oculus as the optional shader-mod integration (Oculus has no 1.21 build); Dummmmmmy/Moonlight were removed from the dev classpath because nothing referenced them.

## Changed behaviour for dependent mods (intentional, see PORTING.md)

- **Enchantments are data-driven.** Tridot's enchantments apply through item tags (`tridot:enchantable/dash_weapon`, `radius_weapon`, `overdrive`, `shield`); dependents must tag their items — the old `EnchantmentCategory` predicates are gone.
- **Capabilities → data attachments.** `INBTSerializable` receives a `HolderLookup.Provider`; providers are created eagerly (no `LazyOptional`); attachments are copied on death and on other clones.
- **Networking.** `SimpleChannel` → `CustomPacketPayload` + `StreamCodec`; `PacketHandler.addRegistration` still lets other mods register on Tridot's channel. `sendToTracking(chunk)` targets players within 64 blocks instead of exact chunk tracking.
- **Attribute modifier ids are `ResourceLocation`s**; `Tridot.BASE_PROJECTILE_DAMAGE_ID` replaces the old UUID.
- **Side-loaded model keys.** `TridotModels.sideLoaded`, `SkinRegistryManager.getModelLocationSkin` and `LargeItemRenderer.getModelResourceLocation` return `ModelResourceLocation.standalone(<mod>:item/<name>)` (loads the same `models/item/...` files as the old `inventory` variant); `TridotModels.addCustomModel` now resolves `models/<name>.json` directly instead of the old `""` blockstate variant — pass `block/<name>` for the previous behaviour. `getBowModels`/`getCrossbowModels` no longer include the base item model.
- **Loot entity targets.** `tridot:mob_effect` and other targeted conditions accept both the 1.20.1 names (`killer`, `direct_killer`, `killer_player`) and the 1.21 names (`attacker`, `direct_attacker`, `attacking_player`).
- **Dungeon music tick is live.** `Events.onServerTick` (broadcasts `DungeonSoundPacket` every 100 ticks to players inside a registered structure) was a static method that Forge never invoked on 1.20.1; it now runs.
- **Percent-based armour** reduces damage in `LivingIncomingDamageEvent` (earlier than the old `LivingHurtEvent`), so ordering relative to other mods' damage modifiers may differ.

## Unverified in game

- Delayed render buffers / post-processing (`glow` post shader), especially under Iris.
- `DotStyle` text effects (codec swap through `StyleSerializerMixin`) and `StringRenderOutputMixin` glyph hooks.
- `AttributeUtilMixin` tooltip integration with Curios (verified against Curios 9.5.1 bytecode only).
- `ClientTick` double-increment quirk (tick counters advanced in both tick and render events) was kept for parity.
- Dungeon music start/stop when entering and leaving structures (first time this code runs).
- Boat passenger attachment points, shield-disable chance and projectile enchantment application follow vanilla 1.21.1 semantics and were not compared frame by frame with 1.20.1.
