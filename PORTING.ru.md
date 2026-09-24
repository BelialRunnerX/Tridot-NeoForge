# Tridot — журнал портирования с Forge 1.20.1 на NeoForge 1.21.1

> 🇬🇧 Основная версия — [PORTING.md](PORTING.md) (английский). Перевод сделан в знак уважения к автору оригинала и может отставать; при расхождениях верна английская версия.

Цель: NeoForge 21.1.251, Minecraft 1.21.1, Java 21, Gradle 8.14.5, ModDevGradle 2.0.147, Parchment 2024.11.17.

Здесь перечислены все нетривиальные переработки. Соответствующие места в коде помечены комментарием `// PORT NOTE:`.
Пункты в разделе **Не проверено в рантайме** компилируются, но не могли быть опробованы без запуска игры.

Результат: `gradlew build` и `gradlew publishToMavenLocal` проходят успешно. Опубликованные координаты:
`pro.komaru:Tridot:1.21.1-1.0.169` (плюс классификаторы `:api` и `:sources`).

## Характер изменений — как читать этот документ и комментарии в коде

Каждый комментарий `// PORT NOTE:` в коде снабжён уточнением, какого рода это изменение; таблицы ниже используют те же формулировки:

| Пометка | Значение | Где встречается |
|---|---|---|
| `PORT NOTE:` (без уточнения) | **Механический перевод API** — поведение такое же, как в 1.20.1, изменился только API (переименования Forge → NeoForge, `RegistryObject` → `DeferredHolder`, новые API событий/рендера/сети, NBT → компоненты данных, кодеки 1.21). Подавляющее большинство мест. | Везде |
| `PORT NOTE (behaviour change …):` | **Поведение в рантайме отличается от 1.20.1**, что навязано платформой. Перечислено в KNOWN_ISSUES.ru.md. | `Events.onServerTick` (мёртвый статический обработчик теперь работает), процентная броня в `Events` (`LivingIncomingDamageEvent` срабатывает раньше `LivingHurtEvent`) |
| `PORT NOTE (upstream bug fix):` | **Исправлена скрытая ошибка оригинального кода.** | `MusicModifier.DungeonMusic.isPlayerInStructure` (bounding box невалидного старта структуры) |
| `PORT NOTE (API change …):` | **Публичный API отличается для зависимых модов**; им нужно адаптироваться или хотя бы знать об этом. Перечислено в KNOWN_ISSUES.ru.md → «Изменённое поведение для зависимых модов». | `TagsRegistry.ENCHANTABLE_*` / `EnchantmentsRegistry` (теги вместо `EnchantmentCategory`), `TridotModels.sideLoaded`/`addCustomModel`/`getBowModels`/`getCrossbowModels`, `SkinRegistryManager.getModelLocationSkin`, `LargeItemRenderer.getModelResourceLocation`, `TargetedLootCondition.TARGET_CODEC` (принимает старые и новые названия целей), `PacketHandler.addRegistration`, `Capabilities` (attachments), идентификаторы модификаторов атрибутов как `ResourceLocation` |

Что **не** менялось: семантика библиотеки и численное поведение тряски экрана, сплэшей, модификаторов музыки, значений процентной брони, билдеров частиц/рендера; идентификаторы и ключи конфигурации; поставляемые ассеты (кроме вспомогательной функции тумана в шейдерах и схемы ключей моделей, описанных ниже).

Точные версии зависимостей со ссылками — в README.ru.md → «Требования и проверенные версии».

## Фаза 1 — сборочные скрипты и метаданные мода

