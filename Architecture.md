# Architecture — 小小寨營 (Tiny Camp)

**專案**：ALSimuGame / 小小寨營  
**平台**：Kotlin Multiplatform + Compose Multiplatform  
**目標**：Desktop (JVM) 為主驗證線，WasmJS 瀏覽器為次要目標  
**GDD 真相來源**：[`game_design.md`](./game_design.md)  
**任務進度**：[`tasks.md`](./tasks.md)  
**Agent 規則**：[`AGENTS.md`](./AGENTS.md)

> GDD §7 建議 Godot；**本 repo 以 Compose KMP 實作**。機制與參數以 GDD 為準，僅引擎與呈現層替換。

---

## 1. 系統分層

```
┌─────────────────────────────────────────────────────────┐
│  Platform entry                                         │
│  desktopMain: Main.kt    wasmJsMain: WasmMain.kt        │
│  → App() → ALSimuGame() / SimScreen                     │
└───────────────────────────┬─────────────────────────────┘
                            │
┌───────────────────────────▼─────────────────────────────┐
│  UI (commonMain, Compose)                               │
│  hud / board / controls / tooltips                      │
│  SimulationController — play/pause, speed, seed, reset  │
└───────────────────────────┬─────────────────────────────┘
                            │ SimSnapshot (immutable)
┌───────────────────────────▼─────────────────────────────┐
│  Simulation (commonMain, pure Kotlin)                   │
│  SimulationEngine.stepHour() / runDays()                │
│  Clock · Grid · Economy · AgentBrain · Pathfinding      │
│  Seeded Random · StatsRecorder                          │
└─────────────────────────────────────────────────────────┘
```

| 層 | 職責 | 禁止 |
|----|------|------|
| **Platform** | 視窗 / `ComposeViewport`、平台 Ktor engine | 遊戲規則 |
| **UI** | 繪製、輸入（僅速度/暫停/seed）、訂閱 snapshot | 自行改寫土地規則 |
| **Simulation** | 時間、土地、糧食、AI、勝負 | Compose / Android / Browser API |

所有**可測試遊戲邏輯必須在 commonMain**，且不依賴 UI。

---

## 2. 建議套件結構

```
composeApp/src/
  commonMain/kotlin/com/neojou/alsimugame/
    App.kt                          # bootstrap + MaterialTheme
    ALSimuGame.kt                   # 根遊戲畫面入口（接 SimScreen）
    sim/
      model/
        TileState.kt                # GRASS, FARM, EMPTY
        Tile.kt
        Agent.kt                    # pos, stamina, carriedFood, gender, ageDays, mode
        AgentMode.kt                # RESTING, EXPLORING, WORKING, RETURNING, ...
        GridPos.kt                  # x,y 0..2；中心 (1,1) = 寨營
        SimConfig.kt                # GDD §11 參數常數
        SimSnapshot.kt              # UI 唯讀投影
      Grid.kt                       # 3×3 tiles；寨營格特殊
      Clock.kt                      # day, hour 0..5；isDay / isNight
      Economy.kt                    # campFood、補給、生產
      path/
        Pathfinder.kt               # 8 向最短路徑（回寨營 / 鄰格）
      ai/
        AgentBrain.kt               # GDD §5 優先序
      SimulationEngine.kt           # stepHour 主迴圈
      StatsRecorder.kt              # 每日摘要 / 簡易 replay
      SimRng.kt                     # Random(seed) 包裝
    ui/
      SimScreen.kt
      BoardView.kt
      HudView.kt
      ControlsView.kt
      StatsPanel.kt
      theme/                        # 日夜 tint 等
    tools/
      MyLog.kt
      SystemSettings.kt

  commonTest/kotlin/com/neojou/alsimugame/sim/
    ... 對應單元測試

  desktopMain/.../Main.kt
  wasmJsMain/.../WasmMain.kt
  wasmJsMain/resources/index.html
```

命名慣例：類型 PascalCase；參數與 GDD 英文欄位對齊時用 camelCase（`pendingHarvest`, `carriedFood`, `campFood`）。

---

## 3. 核心資料模型（對齊 GDD §7.2）

