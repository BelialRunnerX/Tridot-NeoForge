# Tridot — Forge 1.20.1 → NeoForge 1.21.1 porting log

Target: NeoForge 21.1.251, Minecraft 1.21.1, Java 21, Gradle 8.14.5, ModDevGradle 2.0.147, Parchment 2024.11.17.

Every non-trivial rewrite is listed here. Code sites carry a matching `// PORT NOTE:` comment.
Items under **Unverified at runtime** compile but could not be exercised without launching the game.

Result: `gradlew build` and `gradlew publishToMavenLocal` succeed. Published coordinates:
`pro.komaru:Tridot:1.21.1-1.0.169` (plus `:api` and `:sources` classifiers).

## Nature of the changes — how to read this document and the code comments

Every `// PORT NOTE:` comment in the code carries a qualifier that states what kind of change it is; the tables below use the same wording:

| Marker | Meaning | Where it applies |
|---|---|---|
| `PORT NOTE:` (no qualifier) | **Mechanical API translation** — same behaviour as 1.20.1, only the API changed (Forge → NeoForge renames, `RegistryObject` → `DeferredHolder`, new event/rendering/networking APIs, NBT → data components, 1.21 codecs). The vast majority of sites. | Everywhere |
| `PORT NOTE (behaviour change …):` | **Runtime behaviour differs from 1.20.1**, forced by the platform. Listed in KNOWN_ISSUES.md. | `Events.onServerTick` (dead static handler is now live), `Events` percent armour (`LivingIncomingDamageEvent` fires earlier than `LivingHurtEvent`) |
| `PORT NOTE (upstream bug fix):` | **A latent bug of the original code was fixed.** | `MusicModifier.DungeonMusic.isPlayerInStructure` (bounding box of an invalid structure start) |
| `PORT NOTE (API change …):` | **Public API differs for dependent mods**; dependents must adapt or at least know. Listed in KNOWN_ISSUES.md → "Changed behaviour for dependent mods". | `TagsRegistry.ENCHANTABLE_*` / `EnchantmentsRegistry` (tags instead of `EnchantmentCategory`), `TridotModels.sideLoaded`/`addCustomModel`/`getBowModels`/`getCrossbowModels`, `SkinRegistryManager.getModelLocationSkin`, `LargeItemRenderer.getModelResourceLocation`, `TargetedLootCondition.TARGET_CODEC` (accepts old and new target names), `PacketHandler.addRegistration`, `Capabilities` (attachments), attribute modifier ids as `ResourceLocation` |

Things that did **not** change: library semantics and numeric behaviour of screenshake, splashes, music modifiers, percent armour values, particle/rendering builders; ids and config keys; shipped assets (apart from the shader fog helper and model key scheme described below).

Exact dependency versions with links are in README.md → "Requirements and tested versions".

## Phase 1 — build scripts & mod metadata

| Change | Detail |
|---|---|
| Build system | ForgeGradle 6 + MixinGradle + Librarian → ModDevGradle 2.0.147 (`net.neoforged.moddev`). `reobfJar` removed; the plain `jar` is the distributable. |
| Mixins | No refmap (NeoForge runs on official names). `tridot.mixins.json`: `compatibilityLevel` → `JAVA_21`, `refmap` removed. Config is now declared in `neoforge.mods.toml` `[[mixins]]` instead of the jar manifest `MixinConfigs` attribute. |
| Metadata | `META-INF/mods.toml` → `META-INF/neoforge.mods.toml`. `forge [47,)` dep → `neoforge [21.1,)`; loader `[36,)` → `[4,)`; minecraft `[1.20.1,1.21)` → `[1.21.1,1.21.2)`. Added optional `curios [9,)` dependency (the code has Curios integration guarded by `ModList`). Version placeholders now expanded by `processResources`. |
| Access transformer | All 84 entries were SRG-named (`f_NNN_` / `m_NNN_`). Remapped to Mojang names using MCPConfig 1.20.1 `joined.tsrg` + Mojang official `client.txt`, then validated against 1.21.1. Removed entries whose targets no longer exist (`OggAudioStream`, `LivingEntity.getMeleeAttackReferencePosition`). `Camera.setRotation(FF)V` → `setRotation(FFF)V` (NeoForge adds roll). Added `AbstractArrow.setPierceLevel`, `ItemRenderer.TRIDENT_MODEL`, `Style` fields/ctor, `PostChain.passes`. |
| Dependencies | Curios `curios-forge 5.10.0+1.20.1` → `curios-neoforge 9.5.1+1.21.1` (`api` classifier). Oculus 1.7.0 → Iris `1.8.12+1.21.1-neoforge` (same `net.irisshaders.iris` package). Dummmmmmy and Moonlight/Selene were declared but never referenced in source — dropped. |
| Version | `1.20.1-1.0.169` → `1.21.1-1.0.169` (same library revision, new MC prefix). |
| CI | `.github/workflows/runs.yml` JDK 17 → 21. |