| Изменение | Подробности |
|---|---|
| Система сборки | ForgeGradle 6 + MixinGradle + Librarian → ModDevGradle 2.0.147 (`net.neoforged.moddev`). `reobfJar` удалён; распространяемый артефакт — обычный `jar`. |
| Миксины | Refmap отсутствует (NeoForge работает на официальных именах). `tridot.mixins.json`: `compatibilityLevel` → `JAVA_21`, `refmap` удалён. Конфиг объявляется в `neoforge.mods.toml` (`[[mixins]]`) вместо атрибута `MixinConfigs` в манифесте jar. |
| Метаданные | `META-INF/mods.toml` → `META-INF/neoforge.mods.toml`. Зависимость `forge [47,)` → `neoforge [21.1,)`; загрузчик `[36,)` → `[4,)`; minecraft `[1.20.1,1.21)` → `[1.21.1,1.21.2)`. Добавлена необязательная зависимость `curios [9,)` (интеграция с Curios в коде защищена проверкой `ModList`). Плейсхолдеры версий теперь раскрываются через `processResources`. |
| Access transformer | Все 84 записи были в SRG-именах (`f_NNN_` / `m_NNN_`). Переведены в имена Mojang с помощью MCPConfig 1.20.1 `joined.tsrg` + официального `client.txt`, затем проверены по 1.21.1. Удалены записи, целей которых больше нет (`OggAudioStream`, `LivingEntity.getMeleeAttackReferencePosition`). `Camera.setRotation(FF)V` → `setRotation(FFF)V` (NeoForge добавляет крен). Добавлены `AbstractArrow.setPierceLevel`, `ItemRenderer.TRIDENT_MODEL`, поля/конструктор `Style`, `PostChain.passes`. |
| Зависимости | Curios `curios-forge 5.10.0+1.20.1` → `curios-neoforge 9.5.1+1.21.1` (классификатор `api`). Oculus 1.7.0 → Iris `1.8.12+1.21.1-neoforge` (тот же пакет `net.irisshaders.iris`). Dummmmmmy и Moonlight/Selene были объявлены, но в исходниках не использовались — убраны. |
| Версия | `1.20.1-1.0.169` → `1.21.1-1.0.169` (та же ревизия библиотеки, новый префикс MC). |
| CI | `.github/workflows/runs.yml`: JDK 17 → 21. |

## Фаза 2 — регистрация и точка входа

| Изменение | Подробности |
|---|---|
| Точка входа | Конструктор `@Mod` — `Tridot(IEventBus modBus, ModContainer container)`; `FMLJavaModLoadingContext` удалён. Конфиги регистрируются через `container.registerConfig`. `DistExecutor` → `FMLEnvironment.dist.isClient()` для `PROXY`. |
| Реестры | `RegistryObject` → `DeferredHolder`; `ForgeRegistries.*` → `BuiltInRegistries.*`; `DeferredRegister.create(Registries.X, modid)`. Добавлен `TridotDataComponents` (`DeferredRegister.createDataComponents`). |
| Атрибуты | `AttributeRegistry.PROJECTILE_DAMAGE` переопределяет `Attribute.getBaseId()` NeoForge значением `Tridot.BASE_PROJECTILE_DAMAGE_ID`, чтобы базовый модификатор урона снарядов отображался как зелёное базовое значение штатными средствами. Идентификаторы модификаторов — `ResourceLocation` (были `UUID`). |
| Зачарования | В 1.21 зачарования описываются данными. Девять подклассов `Enchantment` (`Dash`, `Radius`, `Overdrive`, `Resonance`, `Vigilance`, `Vanguard`, `IronGrip`, `Deflect`, `Push`) превратились в JSON в `data/tridot/enchantment/*.json` с соответствующими константами `ResourceKey<Enchantment>` в `EnchantmentsRegistry`. Собственные предикаты `EnchantmentCategory` в данных невозможны, поэтому они стали тегами предметов `tridot:enchantable/{dash_weapon,radius_weapon,overdrive,shield}` (`TagsRegistry.ENCHANTABLE_*`) — **зависимые моды должны пометить свои предметы тегами**, иначе зачарования не будут применяться. `EnchantmentsRegistry.getLevel(stack, key)` / `holder(level, key)` заменяют `EnchantmentHelper.getTagEnchantmentLevel(Enchantment, stack)`. Все девять перечислены в `data/minecraft/tags/enchantment/non_treasure.json`, чтобы, как и прежде, появляться в столе зачарований. |
| Лут | Глобальные модификаторы лута используют `MapCodec` и регистрируются в `NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS`. `LootItemConditionType` принимает `MapCodec`; пять условий Tridot переписаны как record-классы с `MapCodec` (исправлена семантика полей day/month у `LocalDateCondition`). Таблицы лута берутся через `server.reloadableRegistries().getLootTable(ResourceKey)`. |
| Конфиг | `ForgeConfigSpec` → `ModConfigSpec`. |
| Команды / аргументы | `ModArgumentTypes` использует `ArgumentTypeInfos.registerByClass` с `SingletonArgumentInfo`; `CommandRegister` на игровой шине. |
| Сохраняемые данные | `GameplayEventManager` использует `SavedData.Factory`. |
| Таблички | `TridotStandingSignBlock`/`TridotWallSignBlock` и др. принимают `(WoodType, Properties)`. |

