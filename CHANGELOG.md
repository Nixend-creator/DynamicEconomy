# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Функции в разработке

## [1.0.0] - 2026-02-18

### Added
- Начальный релиз
- Динамическая система цен (supply & demand)
- 5 категорий товаров: Фермерство, Скотоводство, Ресурсы, Редкости, Дерево
- GUI: главное меню → категории с пагинацией → экран подтверждения продажи
- Сезонный спрос — ротация горячей категории с бонусом x1.5
- Система контрактов — крупные заказы с бонусом +40% к цене
- Бонус за разнообразие — продажа 3+ разных категорий в 30 мин
- JSON persistence — сохранение цен между рестартами
- Мультиязычность (RU / EN)
- Публичное API для интеграции с другими плагинами
- Команды /shop и /shopadmin (reload, reset, setprice, info)
- GitHub Actions CI
- Unit тесты (PriceCalculator, MarketItem, MessageUtils)
