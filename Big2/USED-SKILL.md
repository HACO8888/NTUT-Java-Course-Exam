# Big2 大老二 — 使用技術與服務總覽

## 核心語言與執行環境

| 技術 | 說明 |
|------|------|
| **Java 11** | 主要開發語言（pom.xml 中指定 compiler source/target） |
| **Eclipse Temurin JRE 17** | Docker 容器中使用的 Java 執行環境 |

## 建置與依賴管理

| 技術 | 說明 |
|------|------|
| **Maven 3.9** | 專案建置與依賴管理工具 |
| **Maven Shade Plugin 3.5.1** | 將所有依賴打包成單一 Fat JAR，進入點為 `com.bigtwo.Main` |

## 網路與通訊協定

| 技術 | 說明 |
|------|------|
| **Java-WebSocket 1.5.6** | WebSocket 函式庫，用於即時雙向多人連線通訊 |
| **com.sun.net.httpserver** | Java 內建 HTTP 伺服器，用於提供 Web 客戶端靜態資源 |
| **JSON** | 資料交換格式，使用自行實作的 `JsonUtil`（無外部 JSON 函式庫） |
| **WebSocket 協定 (ws/wss)** | 瀏覽器端與伺服器的即時通訊協定 |
| **HTTP 協定** | 靜態資源傳輸 |

## 桌面端 UI（離線 / 本地模式）

| 技術 | 說明 |
|------|------|
| **Java Swing** | 桌面 GUI 框架（JFrame、JPanel、JButton 等） |
| **Java AWT / Graphics2D** | 2D 繪圖引擎，用於撲克牌渲染與幾何運算 |
| **CardLayout** | 畫面切換機制（大廳、等待室、遊戲畫面） |
| **SwingUtilities** | 確保 UI 更新在 EDT 執行緒安全執行 |
| **UIManager** | 套用系統原生外觀風格 |

## 網頁端 UI（線上模式）

| 技術 | 說明 |
|------|------|
| **HTML5** | 網頁結構標記語言 |
| **CSS3** | 樣式設計，包含漸層背景、毛玻璃效果（Glassmorphism）、Flexbox 排版 |
| **Vanilla JavaScript** | 原生 JS，負責 DOM 操作、狀態管理、WebSocket 連線與遊戲互動邏輯 |

## 遊戲邏輯與 AI

| 技術 | 說明 |
|------|------|
| **自製遊戲引擎** | `BigTwoGame`（離線）/ `ServerBigTwoGame`（線上）管理遊戲狀態 |
| **AI 策略系統** | `AIPlayer` 類別，包含主動出牌策略、防守打法、殘局加速等決策邏輯 |
| **牌型驗證系統** | 支援單張、對子、順子、同花、葫蘆、鐵支、同花順共 7 種牌型 |

## 並行處理與執行緒安全

| 技術 | 說明 |
|------|------|
| **ConcurrentHashMap** | 執行緒安全的房間與連線管理 |
| **ScheduledExecutorService** | 每個房間獨立的排程器 |
| **synchronized 同步區塊** | 座位與玩家管理的同步控制 |

## 部署與容器化

| 技術 | 說明 |
|------|------|
| **Docker** | 多階段建置容器化（Build: maven Alpine / Runtime: JRE Alpine） |
| **Zeabur** | 雲端部署平台（透過環境變數 `PORT`、`HTTP_PORT` 設定埠號） |
| **Headless 伺服器模式** | 使用 `--server` 旗標啟動純伺服器模式（無 GUI） |

## 設計模式

| 模式 | 應用場景 |
|------|----------|
| **MVC** | Model（Card、Deck、Player）/ View（GameWindow、各 Panel）/ Controller（GameServer、Room） |
| **Observer** | `MessageListener` 介面監聽遊戲事件回呼 |
| **Strategy** | AIPlayer 根據不同局勢採用不同出牌策略 |
| **State Machine** | `Room.State` 列舉管理房間狀態（WAITING → PLAYING → FINISHED） |
| **Command** | JSON 訊息協定指令（CREATE_ROOM、PLAY_CARDS、PASS 等） |
| **Session Management** | `ClientSession` 封裝 WebSocket 連線 |

## 國際化

| 項目 | 說明 |
|------|------|
| **繁體中文** | 全介面中文化，包含 UI 文字、玩家名稱、遊戲事件提示 |
| **UTF-8 編碼** | 全專案統一使用 UTF-8 字元編碼 |

## 外部依賴總覽

| 依賴 | 版本 | 用途 |
|------|------|------|
| `org.java-websocket:Java-WebSocket` | 1.5.6 | WebSocket 客戶端與伺服器 |
| `maven-shade-plugin` | 3.5.1 | Fat JAR 打包 |

> 本專案除了 Java-WebSocket 外，未使用任何外部第三方函式庫，JSON 解析、HTTP 伺服器等皆為自行實作或使用 Java 內建 API。