## Фаза 3 — события

| Изменение | Подробности |
|---|---|
| Аннотации шины | `@Mod.EventBusSubscriber` → `net.neoforged.fml.common.EventBusSubscriber` с `Bus.MOD`/`Bus.GAME`; `MinecraftForge.EVENT_BUS` → `NeoForge.EVENT_BUS`. |
| Тики | `TickEvent.ClientTickEvent` → `ClientTickEvent.Post`; `PlayerTickEvent` → `PlayerTickEvent.Post`; `ServerTickEvent` → `ServerTickEvent.Post`; `RenderTickEvent` → `RenderFrameEvent.Post`. `ClientTick.mcPartialTick()` (`Minecraft.getTimer().getGameTimeDeltaPartialTick(!paused)`) заменяет `Minecraft.getFrameTime()/getPartialTick()`. |
| Урон | `LivingAttackEvent`/`LivingHurtEvent` → `LivingIncomingDamageEvent` (процентная броня, `Utils.Entities.canHitTarget`). `ShieldBlockEvent` → `LivingShieldBlockEvent`. `LivingChangeTargetEvent.getNewTarget()` → `getNewAboutToBeSetTarget()`. Отмена через `ICancellableEvent` / `post(...).isCanceled()`. |
| Оверлеи | `RenderGuiOverlayEvent` → `RenderGuiLayerEvent.Pre/Post` + `VanillaGuiLayers`. Полоски боссов и HUD процентной брони регистрируются через `RegisterGuiLayersEvent` (`BossBarsOverlay implements LayeredDraw.Layer`). |
| Capabilities | Capabilities Forge → **data attachments** NeoForge. `Capabilities`/`CapabilityEntry`/`CapProvider` теперь строят `AttachmentType` (`copyOnDeath`) с `IAttachmentSerializer`; `INBTSerializable<CompoundTag>` получил `HolderLookup.Provider`. Порядок wire-id записей сохранён для пакета синхронизации. |
| Дыхание боссов под водой | Использование `LivingBreatheEvent` заменено переопределением `AbstractBoss.decreaseAirSupply` (тот же эффект: боссы не тонут). |

## Фаза 4 — сеть

| Изменение | Подробности |
|---|---|
| Канал | `SimpleChannel` → `RegisterPayloadHandlersEvent` + `PayloadRegistrar.versioned("11")`. `PacketHandler.addRegistration(Consumer<PayloadRegistrar>)` сохраняет схему «несколько модов делят канал Tridot». |
| Пакеты | `Packet extends CustomPacketPayload`; у каждого пакета есть `TYPE`, `STREAM_CODEC`, `type()`. `ClientPacket`/`ServerPacket` обрабатываются через `IPayloadContext`. `UpdateBossbarPacket` использует `RegistryFriendlyByteBuf` + `ComponentSerialization.TRUSTED_STREAM_CODEC`. |
| Отправка | `PacketDistributor.sendToPlayer/sendToServer/sendToPlayersTrackingEntityAndSelf`; `sendToTracking(chunk)` эмулируется перебором игроков в радиусе 64 блоков от чанка (NeoForge убрал цель «чанк»). |
| Идентификаторы реестров в сети | `FriendlyByteBuf.writeRegistryId/readRegistryId` (расширения Forge) → `writeById/readById` с `BuiltInRegistries.X.asHolderIdMap()`. Зачарования передаются как `ResourceLocation`, так как они не входят во встроенный реестр. |