## Phase 2 — registration & entrypoint

| Change | Detail |
|---|---|
| Entrypoint | `@Mod` constructor is `Tridot(IEventBus modBus, ModContainer container)`; `FMLJavaModLoadingContext` removed. Configs registered through `container.registerConfig`. `DistExecutor` → `FMLEnvironment.dist.isClient()` for `PROXY`. |
| Registries | `RegistryObject` → `DeferredHolder`; `ForgeRegistries.*` → `BuiltInRegistries.*`; `DeferredRegister.create(Registries.X, modid)`. `TridotDataComponents` added (`DeferredRegister.createDataComponents`). |
| Attributes | `AttributeRegistry.PROJECTILE_DAMAGE` overrides NeoForge's `Attribute.getBaseId()` with `Tridot.BASE_PROJECTILE_DAMAGE_ID` so the base projectile-damage modifier renders as a green base value natively. Modifier ids are `ResourceLocation`s (was `UUID`). |
| Enchantments | Enchantments are data-driven in 1.21. The nine `Enchantment` subclasses (`Dash`, `Radius`, `Overdrive`, `Resonance`, `Vigilance`, `Vanguard`, `IronGrip`, `Deflect`, `Push`) became JSON at `data/tridot/enchantment/*.json` with matching `ResourceKey<Enchantment>` constants in `EnchantmentsRegistry`. Custom `EnchantmentCategory` predicates cannot exist in data, so they became item tags `tridot:enchantable/{dash_weapon,radius_weapon,overdrive,shield}` (`TagsRegistry.ENCHANTABLE_*`) — **dependent mods must tag their items** for the enchantments to apply. `EnchantmentsRegistry.getLevel(stack, key)` / `holder(level, key)` replace `EnchantmentHelper.getTagEnchantmentLevel(Enchantment, stack)`. All nine are listed in `data/minecraft/tags/enchantment/non_treasure.json` so they appear in the enchanting table like before. |
| Loot | Global loot modifiers use `MapCodec`, registered to `NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS`. `LootItemConditionType` takes a `MapCodec`; the five Tridot conditions were rewritten as records with `MapCodec`s (`LocalDateCondition` day/month field semantics fixed). Loot tables are fetched via `server.reloadableRegistries().getLootTable(ResourceKey)`. |
| Config | `ForgeConfigSpec` → `ModConfigSpec`. |
| Commands / args | `ModArgumentTypes` uses `ArgumentTypeInfos.registerByClass` with `SingletonArgumentInfo`; `CommandRegister` on the game bus. |
| Saved data | `GameplayEventManager` uses `SavedData.Factory`. |
| Signs | `TridotStandingSignBlock`/`TridotWallSignBlock` etc. take `(WoodType, Properties)`. |

## Phase 3 — events

