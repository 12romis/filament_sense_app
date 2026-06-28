# FilamentSenseApp — Android

## Що це
Android-додаток (Kotlin + Jetpack Compose + Material 3) для керування ESP32-пристроєм FilamentSense через BLE. Відображає вагу котушок з філаментом, температуру/вологість в боксі, стан принтера Bambu P1S, дозволяє надсилати команди на принтер через ESP32.

**UX-контекст**: додаток тримається в руці біля 3D-принтера в напівтемному приміщенні — великі елементи, темна тема, ключова інформація на першому екрані.

## Пов'язаний проєкт
**ESP32 прошивка**: `../../FilamentSense/` (відносно кореня цього репо)
Android — BLE central (клієнт), ESP32 — BLE peripheral (сервер). Весь обмін через BLE GATT.

---

## Технічний стек

| Компонент | Рішення |
|-----------|---------|
| Мова | Kotlin 2.1.20 |
| UI | **Jetpack Compose** + Material 3 (BOM 2025.03.00) |
| DI | Hilt 2.51.1 |
| Навігація | Navigation Compose 2.8.9 |
| BLE | blessed-android 2.5.2 (`com.welie.blessed`) |
| БД | Room 2.7.0 |
| Async | Coroutines + Flow / StateFlow |
| Графіки | Vico 2.0.1 (`vico.compose.m3`) |
| minSdk | 31 (Android 12), targetSdk 36 |
| AGP | 9.1.0, KSP 2.1.20-2.0.1 |

---

## Тема та кольори

**Dark-only** — `darkColorScheme()`, `lightColorScheme()` не додавати.
Всі кольори визначені у `ui/theme/Color.kt`. У composable — **тільки `MaterialTheme.colorScheme.*`**, не хардкодити hex.

```kotlin
// ui/theme/Color.kt
Primary            = Color(0xFFFFB300)   // Amber — CTA, активні стани
OnPrimary          = Color(0xFF3D2900)
PrimaryContainer   = Color(0xFF563D00)   // чіпи, бейджі
OnPrimaryContainer = Color(0xFFFFDF9B)
Secondary          = Color(0xFF4DB6AC)   // Teal — сенсори, вторинні дії
OnSecondary        = Color(0xFF003731)   // УВАГА: темний колір, погано видно на surface
SecondaryContainer = Color(0xFF005048)   // карточки env-даних
OnSecondaryContainer = Color(0xFF70F7E5)
Background         = Color(0xFF121212)
Surface            = Color(0xFF1E1E1E)
SurfaceVariant     = Color(0xFF2C2C2C)
OnSurface / OnBackground = Color(0xFFE6E1E5)
OnSurfaceVariant   = Color(0xFFCAC4D0)
Error              = Color(0xFFF2B8B5)
// Додаткові:
StatusConnected    = Color(0xFF4CC96E)   // зелений — "Підключено"
StatusConnectedBg  = Color(0xFF12331F)
```

### Дизайн-правила (зафіксовані вручну)
- Кольорові крапки котушок — `Ellipse` без іконок
- Прогрес-бар показує **номінальну вагу** ("3000 г"), не залишок
- Рядок "Активна котушка" — фон `SecondaryContainer`
- Spacing кратний 4dp: `8.dp`, `16.dp`, `24.dp`

---

## Figma
**Файл**: https://www.figma.com/design/dqI0XvbtxJ2kJF7cYbSQxg
Сторінки: `🎨 Foundation` (токени), `🧩 Components`, `📱 Screens`

| Node ID | Екран |
|---------|-------|
| `17:2`  | Головний — підключено (Hero + активна котушка + env) |
| `17:68` | Пристрій не додано (empty state) |
| `18:2`  | Пошук BLE (radar + список) |
| `18:35` | Список котушок |
| `19:2`  | Деталі котушки |
| `26:2`  | Налаштування |
| `26:49` | Створення котушки |
| `26:111`| Редагування котушки |

---

## Архітектура (Clean Architecture + MVVM + Compose)

**Підхід UI**: виключно **Jetpack Compose**. Не використовувати XML layout, View або Fragment.

```
presentation/ ← domain/ ← data/
   (UI)         (логіка)  (BLE, DB)
```

```
com.filament.sense/
├── data/
│   ├── ble/
│   │   ├── BleManager       # Singleton BLE Central, state flows, sendCommand()
│   │   ├── BleDataParser    # bytes → domain models + buildXxxCmd()
│   │   └── GattConstants    # UUID характеристик, DEVICE_NAME
│   ├── local/               # Room: AppDatabase, SpoolDao, MeasurementDao, entities
│   └── repository/
├── di/                      # AppModule (Room, UseCases), BleModule (BleManager)
├── domain/
│   ├── model/               # SpoolSlot, EnvData, PrinterStatus, ConfigData…
│   ├── repository/          # Інтерфейси
│   └── usecase/             # Один UseCase на дію
└── ui/
    ├── theme/               # Color.kt, Theme.kt, Typography.kt
    ├── navigation/NavGraph  # Compose Navigation граф
    ├── components/          # Shared composables (BottomNav, DataRow, EnvDataCard…)
    └── screen/              # home/, printer/, scan/, spools/, spool/, analytics/, settings/
```