## Фаза 5 — предметы, сущности, NBT → компоненты данных

| Изменение | Подробности |
|---|---|
| Скины | `ItemSkin` хранит идентификатор скина в строковом компоненте `tridot:skin`, с откатом на устаревший NBT `CustomData` для старых стаков. |
| Материалы брони | `ArmorMaterial` — запись реестра. `AbstractArmorRegistry` описывает и регистрирует материалы в `Registries.ARMOR_MATERIAL`; карты эффектов ключуются по `ResourceKey<ArmorMaterial>`. `TridotArmorMat` → `Holder<ArmorMaterial>`. `SkinableArmorItem.getArmorTexture(stack, entity, slot, layer, innerModel)`. |
| Модификаторы атрибутов | `ItemAttributeModifiers` с `EquipmentSlotGroup`; `PercentageArmorItem.getDefaultAttributeModifiers()`; `SwordItem.createAttributes`. |
| Прочность / использование | `hurtAndBreak(int, LivingEntity, EquipmentSlot)`, `getUseDuration(ItemStack, LivingEntity)`, `appendHoverText(stack, TooltipContext, list, flag)`. |
| Арбалеты / луки | `ConfigurableCrossbow` использует компонент `ChargedProjectiles`; `shootAtTarget`/`getProjectileShotVector`/`performShooting` переписаны на API `ProjectileWeaponItem` 1.21. Power/punch/flame/piercing применяются через `EnchantmentHelper.onProjectileSpawned/getPiercingCount/modifyDamage/modifyKnockback` с `AbstractArrow.firedFromWeapon` (`ConfigurableBowItem.applyWeaponEnchantments`). |
| Щиты | `ConfiguredShield`: `shieldDisableChance` (заменяет жёстко зашитое выбивание топором), `StringUtil.formatTickDuration(ticks, 20f)`. |
| Эффекты | `MobEffectInstance` принимает `Holder<MobEffect>`; `save()/load()` заменяют `writeNbt/read`. `MobEffectUtil.formatDuration(inst, factor, 20f)`. |
| Сущности | `defineSynchedData(SynchedEntityData.Builder)`; `igniteForSeconds`; `PathType`; `getPassengerAttachmentPoint` для лодок; `EventHooks.canEntityGrief/onArrowLoose`; `BaseSpawner.getSpawnAABB`. Блок-сущности: `loadAdditional/saveAdditional(tag, provider)`. |
| Контейнеры | `ItemBackedInventory` хранит содержимое в `ItemContainerContents`. |
| Торговля | `TreasureMapItemListing` использует `Holder<MapDecorationType>` и `ItemCost`. |
| Музыка | Проверка структуры в `MusicModifier` через `getStructureWithPieceAt(pos, holder -> holder.is(key))`. |

## Фаза 6 — рендер и клиент

