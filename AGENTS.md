# FilamentSense Android — AGENTS.md

## Що це за проєкт

Android BLE-застосунок для моніторингу філаменту 3D-принтера.  
Підключається по Bluetooth Low Energy до ESP32-C6 пристрою ("FilamentSense"),  
відображає в реальному часі: вагу котушок, залишок філаменту, температуру, вологість.

**Контекст використання**: тримається в руці біля 3D-принтера, в напівтемному приміщенні → великі елементи, темна тема, ключова інформація на першому екрані.

---

## Дизайн (Figma)

Усі макети готові у Figma. **Завжди читай їх перед реалізацією екрана.**

**Figma файл**: https://www.figma.com/design/dqI0XvbtxJ2kJF7cYbSQxg  
Сторінки: `🎨 Foundation` (токени), `🧩 Components`, `📱 Screens`

Якщо є доступ до Figma MCP — використовуй `get_design_context` з node ID екрана.  
Якщо немає — читай дизайн-токени нижче і роби відповідно до описів екранів.

### Node IDs екранів (📱 Screens)
| Node ID | Екран |
|---------|-------|
| `17:2`  | Головний — пристрій підключено (Hero + активна котушка + env) |
| `17:68` | Пристрій не додано (empty state + CTA) |
| `18:2`  | Пошук пристрою BLE (radar + список) |
| `18:35` | Список котушок (5 котушок, progress bars, FAB) |
| `19:2`  | Деталі котушки (вага, пороги, toggle активності) |
| `26:2`  | Налаштування (тільки "Відключити пристрій") |
| `26:49` | Створення котушки (форма: назва, колір, номінал) |
| `26:111`| Редагування котушки (та сама форма заповнена) |
| `30:2`  | Нова котушка — M3 AlertDialog |
| `30:71` | Деталі котушки — M3 AlertDialog "Зробити активною?" |

### Дизайн-токени (Material 3 Dark)
```kotlin
// ui/theme/Color.kt
val Primary            = Color(0xFFFFB300)  // Amber — CTA, активні стани
val OnPrimary          = Color(0xFF3D2900)
val PrimaryContainer   = Color(0xFF563D00)  // чіпи, бейджі
val OnPrimaryContainer = Color(0xFFFFDF9B)
val Secondary          = Color(0xFF4DB6AC)  // Teal — сенсори, вторинні дії
val OnSecondary        = Color(0xFF003731)
val SecondaryContainer = Color(0xFF005048)  // карточки env-даних
val OnSecondaryContainer = Color(0xFF70F7E5)
val Background         = Color(0xFF121212)
val Surface            = Color(0xFF1E1E1E)
val SurfaceVariant     = Color(0xFF2C2C2C)
val OnBackground       = Color(0xFFE6E1E5)
val OnSurface          = Color(0xFFE6E1E5)
val OnSurfaceVariant   = Color(0xFFCAC4D0)
val Error              = Color(0xFFF2B8B5)
val OnError            = Color(0xFF601410)
val Outline            = Color(0xFF938F99)
```

### Дизайн-правила (зафіксовані вручну)
- Кольорові крапки котушок — прості `Ellipse`, без іконок
- Прогрес-бар праворуч показує **номінальну вагу** ("3000 г"), не залишок
- Рядок "Активна котушка" — фон `SecondaryContainer` (teal)
- Налаштування — тільки "Відключити пристрій" (без "Видалити")
- M3 AlertDialog: ширина 312dp, cornerRadius 28dp, TextButton без роздільника, кнопки right-aligned, іконка без фону-контейнера

---

## Технічний стек

| Компонент | Рішення | Версія |
|-----------|---------|--------|
| Мова | Kotlin | 2.1.20 |
| AGP | Android Gradle Plugin | 9.1.0 |
| KSP | Kotlin Symbol Processing | 2.1.20-2.0.1 |
| UI | Jetpack Compose + Material 3 | BOM 2025.03.00 |
| DI | Hilt | 2.51.1 |
| DI nav | hilt-navigation-compose | 1.3.0 |
| Навігація | Navigation Compose | 2.8.9 |
| BLE | `com.github.weliem:blessed-android` | 2.5.2 |
| Local DB | Room | 2.7.0 |
| Async | Coroutines + Flow/StateFlow | |
| Графіки | Vico (`com.patrykandpatrick.vico:compose-m3`) | 2.0.1 |
| Шрифт | Roboto (системний) | |
| Min SDK | 31 (Android 12) | |
| Target SDK | 36 | |

---

## Архітектура

**Clean Architecture + MVVM** — три шари з чіткими межами:

```
presentation/  ←  domain/  ←  data/
   (UI)          (логіка)    (BLE, DB)
```

### Пакетна структура
```
com.filament.sense/
├── MainActivity.kt
├── di/
│   ├── AppModule.kt          # Hilt: Room, репозиторії
│   └── BleModule.kt          # Hilt: BleManager
├── domain/
│   ├── model/
│   │   ├── SpoolSlot.kt      # index, name, material, baselineWeight, remainingGrams, hasFilament
│   │   ├── EnvData.kt        # temperature, humidity, pressure
│   │   └── DeviceState.kt    # enum: DISCONNECTED, SCANNING, CONNECTING, CONNECTED
│   ├── repository/
│   │   ├── SpoolRepository.kt      # interface
│   │   └── DeviceRepository.kt     # interface
│   └── usecase/
│       ├── GetSpoolsUseCase.kt
│       ├── GetMeasurementsUseCase.kt   # 24h measurements from Room
│       ├── SetActiveSpoolUseCase.kt
│       ├── SetBaselineUseCase.kt
│       └── UpdateSpoolConfigUseCase.kt
├── data/
│   ├── ble/
│   │   ├── BleManager.kt           # blessed-android: scan, connect, notify
│   │   ├── GattConstants.kt        # UUID константи
│   │   └── BleDataParser.kt        # bytes → domain models
│   ├── local/
│   │   ├── AppDatabase.kt          # Room DB, version = 1
│   │   ├── dao/SpoolDao.kt
│   │   ├── dao/MeasurementDao.kt
│   │   └── entity/
│   │       ├── SpoolEntity.kt
│   │       └── MeasurementEntity.kt  # FK → spools CASCADE, indices: spoolIndex, timestamp
│   └── repository/
│       ├── SpoolRepositoryImpl.kt    # Room + BLE; зберігає вимірювання кожні 5 хв
│       └── DeviceRepositoryImpl.kt
└── ui/
    ├── theme/
    │   ├── Color.kt
    │   ├── Typography.kt        # M3 type scale, Roboto
    │   └── Theme.kt             # FilamentSenseTheme (dark only)
    ├── navigation/
    │   └── NavGraph.kt          # sealed class Screen + NavHost
    ├── components/              # shared composables
    │   ├── BottomNav.kt
    │   ├── StatusBadge.kt
    │   ├── ThresholdBar.kt
    │   ├── SpoolListItem.kt
    │   ├── DataRow.kt
    │   └── EnvDataCard.kt
    └── screen/
        ├── home/
        │   ├── HomeScreen.kt
        │   └── HomeViewModel.kt
        ├── scan/
        │   ├── ScanScreen.kt
        │   └── ScanViewModel.kt
        ├── spools/
        │   ├── SpoolListScreen.kt
        │   ├── SpoolListViewModel.kt
        │   ├── SpoolFormScreen.kt      # спільна форма для Create/Edit
        │   ├── SpoolCreateScreen.kt
        │   ├── SpoolCreateViewModel.kt
        │   ├── SpoolEditScreen.kt
        │   └── SpoolEditViewModel.kt   # SavedStateHandle["index"] для завантаження
        ├── spool/
        │   ├── SpoolDetailScreen.kt    # Vico LineChart (24h history)
        │   └── SpoolDetailViewModel.kt
        └── settings/
            ├── SettingsScreen.kt
            └── SettingsViewModel.kt
```

### ViewModel патерн (однаковий для всіх)
```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSpools: GetSpoolsUseCase,
    private val deviceRepo: DeviceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(deviceRepo.deviceState, getSpools()) { device, spools ->
                HomeUiState(deviceState = device, spools = spools)
            }.collect { _state.value = it }
        }
    }
}

data class HomeUiState(
    val deviceState: DeviceState = DeviceState.DISCONNECTED,
    val spools: List<SpoolSlot> = emptyList(),
    val activeSpool: SpoolSlot? = null,
    val envData: EnvData? = null,
)
```

### Навігація
```kotlin
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scan : Screen("scan")
    object SpoolList : Screen("spools")
    object SpoolDetail : Screen("spools/{index}") {
        fun createRoute(index: Int) = "spools/$index"
    }
    object SpoolCreate : Screen("spools/create")
    object Settings : Screen("settings")
}
```

---

## BLE GATT протокол

**Пристрій**: ESP32-C6 з іменем `"FilamentSense"`

### UUID сервісу та характеристик

UUID зафіксовані у прошивці (`src/config/BleConfig.h`). Використовуй **точно ці значення** — зміна будь-якого UUID зламає з'єднання з пристроєм.

