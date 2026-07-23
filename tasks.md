# tasks.md — 小小寨營開發任務板

**產品**：小小寨營 MVP（GDD v0.1）  
**工程**：Kotlin Multiplatform + Compose（Desktop 主、Wasm 次）  
**相關**：[`game_design.md`](./game_design.md) · [`Architecture.md`](./Architecture.md) · [`AGENTS.md`](./AGENTS.md)

---

## 狀態總覽

| 欄位 | 值 |
|------|-----|
| 當前 Milestone | **M3 最小可視原型** |
| 下一張待辦 | **M3-T3** |
| MVP 任務卡總數 | 20（M0–M6） |
| 單卡目標用量 | ~週用量 10%（見 AGENTS §6） |

### Milestone 進度

- [x] **M0** 專案憲章（文件）
- [x] **M1** 模擬核心
- [x] **M2** 村民 AI
- [ ] **M3** 最小可視原型
- [ ] **M4** HUD 與控制
- [ ] **M5** 視覺 polish
- [ ] **M6** Seed / 統計 / Wasm 收斂

---

## 你怎麼用這份文件（人類操作手冊）

1. 開 **新的** Grok Build session（避免舊對話拖垮 context）。  
2. 複製下一張卡的 **Grok Prompt** 貼上（可再加一句 `Follow AGENTS.md`）。  
3. Agent 回報 Done 後，依 **DoD** 自己跑指令驗證。  
4. 通過：把該卡 `- [ ]` 改成 `- [x]`，更新上方「下一張待辦」。  
5. 失敗：新增一張 `FIX-…` 小卡（同結構、更窄範圍），不要在同卡無限加戲。  
6. 建議節奏：一週 6–10 卡；忙時可一週 3–4 卡。

**不要**對 Grok 說：「把整個遊戲做完」。  
**要**說：「Implement tasks.md M1-T2 only」。

---

## 通用 Prompt 頭（可選，卡內已內嵌）

```
You are implementing one task card for Tiny Camp (小小寨營).
Read AGENTS.md, Architecture.md, and only the relevant sections of game_design.md.
Implement ONLY the task id named below. Do not start the next task.
Stop when the Definition of Done is satisfied and report verification commands.
```

---

# M0 — 專案憲章

## M0-T1 — 建立專案文件三件套

- [x] **狀態**：done（2026-07-23）
- **目標**：讓後續 session 零暖身即可開工。
- **交付**：`Architecture.md`、`AGENTS.md`、`tasks.md`
- **用量**：~10%
- **DoD**：
  - [x] 三檔位於 repo 根目錄
  - [x] 任務卡含 M1–M6 與可複製 Prompt
  - [x] 無遊戲邏輯大改

---

# M1 — 模擬核心（pure Kotlin + tests）

> 本階段**不要**做 Compose 棋盤。以 `commonTest` 驗證。

## M1-T1 — 領域模型與 3×3 地圖

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M0-T1  
- **用量**：~10%  
- **目標**：建立不可變/可變模型與 Grid，寨營格規則正確。  
- **範圍**：
  - 新增 `sim/model/*`：`TileState`, `Tile`, `GridPos`, `Gender`, `Agent`（欄位可先簡化 mode）, `SimConfig`
  - `Grid`：3×3、中心 `(1,1)` 為寨營、`isCamp`、外圍 8 格初始 GRASS
  - 單元測試：座標、寨營不可當耕地、初始 pending=0
- **非範圍**：stepHour、AI、UI、序列化  
- **主要路徑**：`composeApp/src/commonMain/kotlin/com/neojou/alsimugame/sim/`  
  `composeApp/src/commonTest/kotlin/com/neojou/alsimugame/sim/`

**DoD**：
- [x] `./gradlew :composeApp:compileKotlinDesktop` 成功
- [x] `./gradlew :composeApp:desktopTest`（或專案對應 test task）通過，含 ≥3 則與 Grid/Config 相關測試
- [x] `SimConfig` 數值對齊 GDD §11