| Изменение | Подробности |
|---|---|
| API вершин | `VertexConsumer.vertex/color/uv/uv2/normal/endVertex` → `addVertex/setColor/setUv/setUv1/setUv2/setLight/setNormal/setOverlay` (без `endVertex`). `RenderBuilder.CONSUMER_INFO_MAP` ключуется по `VertexFormatElement.POSITION/COLOR/UV0/UV1/UV2/NORMAL`; `ELEMENT_PADDING` больше нет. `GhostVertexConsumer`, `ParticleBehavior`, `QuadScreenParticle`, `LightningEffect` переписаны на новые вызовы. |
| Буферы | `BufferBuilder` одноразовые. `LevelRenderHandler.getDelayedRender()` строит `MultiBufferSource.immediateWithBuffers(SequencedMap<RenderType, ByteBufferBuilder>, ByteBufferBuilder)`. У `TridotRenderTypes.ScreenParticleRenderType` изменилась форма: `BufferBuilder begin(Tesselator, TextureManager)` / `void end(BufferBuilder)` (загрузка через `BufferUploader.drawWithShader`). `GenericParticleRenderType.begin` возвращает свежий `tesselator.begin(QUADS, PARTICLE)`; `end` исчез. |
| Model-view | `RenderSystem.getModelViewStack()` — JOML `Matrix4fStack` (`pushMatrix/identity/mul/rotate/popMatrix`) — `LevelRenderHandler.shadersDelayedRender`, `Utils.Render.renderItemModelInGui`. `PoseStack.mulPoseMatrix` → `mulPose`. |
| Модели | Карта запечённых моделей — `Map<ModelResourceLocation, BakedModel>`; `ModelResourceLocation.inventory(rl)` / `standalone(rl)` заменяют 3-аргументный конструктор (`TridotModels`, `LargeItemRenderer`, `SkinRegistryManager`). `ForgeHooksClient.handleCameraTransforms` → `ClientHooks`. `CustomItemRenderer.renderItem` повторяет `ItemRenderer.render` 1.21.1 (удалён «direct» буфер фойла компаса; `IClientItemExtensions.of(stack)`). `Model.renderToBuffer`/`ModelPart.render` принимают упакованный ARGB int (`CustomBlockModel`, `ArmorModel`, `LuminescentLayer`); float-перегрузки оставлены как вспомогательные. |
| Переопределения предметов | `CrossbowItemOverrides`/`BowItemOverrides` используют `stack.getUseDuration(entity)` и `ConfigurableCrossbow.getCustomChargeDuration` (ванильному `getChargeDuration` нужен стрелок). |
| Частицы | Подклассы `ParticleType` должны предоставлять `MapCodec codec()` и `StreamCodec streamCodec()`; `AbstractParticleType` возвращает unit-кодеки (опции Tridot — только клиентские и не несут сериализуемого состояния). `ParticleOptions.writeToNetwork/writeToString` удалены. `Particle.shouldCull()` больше ничего не переопределяет (оставлен как обычный аксессор). `ParticleBuilder.create(Supplier<? extends ParticleType<?>>)` заменяет перегрузку с `RegistryObject`. |
| Шейдеры / постобработка | `RegisterShadersEvent` из `net.neoforged.neoforge.client.event`. `PostProcess` использует `ClientTick.mcPartialTick()`, `getTimer().getRealtimeDeltaTicks()` (был `getDeltaFrameTime`). `TridotGlslPreprocessor` использует `ResourceLocation.parse`. |
| Стили текста | **Сериализация `DotStyle` переписана.** В 1.20.1 подменялся `Component.Serializer.GSON` адаптерами Gson; в 1.21 компоненты работают на кодеках. `DotStyle.Codecs` — `MapCodec<Style>`, оборачивающий ванильный `Style.Serializer.MAP_CODEC` и читающий/записывающий ключи `tridot`/`tridot_effects` (та же форма JSON, что раньше). `StyleSerializerMixin` подменяет `MAP_CODEC`, `CODEC` и `TRUSTED_STREAM_CODEC` в конце `Style.Serializer.<clinit>`, так что JSON, NBT и сетевые компоненты сохраняют эффекты DotStyle. Внутренние классы Gson `Serializer`/`DefaultSerializer` удалены. |
| Камера | `Camera.setRotation(yaw, pitch, roll)` (`ScreenshakeHandler.cameraTick` передаёт `camera.getRoll()`). |
| GUI | `GuiGraphics.blitSprite`, `Screen.renderBackground(g, mx, my, pt)`, `ResourceLocation.parse/fromNamespaceAndPath` в `DotScreen`/`GuiDraw`/`IGuiDrawer`. |
| Шейдер-моды | `ShadersIntegration` проверяет `ModList.get().isLoaded("iris")` (у Oculus нет сборки для 1.21; Iris выпускает сборки для NeoForge). |
| `Utils` | `EnchantmentHelper.getSweepingDamageRatio` → `Attributes.SWEEPING_DAMAGE_RATIO`; `getDamageBonus` → `EnchantmentHelper.modifyDamage`; `getFireAspect` → `EnchantmentsRegistry.getLevel(stack, Enchantments.FIRE_ASPECT)`; `ForgeHooks.getProjectile` → `CommonHooks.getProjectile`; `ItemHandlerHelper` из `net.neoforged.neoforge.items`; `Utils.Items.deserializeEnchantment/enchantmentFromNetwork/enchantmentToNetwork` теперь работают с `ResourceKey<Enchantment>` (плюс перегрузка с `HolderLookup.Provider`, возвращающая `Holder`), поскольку зачарования нельзя разрешить без доступа к реестру. |