```kotlin
// data/ble/GattConstants.kt
import java.util.UUID

object GattConstants {
    // BLE device name to scan for
    const val DEVICE_NAME = "FilamentSense"

    // Primary GATT service
    val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

    // Read + Notify: 12 bytes (remaining: Float, gross: Float, hasFilament: Float)
    // One characteristic per spool slot (slots 0–4)
    val SPOOL_DATA_UUIDS = listOf(
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a0"),
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a1"),
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a2"),
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a3"),
        UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a4"),
    )

    // Read + Notify: 12 bytes (temperature: Float, humidity: Float, pressure: Float)
    val ENV_DATA_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b0")

    // Read: 1 byte — number of active spool slots (UInt8)
    val SPOOL_COUNT_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b1")

    // Write (no response): JSON command string
    // {"cmd":"save_baseline","slot":0}
    // {"cmd":"set_name","slot":0,"value":"PLA White"}
    // {"cmd":"set_tare","slot":0,"value":230.5}
    // {"cmd":"set_threshold","warning":500,"critical":100,"empty":10}
    val CMD_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b2")

    // Read + Write: JSON config string (thresholds, spool names)
    val CONFIG_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26b3")
}
```

### Формат команди (CMD_UUID, Write)
```json
{"cmd": "save_baseline", "slot": 0}
{"cmd": "set_name", "slot": 0, "value": "PLA White"}
{"cmd": "set_tare", "slot": 0, "value": 230.5}
{"cmd": "set_threshold", "warning": 500, "critical": 100, "empty": 10}
```

### Парсинг даних слоту (SPOOL_DATA)
```kotlin
// BleDataParser.kt
fun parseSpoolData(bytes: ByteArray, index: Int): SpoolSlot {
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    return SpoolSlot(
        index = index,
        remainingGrams = buf.float,
        grossWeight = buf.float,
        hasFilament = buf.get() != 0.toByte(),
    )
}
```

---

## Черговість реалізації (по фазах)

### ✅ Фаза 1 — Скелет і тема (ЗАВЕРШЕНО)
1. Gradle setup (всі залежності)
2. `ui/theme/` — Color, Typography, Theme (темна тема, M3)
3. `NavGraph.kt` — навігація між екранами
4. `MainActivity.kt` — `setContent { FilamentSenseTheme { NavGraph() } }`
5. Shared components: `StatusBadge`, `DataRow`, `ThresholdBar`

### ✅ Фаза 2 — BLE шар (ЗАВЕРШЕНО)
1. `GattConstants.kt`
2. `BleManager.kt` — blessed-android 2.5.2: scan → connect → subscribe to notify
3. `BleDataParser.kt`
4. `DeviceRepositoryImpl.kt` — StateFlow<DeviceState>
5. `SpoolRepositoryImpl.kt` — Flow<List<SpoolSlot>> з BLE notify

### ✅ Фаза 3 — Екрани (ЗАВЕРШЕНО)
1. **HomeScreen** + HomeViewModel
2. **ScanScreen** + ScanViewModel
3. **SpoolListScreen** + SpoolListViewModel
4. **SpoolDetailScreen** + SpoolDetailViewModel
5. **SettingsScreen** + SettingsViewModel
6. **SpoolFormScreen** (спільна форма Create/Edit)
7. **SpoolCreateScreen** + SpoolCreateViewModel
8. **SpoolEditScreen** + SpoolEditViewModel
9. **BottomNav**, **EnvDataCard**, **SpoolListItem**

### ✅ Фаза 4 — Локальна БД і аналітика (ЗАВЕРШЕНО)
1. Room: `SpoolEntity`, `MeasurementEntity`, `SpoolDao`, `MeasurementDao`, `AppDatabase`
2. `SpoolRepositoryImpl` — зберігає вимірювання кожні 5 хв в Room
3. `GetMeasurementsUseCase` — повертає 24h вимірювань
4. `SpoolDetailScreen` — Vico `CartesianChartHost` + `LineCartesianLayer` (графік залишку)

### Фаза 5 — Наступна
- AnalyticsScreen (зараз "Coming Soon" placeholder)
- BH1750 сенсор освітленості
- Push-notifications при низькому залишку
- Wi-Fi / Telegram конфіг

---

## Залежності (build.gradle.kts)

```kotlin
// BOM
implementation(platform("androidx.compose:compose-bom:2025.03.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")

// Navigation
implementation("androidx.navigation:navigation-compose:2.8.9")

// Hilt
implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-android-compiler:2.51.1")
implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

// BLE — максимальна версія на JitPack: 2.5.2 (3.0.1 не існує)
implementation("com.github.weliem:blessed-android:2.5.2")

// Room
implementation("androidx.room:room-runtime:2.7.0")
implementation("androidx.room:room-ktx:2.7.0")
ksp("androidx.room:room-compiler:2.7.0")

// Lifecycle + ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// Vico charts
implementation("com.patrykandpatrick.vico:compose-m3:2.0.1")
```

