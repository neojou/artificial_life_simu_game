# AGENTS.md — 小小寨營 / ALSimuGame

本檔會被 Grok Build 自動載入。在此專案工作時**必須遵守**下列規則。

---

## 1. 專案是什麼

- **遊戲**：小小寨營（Tiny Camp）— 3×3 土地 + 兩位自主村民的觀察型模擬。
- **GDD**：[`game_design.md`](./game_design.md)（機制與數值的唯一產品真相）。
- **架構**：[`Architecture.md`](./Architecture.md)（套件、tick、AI、UI 邊界）。
- **進度與任務卡**：[`tasks.md`](./tasks.md)（一次只做一張卡）。
- **技術棧**：Kotlin Multiplatform + Compose Multiplatform  
  - `desktop` JVM（主驗證）  
  - `wasmJs` 瀏覽器（次要；勿每卡都跑）

現況：骨架可跑 Desktop / Wasm；遊戲邏輯依 `tasks.md` 逐步實作。

---

## 2. 開幹前必讀順序

1. `tasks.md` — 確認要做的 **Task ID** 與 DoD  
2. `Architecture.md` — 該放哪個 package、能否碰 UI  
3. `game_design.md` — 僅讀本卡相關章節（例如 AI 讀 §5，參數讀 §11）  
4. 現有 `composeApp/src` 對應程式

**不要**整份 GDD 重抄進回答；引用章節即可。

---

## 3. 建置與驗證指令

```bash
# 編譯
./gradlew :composeApp:compileKotlinDesktop
./gradlew :composeApp:compileKotlinWasmJs

# Desktop 執行（日常主驗證）
./gradlew :composeApp:run

# Wasm（僅任務要求或 M6 時）
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# 測試（任務有改 commonTest 時必跑）
./gradlew :composeApp:desktopTest
# 或：./gradlew :composeApp:allTests
```

邏輯任務：**以 unit test + compile 為 DoD**，不必強求每次開 UI。  
UI 任務：Desktop `run` 可啟動且無 crash 即可；Wasm 非必要不驗證。

---

## 4. 程式與架構約定

- 遊戲規則與 AI → **`commonMain` pure Kotlin**（見 Architecture §1–6）。
- Compose UI → `commonMain` 的 `ui` 套件；**不**在 UI 內複製一套規則。
- Platform entry 保持薄：`Main.kt` / `WasmMain.kt` 只掛 `App()`。
- **`commonMain` 禁止**：`java.*`、檔案 I/O、`ktor-client-cio`、browser DOM。
- Ktor：core 在 common；CIO 僅 `desktopMain`；JS 僅 `wasmJsMain`。
- 參數集中 `SimConfig`，數值對齊 GDD §11；改數值要同步 GDD 與測試。
- 隨機必須 **seeded**（`SimRng` / `Random(seed)`），保證可重現。
- 日誌可用既有 `com.neojou.tools.MyLog`；模組 tag 簡短（如 `SIM`, `AI`, `UI`）。
- 公開 API 與非顯而易見邏輯寫 KDoc（精簡即可）。

### 套件根

`com.neojou.alsimugame`  
模擬：`...alsimugame.sim.*`  
UI：`...alsimugame.ui.*`

---

## 5. v0.1 Scope 邊界（嚴格）

### 做（GDD §8 包含）

- 3×3、三種土地狀態、12 日轉換、每日田地生產  
- 兩位固定村民 + 自主 AI（GDD §5）  
- 日夜 6 tick/日、糧食與體力  
- 基本 HUD、速度、暫停、重置、seed  
- 統計與簡易 replay（後期卡）  
- Desktop 可玩；Wasm 可玩（收斂卡）

### 不做（v0.1）

- 繁殖、小孩、更多職業  
- 玩家點擊指揮村民  
- 水/肥料/工具/天氣/災害  
- 音效配樂、成就、雲存檔、多人  
- 地圖 > 3×3、鏡頭滾動縮放  
- 全專案無關重構、依賴大升級、文件大翻修（除非任務指定）

若使用者要求超出範圍：先提醒，**預設拒絕擴 scope**，建議開新任務卡。

---

## 6. 用量紀律（SuperGrok ~10% / 任務）

使用者希望**單次任務約週用量 10%**。遵守：

| 做 | 不要 |
|----|------|
| 只實作 `tasks.md` 指定的 **一個** Task ID | 「順便」做下一張卡或整個 milestone |
| 改動約 3–8 檔、聚焦 DoD | 全目錄格式化、重命名大爆炸 |
| 邏輯卡附 focused unit tests | 無測試的大段規則碼 |
| DoD 達成後**停止**並簡短回報 | 無止境 polish / 重構 |
| 單 agent 直線完成 | 預設開一堆 parallel subagent / design 長迴圈 |
| 失敗時建議一張小 Fix 卡 | 同一 session 無限加需求 |

### 完成回報格式

```
## Done: <Task ID>
- 變更：...
- 驗證：<指令與結果>
- 未做（超出本卡）：...
- 建議下一張：...
```

### 使用者貼卡時的標準起手（可極短）

```
Implement tasks.md <TASK_ID> only.
Follow AGENTS.md and Architecture.md.
Stop when DoD is met.
```

---

## 7. 測試與品質

- 新模擬行為 → 至少 3 個單元測試（正常 / 邊界 / 回歸）。
- Seed 重現：同 seed 跑 N hour，關鍵欄位相等。
- 不刪除既有測試 unless 行為依 GDD 變更且已更新 DoD。
- 編譯警告可修與本卡相關者；無關警告不展開。

---

## 8. Git

- 非經使用者明確要求，**不要** `git commit` / `push`。
- 若要求 commit：訊息用完整句子說明「為什麼」，只 stage 本卡相關檔。

---

## 9. 與 GDD 衝突時

1. 產品規則 → 以 `game_design.md` 為準。  
2. 工程結構 → 以 `Architecture.md` 為準（Compose KMP 取代 Godot）。  
3. 模糊時：選**較小可測**的實作，在回報中註明假設，勿默默擴大系統。

---

## 10. 美術與資產

- M3–M4：色塊、文字、簡單 shape 即可。  
- M5+：若生成 sprite，使用專案內 `composeResources` 慣例；風格見 GDD §6（Q 版、淡雅）。  
- 美術 session 與邏輯 session **分開**，避免單卡過大。