```kotlin
enum class TileState { GRASS, FARM, EMPTY }

// 外圍可耕格
data class Tile(
    var state: TileState = TileState.GRASS,
    var ageDays: Int = 0,           // 進入當前狀態後累計日數
    var pendingHarvest: Int = 0,    // 僅 FARM 有意義
)

enum class AgentMode {
    RESTING,      // 寨營休息 / 夜間
    SUPPLYING,    // 日初存糧 + 補給體力
    EXPLORING,    // 白天外出
    TILLING,      // 開墾中（可併入 WORKING）
    HARVESTING,   // 採收中
    RETURNING,    // 回寨營
    DEAD,
}

data class Agent(
    val id: String,
    val gender: Gender,             // MALE / FEMALE
    var pos: GridPos,
    var stamina: Int = SimConfig.MAX_STAMINA,
    var carriedFood: Int = 0,
    var ageDays: Int = 0,
    var mode: AgentMode = AgentMode.RESTING,
    var returnHome: Boolean = false,
    var path: List<GridPos> = emptyList(),
)

// 引擎可變狀態（內部）；對外給 UI 用 immutable snapshot
```

**地圖約束**：

- 座標 `(x, y)`，`0..2`；**寨營 = `(1, 1)`**，不可耕種、不可變成田地。
- 外圍 8 格可為 GRASS / FARM / EMPTY。
- 初始：外圍全 GRASS；`campFood = 10`；兩 agent 在寨營，體力 10。

**SimConfig（GDD §11，單一真相）**：

| 常數 | 值 |
|------|-----|
| MAX_STAMINA | 10 |
| FOOD_TO_STAMINA | 3（1 糧 → +3 體力） |
| FARM_YIELD_PER_DAY | 1 |
| MOVE_STAMINA / MOVE_HOURS | 1 / 1 |
| TILL_EXTRA_STAMINA / TILL_HOURS | 1 / 1 |
| HARVEST_HOURS | 1（無體力） |
| LAND_STATE_DAYS | 12 |
| INITIAL_CAMP_FOOD | 10 |
| LIFESPAN_DAYS | 60（5 年 × 12 日/年） |
| HOURS_PER_DAY | 6（0–2 白天，3–5 晚上） |

改參數必須同步 `game_design.md` §11 與測試。

---

## 4. Tick 主迴圈（對齊 GDD §7.3）

單一入口：`SimulationEngine.stepHour()`。

```
stepHour():
  1. 推進時間
     hour += 1
     if hour == 6:
       hour = 0
       day += 1
       onNewDay()          // 土地 age、狀態轉換、agent ageDays、壽命檢查
  2. if hour == 0 (新一日開始):
       每塊 FARM: pendingHarvest += FARM_YIELD_PER_DAY
       每位存活 agent 在寨營: deposit carriedFood; 用 campFood 補給體力
  3. 更新每位存活 Agent（AgentBrain + 移動/動作）
     - 夜間且不在寨營 → 最高優先 RETURNING
     - 執行一「小時動作」：移動一步 / 開墾 / 採收 / 休息
  4. 統計鉤子（StatsRecorder）
  5. 回傳或更新 latest SimSnapshot
```

**重要**：一 tick = 一小時。Agent 在單一 tick 最多完成「一個」耗時動作（移動 1 格、開墾、或採收），與 GDD 成本表一致。

`runDays(n)` = 連續 `stepHour()` `n * 6` 次，供 headless / 測試 / 最大速度。

---

## 5. 土地狀態機

```
GRASS --(agent till)--> FARM (ageDays=0)
FARM  --(ageDays >= 12 at day boundary)--> EMPTY (ageDays=0)
EMPTY --(ageDays >= 12 at day boundary)--> GRASS (ageDays=0)
```

- 開墾：**立即**變 FARM。
- 日界線：所有外圍格 `ageDays++`，再檢查轉換。
- 寨營格永不進入此狀態機。

---

## 6. AI 優先序（GDD §5，Priority List）

實作於 `AgentBrain.decide(agent, world): AgentAction`，**不要**引入完整 Behavior Tree 框架。

順序（高 → 低）：