| Change | Detail |
|---|---|
| Bus annotations | `@Mod.EventBusSubscriber` → `net.neoforged.fml.common.EventBusSubscriber` with `Bus.MOD`/`Bus.GAME`; `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`. |
| Ticks | `TickEvent.ClientTickEvent` → `ClientTickEvent.Post`; `PlayerTickEvent` → `PlayerTickEvent.Post`; `ServerTickEvent` → `ServerTickEvent.Post`; `RenderTickEvent` → `RenderFrameEvent.Post`. `ClientTick.mcPartialTick()` (`Minecraft.getTimer().getGameTimeDeltaPartialTick(!paused)`) replaces `Minecraft.getFrameTime()/getPartialTick()`. |
| Damage | `LivingAttackEvent`/`LivingHurtEvent` → `LivingIncomingDamageEvent` (percent armor, `Utils.Entities.canHitTarget`). `ShieldBlockEvent` → `LivingShieldBlockEvent`. `LivingChangeTargetEvent.getNewTarget()` → `getNewAboutToBeSetTarget()`. Cancellation via `ICancellableEvent` / `post(...).isCanceled()`. |
| Overlays | `RenderGuiOverlayEvent` → `RenderGuiLayerEvent.Pre/Post` + `VanillaGuiLayers`. Boss bars and the percent-armor HUD register through `RegisterGuiLayersEvent` (`BossBarsOverlay implements LayeredDraw.Layer`). |
| Capabilities | Forge capabilities → NeoForge **data attachments**. `Capabilities`/`CapabilityEntry`/`CapProvider` now build `AttachmentType`s (`copyOnDeath`) with `IAttachmentSerializer`; `INBTSerializable<CompoundTag>` gained `HolderLookup.Provider`. Wire-id ordering of entries is preserved for the sync packet. |
| Boss water breathing | `LivingBreatheEvent` usage replaced by `AbstractBoss.decreaseAirSupply` override (same effect: bosses never drown). |

## Phase 4 — networking

| Change | Detail |
|---|---|
| Channel | `SimpleChannel` → `RegisterPayloadHandlersEvent` + `PayloadRegistrar.versioned("11")`. `PacketHandler.addRegistration(Consumer<PayloadRegistrar>)` keeps the "many mods share Tridot's channel" pattern. |
| Packets | `Packet extends CustomPacketPayload`; each packet has `TYPE`, `STREAM_CODEC`, `type()`. `ClientPacket`/`ServerPacket` handle through `IPayloadContext`. `UpdateBossbarPacket` uses `RegistryFriendlyByteBuf` + `ComponentSerialization.TRUSTED_STREAM_CODEC`. |
| Sending | `PacketDistributor.sendToPlayer/sendToServer/sendToPlayersTrackingEntityAndSelf`; `sendToTracking(chunk)` is emulated by iterating players within 64 blocks of the chunk (NeoForge dropped the chunk target). |
| Registry ids on the wire | `FriendlyByteBuf.writeRegistryId/readRegistryId` (Forge extensions) → `writeById/readById` with `BuiltInRegistries.X.asHolderIdMap()`. Enchantments are written as `ResourceLocation`s because they are not in a built-in registry. |

## Phase 5 — items, entities, NBT → data components

