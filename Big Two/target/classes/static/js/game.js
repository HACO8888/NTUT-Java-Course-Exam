// 大老二遊戲前端邏輯
const game = {
    gameId: null,
    playerId: 0,
    playerName: '玩家',
    state: null,
    selectedCards: [],
    pollingInterval: null,

    // 花色符號對應
    suitSymbols: {
        'SPADES': '♠',
        'HEARTS': '♥',
        'DIAMONDS': '♦',
        'CLUBS': '♣'
    },

    // 建立新遊戲
    create() {
        this.playerName = document.getElementById('playerName').value || '玩家';
        
        fetch('/api/game/create', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerName: this.playerName })
        })
        .then(res => res.json())
        .then(data => {
            this.gameId = data.gameId;
            this.start();
        })
        .catch(err => this.showToast('建立遊戲失敗', 'error'));
    },

    // 開始遊戲
    start() {
        fetch(`/api/game/${this.gameId}/start`, { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            document.getElementById('startScreen').classList.add('hidden');
            document.getElementById('gameScreen').classList.remove('hidden');
            document.getElementById('myName').textContent = this.playerName;
            
            this.updateState(data);
            this.startPolling();
        })
        .catch(err => this.showToast('開始遊戲失敗', 'error'));
    },

    // 開始輪詢
    startPolling() {
        if (this.pollingInterval) clearInterval(this.pollingInterval);
        this.pollingInterval = setInterval(() => this.pollState(), 1000);
    },

    // 輪詢遊戲狀態
    pollState() {
        fetch(`/api/game/${this.gameId}/state?playerId=${this.playerId}`)
        .then(res => res.json())
        .then(data => {
            this.updateState(data);
        })
        .catch(err => console.error('輪詢失敗:', err));
    },

    // 更新遊戲狀態
    updateState(state) {
        this.state = state;
        
        // 更新玩家資訊
        this.updatePlayers(state.players);
        
        // 更新我的手牌
        if (state.myCards) {
            this.renderMyHand(state.myCards);
        }
        
        // 更新出牌區域
        this.renderPlayArea(state.lastPlay);
        
        // 更新按鈕狀態
        this.updateButtons(state);
        
        // 更新訊息
        if (state.message) {
            document.getElementById('lastPlayInfo').textContent = state.message;
            this.addLog(state.message);
        }
        
        // 檢查遊戲結束
        if (state.finished) {
            this.showGameOver(state.players);
        }
    },

    // 更新玩家顯示
    updatePlayers(players) {
        players.forEach(player => {
            const el = document.getElementById(`player${player.id}`);
            if (!el) return;
            
            const infoEl = el.querySelector('.player-info');
            const countEl = el.querySelector('.card-count');
            
            // 更新名字
            if (player.id === 0) {
                infoEl.querySelector('.player-name').textContent = this.playerName;
            }
            
            // 更新牌數
            countEl.textContent = `${player.cardCount} 張`;
            
            // 更新活躍狀態
            if (player.isCurrent) {
                infoEl.classList.add('active');
            } else {
                infoEl.classList.remove('active');
            }
            
            // 更新電腦玩家的牌背
            if (!player.isHuman) {
                this.renderBackCards(el.querySelector('.player-hand'), player.cardCount);
            }
        });
    },

    // 渲染牌背
    renderBackCards(container, count) {
        if (!container) return;
        container.innerHTML = '';
        for (let i = 0; i < count; i++) {
            const card = document.createElement('div');
            card.className = 'back-card';
            container.appendChild(card);
        }
    },

    // 渲染我的手牌
    renderMyHand(cards) {
        const container = document.getElementById('myHand');
        container.innerHTML = '';
        
        cards.forEach((card, index) => {
            const cardEl = this.createCardElement(card, index);
            container.appendChild(cardEl);
        });
        
        // 更新牌數顯示
        document.getElementById('myCardCount').textContent = `${cards.length} 張`;
    },

    // 建立卡牌元素
    createCardElement(card, index) {
        const div = document.createElement('div');
        div.className = `card suit-${card.suit.toLowerCase()}`;
        div.dataset.index = index;
        div.dataset.suit = card.suit;
        div.dataset.rank = card.rank;
        
        const suitSymbol = this.suitSymbols[card.suit];
        const rankDisplay = this.getRankDisplay(card.rank);
        
        div.innerHTML = `
            <div class="card-top">
                <span>${rankDisplay}</span>
                <span>${suitSymbol}</span>
            </div>
            <div class="card-center">${suitSymbol}</div>
            <div class="card-bottom">
                <span>${suitSymbol}</span>
                <span>${rankDisplay}</span>
            </div>
        `;
        
        div.onclick = () => this.toggleCardSelection(div, card);
        
        return div;
    },

    // 取得點數顯示
    getRankDisplay(rank) {
        const display = {
            'ACE': 'A',
            'TWO': '2',
            'THREE': '3',
            'FOUR': '4',
            'FIVE': '5',
            'SIX': '6',
            'SEVEN': '7',
            'EIGHT': '8',
            'NINE': '9',
            'TEN': '10',
            'JACK': 'J',
            'QUEEN': 'Q',
            'KING': 'K'
        };
        return display[rank] || rank;
    },

    // 切換卡牌選擇
    toggleCardSelection(element, card) {
        const index = this.selectedCards.findIndex(c => 
            c.suit === card.suit && c.rank === card.rank
        );
        
        if (index === -1) {
            this.selectedCards.push(card);
            element.classList.add('selected');
        } else {
            this.selectedCards.splice(index, 1);
            element.classList.remove('selected');
        }
    },

    // 渲染出牌區域
    renderPlayArea(play) {
        const container = document.getElementById('playArea');
        container.innerHTML = '';
        
        if (!play || !play.cards || play.cards.length === 0) {
            return;
        }
        
        // 顯示牌型標籤
        if (play.handType) {
            const label = document.createElement('div');
            label.className = 'hand-type-label';
            label.textContent = play.handType.name;
            container.appendChild(label);
        }
        
        // 顯示卡牌
        play.cards.forEach(card => {
            const cardEl = this.createCardElement(card, -1);
            cardEl.style.cursor = 'default';
            cardEl.onclick = null;
            container.appendChild(cardEl);
        });
    },

    // 更新按鈕狀態
    updateButtons(state) {
        const passBtn = document.getElementById('passBtn');
        const playBtn = document.getElementById('playBtn');
        
        passBtn.disabled = !state.canPass;
        playBtn.disabled = !state.canPlay || this.selectedCards.length === 0;
    },

    // 出牌
    play() {
        if (this.selectedCards.length === 0) {
            this.showToast('請選擇要出的牌', 'error');
            return;
        }
        
        fetch(`/api/game/${this.gameId}/play`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                playerId: this.playerId,
                cards: this.selectedCards
            })
        })
        .then(res => res.json())
        .then(data => {
            this.selectedCards = [];
            this.updateState(data);
            
            if (data.message && data.message.includes('失敗')) {
                this.showToast(data.message, 'error');
            }
        })
        .catch(err => this.showToast('出牌失敗', 'error'));
    },

    // Pass
    pass() {
        fetch(`/api/game/${this.gameId}/pass`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ playerId: this.playerId })
        })
        .then(res => res.json())
        .then(data => {
            this.selectedCards = [];
            document.querySelectorAll('.card.selected').forEach(el => {
                el.classList.remove('selected');
            });
            this.updateState(data);
        })
        .catch(err => this.showToast('操作失敗', 'error'));
    },

    // 提示
    hint() {
        if (!this.state || !this.state.myCards) return;
        
        const cards = document.querySelectorAll('#myHand .card');
        
        // 簡單提示：選擇最小的可出牌
        if (this.state.lastPlay) {
            // 根據上一手提示
            const type = this.state.lastPlay.handType;
            if (type && type.cardCount) {
                // 清除現有選擇
                this.selectedCards = [];
                cards.forEach(el => el.classList.remove('selected'));
                
                // 嘗試選擇相同數量的牌
                for (let i = 0; i < Math.min(type.cardCount, cards.length); i++) {
                    cards[i].click();
                }
            }
        } else {
            // 自由出牌，選最小的
            this.selectedCards = [];
            cards.forEach(el => el.classList.remove('selected'));
            if (cards.length > 0) {
                cards[0].click();
            }
        }
        
        this.showToast('已選擇建議的牌', 'success');
    },

    // 排序手牌
    sortHand() {
        if (!this.state || !this.state.myCards) return;
        
        // 重新取得狀態會自動排序
        this.pollState();
        this.showToast('手牌已排序', 'success');
    },

    // 顯示遊戲結束
    showGameOver(players) {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
        
        const modal = document.getElementById('gameOverModal');
        const rankings = document.getElementById('rankings');
        
        // 排序玩家
        const sortedPlayers = [...players].sort((a, b) => {
            if (a.finishRank === 0) return 1;
            if (b.finishRank === 0) return -1;
            return a.finishRank - b.finishRank;
        });
        
        rankings.innerHTML = sortedPlayers.map((p, i) => {
            const name = p.id === 0 ? this.playerName : p.name;
            const rankClass = `rank-${p.finishRank}`;
            const rankText = p.finishRank === 1 ? '🥇' : 
                            p.finishRank === 2 ? '🥈' : 
                            p.finishRank === 3 ? '🥉' : '💩';
            return `
                <div class="rank-item ${rankClass}">
                    <span>${rankText} ${name}</span>
                    <span class="rank-number">第 ${p.finishRank} 名</span>
                </div>
            `;
        }).join('');
        
        modal.classList.add('show');
    },

    // 重新開始
    restart() {
        document.getElementById('gameOverModal').classList.remove('show');
        
        fetch(`/api/game/${this.gameId}/restart`, { method: 'POST' })
        .then(res => res.json())
        .then(data => {
            this.selectedCards = [];
            document.getElementById('logContent').innerHTML = '';
            this.updateState(data);
            this.startPolling();
        })
        .catch(err => this.showToast('重新開始失敗', 'error'));
    },

    // 添加日誌
    addLog(message) {
        const logContent = document.getElementById('logContent');
        const entry = document.createElement('div');
        entry.className = 'log-entry';
        
        if (message.includes('出了')) {
            entry.classList.add('play');
        } else if (message.includes('跳過')) {
            entry.classList.add('pass');
        } else {
            entry.classList.add('system');
        }
        
        entry.textContent = message;
        logContent.appendChild(entry);
        logContent.scrollTop = logContent.scrollHeight;
    },

    // 顯示 Toast
    showToast(message, type = 'info') {
        const toast = document.getElementById('toast');
        toast.textContent = message;
        toast.className = `toast ${type} show`;
        
        setTimeout(() => {
            toast.classList.remove('show');
        }, 3000);
    }
};

// 頁面載入完成
window.onload = () => {
    console.log('大老二遊戲已載入');
};