```kotlin
// settings.gradle.kts — maven repos
maven { url = uri("https://jitpack.io") }  // для blessed-android
```

---

## Правила кодування

- **Тільки темна тема** — `darkColorScheme()`, не додавати `lightColorScheme()`
- **StateFlow, не LiveData** — весь стан через `StateFlow` / `collectAsStateWithLifecycle()`
- **Immutable UI state** — один `data class XxxUiState` на ViewModel, не окремі поля
- **Composable функції без бізнес-логіки** — вся логіка в ViewModel/UseCase
- **Hilt скрізь** — не створювати об'єкти вручну там де є DI
- **Spacing кратний 4dp** — `8.dp`, `16.dp`, `24.dp` тощо
- **MaterialTheme.colorScheme.\*** — не хардкодити hex у Composable
- **Не реалізовувати поки**: аналітика, BH1750 сенсор, push-notifications, Wi-Fi/Telegram конфіг (Coming Soon placeholder)
- **Пояснювати кожну зміну в чаті** перед реалізацією

---

## Що зробити перед стартом

1. **Прочитай Figma**: відкрий `get_design_context` для node `17:2` (головний екран) — зрозумій загальний стиль
2. **UUID зафіксовані** у `GattConstants.kt` та `src/config/BleConfig.h` (ESP32) — вони вже синхронізовані, не змінюй
3. **Проєкт вже створений** — не перезаписуй існуючі файли без потреби

---

## Відомі проблеми збірки

### AGP 9.1.0 + Kotlin plugin конфлікт
AGP 9.x застосовує Kotlin автоматично. Без обхідних прапорів — помилка "Cannot add extension with name 'kotlin'".

Обов'язково у `gradle.properties`:
```properties
android.builtInKotlin=false
android.newDsl=false
```

### KSP STAR null bug
При збірці через термінал виникає `KSTypeArgument.type should not have been null` (Room KSP annotation processing).  
**Рішення**: збирати через **Android Studio** — IDE обходить баг через власний toolchain.  
Спроби фікса через CLI: KSP 2.1.20-2.0.1, Kotlin 2.1.20 — не вирішили.

### blessed-android — реальна максимальна версія
JitPack публікує лише до **2.5.2**. Версії 3.0.x відсутні. Не оновлювати вище 2.5.2.

### Room 2.8.x — не оновлювати без тестування
Room 2.8.0+ вмикає KSP2 за замовчуванням — ризик погіршити KSP STAR null bug. Залишати на 2.7.0 до стабілізації збірки.

---

## Важливі правила для агентів

### Clean Architecture — заборони по шарах
- **domain/** і **data/** — не імпортувати `androidx.compose.ui.graphics.Color`
- Кольори в `SpoolEntity` і `SpoolSlot` — `Int` (ARGB), константа `COLOR_WHITE_ARGB = -1`
- Лише **ui/** може імпортувати Compose бібліотеки

### Перед додаванням нового файлу
1. Перевір, чи він вже існує (`Glob`)
2. Перевір, чи він вже імпортується деінде (`Grep`)
3. Не дублювати composable-функції між файлами (раніше `SpoolCreateScreen` існував в двох місцях)

### Навігація
- Маршрут `spools/create` має бути зареєстрований **до** `spools/{index}` в NavHost (інакше "create" розпізнається як index)
- Кожен маршрут у `BottomNav` **обов'язково** має відповідний `composable {}` у NavGraph

### ViewModel
- Завжди `@HiltViewModel` + `@Inject constructor`
- Стан — один `data class XxxUiState`, не окремі `MutableStateFlow` на кожне поле
- `SpoolEditViewModel` — завантажує дані через `SavedStateHandle["index"]`

### Залежності
- Не додавати залежності без перевірки: чи є в `libs.versions.toml`, чи не дублюється
- `datastore-preferences` — видалено як невикористана; не повертати
- Після додавання нової залежності — перевірити сумісність з KSP та AGP версіями

---

## Корисні посилання
- ESP32 прошивка: `/root/projects/FilamentSense/repo/`
- Figma design brief: `/root/projects/FilamentSense/figma-design-system-prompt.md`
- BLE бібліотека: https://github.com/weliem/blessed-android
- Material 3 Compose: https://m3.material.io/develop/android/jetpack-compose
- Vico charts: https://patrykandpatrick.com/vico/