| Change | Detail |
|---|---|
| Skins | `ItemSkin` stores the skin id in the `tridot:skin` String component, falling back to legacy `CustomData` NBT for old stacks. |
| Armor materials | `ArmorMaterial` is a registry record. `AbstractArmorRegistry` describes and registers materials into `Registries.ARMOR_MATERIAL`; effect maps are keyed by `ResourceKey<ArmorMaterial>`. `TridotArmorMat` → `Holder<ArmorMaterial>`. `SkinableArmorItem.getArmorTexture(stack, entity, slot, layer, innerModel)`. |
| Attribute modifiers | `ItemAttributeModifiers` with `EquipmentSlotGroup`; `PercentageArmorItem.getDefaultAttributeModifiers()`; `SwordItem.createAttributes`. |
| Durability / use | `hurtAndBreak(int, LivingEntity, EquipmentSlot)`, `getUseDuration(ItemStack, LivingEntity)`, `appendHoverText(stack, TooltipContext, list, flag)`. |
| Crossbows / bows | `ConfigurableCrossbow` uses the `ChargedProjectiles` component; `shootAtTarget`/`getProjectileShotVector`/`performShooting` reimplemented on the 1.21 `ProjectileWeaponItem` API. Power/punch/flame/piercing are applied via `EnchantmentHelper.onProjectileSpawned/getPiercingCount/modifyDamage/modifyKnockback` with `AbstractArrow.firedFromWeapon` (`ConfigurableBowItem.applyWeaponEnchantments`). |
| Shields | `ConfiguredShield`: `shieldDisableChance` (replaces axe-disable hardcoding), `StringUtil.formatTickDuration(ticks, 20f)`. |
| Effects | `MobEffectInstance` takes `Holder<MobEffect>`; `save()/load()` replace `writeNbt/read`. `MobEffectUtil.formatDuration(inst, factor, 20f)`. |
| Entities | `defineSynchedData(SynchedEntityData.Builder)`; `igniteForSeconds`; `PathType`; `getPassengerAttachmentPoint` for boats; `EventHooks.canEntityGrief/onArrowLoose`; `BaseSpawner.getSpawnAABB`. Block entities `loadAdditional/saveAdditional(tag, provider)`. |
| Containers | `ItemBackedInventory` stores contents in `ItemContainerContents`. |
| Trades | `TreasureMapItemListing` uses `Holder<MapDecorationType>` and `ItemCost`. |
| Music | `MusicModifier` structure check via `getStructureWithPieceAt(pos, holder -> holder.is(key))`. |

## Phase 6 — rendering & client

