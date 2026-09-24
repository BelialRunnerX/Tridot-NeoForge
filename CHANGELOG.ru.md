# История изменений — порт Tridot на NeoForge 1.21.1

> 🇬🇧 Основная версия — [CHANGELOG.md](CHANGELOG.md) (английский). Перевод может отставать.

Сначала новые записи. Версия мода остаётся `1.21.1-1.0.169` (Tridot 1.0.169 из оригинального репозитория); записи обозначаются датой и коммитом. Каждое изменение в коде помечено комментарием `// PORT NOTE:` и описано в [PORTING.ru.md](PORTING.ru.md). Открытые проблемы — в [KNOWN_ISSUES.ru.md](KNOWN_ISSUES.ru.md).

## 2026-09-23 — исправления после первого игрового теста

- `ded8025` **Исправлено** серверное `NullPointerException` в `AbstractTridotArrow.getDefaultPickupItem` при создании любой стрелы Tridot (Phantasm Bow из Valoria ничего не выпускал): конструктор ванильного `AbstractArrow` в 1.21 вызывает этот метод до инициализации поля подкласса `arrowItem`, поэтому теперь метод устойчив к null.
- `7e92bd1` **Исправлено** падение клиента «Rendering screen … `particle.sprite` is null» при отрисовке GUI-частицы (впервые проявилось при открытии Кодекса Valoria, где у предметов в записях есть частицы-огоньки). `TridotLibClient.registerParticleFactory` выполнялся раньше, чем `TridotParticles` регистрировал мировые частицы, поэтому все фабрики экранных частиц захватывали пустой набор спрайтов. Теперь слушатель работает с приоритетом `LOWEST`, `TridotScreenParticleType.Factory` находит набор спрайтов лениво по id, а частица без спрайта пропускается вместо падения.
- `7e92bd1` **Исправлено** `NullPointerException` в `AttributeUtilMixin` во время индексации подсказок курио в JEI: Curios 9 передаёт свои атрибуты слотов как `Holder.direct(...)` (без ключа реестра), а миксин копировал их в `AttributeUtil.sortedMap()`, компаратор которой сортирует по ключу. Пересобранная карта теперь сохраняет форму входной (`LinkedHashMultimap`, если вход не был отсортированным).

## 2026-09-23 — исправления после первых тестов клиента и сервера

- `c957952` **Исправлено** падение серверного тика «Unable to calculate boundingbox without pieces»: `MusicModifier.DungeonMusic.isPlayerInStructure` теперь возвращает false, когда игрок находится вне подходящей структуры, вместо запроса bounding box у `StructureStart.INVALID_START`.
- `a60f926` **Изменено**: условия лута с целью (`tridot:mob_effect`) принимают названия целей из 1.20.1 — `killer`, `direct_killer`, `killer_player` — как синонимы названий 1.21 `attacker`, `direct_attacker`, `attacking_player`, чтобы существующие датапаки продолжали загружаться.
- `fb7f1d1` **Исправлено**: все базовые шейдеры Tridot не компилировались на 1.21.1 (у `fog_distance` исчез параметр-матрица), из-за чего игра зависала на экране загрузки Mojang. `include/common.glsl` сохраняет старую обёртку `fogDistance(mat4, vec3, int)` для зависимых модов.
- `774c83c` **Исправлено** «Side-loaded models must use the 'standalone' variant» при первом запуске: модели скинов, луков, арбалетов и `_in_hand` регистрируются и ищутся по ключу `ModelResourceLocation.standalone(<mod>:item/<name>)`; базовая модель предмета больше не передаётся в `RegisterAdditional`.
- `6578718` **Исправлено**: сбой конструирования мода из-за строгой шины событий NeoForge: удалён `forgeBus.register(this)` (не было слушателей-экземпляров), а `Events.onServerTick` стал методом экземпляра. Примечание: этот обработчик (пакеты музыки данжей) на 1.20.1 никогда не выполнялся, а теперь активен.

## 2026-09-23 — первоначальный порт (`dcb1df6`)

- Forge 1.20.1 / Java 17 → NeoForge 21.1.251 / Java 21, ModDevGradle 2.0.147, Parchment 2024.11.17.
- Реестры, события, сеть (`CustomPacketPayload`), capabilities → data attachments, зачарования → данные с тегами `tridot:enchantable/*`, рендер (`Matrix4fStack`, новый API вершин), миксины перенацелены, access transformer переписан на имена Mojang.
- Опубликовано в локальный Maven как `pro.komaru:Tridot:1.21.1-1.0.169` (jar, `api`, `sources`).