**Grok Prompt**：
```
Implement tasks.md M1-T1 only.
Follow AGENTS.md and Architecture.md (model + Grid sections).
Create sim domain models and 3×3 Grid with camp at (1,1).
Add unit tests. No SimulationEngine, no UI.
Stop when DoD is met.
```

---

## M1-T2 — 時鐘與土地循環 + 每日生產

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M1-T1  
- **用量**：~10%  
- **目標**：`Clock` + 日界線土地 aging / 狀態轉換 + 新日 FARM `pendingHarvest++`。  
- **範圍**：
  - `Clock`：`day`, `hour` 0..5；`isDay`/`isNight`（0–2 日，3–5 夜）
  - 日結束：外圍格 `ageDays++`；FARM≥12→EMPTY；EMPTY≥12→GRASS
  - 新日 hour==0：每 FARM `pendingHarvest += 1`
  - 可用精簡 `SimulationEngine` 只做時間+土地（或 `LandSystem` + 測試驅動）
- **非範圍**：Agent 移動/AI、糧食補給、UI

**DoD**：
- [x] 測試：跑 12 日 FARM 變 EMPTY；再 12 日 EMPTY 變 GRASS
- [x] 測試：連續 3 個新日 FARM pending 累加為 3
- [x] 測試：hour 0..5 循環與 isNight
- [x] Desktop compile + tests 綠

**Grok Prompt**：
```
Implement tasks.md M1-T2 only.
Follow AGENTS.md, Architecture.md §4–5, game_design.md land rules.
Add Clock and land state transitions + daily farm yield.
Unit tests required. No agent AI/UI.
Stop when DoD is met.
```

---

## M1-T3 — 資源經濟（糧食 / 體力成本）

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M1-T2  
- **用量**：~10%  
- **目標**：寨營庫存、補給公式、移動/開墾/採收成本函式可測。  
- **範圍**：
  - `Economy` 或 engine 方法：`deposit`, `supplyStaminaFromCamp(agent)`（1 糧 = 3 體力，不超上限，糧不足則部分補）
  - 成本：移動 1 體力+1 小時；開墾額外 1 體力+1 小時；採收 1 小時無體力
  - 採收：`pendingHarvest` → `carriedFood`，pending 清 0
  - 測試覆蓋上述公式與邊界（糧=0、體力滿）
- **非範圍**：完整 AI 決策、UI

**DoD**：
- [x] ≥5 則經濟相關單元測試通過
- [x] 參數來自 `SimConfig`，無魔法數字散落

**Grok Prompt**：
```
Implement tasks.md M1-T3 only.
Follow AGENTS.md, Architecture.md, game_design.md §4 and §11.
Implement camp food, stamina supply, move/till/harvest costs as testable APIs.
No full AI or UI. Stop when DoD is met.
```

---

## M1-T4 — SimulationEngine + Seeded RNG + 狀態快照骨架

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M1-T3  
- **用量**：~10%  
- **目標**：可 headless 推進的引擎；同 seed 可重現；`SimSnapshot` 骨架。  
- **範圍**：
  - `SimRng(seed)`
  - `SimulationEngine(seed)`：建立初始世界（兩 agent 在營、campFood=10、全草）
  - `stepHour()` 整合時間/土地/（agent 可先 no-op 或 REST）
  - `runDays(n)`
  - `snapshot(): SimSnapshot`（供日後 UI）
  - 可選：kotlinx.serialization 對 snapshot 或 config 的基礎 encode（能編解一個最小結構即可）
  - 測試：同 seed 跑 24 hour，campFood/day/hour/土地摘要一致
- **非範圍**：AI 優先序、Compose

**DoD**：
- [x] Seed 重現測試通過
- [x] `runDays(12)` 不崩潰
- [x] compile + tests 綠