| Change | Detail |
|---|---|
| Vertex API | `VertexConsumer.vertex/color/uv/uv2/normal/endVertex` → `addVertex/setColor/setUv/setUv1/setUv2/setLight/setNormal/setOverlay` (no `endVertex`). `RenderBuilder.CONSUMER_INFO_MAP` is keyed by `VertexFormatElement.POSITION/COLOR/UV0/UV1/UV2/NORMAL`; `ELEMENT_PADDING` no longer exists. `GhostVertexConsumer`, `ParticleBehavior`, `QuadScreenParticle`, `LightningEffect` rewritten to the new calls. |
| Buffers | `BufferBuilder`s are single-use. `LevelRenderHandler.getDelayedRender()` builds `MultiBufferSource.immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder>, ByteBufferBuilder)`. `TridotRenderTypes.ScreenParticleRenderType` changed shape: `BufferBuilder begin(Tesselator, TextureManager)` / `void end(BufferBuilder)` (uploads with `BufferUploader.drawWithShader`). `GenericParticleRenderType.begin` returns a fresh `tesselator.begin(QUADS, PARTICLE)`; `end` is gone. |
| Model-view | `RenderSystem.getModelViewStack()` is a JOML `Matrix4fStack` (`pushMatrix/identity/mul/rotate/popMatrix`) — `LevelRenderHandler.shadersDelayedRender`, `Utils.Render.renderItemModelInGui`. `PoseStack.mulPoseMatrix` → `mulPose`. |
| Models | Bake map is `Map<ModelResourceLocation, BakedModel>`; `ModelResourceLocation.inventory(rl)` / `standalone(rl)` replace the 3-arg constructor (`TridotModels`, `LargeItemRenderer`, `SkinRegistryManager`). `ForgeHooksClient.handleCameraTransforms` → `ClientHooks`. `CustomItemRenderer.renderItem` mirrors 1.21.1 `ItemRenderer.render` (compass foil "direct" buffer removed; `IClientItemExtensions.of(stack)`). `Model.renderToBuffer`/`ModelPart.render` take a packed ARGB int (`CustomBlockModel`, `ArmorModel`, `LuminescentLayer`); float overloads kept as helpers. |
| Item overrides | `CrossbowItemOverrides`/`BowItemOverrides` use `stack.getUseDuration(entity)` and `ConfigurableCrossbow.getCustomChargeDuration` (vanilla `getChargeDuration` needs the shooter). |
| Particles | `ParticleType` subclasses must provide `MapCodec codec()` and `StreamCodec streamCodec()`; `AbstractParticleType` returns unit codecs (Tridot options are client-only and carry no serialisable state). `ParticleOptions.writeToNetwork/writeToString` removed. `Particle.shouldCull()` no longer overrides anything (kept as a plain accessor). `ParticleBuilder.create(Supplier<? extends ParticleType<?>>)` replaces the `RegistryObject` overload. |
| Shaders / post-process | `RegisterShadersEvent` from `net.neoforged.neoforge.client.event`. `PostProcess` uses `ClientTick.mcPartialTick()`, `getTimer().getRealtimeDeltaTicks()` (was `getDeltaFrameTime`). `TridotGlslPreprocessor` uses `ResourceLocation.parse`. |
| Text styles | **`DotStyle` serialization rewritten.** 1.20.1 replaced `Component.Serializer.GSON` with Gson adapters; 1.21 Components are codec-driven. `DotStyle.Codecs` is a `MapCodec<Style>` wrapping vanilla `Style.Serializer.MAP_CODEC` that reads/writes the `tridot`/`tridot_effects` keys (same JSON shape as before). `StyleSerializerMixin` swaps `MAP_CODEC`, `CODEC` and `TRUSTED_STREAM_CODEC` at the tail of `Style.Serializer.<clinit>`, so JSON, NBT and network Components all round-trip DotStyle effects. Gson `Serializer`/`DefaultSerializer` inner classes removed. |
| Camera | `Camera.setRotation(yaw, pitch, roll)` (`ScreenshakeHandler.cameraTick` passes `camera.getRoll()`). |
| GUI | `GuiGraphics.blitSprite`, `Screen.renderBackground(g, mx, my, pt)`, `ResourceLocation.parse/fromNamespaceAndPath` in `DotScreen`/`GuiDraw`/`IGuiDrawer`. |
| Shader mods | `ShadersIntegration` checks `ModList.get().isLoaded("iris")` (Oculus has no 1.21 build; Iris ships NeoForge builds). |
| `Utils` | `EnchantmentHelper.getSweepingDamageRatio` → `Attributes.SWEEPING_DAMAGE_RATIO`; `getDamageBonus` → `EnchantmentHelper.modifyDamage`; `getFireAspect` → `EnchantmentsRegistry.getLevel(stack, Enchantments.FIRE_ASPECT)`; `ForgeHooks.getProjectile` → `CommonHooks.getProjectile`; `ItemHandlerHelper` from `net.neoforged.neoforge.items`; `Utils.Items.deserializeEnchantment/enchantmentFromNetwork/enchantmentToNetwork` now deal in `ResourceKey<Enchantment>` (plus a `HolderLookup.Provider` overload returning a `Holder`) because enchantments cannot be resolved without registry access. |

## Phase 7 — mixins

