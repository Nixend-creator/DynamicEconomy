# Changelog

## [1.2.2] - 2026-02-20 — GUI критические фиксы + Auction GUI

### Исправлено
- **Главное меню — переходы в категории не работали**
  `GuiListener` использовал `event.getInventory()` для получения holder'а.
  В Paper 1.21 этот метод возвращает inventory где физически произошёл клик —
  при клике в нижней части экрана (инвентарь игрока) возвращался `HumanEntity`,
  а не `GuiHolder`. Весь роутинг молча падал.
  **Исправление:** везде заменено на `event.getView().getTopInventory()`.

- **Константы CategoryGui не были public** — `SLOT_BACK`, `SLOT_PREV`, `SLOT_NEXT`,
  `SLOT_HEADER` объявлены без модификатора доступа, `GuiListener` из другого пакета
  не мог их читать. Добавлен `public`.

- **RegionalMarketService — WG API**
  `WorldGuardPlugin.inst().wrapWorld()` удалён в WG 7.0.7+.
  Заменено на `com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world)`.
  Итератор регионов явно типизирован (`ProtectedRegion r`), что убрало
  ошибку с `getPriority()` при type inference.

### Добавлено
- **AuctionGui** — полноценный GUI для аукционного дома:
  - 28 слотов лотов (6-рядный инвентарь, та же сетка что у CategoryGui)
  - Каждый лот: название, продавец, количество, цена, рыночная цена сервера, время истечения
  - ЛКМ по лоту — купить (чужой) / отменить (свой)
  - Кнопка «Мои лоты» — переключение между всеми и своими лотами
  - Пагинация
  - `/auction` теперь открывает GUI

---

## [1.2.1] - 2026-02-20 — Стабильный билд

### Исправлено
- GUI идентификация: `GuiHolder` вместо title-строк
- Иконки перетаскивались: добавлен `InventoryDragEvent` handler
- Новые предметы не добавлялись: version-aware `saveResource` в `MarketLoader`
- `GUIHelper.makeItem()` не существовал — заменён на `GUIHelper.item()`
- kyori Adventure broadcast заменён на `broadcastMessage()`

### Добавлено
- `GuiHolder` — custom `InventoryHolder` с `GuiType` enum
- MySQL / SQLite поддержка через `DatabaseManager`
- `config-version: 2` в `items.yml`

---

## [1.2.0] - 2026-02-20
- Режим покупки, история цен, казна, события рынка, лицензии, аукцион, регионы, REST API, PlaceholderAPI

## [1.1.0] - 2026-02-20
- 239 предметов, 11 категорий

## [1.0.0] - 2026-02-18
- Первый релиз