**Grok Prompt**：
```
Implement tasks.md M1-T4 only.
Follow AGENTS.md and Architecture.md §4, §7, §9.
Add SimulationEngine with seeded RNG, stepHour/runDays, SimSnapshot skeleton.
Determinism tests required. No agent brain UI. Stop when DoD is met.
```

---

# M2 — 村民 AI

## M2-T1 — 八向移動與回營路徑

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M1-T4  
- **用量**：~10%  
- **目標**：`Pathfinder` + agent 移動一步（扣體力與時間由 engine 執行）。  
- **範圍**：
  - 8 向鄰格；邊界夾住 0..2
  - 任意外圍格到寨營最短路徑（長度應為 1）
  - `tryMove(agent, dest)` 失敗條件：體力不足、死亡
  - 測試：八向、回營一步、不可越界
- **非範圍**：開墾決策、夜間邏輯全文

**DoD**：
- [x] Path/move 單元測試 ≥4 則通過
- [x] Engine 可在測試中手動移動 agent

**Grok Prompt**：
```
Implement tasks.md M2-T1 only.
Follow Architecture.md path section and GDD §5.2.
Add 8-direction pathfinding and one-step move with stamina cost.
Tests required. No full brain. Stop when DoD is met.
```

---

## M2-T2 — 行為優先序（AgentBrain）

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M2-T1  
- **用量**：~10%  
- **目標**：實作 GDD §5 優先序 + 體力過低回營保險。  
- **範圍**：
  - `AgentBrain` / `AgentAction`
  - 夜間強制返回；日初補給後外出；GRASS 開墾；FARM 有 pending 採收；否則續探索；returnHome 回營存糧
  - 低體力保險（如 stamina≤1 不在營 → 回營）
  - 測試用固定 seed 或注入 Random 的可預測序列；測試夜間、開墾設 return 旗標、採收等案例
- **非範圍**：UI、第二階段個性差異

**DoD**：
- [x] 優先序相關測試 ≥5 則通過
- [x] `runDays(3)` 有 agent 離開過寨營或改變過土地（依 seed；至少一個整合測試證明 brain 有接上 engine）

**Grok Prompt**：
```
Implement tasks.md M2-T2 only.
Follow game_design.md §5 and Architecture.md §6.
Implement AgentBrain priority list wired into SimulationEngine.stepHour.
Unit + light integration tests. No UI. Stop when DoD is met.
```

---

## M2-T3 — 雙村民、壽命與死亡

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M2-T2  
- **用量**：~10%  
- **目標**：男+女兩 agent；壽命 5 年（60 日）；死亡規則可測。  
- **範圍**：
  - 初始兩 agent（MALE/FEMALE），允許同格
  - 日界線 `ageDays++`；≥ LIFESPAN → DEAD
  - 體力 0 且無法行動：簡化為「若夜間仍不在寨營則死亡」或 GDD 虛弱規則之明確簡化（寫進 KDoc 與測試）
  - 人口 0 時 engine 標記 `isGameOver`
  - 測試：壽命、雙人同在、game over
- **非範圍**：繁殖、UI
- **參數更新（2026-07-23）**：`LIFESPAN_DAYS` 由 12（1 年）改為 **60（5 年）**，土地仍 12 日一轉換。

**DoD**：
- [x] 雙 agent 初始測試
- [x] 壽命/死亡測試
- [x] compile + tests 綠

**Grok Prompt**：
```
Implement tasks.md M2-T3 only.
Follow GDD lifespan/death notes and Architecture.md.
Two agents, lifespan 60 days (5 years), death + isGameOver. Tests required.
No UI. Stop when DoD is met.
```

---

# M3 — 最小可視原型

## M3-T1 — SimulationController（Compose 狀態橋）

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M2-T3  
- **用量**：~10%  
- **目標**：UI 可 play/pause/speed/reset 驅動 `stepHour`。  
- **範圍**：
  - `ui/SimulationController` 或 `SimViewModel`：`StateFlow`/`mutableStateOf` 暴露 `SimSnapshot`
  - play 迴圈用 coroutine + delay；speed 1/2/5/10
  - reset(seed)
  - 尚可用極簡 Text 顯示 day/hour/food 驗證（完整棋盤在下一卡）