## Фаза 7 — миксины

| Миксин | Изменение |
|---|---|
| `ForgeHooksMixin` → `LootDataTypeMixin` | `ForgeHooks.loadLootTable` больше не существует. NeoForge вычисляет `neoforge:conditions` внутри `LootDataType.deserialize`; миксин копирует ключ `tridot:conditions` в `neoforge:conditions` до запуска условного кодека, так что существующие датапаки продолжают работать. |
| `ItemStackMixin` + `curios.CurioTooltipMixin` → `AttributeUtilMixin` (+ облегчённый `ItemStackMixin`) | Строки подсказок с атрибутами формирует `AttributeUtil.applyTextFor` NeoForge как для ванильных предметов, **так и** для Curios 9 (проверено по байткоду `curios-neoforge-9.5.1`: `ClientEventHandler.onAttributeTooltip` вызывает `applyTextFor`). Один `@ModifyVariable` на этом методе применяет все `AttributeTooltipModifier`; модификаторам «базы инструмента» присваивается `getBaseId()` атрибута, чтобы NeoForge отображал их как базовые значения. `ItemStackMixin` лишь записывает `TooltipModifierHandler.attributeTooltipSize` (через `@Local` из MixinExtras). Отдельный миксин для Curios (и его `IMixinConfigPlugin`) больше не нужен. |
| `StringRenderOutputMixin` | В 1.21.1 у `accept` та же раскладка локальных переменных, поэтому захваты не изменились. Хуки альфы/продвижения использовали `@ModifyVariable(name=…)`, что требует таблицы локальных переменных, отсутствующей в production-jar; заменены на `@ModifyExpressionValue` (MixinExtras) на `this.a` и `GlyphInfo.getAdvance(Z)`. |
| `GameRendererMixin` | `render(DeltaTracker, boolean)`, `renderItemInHand(Camera, float, Matrix4f)`; удалён устаревший shadow `renderLevel`. |
| `GuiMixin` | `renderHotbar(GuiGraphics, DeltaTracker)`. |
| `LivingEntityRendererMixin` | `setupRotations` получил завершающий параметр `float scale`. |
| `BlockItemMixin` | `BlockItem.getBlockEntityData` исчез (NBT блок-сущности — компонент `block_entity_data`). Хук перенесён в `updateCustomBlockEntityTag(Level, Player, BlockPos, ItemStack)`, повторяет его тело и позволяет `ICustomBlockEntityDataItem` переписать тег до `CustomData.loadInto`. |
| `BlocksMixin` | Дескриптор `FlowerBlock(Holder<MobEffect>, float, Properties)`; `flowerPot(Block)` лишился параметра `FeatureFlag...`. |
| `CrossbowMixin` | `getChargeDuration(ItemStack, LivingEntity)`. |
| `TntBlockMixin` | `use` → `useItemOn(...)`, возвращающий `ItemInteractionResult`. |
| `AbstractClientPlayerMixin` | `ForgeHooksClient` → `ClientHooks.getFieldOfViewModifier`. |
| `StyleSerializerMixin` (новый) | См. фазу 6 / DotStyle. |
| Неизменённые цели (проверены по исходникам 1.21.1) | `CameraMixin`, `EffectProgramMixin`, `FontMixin`, `GuiGraphicsMixin`, `HumanoidModelMixin`, `ItemEntityMixin` (клиент+общий), `ItemInHandLayerMixin`, `ItemInHandRendererMixin`, `SplashManagerMixin`, `BossHealthOverlayAccessor`, `CreeperMixin`, `ServerItemCooldownsMixin`, `TooltipRenderUtilMixin`. |

