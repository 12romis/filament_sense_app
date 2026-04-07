# FilamentSense

Android-додаток для моніторингу котушок філаменту 3D-принтера через BLE.

## Призначення

FilamentSense підключається до ESP32-C6 пристрою ("FilamentSense") по Bluetooth Low Energy і в реальному часі відображає:
- залишок філаменту (г) по активній котушці
- температуру та вологість навколишнього середовища
- графік витрати філаменту

---

## Функціонал

| Екран | Опис |
|---|---|
| **Home** | Активна котушка + BLE-показники (залишок, температура, вологість) |
| **Spool List** | Список усіх котушок з індикатором залишку |
| **Spool Detail** | Детальна інфо + графік вимірювань за 24 год |
| **Spool Create/Edit** | Додавання та редагування котушки (назва, колір, матеріал, вага) |
| **Scan** | BLE-сканування та підключення до пристрою |
| **Settings** | Налаштування застосунку |
| **Analytics** | (заплановано) |

---

## Технічний стек

| Шар | Технологія |
|---|---|
| UI | Jetpack Compose + Material 3 (dark-only theme) |
| Навігація | Navigation Compose 2.8.9 |
| DI | Hilt 2.51.1 + KSP |
| База даних | Room 2.7.0 |
| BLE | blessed-android 2.5.2 |
| Графіки | Vico 2.0.1 (`compose-m3`) |
| Архітектура | Clean Architecture + MVVM |

---

## Архітектура

```
presentation (ui)
    └── screen/*, components/*, navigation/
domain
    └── model/, repository/ (інтерфейси), usecase/
data
    └── ble/, local/ (Room), repository/ (реалізації), di/
```

Залежності спрямовані тільки вниз: `ui → domain ← data`.

---

## База даних (Room)

### `spools`
| Колонка | Тип | Опис |
|---|---|---|
| `index` | INT (PK) | Номер слота |
| `name` | TEXT | Назва котушки |
| `colorArgb` | INT | Колір (ARGB, default = White) |
| `nominalWeightGrams` | INT | Номінальна вага філаменту, г |
| `baselineWeight` | REAL | Вага порожньої котушки (tare), г |
| `isActive` | INTEGER | Активна котушка (0/1) |
| `startDate` | INTEGER? | Дата початку використання (ms) |

### `measurements`
| Колонка | Тип | Опис |
|---|---|---|
| `id` | INTEGER (PK, autoincrement) | |
| `spoolIndex` | INT (FK → spools) | CASCADE DELETE |
| `remainingGrams` | REAL | Залишок філаменту, г |
| `temperature` | REAL? | Температура, °C |
| `humidity` | REAL? | Вологість, % |
| `timestamp` | INTEGER | Час виміру (ms) |

Вимірювання записуються кожні **5 хвилин** автоматично з BLE-даних.

---

## BLE-протокол

Пристрій: `FilamentSense` (ESP32-C6)

| Характеристика | UUID (suffix) | Дані |
|---|---|---|
| Spool Data | `...0001` | Залишок |
| Environment | `...0002` | Температура + вологість |
| Filament Sensor | `...0003` | Наявність філаменту |

---

## Вимоги

- **Android**: 12+ (API 31)
- **Bluetooth**: BLE підтримка + дозволи `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`
- **Kotlin**: 2.1.20
- **AGP**: 9.1.0

### Відомі проблеми збірки

AGP 9.1.0 потребує двох прапорів у `gradle.properties`:
```properties
android.builtInKotlin=false
android.newDsl=false
```

KSP STAR null bug (KSP 2.1.20-2.0.1) може виникати при збірці через термінал — збирати через **Android Studio**.

---

## Структура проєкту

```
app/src/main/java/com/filament/sense/
├── data/
│   ├── ble/          # GattConstants, BleDataParser, BleManager
│   ├── local/        # AppDatabase, SpoolDao, MeasurementDao, entities/
│   └── repository/   # SpoolRepositoryImpl, DeviceRepositoryImpl
├── di/               # AppModule, BleModule
├── domain/
│   ├── model/        # SpoolSlot, Measurement, DeviceState, EnvData
│   ├── repository/   # SpoolRepository, DeviceRepository (інтерфейси)
│   └── usecase/      # GetSpools, SetActiveSpool, SetBaseline, UpdateSpoolConfig, GetMeasurements
└── ui/
    ├── components/   # BottomNav, EnvDataCard, SpoolListItem, StatusBadge, DataRow, ThresholdBar
    ├── navigation/   # NavGraph, Screen
    ├── screen/       # home/, scan/, spool/, spools/, settings/
    └── theme/        # Color, Typography, Theme
```