- **非範圍**：精美 HUD、hover、美術

**DoD**：
- [x] Desktop compile
- [x] `ALSimuGame`/`App` 能顯示 day/hour 並隨 play 變化（手動驗）
- [x] 暫停後時間不動

**Grok Prompt**：
```
Implement tasks.md M3-T1 only.
Follow Architecture.md §7.
Add SimulationController bridging SimulationEngine to Compose state with play/pause/speed/reset.
Minimal text readout is enough. Stop when DoD is met.
```

---

## M3-T2 — 3×3 棋盤 UI（色塊占位）

- [x] **狀態**：done（2026-07-23）  
- **依賴**：M3-T1  
- **用量**：~10%  
- **目標**：固定視角畫出 9 格與 agent 位置。  
- **範圍**：
  - `BoardView`：營=木色、GRASS 綠、FARM 褐、EMPTY 灰
  - Agent 用圓點/縮寫（M/F）疊在格子上
  - pendingHarvest 可顯示小數字
- **非範圍**：日夜 tint 大作、粒子、控制列美化

**DoD**：
- [x] Desktop run 可見 3×3 與兩 agent
- [x] 土地狀態顏色可區分

**Grok Prompt**：
```
Implement tasks.md M3-T2 only.
Follow GDD §3 map and Architecture UI section.
Compose 3×3 board with color placeholders and agent markers from SimSnapshot.
No polish FX. Stop when DoD is met.
```

---

## M3-T3 — 自動推進可視驗證

- [ ] **狀態**：todo  
- **依賴**：M3-T2  
- **用量**：~10%  
- **目標**：Play 時可見時間流逝、agent 移動、土地變化。  
- **範圍**：
  - 接好預設 autoplay（或明顯 Play 鈕）
  - 日夜可用背景灰階差表示（簡單）
  - 修 snapshot 未更新、位置不同步等整合 bug
- **非範圍**：完整控制列（M4）、美術資源

**DoD**：
- [ ] Desktop：按 Play 後 30 秒內可觀察到 agent 位移或土地/pending 變化（多數 seed）
- [ ] 無連續 crash

**Grok Prompt**：
```
Implement tasks.md M3-T3 only.
Wire board + controller so play mode shows living simulation.
Fix integration bugs only. Stop when DoD is met.
```

---

# M4 — HUD 與控制

## M4-T1 — 頂部 HUD

- [ ] **狀態**：todo  
- **依賴**：M3-T3  
- **用量**：~10%  
- **目標**：時間與糧食常駐顯示。  
- **範圍**：年（可由 day/12 推）、日、晝夜圖示/文字、campFood 大數字  
- **非範圍**：統計面板、seed 輸入

**DoD**：
- [ ] HUD 數值與 snapshot 一致
- [ ] Desktop compile + 手動對一下 day/food

**Grok Prompt**：
```
Implement tasks.md M4-T1 only.
GDD §6.3 HUD. Top bar: day/time-of-day + camp food. Stop when DoD is met.
```

---

## M4-T2 — 控制列（暫停 / 速度 / 重置 / Seed）

- [ ] **狀態**：todo  
- **依賴**：M4-T1  
- **用量**：~10%  
- **目標**：玩家可控制模擬節奏與重開。  
- **範圍**：Play/Pause、速度 1×2×5×10、Reset、Seed 輸入（Long）  
- **非範圍**：replay 匯出、hover

**DoD**：
- [ ] 切速度明顯改變推進快慢
- [ ] Reset+同 seed 可重現開局
- [ ] Desktop 手動驗

**Grok Prompt**：
```
Implement tasks.md M4-T2 only.
GDD §6.3 controls: pause/play, speed, reset, seed.
Stop when DoD is met.
```