| Mixin | Change |
|---|---|
| `ForgeHooksMixin` → `LootDataTypeMixin` | `ForgeHooks.loadLootTable` no longer exists. NeoForge evaluates `neoforge:conditions` inside `LootDataType.deserialize`; the mixin copies Tridot's `tridot:conditions` key into `neoforge:conditions` before the conditional codec runs, so existing data packs keep working. |
| `ItemStackMixin` + `curios.CurioTooltipMixin` → `AttributeUtilMixin` (+ slim `ItemStackMixin`) | Attribute tooltip lines are produced by NeoForge's `AttributeUtil.applyTextFor` for vanilla items **and** for Curios 9 (verified against `curios-neoforge-9.5.1` bytecode: `ClientEventHandler.onAttributeTooltip` calls `applyTextFor`). One `@ModifyVariable` on that method applies every `AttributeTooltipModifier`; "tool base" modifiers are given the attribute's `getBaseId()` so NeoForge renders them as base values. `ItemStackMixin` only records `TooltipModifierHandler.attributeTooltipSize` (via MixinExtras `@Local`). The Curios-specific mixin (and its `IMixinConfigPlugin` gate) is therefore no longer needed. |
| `StringRenderOutputMixin` | `accept` keeps the same local layout in 1.21.1, so captures are unchanged. The alpha/advance hooks used `@ModifyVariable(name=…)`, which requires a local-variable table that production jars lack; replaced with MixinExtras `@ModifyExpressionValue` on `this.a` and `GlyphInfo.getAdvance(Z)`. |
| `GameRendererMixin` | `render(DeltaTracker, boolean)`, `renderItemInHand(Camera, float, Matrix4f)`; removed obsolete `renderLevel` shadow. |
| `GuiMixin` | `renderHotbar(GuiGraphics, DeltaTracker)`. |
| `LivingEntityRendererMixin` | `setupRotations` gained a trailing `float scale`. |
| `BlockItemMixin` | `BlockItem.getBlockEntityData` is gone (block entity NBT is the `block_entity_data` component). Hook moved to `updateCustomBlockEntityTag(Level, Player, BlockPos, ItemStack)`, mirroring its body and letting `ICustomBlockEntityDataItem` rewrite the tag before `CustomData.loadInto`. |
| `BlocksMixin` | `FlowerBlock(Holder<MobEffect>, float, Properties)` descriptor; `flowerPot(Block)` lost its `FeatureFlag...` parameter. |
| `CrossbowMixin` | `getChargeDuration(ItemStack, LivingEntity)`. |
| `TntBlockMixin` | `use` → `useItemOn(...)` returning `ItemInteractionResult`. |
| `AbstractClientPlayerMixin` | `ForgeHooksClient` → `ClientHooks.getFieldOfViewModifier`. |
| `StyleSerializerMixin` (new) | See Phase 6 / DotStyle. |
| Unchanged targets (verified against 1.21.1 sources) | `CameraMixin`, `EffectProgramMixin`, `FontMixin`, `GuiGraphicsMixin`, `HumanoidModelMixin`, `ItemEntityMixin` (client+common), `ItemInHandLayerMixin`, `ItemInHandRendererMixin`, `SplashManagerMixin`, `BossHealthOverlayAccessor`, `CreeperMixin`, `ServerItemCooldownsMixin`, `TooltipRenderUtilMixin`. |

## Phase 8 — data generation

No datagen sources exist in the repository (`src/generated` absent, no `GatherDataEvent` listeners). Nothing to port; the `data` run configuration is still declared for downstream use.

## Runtime fixes (found while booting the Valoria datagen against this jar)

- **Event bus strictness** — NeoForge's bus throws where Forge silently ignored: `forgeBus.register(this)` in `Tridot` (no instance `@SubscribeEvent` methods) was removed, and `common.Events.onServerTick` was `static` inside an instance-registered object. In 1.20.1 Forge skipped that method, so the dungeon-music `DungeonSoundPacket` broadcast **never ran**; it is now an instance method and active. Remove its `@SubscribeEvent` to restore the old (dead) behaviour.

- **Side-loaded model keys** (found on the first real client launch) — `ModelEvent.RegisterAdditional.register` only accepts the `standalone` variant in 1.21 and Minecraft's fallback resource reload then re-fired `FMLCommonSetupEvent` for every mod. 1.20.1 registered skin / bow / crossbow / `_in_hand` models with the `inventory` variant, which loaded `models/item/<name>.json`; they are now `ModelResourceLocation.standalone(<modid>:item/<name>)` (`TridotModels.sideLoaded`, `SkinRegistryManager.getModelLocationSkin`, `LargeItemRenderer.getModelResourceLocation`) so the same files load, and every lookup uses the same key. The base item model is no longer passed to `RegisterAdditional` (it is baked automatically; an `inventory` key would be rejected). `TridotModels.addCustomModel` now yields `standalone(<modid>:<model>)` — callers that relied on the old `""` blockstate variant must pass `block/<name>`.