## Фаза 8 — генерация данных

В репозитории нет исходников datagen (`src/generated` отсутствует, слушателей `GatherDataEvent` нет). Переносить нечего; конфигурация запуска `data` по-прежнему объявлена для зависимых проектов.

## Исправления в рантайме (найдены при запуске datagen Valoria с этим jar)

- **Строгость шины событий** — шина NeoForge выбрасывает исключение там, где Forge молча игнорировал: `forgeBus.register(this)` в `Tridot` (нет методов `@SubscribeEvent` у экземпляра) удалён, а `common.Events.onServerTick` был `static` внутри объекта, зарегистрированного как экземпляр. В 1.20.1 Forge пропускал этот метод, поэтому рассылка `DungeonSoundPacket` для музыки данжей **никогда не выполнялась**; теперь это метод экземпляра, и он активен. Чтобы вернуть старое («мёртвое») поведение, уберите его `@SubscribeEvent`.

- **Ключи подгружаемых моделей** (найдено при первом реальном запуске клиента) — `ModelEvent.RegisterAdditional.register` в 1.21 принимает только вариант `standalone`, а аварийная перезагрузка ресурсов Minecraft затем повторно вызывала `FMLCommonSetupEvent` у всех модов. В 1.20.1 модели скинов / луков / арбалетов / `_in_hand` регистрировались с вариантом `inventory`, который загружал `models/item/<name>.json`; теперь это `ModelResourceLocation.standalone(<modid>:item/<name>)` (`TridotModels.sideLoaded`, `SkinRegistryManager.getModelLocationSkin`, `LargeItemRenderer.getModelResourceLocation`), так что загружаются те же файлы, а все поиски используют тот же ключ. Базовая модель предмета больше не передаётся в `RegisterAdditional` (она запекается автоматически; ключ `inventory` был бы отклонён). `TridotModels.addCustomModel` теперь возвращает `standalone(<modid>:<model>)` — вызывающие, рассчитывавшие на старый вариант blockstate `""`, должны передавать `block/<name>`.

- **Шейдеры** (второй запуск клиента) — ванильный `fog.glsl` 1.21 изменил `fog_distance(mat4, vec3, int)` на `fog_distance(vec3, int)` (позиции теперь относительно камеры). Обёртка `fogDistance` в `include/common.glsl` сохраняет сигнатуру 1.20.1, но игнорирует матрицу, поэтому все базовые шейдеры Tridot (`additive`, `additive_texture`, `translucent`, `translucent_texture`) и зависимые, включающие `tridot:common.glsl`, снова компилируются. Неудачный `RegisterShadersEvent` заставляет Minecraft бесконечно повторять перезагрузку ресурсов (зависание на экране Mojang) — так это и обнаружилось.

- **Проверка структуры для музыки данжей** — `MusicModifier.DungeonMusic.isPlayerInStructure` вызывал `getBoundingBox()` у `StructureStart.INVALID_START` (игрок вне структуры), что выбрасывает исключение и обрушивало серверный тик, как только заработал `Events.onServerTick`. Для невалидного старта возвращается false.
- **Цели в условиях лута** — в 1.21 ванилла переименовала `killer/direct_killer/killer_player` в `attacker/direct_attacker/attacking_player`. `TargetedLootCondition.TARGET_CODEC` принимает оба написания (записывает новое), чтобы модификаторы лута из 1.20.1, например у Valoria, продолжали загружаться.
- **Наборы спрайтов экранных частиц** (первый игровой тест) — `TridotScreenParticles.registerParticleFactory` берёт наборы спрайтов мировых частиц из `ParticleEngine.spriteSets`, где они появляются только после выполнения `TridotParticles.ClientRegistryEvents.registerParticles`. Оба слушают `RegisterParticleProvidersEvent`; NeoForge вызвал слушатель экранных частиц первым, поэтому у всех GUI-частиц спрайт был null, и первая же отрисованная (предмет в Кодексе Valoria) обрушила поток рендера. Теперь слушатель имеет приоритет `EventPriority.LOWEST`, `TridotScreenParticleType.Factory` принимает также id частицы и лениво находит набор спрайтов при первом использовании, а `GenericScreenParticle.render` пропускает частицу без спрайта. Код 1.20.1 полагался на тот же неопределённый порядок и работал по стечению обстоятельств.
- **Подсказки курио с атрибутами слотов** — `AttributeUtilMixin` пересобирал карту модификаторов через `AttributeUtil.sortedMap()` — `TreeMultimap`, сортируемый по `Holder#getKey`. Curios 9 помещает в карту свои атрибуты слотов как `Holder.direct(SlotAttribute)`, у которых `getKey()` равен null, поэтому компаратор выбрасывал исключение (проявилось, когда JEI строил поисковый индекс по всем курио). Копия теперь повторяет форму входной карты: `sortedMap()`, если вход был `SortedSetMultimap`, иначе `LinkedHashMultimap`, так что порядок, выбранный вызывающим кодом, сохраняется.