---

## M4-T3 — 統計面板 + Hover 提示

- [ ] **狀態**：todo  
- **依賴**：M4-T2  
- **用量**：~10%  
- **目標**：土地計數、人口、累計生產；tile/agent 提示。  
- **範圍**：
  - 側邊/底部可收合統計
  - 滑鼠 hover 格：state、pending、age/剩餘日
  - hover agent：體力、攜帶、mode 文字
  - `StatsRecorder` 若尚未有累計生產，在 sim 層補最小計數
- **非範圍**：粒子特效、音效

**DoD**：
- [ ] 統計數字與引擎一致（抽樣）
- [ ] Hover 有資訊（Desktop）

**Grok Prompt**：
```
Implement tasks.md M4-T3 only.
Stats panel + hover tooltips per GDD §6.3.
Minimal StatsRecorder fields if missing. Stop when DoD is met.
```

---

# M5 — 視覺 polish

## M5-T1 — 日夜色調

- [ ] **狀態**：todo  
- **依賴**：M4-T3  
- **用量**：~10%  
- **目標**：白天暖亮、夜晚冷暗、寨營夜晚略亮。  
- **範圍**：theme/tint overlay；勿大改邏輯  
- **非範圍**：完整 sprite sheet

**DoD**：
- [ ] 日夜切換肉眼可辨
- [ ] 模擬邏輯測試仍綠

**Grok Prompt**：
```
Implement tasks.md M5-T1 only.
Day/night visual tint only. No simulation rule changes.
Stop when DoD is met.
```

---

## M5-T2 — Agent 狀態表現

- [ ] **狀態**：todo  
- **依賴**：M5-T1  
- **用量**：~10%  
- **目標**：依 mode 顯示不同圖示/emoji/簡單動畫狀態。  
- **範圍**：REST/EXPLORE/WORK/RETURN/SLEEP/DEAD 可辨；男女外觀差  
- **非範圍**：8 向逐幀 sprite 大工程（可簡化）

**DoD**：
- [ ] 至少 4 種 mode 視覺可辨
- [ ] Desktop 手動驗

**Grok Prompt**：
```
Implement tasks.md M5-T2 only.
Visual agent modes (icons/simple anim). Keep scope small.
Stop when DoD is met.
```

---

## M5-T3 — 土地可採收高亮與簡易特效

- [ ] **狀態**：todo  
- **依賴**：M5-T2  
- **用量**：~10%  
- **目標**：pending>0 高亮；開墾/採收短暫提示。  
- **範圍**：Compose 動畫/粒子簡化版；可為閃爍邊框  
- **非範圍**：音效、Spine 級動畫

**DoD**：
- [ ] 可採收田地可辨
- [ ] 不影響 tick 正確性（tests 綠）

**Grok Prompt**：
```
Implement tasks.md M5-T3 only.
Harvest highlight + simple till/harvest feedback FX.
No audio. Stop when DoD is met.
```

---

# M6 — 收斂與跨平台

## M6-T1 — 三預設 Seed + 參數核對

- [ ] **狀態**：todo  
- **依賴**：M5-T3  
- **用量**：~10%  
- **目標**：UI 可選 3 個預設 seed；參數表與 GDD §11 一致。  
- **範圍**：預設 seed 常數、UI 快捷鈕、對 `SimConfig` 做一次對表測試  
- **非範圍**：線上排行榜

**DoD**：
- [ ] 3 seed 可一鍵載入
- [ ] Config 對表測試通過

**Grok Prompt**：
```
Implement tasks.md M6-T1 only.
Three preset seeds + SimConfig alignment tests vs GDD §11.
Stop when DoD is met.
```

---

## M6-T2 — 每日統計與簡易 Replay 記錄

- [ ] **狀態**：todo  
- **依賴**：M6-T1  
- **用量**：~10%  
- **目標**：每日結束記錄糧食、人口、土地組成；可查看歷史列表。  
- **範圍**：`StatsRecorder` 日摘要 list；UI 簡單列表或文字 log  
- **非範圍**：影格級 replay 影片、分享連線

