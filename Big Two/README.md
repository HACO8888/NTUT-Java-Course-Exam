# 大老二 Big Two 撲克牌遊戲

一個使用 Java Spring Boot 開發的大老二撲克牌遊戲，支援網頁視覺化介面。

## 功能特色

- 🎮 四人對戰（1位真人玩家 + 3位電腦AI）
- 🃏 完整的撲克牌規則實作
- 🎨 精美的網頁視覺化介面
- 🤖 智能AI對手
- 📱 響應式設計，支援手機/平板

## 遊戲規則

### 牌型大小順序
同花順 > 鐵支 > 葫蘆 > 同花 > 順子 > 對子 > 單張

### 點數大小順序
2 > A > K > Q > J > 10 > 9 > 8 > 7 > 6 > 5 > 4 > 3

### 花色大小順序
♠ 黑桃 > ♥ 紅心 > ♦ 方塊 > ♣ 梅花

### 基本規則
1. 遊戲開始時，持有 **梅花3** 的玩家先出牌
2. 第一回合必須包含梅花3
3. 後續玩家必須出相同牌型且更大的牌，或選擇跳過(Pass)
4. 當連續三位玩家都Pass時，最後出牌的玩家可重新自由出牌
5. 最先出完手牌的玩家獲勝

## 技術架構

### 後端 (Java)
- **Spring Boot 3.2** - Web 框架
- **Lombok** - 簡化程式碼
- **Jackson** - JSON 處理

### 核心類別
| 類別 | 說明 |
|------|------|
| `Card` | 撲克牌（花色 + 點數） |
| `Suit` | 花色枚舉（黑桃、紅心、方塊、梅花） |
| `Rank` | 點數枚舉（3~10、J、Q、K、A、2） |
| `HandType` | 牌型枚舉（單張、對子、順子等） |
| `Deck` | 牌組（洗牌、發牌） |
| `Player` | 玩家 |
| `Play` | 出牌動作（牌型分析） |
| `BigTwoGame` | 遊戲引擎 |
| `AIPlayerService` | AI 邏輯 |

### 前端
- **HTML5** - 結構
- **CSS3** - 樣式（漸層、動畫、響應式）
- **JavaScript** - 遊戲互動（AJAX 輪詢）

## 執行方式

### 需求
- Java 17 或更高版本
- Maven 3.6+

### 執行步驟

1. 編譯並執行
```bash
mvn spring-boot:run
```

2. 開啟瀏覽器
```
http://localhost:8080
```

3. 輸入你的名字，點擊「開始遊戲」

### 打包成 JAR
```bash
mvn clean package
java -jar target/bigtwo-game-1.0.0.jar
```

## 專案結構

```
Big Two/
├── pom.xml                           # Maven 配置
├── README.md                         # 說明文件
├── src/
│   ├── main/
│   │   ├── java/com/bigtwo/
│   │   │   ├── BigTwoApplication.java        # 主程式
│   │   │   ├── config/
│   │   │   │   └── WebConfig.java            # Web 配置
│   │   │   ├── controller/
│   │   │   │   └── GameController.java       # REST API 控制器
│   │   │   ├── model/
│   │   │   │   ├── BigTwoGame.java           # 遊戲引擎
│   │   │   │   ├── Card.java                 # 撲克牌
│   │   │   │   ├── Deck.java                 # 牌組
│   │   │   │   ├── GameState.java            # 遊戲狀態
│   │   │   │   ├── HandType.java             # 牌型枚舉
│   │   │   │   ├── Play.java                 # 出牌動作
│   │   │   │   ├── Player.java               # 玩家
│   │   │   │   ├── Rank.java                 # 點數枚舉
│   │   │   │   └── Suit.java                 # 花色枚舉
│   │   │   └── service/
│   │   │       ├── AIPlayerService.java      # AI 邏輯
│   │   │       └── GameService.java          # 遊戲服務
│   │   └── resources/
│   │       ├── static/
│   │       │   ├── css/style.css             # 遊戲樣式
│   │       │   └── js/game.js                # 遊戲邏輯
│   │       └── templates/
│   │           └── index.html                # 遊戲頁面
```

## API 端點

| 方法 | 端點 | 說明 |
|------|------|------|
| POST | `/api/game/create` | 建立遊戲 |
| POST | `/api/game/{gameId}/start` | 開始遊戲 |
| GET | `/api/game/{gameId}/state` | 取得遊戲狀態 |
| POST | `/api/game/{gameId}/play` | 出牌 |
| POST | `/api/game/{gameId}/pass` | 跳過 |
| POST | `/api/game/{gameId}/restart` | 重新開始 |

## 畫面預覽

- **開始畫面**：輸入玩家名稱，顯示遊戲規則
- **遊戲桌**：四人座位，中央出牌區
- **手牌區**：可點擊選擇要出的牌
- **記錄區**：顯示遊戲進行記錄

## 未來擴充

- [ ] WebSocket 即時通訊
- [ ] 多人連線對戰
- [ ] 更強大的 AI 策略
- [ ] 音效和動畫
- [ ] 儲存遊戲記錄

## 授權

MIT License