- **Shaders** (second client launch) — vanilla 1.21 `fog.glsl` changed `fog_distance(mat4, vec3, int)` to `fog_distance(vec3, int)` (positions are camera-relative now). `include/common.glsl`'s `fogDistance` wrapper keeps its 1.20.1 signature but ignores the matrix, so all Tridot core shaders (`additive`, `additive_texture`, `translucent`, `translucent_texture`) and dependents including `tridot:common.glsl` compile again. A failing `RegisterShadersEvent` makes Minecraft retry the resource reload endlessly (stuck on the Mojang screen), which is how this surfaced.

- **Dungeon music structure check** — `MusicModifier.DungeonMusic.isPlayerInStructure` called `getBoundingBox()` on `StructureStart.INVALID_START` (player outside the structure), which throws and crashed the server tick as soon as the now-active `Events.onServerTick` ran. Returns false for an invalid start.
- **Loot entity targets** — vanilla renamed `killer/direct_killer/killer_player` to `attacker/direct_attacker/attacking_player` in 1.21. `TargetedLootCondition.TARGET_CODEC` accepts both spellings (writes the new one) so 1.20.1 loot modifiers such as Valoria's keep loading.

## Unverified at runtime

These compile and follow the 1.21.1 APIs, but were not exercised in a running game:

- **DotStyle codec swap** — depends on `Style.Serializer` being class-initialised before `ComponentSerialization.createCodec` reads `MAP_CODEC` (it is read lazily through `Codec.recursive`). If a mod forces the Component codec extremely early, effects would silently not serialise.
- **`LootDataTypeMixin`** — `@ModifyVariable` on the generic `V value` argument of `LootDataType.deserialize`; it only acts when the value is a `JsonObject`, which is the case for datapack loading.
- **`AttributeUtilMixin` / Curios** — verified against Curios 9.5.1 bytecode only; a future Curios release could stop calling `AttributeUtil.applyTextFor`.
- **`StringRenderOutputMixin` local captures** — `CAPTURE_FAILSOFT`; if a coremod changes `Font.StringRenderOutput.accept`'s local layout the glyph hooks are skipped rather than crashing.
- **Delayed render buffers under Iris** — `shadersDelayedRender` was ported 1:1 to `Matrix4fStack`; Iris' 1.21 pipeline was not tested.
- **`GenericParticleRenderType`** returns an empty per-frame builder because Tridot particles write into the delayed render buffers; `ParticleEngine` skips uploading empty meshes.
- **Percent armor via `LivingIncomingDamageEvent`** — fires earlier than the old `LivingHurtEvent`; ordering relative to other mods' damage modifiers may differ.
- **Enchantable item tags** — dependent mods must add their items to `tridot:enchantable/*` tags; behaviour was previously provided by `EnchantmentCategory` predicates.
- **Attachments vs capabilities** — `copyOnDeath` mirrors the old `PlayerEvent.Clone` copy; providers that relied on lazy `LazyOptional` semantics now get eager attachment creation.
- **`sendToTracking(chunk)`** — Tridot's own `TRACKING_CHUNK_AND_NEAR` distributor (players tracking the chunk *and* within 64 blocks) is re-implemented on `ChunkMap.getPlayers`; same semantics, not yet exercised with many players.
- **`ClientTick`** double increment quirk (tick counters advanced both in the tick and render events) was kept as-is for parity.
- **Boat passenger attachment points**, **shield disable chance**, **projectile enchantment application via `EnchantmentHelper.onProjectileSpawned`** — semantics follow vanilla 1.21.1 but were not compared frame-by-frame with 1.20.1.
- **`mods.toml` license field** declares `GPL-2.0` as in the original repository, while the shipped `LICENSE` file is GPL-3.0; both were preserved unchanged.