### Clean Architecture межі — заборони
- `domain/` і `data/` **не можуть** імпортувати `androidx.compose.*`
- Кольори в `SpoolEntity` і `SpoolSlot` — `Int` (ARGB), не `Color`
- Лише `ui/` імпортує Compose-бібліотеки

### ViewModel патерн (обов'язковий)
```kotlin
@HiltViewModel
class XxxViewModel @Inject constructor(...) : ViewModel() {
    private val _state = MutableStateFlow(XxxUiState())
    val state: StateFlow<XxxUiState> = _state.asStateFlow()
}

data class XxxUiState(          // один immutable data class, не окремі поля
    val deviceState: DeviceState = DeviceState.DISCONNECTED,
    val spools: List<SpoolSlot> = emptyList(),
)
```

---

## BLE протокол

**Service UUID**: `4fafc201-1fb5-459e-8fcc-c5c9c331914b`

| Characteristic | UUID (suffix) | Напрям | Формат |
|----------------|---------------|--------|--------|
| SPOOL_DATA | `...26a0` | ESP32→Android | 21 байт binary |
| ENV_DATA | `...26b0` | ESP32→Android | 12 байт binary |
| CMD | `...26b2` | Android→ESP32 | JSON рядок |
| CONFIG | `...26b3` | Обидва | JSON рядок |
| PRINTER_STATUS | `...26b4` | ESP32→Android | JSON рядок |

MTU запитується 512 байт. `CONNECTED` виставляється в `onMtuChanged`, не в `onConnectedPeripheral`.
Повний формат байтів — у `BleDataParser.kt` і `../../FilamentSense/CLAUDE.md`.

---

## Екрани та навігація

| Маршрут | Екран |
|---------|-------|
| `home` | BLE статус, env дані, активна котушка |
| `printer` | Стан Bambu P1S, heat bed, reprint |
| `scan` | Пошук і підключення BLE |
| `spools` | Список котушок |
| `spool/{id}` | Деталі, виміри, графік ваги |
| `analytics` | Аналітика (placeholder) |
| `settings` | Пороги, MQTT, калібрування |

**Навігаційний нюанс**: маршрут `spools/create` **обов'язково** реєструвати в NavHost **раніше** `spools/{index}` — інакше "create" розпізнається як index.

### PrinterScreen — identity card
Ліворуч: фото принтера (`p1s_printer.png` — PNG з alpha, прозорий фон, `ContentScale.Fit`).
Праворуч: назва, gcode-статус badge, стан BLE (colored dot), час синхронізації.
`lastSyncTime` трекується в `PrinterViewModel` через `viewModelScope.launch { printerStatus.collect {...} }`.

---

## Правила кодування

- **Тільки Compose** — без XML, View, Fragment
- **StateFlow, не LiveData** — `collectAsStateWithLifecycle()`
- **Immutable UiState** — один `data class XxxUiState` на ViewModel
- **Hilt скрізь** — `@HiltViewModel` + `@Inject constructor`
- **Тільки `MaterialTheme.colorScheme.*`** у composable — всі значення в `Color.kt`
- Не використовувати `Icons.Extended` — тільки `material-icons-core`
- Drawable names: лише `[a-z0-9_.]`, **без дефісів** (AAPT2 відмовляє)
- `fillType="evenOdd"` у vector drawable — ок (minSdk=31)

---

## Відомі проблеми збірки

### gradle.properties — обов'язково
```properties
android.builtInKotlin=false
android.newDsl=false
```
Без цих прапорів AGP 9.x → помилка "Cannot add extension with name 'kotlin'".

### KSP STAR null bug
`KSTypeArgument.type should not have been null` при збірці через термінал (Room + KSP annotation processing).
**Рішення**: збирати через **Android Studio**. CLI-фікс не знайдено.

Якщо потрібно збирати через термінал:
```bash
JAVA_HOME=/home/romiskub/Applications/android-studio/jbr ./gradlew :app:compileDebugKotlin
```

### Залежності — версії зафіксовані
- `blessed-android`: **max 2.5.2** (JitPack не публікує 3.x)
- `Room`: залишати на **2.7.0** (2.8.x вмикає KSP2, ризик погіршити bug)
- Не оновлювати без тестування збірки

---

## Оновлення цього файлу
**Оновлювати при**: нові екрани або маршрути, зміна BLE протоколу, нові команди, зміна dependency stack, нові domain models, нові build-проблеми.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
