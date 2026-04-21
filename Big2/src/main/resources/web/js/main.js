(function() {
    'use strict';

    // ── State ────────────────────────────────────────────────────────
    let ws = null;
    let mySeat = -1;
    let playerCount = 0;
    let currentSeat = -1;
    let playerNames = [];
    let cardCounts = [];
    let myHand = [];
    let selectedCards = new Set();
    let isCreator = false;
    let myTurn = false;
    let canPassFlag = false;

    const suitSymbols = { CLUBS: '♣', DIAMONDS: '♦', HEARTS: '♥', SPADES: '♠' };
    const suitNames = { CLUBS: '梅', DIAMONDS: '方', HEARTS: '紅', SPADES: '黑' };
    const rankSymbols = {
        THREE:'3', FOUR:'4', FIVE:'5', SIX:'6', SEVEN:'7', EIGHT:'8', NINE:'9',
        TEN:'10', JACK:'J', QUEEN:'Q', KING:'K', ACE:'A', TWO:'2'
    };
    const handTypeNames = {
        SINGLE:'單張', PAIR:'對子', STRAIGHT:'順子', FLUSH:'同花',
        FULL_HOUSE:'葫蘆', FOUR_OF_A_KIND:'四條', STRAIGHT_FLUSH:'同花順'
    };
    const avatarColors = ['#2E8B57', '#4A8AFF', '#D4A843', '#A06AFF'];

    // ── DOM refs ─────────────────────────────────────────────────────
    const $ = id => document.getElementById(id);

    function showScreen(id) {
        document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
        $(id).classList.add('active');
    }

    // ── WebSocket ────────────────────────────────────────────────────
    function connect(callback) {
        const wsUrl = window.location.hostname === 'localhost'
            ? 'ws://localhost:5555'
            : 'wss://api-big2.haco.tw';
        ws = new WebSocket(wsUrl);
        ws.onopen = () => callback();
        ws.onclose = () => { ws = null; };
        ws.onerror = () => alert('無法連接伺服器！');
        ws.onmessage = e => handleMessage(JSON.parse(e.data));
    }

    function send(obj) {
        if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj));
    }

    // ── Message handler ──────────────────────────────────────────────
    function handleMessage(msg) {
        switch (msg.type) {
            case 'ROOM_CREATED': onRoomCreated(msg); break;
            case 'ROOM_JOINED': onRoomJoined(msg); break;
            case 'PLAYER_JOINED': onPlayerJoined(msg); break;
            case 'PLAYER_LEFT': onPlayerLeft(msg); break;
            case 'AI_ADDED': onAIAdded(msg); break;
            case 'AI_REMOVED': onAIRemoved(msg); break;
            case 'GAME_STARTED': onGameStarted(msg); break;
            case 'PLAY_RESULT': onPlayResult(msg); break;
            case 'PASS_RESULT': onPassResult(msg); break;
            case 'YOUR_TURN': onYourTurn(msg); break;
            case 'PLAY_ERROR': setStatus(msg.message); break;
            case 'GAME_OVER': onGameOver(msg); break;
            case 'PLAYER_DISCONNECTED': onPlayerDisconnected(msg); break;
            case 'ERROR': alert(msg.message); break;
        }
    }

    // ── Lobby ────────────────────────────────────────────────────────
    $('createRoomBtn').onclick = () => {
        const name = $('playerName').value.trim();
        if (!name) { alert('請輸入玩家名稱！'); return; }
        connect(() => send({ type: 'CREATE_ROOM', playerName: name }));
    };

    $('joinRoomBtn').onclick = () => {
        const name = $('playerName').value.trim();
        const code = $('roomCodeInput').value.trim().toUpperCase();
        if (!name) { alert('請輸入玩家名稱！'); return; }
        if (!code) { alert('請輸入房間代碼！'); return; }
        connect(() => send({ type: 'JOIN_ROOM', playerName: name, roomCode: code }));
    };

    // ── Waiting Room ─────────────────────────────────────────────────
    const seatData = [{},{},{},{}];

    function onRoomCreated(msg) {
        mySeat = msg.seatIndex;
        isCreator = true;
        $('roomCodeDisplay').textContent = msg.roomCode;
        resetSeats();
        setSeatOccupied(mySeat, $('playerName').value.trim(), false);
        $('startGameBtn').style.display = '';
        updateWaitingUI();
        showScreen('waitingRoom');
    }

    function onRoomJoined(msg) {
        mySeat = msg.seatIndex;
        isCreator = false;
        $('roomCodeDisplay').textContent = msg.roomCode;
        resetSeats();
        msg.players.forEach(p => {
            if (p.isAI) setSeatAI(p.seat, p.name);
            else setSeatOccupied(p.seat, p.name, false);
        });
        $('startGameBtn').style.display = 'none';
        updateWaitingUI();
        showScreen('waitingRoom');
    }

    function onPlayerJoined(msg) {
        setSeatOccupied(msg.seatIndex, msg.playerName, false);
        updateWaitingUI();
    }

    function onPlayerLeft(msg) {
        clearSeat(msg.seatIndex);
        updateWaitingUI();
    }

    function onAIAdded(msg) {
        setSeatAI(msg.seatIndex, msg.name);
        updateWaitingUI();
    }

    function onAIRemoved(msg) {
        clearSeat(msg.seatIndex);
        updateWaitingUI();
    }

    function resetSeats() {
        for (let i = 0; i < 4; i++) {
            seatData[i] = { occupied: false, name: null, isAI: false };
            const slot = document.querySelector(`.seat-slot[data-seat="${i}"]`);
            slot.className = 'seat-slot';
            slot.querySelector('.seat-name').textContent = '空位';
            slot.querySelector('.seat-status').textContent = '座位 ' + (i+1);
        }
    }

    function setSeatOccupied(idx, name, ai) {
        seatData[idx] = { occupied: true, name, isAI: ai };
        const slot = document.querySelector(`.seat-slot[data-seat="${idx}"]`);
        slot.classList.add('occupied');
        slot.classList.remove('ai');
        slot.querySelector('.seat-name').textContent = name;
        slot.querySelector('.seat-status').textContent = '玩家';
    }

    function setSeatAI(idx, name) {
        seatData[idx] = { occupied: true, name, isAI: true };
        const slot = document.querySelector(`.seat-slot[data-seat="${idx}"]`);
        slot.classList.remove('occupied');
        slot.classList.add('ai');
        slot.querySelector('.seat-name').textContent = name;
        slot.querySelector('.seat-status').textContent = 'AI';
    }

    function clearSeat(idx) {
        seatData[idx] = { occupied: false, name: null, isAI: false };
        const slot = document.querySelector(`.seat-slot[data-seat="${idx}"]`);
        slot.className = 'seat-slot';
        slot.querySelector('.seat-name').textContent = '空位';
        slot.querySelector('.seat-status').textContent = '座位 ' + (idx+1);
    }

    function updateWaitingUI() {
        let count = seatData.filter(s => s.occupied).length;
        $('waitingStatus').textContent = count >= 3 ? '可以開始遊戲！' : '等待玩家加入... (' + count + '/4)';
        if (isCreator) {
            $('startGameBtn').disabled = count < 3;
        }
        document.querySelectorAll('.seat-slot').forEach(slot => {
            const idx = parseInt(slot.dataset.seat);
            const btn = slot.querySelector('.ai-btn');
            if (!isCreator) { btn.style.display = 'none'; return; }
            if (seatData[idx].occupied && !seatData[idx].isAI) {
                btn.style.display = 'none';
            } else if (seatData[idx].isAI) {
                btn.style.display = '';
                btn.textContent = '移除';
                btn.className = 'ai-btn btn btn-danger btn-sm';
                btn.onclick = () => send({ type: 'REMOVE_AI', slotIndex: idx });
            } else {
                btn.style.display = '';
                btn.textContent = '+ AI';
                btn.className = 'ai-btn btn btn-ghost btn-sm';
                btn.onclick = () => send({ type: 'ADD_AI', slotIndex: idx });
            }
        });
    }

    $('startGameBtn').onclick = () => send({ type: 'START_GAME' });
    $('leaveRoomBtn').onclick = () => {
        send({ type: 'LEAVE_ROOM' });
        if (ws) ws.close();
        showScreen('lobby');
    };

    // ── Game ─────────────────────────────────────────────────────────
    function onGameStarted(msg) {
        playerCount = msg.playerCount;
        mySeat = msg.yourSeat;
        currentSeat = msg.currentSeat;
        playerNames = msg.seatOrder.map(p => p.name);
        cardCounts = msg.seatOrder.map(p => p.cardCount);
        myHand = msg.yourHand;
        selectedCards.clear();
        myTurn = false;

        buildOpponents();
        renderHand();
        clearTable();
        $('gameLog').innerHTML = '<div>遊戲開始！</div>';
        setStatus('');
        updateGameUI();
        showScreen('gameScreen');
    }

    function buildOpponents() {
        const row = $('opponentRow');
        row.innerHTML = '';
        for (let i = 1; i < playerCount; i++) {
            const actualSeat = (mySeat + i) % playerCount;
            const slot = document.createElement('div');
            slot.className = 'panel opponent-slot';
            slot.dataset.seat = actualSeat;

            const badge = document.createElement('div');
            badge.className = 'player-badge';
            badge.id = 'badge-' + actualSeat;

            const avatar = document.createElement('div');
            avatar.className = 'avatar';
            avatar.style.background = avatarColors[actualSeat % avatarColors.length];
            avatar.textContent = playerNames[actualSeat].charAt(0);

            const text = document.createElement('div');
            text.className = 'badge-text';
            text.innerHTML = `<span class="badge-name">${playerNames[actualSeat]}</span><span class="badge-cards" id="cards-${actualSeat}">${cardCounts[actualSeat]} 張</span>`;

            badge.appendChild(avatar);
            badge.appendChild(text);
            slot.appendChild(badge);

            const cardsDiv = document.createElement('div');
            cardsDiv.className = 'opponent-cards';
            cardsDiv.id = 'fan-' + actualSeat;
            renderFan(cardsDiv, cardCounts[actualSeat]);
            slot.appendChild(cardsDiv);

            row.appendChild(slot);
        }
    }

    function renderFan(container, count) {
        container.innerHTML = '';
        if (count === 0) return;
        const totalAngle = Math.min(50, count * 3.5);
        const startAngle = -totalAngle / 2;
        for (let i = 0; i < count; i++) {
            const card = document.createElement('div');
            card.className = 'card-back';
            const angle = count > 1 ? startAngle + (totalAngle * i / (count - 1)) : 0;
            card.style.transform = `rotate(${angle}deg)`;
            container.appendChild(card);
        }
    }

    function renderHand() {
        const container = $('myHand');
        container.innerHTML = '';
        myHand.forEach((card, idx) => {
            const el = createCardElement(card, false);
            if (selectedCards.has(idx)) el.classList.add('selected');
            el.onclick = () => {
                if (!myTurn) return;
                if (selectedCards.has(idx)) selectedCards.delete(idx);
                else selectedCards.add(idx);
                renderHand();
                updateHandTypeChip();
            };
            container.appendChild(el);
        });
        const myActualCards = myHand.length;
        cardCounts[mySeat] = myActualCards;
        const myBadgeCards = document.querySelector('#myInfoPanel .badge-cards');
        if (myBadgeCards) myBadgeCards.textContent = myActualCards + ' 張';
    }

    function createCardElement(card, isTableCard) {
        const el = document.createElement('div');
        const isRed = card.suit === 'HEARTS' || card.suit === 'DIAMONDS';
        el.className = 'card ' + (isRed ? 'red' : 'black') + (isTableCard ? ' table-card' : '');

        const corner = document.createElement('div');
        corner.className = 'card-corner';
        corner.innerHTML = rankSymbols[card.rank] + '<br>' + suitSymbols[card.suit];

        const center = document.createElement('div');
        center.className = 'card-center';
        center.textContent = suitSymbols[card.suit];

        el.appendChild(corner);
        el.appendChild(center);
        return el;
    }

    function clearTable() {
        $('tableCards').innerHTML = '<div class="table-empty">等待出牌</div>';
        $('tableInfo').textContent = '';
    }

    function setTableCards(cards, playerName, handType) {
        const tc = $('tableCards');
        tc.innerHTML = '';
        cards.forEach(card => tc.appendChild(createCardElement(card, true)));
        $('tableInfo').textContent = playerName + ' — ' + (handTypeNames[handType] || handType);
    }

    function onPlayResult(msg) {
        const seat = msg.seatIndex;
        cardCounts[seat] = msg.cardsRemaining;
        currentSeat = msg.nextSeat;
        if (seat === mySeat) myTurn = false;

        if (seat === mySeat) {
            const playedCards = msg.cards;
            myHand = myHand.filter(c => !playedCards.some(p => p.rank === c.rank && p.suit === c.suit));
            selectedCards.clear();
            renderHand();
        }

        setTableCards(msg.cards, msg.playerName, msg.handType);
        addLog(msg.playerName + ' 出牌: ' + msg.cards.map(c => suitSymbols[c.suit] + rankSymbols[c.rank]).join(' '));

        if (msg.newRound) {
            addLog('── 新的一輪 ──');
            clearTable();
        }

        updateGameUI();
    }

    function onPassResult(msg) {
        if (msg.seatIndex === mySeat) myTurn = false;
        currentSeat = msg.nextSeat;
        addLog(playerNames[msg.seatIndex] + ' Pass');

        if (msg.newRound) {
            addLog('── 新的一輪 ──');
            clearTable();
        }

        updateGameUI();
    }

    function onYourTurn(msg) {
        myTurn = true;
        canPassFlag = msg.canPass;
        const hint = msg.mustIncludeClubThree ? '第一手必須包含梅花 3'
            : !msg.canPass ? '你控制桌面，請出牌' : '出牌或 Pass';
        setStatus(hint);
        updateButtons();
    }

    function onGameOver(msg) {
        myTurn = false;
        updateButtons();
        const iWon = msg.winnerSeat === mySeat;
        $('resultIcon').textContent = iWon ? '🏆' : '😔';
        $('resultTitle').textContent = iWon ? '恭喜獲勝！' : '本局結束';
        $('resultTitle').style.color = iWon ? 'var(--gold-bright)' : 'var(--text-secondary)';
        $('resultSub').textContent = iWon ? '你擊敗了所有對手！' : msg.winnerName + ' 贏得本局';
        $('resultModal').style.display = 'flex';
    }

    function onPlayerDisconnected(msg) {
        if (msg.replacedByAI && msg.aiName) {
            playerNames[msg.seatIndex] = msg.aiName;
            addLog(msg.aiName + ' (AI) 接替斷線玩家');
        }
        updateGameUI();
    }

    function updateGameUI() {
        for (let i = 1; i < playerCount; i++) {
            const actualSeat = (mySeat + i) % playerCount;
            const fan = document.getElementById('fan-' + actualSeat);
            if (fan) renderFan(fan, cardCounts[actualSeat]);
            const cardsEl = document.getElementById('cards-' + actualSeat);
            if (cardsEl) cardsEl.textContent = cardCounts[actualSeat] + ' 張';

            const badge = document.getElementById('badge-' + actualSeat);
            if (badge) {
                badge.classList.toggle('active', currentSeat === actualSeat);
            }
        }

        const myBadge = $('myInfoPanel');
        myBadge.classList.toggle('active', currentSeat === mySeat);

        if (currentSeat !== mySeat && !myTurn) {
            setStatus(playerNames[currentSeat] + ' 的回合…');
        }
        updateButtons();
    }

    function updateButtons() {
        $('playBtn').disabled = !myTurn;
        $('passBtn').disabled = !myTurn || !canPassFlag;
    }

    function setStatus(text) { $('statusText').textContent = text; }

    function addLog(text) {
        const log = $('gameLog');
        const div = document.createElement('div');
        div.textContent = text;
        log.appendChild(div);
        log.scrollTop = log.scrollHeight;
    }

    function updateHandTypeChip() {
        const chip = $('handTypeChip');
        if (selectedCards.size === 0) { chip.textContent = ''; return; }
        const cards = [...selectedCards].map(i => myHand[i]);
        const type = detectHandType(cards);
        chip.textContent = type ? handTypeNames[type] || type : '無效牌型';
    }

    // ── Simple hand type detection (client-side) ─────────────────────
    function cardValue(card) {
        const rankOrder = ['THREE','FOUR','FIVE','SIX','SEVEN','EIGHT','NINE','TEN','JACK','QUEEN','KING','ACE','TWO'];
        const suitOrder = ['CLUBS','DIAMONDS','HEARTS','SPADES'];
        return rankOrder.indexOf(card.rank) * 4 + suitOrder.indexOf(card.suit);
    }

    function detectHandType(cards) {
        const n = cards.length;
        if (n === 1) return 'SINGLE';
        if (n === 2) return cards[0].rank === cards[1].rank ? 'PAIR' : null;
        if (n !== 5) return null;

        const rankOrder = ['THREE','FOUR','FIVE','SIX','SEVEN','EIGHT','NINE','TEN','JACK','QUEEN','KING','ACE','TWO'];
        const vals = cards.map(c => rankOrder.indexOf(c.rank)).sort((a,b) => a-b);
        const suits = cards.map(c => c.suit);

        const isFlush = suits.every(s => s === suits[0]);
        let isStraight = true;
        for (let i = 1; i < 5; i++) {
            if (vals[i] !== vals[i-1] + 1) { isStraight = false; break; }
        }
        if (!isStraight && vals[0]===0 && vals[1]===1 && vals[2]===2 && vals[3]===11 && vals[4]===12) isStraight = true;

        const freq = {};
        cards.forEach(c => freq[c.rank] = (freq[c.rank]||0) + 1);
        const freqVals = Object.values(freq);

        if (isFlush && isStraight) return 'STRAIGHT_FLUSH';
        if (freqVals.includes(4)) return 'FOUR_OF_A_KIND';
        if (freqVals.includes(3) && freqVals.includes(2)) return 'FULL_HOUSE';
        if (isFlush) return 'FLUSH';
        if (isStraight) return 'STRAIGHT';
        return null;
    }

    // ── Game buttons ─────────────────────────────────────────────────
    $('playBtn').onclick = () => {
        if (!myTurn) return;
        if (selectedCards.size === 0) { setStatus('請選擇要出的牌！'); return; }
        const cards = [...selectedCards].map(i => myHand[i]);
        send({ type: 'PLAY_CARDS', cards: cards.map(c => ({ rank: c.rank, suit: c.suit })) });
    };

    $('passBtn').onclick = () => {
        if (!myTurn) return;
        send({ type: 'PASS' });
    };

    $('backBtn').onclick = () => {
        send({ type: 'LEAVE_ROOM' });
        if (ws) ws.close();
        showScreen('lobby');
    };

    $('resultOkBtn').onclick = () => {
        $('resultModal').style.display = 'none';
    };

    // ── Keyboard shortcuts ───────────────────────────────────────────
    document.addEventListener('keydown', e => {
        if (e.target.tagName === 'INPUT') return;
        if (e.key === 'Enter') $('playBtn').click();
        if (e.key === ' ') { e.preventDefault(); $('passBtn').click(); }
    });

})();