**DoD**：
- [ ] 跑 5 日後有 5 筆日摘要
- [ ] 測試 recorder 行為

**Grok Prompt**：
```
Implement tasks.md M6-T2 only.
Daily stats log + simple in-app history list.
Tests for recorder. Stop when DoD is met.
```

---

## M6-T3 — Wasm 可玩 parity

- [ ] **狀態**：todo  
- **依賴**：M6-T2  
- **用量**：~10%  
- **目標**：瀏覽器可玩同一套模擬，無明顯平台崩潰。  
- **範圍**：
  - 修 wasm 編譯/runtime 問題
  - 確認 `index.html`、ComposeViewport、時間迴圈在 browser 正常
  - 不引入 JVM-only API
- **非範圍**：新功能

**DoD**：
- [ ] `./gradlew :composeApp:compileKotlinWasmJs` 成功
- [ ] `./gradlew :composeApp:wasmJsBrowserDevelopmentRun` 可操作 play/pause（手動）
- [ ] Desktop 回歸仍可用

**Grok Prompt**：
```
Implement tasks.md M6-T3 only.
Make wasmJs browser run playable; fix platform issues only.
Verify desktop still works. Stop when DoD is met.
```

---

# FIX 卡模板（臨時用）

複製新增到文件底部：

```markdown
## FIX-YYYMMDD-短名

- [ ] **狀態**：todo
- **依賴**：—
- **用量**：~5–10%
- **目標**：<一個可驗證的 bug>
- **範圍**：僅修復所需檔案
- **非範圍**：重構、新功能
- **DoD**：
  - [ ] 重現步驟失敗 → 通過
  - [ ] 相關測試綠
**Grok Prompt**：
\`\`\`
Fix only: <symptom>.
Reproduce: <steps>.
Do not add features. Stop when fixed and tests pass.
\`\`\`
```

---

# 建議排程（可調）

| 週 | 任務 | 預估卡數 |
|----|------|----------|
| W0 | M0（本文件） | 1 |
| W1 | M1 全 + M2 起 | 5–7 |
| W2 | M2 完 + M3 | 5–6 |
| W3 | M4 + M5 起 | 5–6 |
| W4 | M5 完 + M6 | 4–5 |

密集可壓成 ~2 週（每天 2–3 卡）；從容則 4–5 週。

---

# 變更紀錄

| 日期 | 變更 |
|------|------|
| 2026-07-23 | 初版任務板；M0-T1 完成；下一張 M1-T1 |
| 2026-07-23 | M1-T1 完成：sim 領域模型 + Grid + unit tests；下一張 M1-T2 |
| 2026-07-23 | M1-T2 完成：Clock + LandSystem + slim SimulationEngine；下一張 M1-T3 |
| 2026-07-23 | M1-T3 完成：Economy API + campFood on engine；下一張 M1-T4 |
| 2026-07-23 | M1-T4 完成：SimRng + snapshot + 雙 agent 初始世界；M1 結束；下一張 M2-T1 |
| 2026-07-23 | M2 整包完成（T1 Pathfinder/move、T2 AgentBrain、T3 壽命/死亡）；下一張 M3-T1 |
| 2026-07-23 | M3-T1 完成：SimulationController + 文字 readout；下一張 M3-T2 |
| 2026-07-23 | 參數：村民壽命 1 年→5 年（LIFESPAN_DAYS 12→60）；土地 12 日不變 |
| 2026-07-23 | M3-T2 完成：BoardView 3×3 色塊 + M/F 標記；下一張 M3-T3 |
| 2026-07-23 | 設計更新：地圖 3×3→5×5（營 2,2）；pending_harvest 上限 3 |
| 2026-07-23 | Wasm 中文：內嵌 Noto Sans TC + AppTheme（Skiko 無系統 CJK 回退） |