1. **DEAD** → 無動作  
2. **夜間**且不在寨營 → `RETURNING`（即使體力不足也嘗試）  
3. **體力過低保險**（建議：`stamina <= 1` 且不在寨營）→ `RETURNING`（防死鎖，GDD §10）  
4. **日初在寨營** → 存糧 + 補給；若白天且 stamina > 0 → 選隨機外圍方向 `EXPLORING`  
5. **RETURNING** 或 `returnHome` → 沿 path 向寨營；抵達則存糧、RESTING  
6. **EXPLORING** 抵達土地格：  
   - GRASS → TILL → 設 returnHome  
   - FARM && pendingHarvest > 0 → HARVEST → 設 returnHome  
   - 否則 → 繼續探索（偏好遠離中心或隨機相鄰外圍格，**不**空手立刻回營）  
7. **RESTING** 夜間在寨營 → 建議 +2 體力/夜（GDD §4.2 次要恢復；實作時寫入 SimConfig）

移動：8 方向；外圍 ↔ 寨營通常 1 步。`Pathfinder` 可用 BFS/貪婪；地圖僅 9 格。

多 agent：允許同格；互不阻擋（v0.1）。

---

## 7. UI 訂閱模型

```kotlin
// 概念 API
class SimulationController(
    initialSeed: Long,
) {
    val snapshot: StateFlow<SimSnapshot>   // 或 Compose State
    fun play()
    fun pause()
    fun setSpeed(mult: Int)                // 1,2,5,10
    fun reset(seed: Long)
    fun stepOnce()                         // 除錯用
}
```

- Controller 內 coroutine：`while (playing) { engine.stepHour(); delay(baseMs / speed) }`
- **UI 只讀 `SimSnapshot`**，禁止直接 mutate `Tile` / `Agent`。
- 固定 2.5D / 俯視 3×3 全圖；**無相機平移縮放**（GDD §6.1）。

M3 可用色塊 + 文字；M5 再換 sprite / tint。

---

## 8. 平台邊界

| 項目 | Desktop | WasmJS |
|------|---------|--------|
| Entry | `MainKt` + `Window` | `WasmMain` + `ComposeViewport(document.body)` |
| 模擬 | 共用 commonMain | 同左 |
| Ktor | `ktor-client-cio`（desktopMain） | `ktor-client-js`（wasmJsMain） |
| 驗證 | 每任務必跑 | 主要在 M6-T3 |

`commonMain` **禁止**：`java.*`、CIO engine、檔案系統、`document` / browser API。

---

## 9. 序列化與 Seed

- `SimRng(seed: Long)` 包一層 `kotlin.random.Random`
- 相同 seed + 相同 `stepHour` 次數 → 狀態一致（測試保證）
- 狀態序列化：kotlinx.serialization JSON（M1-T4 骨架，M6 可存每日摘要）
- 預設至少 3 個 seed（M6-T1），例如 `1L, 42L, 2026L`

---

## 10. 測試策略

| 層級 | 工具 | 覆蓋 |
|------|------|------|
| Domain | `commonTest` + kotlin.test | 土地轉換、生產、補給、成本、AI 優先序、seed 重現 |
| Headless 平衡 | 測試或小 main | 多 seed 跑一壽命週期（60 日）存活率（可選） |
| UI | 手動 Desktop | 棋盤、HUD、速度 |
| Wasm | 手動 browser | 冒煙 |

**邏輯回歸以單元測試為主**，減少視覺除錯消耗的 AI 用量。

---

## 11. 建置與執行

```bash
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileKotlinWasmJs
./gradlew :composeApp:run                          # Desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun  # Wasm
./gradlew :composeApp:allTests                     # 若任務配置了 test task
```

（實際 test task 名稱以 Gradle 為準：`:composeApp:desktopTest` / `wasmJsTest` 等。）

---

## 12. 擴充點（v0.2+，勿在 v0.1 實作）

- 繁殖 / 小孩：`Agent` 生命週期狀態  
- 玩家指令：在 Controller 加 command queue，與自主 AI 互斥  
- 5×5 地圖：抽象 `Grid` 寬高，Pathfinder 已與尺寸無關則較易擴  
- 事件系統：在 `onNewDay` 掛 hook  

---

## 13. 關鍵決策摘要

1. **純 commonMain 模擬** — 雙平台與可測性。  
2. **`stepHour` 驅動** — 速度、重播、測試一致。  
3. **Priority list AI** — 符合 3×3 規模，避免 BT 框架成本。  
4. **Immutable Snapshot → UI** — Compose 單向資料流。  
5. **色塊先於美術** — M3/M4 可玩，M5 再 polish。