## Не проверено в рантайме

Эти части компилируются и соответствуют API 1.21.1, но не были опробованы в работающей игре:

- **Подмена кодека DotStyle** — зависит от того, что `Style.Serializer` инициализируется до того, как `ComponentSerialization.createCodec` прочитает `MAP_CODEC` (читается лениво через `Codec.recursive`). Если какой-то мод форсирует кодек компонентов очень рано, эффекты молча не будут сериализоваться.
- **`LootDataTypeMixin`** — `@ModifyVariable` на обобщённом аргументе `V value` в `LootDataType.deserialize`; срабатывает только когда значение — `JsonObject`, что верно при загрузке датапаков.
- **`AttributeUtilMixin` / Curios** — теперь опробовано в игре (индексация всех подсказок курио в JEI проходит через него без ошибок), но будущий релиз Curios всё ещё может перестать вызывать `AttributeUtil.applyTextFor`.
- **Захваты локальных переменных в `StringRenderOutputMixin`** — `CAPTURE_FAILSOFT`; если coremod изменит раскладку локальных переменных `Font.StringRenderOutput.accept`, хуки глифов будут пропущены, а не вызовут падение.
- **Отложенные буферы рендера под Iris** — `shadersDelayedRender` перенесён 1:1 на `Matrix4fStack`; конвейер Iris для 1.21 не тестировался.
- **`GenericParticleRenderType`** возвращает пустой покадровый билдер, поскольку частицы Tridot пишут в отложенные буферы рендера; `ParticleEngine` пропускает загрузку пустых мешей.
- **Процентная броня через `LivingIncomingDamageEvent`** — срабатывает раньше старого `LivingHurtEvent`; порядок относительно модификаторов урона других модов может отличаться.
- **Теги зачаровываемых предметов** — зависимые моды должны добавить свои предметы в теги `tridot:enchantable/*`; раньше это обеспечивали предикаты `EnchantmentCategory`.
- **Attachments вместо capabilities** — `copyOnDeath` повторяет старое копирование в `PlayerEvent.Clone`; провайдеры, полагавшиеся на ленивую семантику `LazyOptional`, теперь создаются сразу.
- **`sendToTracking(chunk)`** — собственный распределитель Tridot `TRACKING_CHUNK_AND_NEAR` (игроки, отслеживающие чанк *и* находящиеся в пределах 64 блоков) переписан на `ChunkMap.getPlayers`; семантика та же, но с большим числом игроков ещё не проверялась.
- **Двойной инкремент `ClientTick`** (счётчики тиков увеличиваются и в событии тика, и в событии рендера) сохранён как есть для совместимости.
- **Точки крепления пассажиров лодок**, **шанс выбить щит**, **применение зачарований к снарядам через `EnchantmentHelper.onProjectileSpawned`** — семантика ванильной 1.21.1, покадрово с 1.20.1 не сравнивалась.
- **Поле лицензии в `mods.toml`** объявляет `GPL-2.0`, как в оригинальном репозитории, тогда как прилагаемый `LICENSE` — GPL-3.0; оба сохранены без изменений.
