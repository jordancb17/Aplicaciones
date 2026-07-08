/* ============================================================
   TITANES DE GUERRA — juego de estrategia estilo Dawn of Titans
   Batallas por carriles, campaña de conquista y mejoras de tropas.
   ============================================================ */
'use strict';

/* ---------------- Datos de unidades ---------------- */
const UNIT_TYPES = {
  sword: {
    name: 'Espadachín', icon: '⚔️', cost: 3, squad: 3,
    hp: 62, dmg: 9, range: 26, speed: 46, atkTime: 0.9, radius: 11,
    desc: 'Infantería equilibrada cuerpo a cuerpo.'
  },
  archer: {
    name: 'Arquero', icon: '🏹', cost: 4, squad: 3,
    hp: 36, dmg: 8, range: 170, speed: 40, atkTime: 1.15, radius: 10,
    desc: 'Ataca a distancia con flechas.'
  },
  cavalry: {
    name: 'Caballería', icon: '🐎', cost: 5, squad: 2,
    hp: 95, dmg: 15, range: 28, speed: 88, atkTime: 0.85, radius: 13,
    desc: 'Rápida y letal en la carga.'
  },
  titan: {
    name: 'Titán', icon: '🗿', cost: 10, squad: 1,
    hp: 560, dmg: 48, range: 42, speed: 24, atkTime: 1.4, radius: 24,
    desc: 'Coloso lento pero devastador.'
  }
};
const UNIT_ORDER = ['sword', 'archer', 'cavalry', 'titan'];
const LEVEL_BONUS = 0.18; // +18% de vida y daño por nivel

/* ---------------- Territorios de la campaña ---------------- */
const TERRITORIES = [
  { name: 'Valle Verde',        icon: '🌿', x: .09, y: .72 },
  { name: 'Puente Roto',        icon: '🌉', x: .20, y: .48 },
  { name: 'Bosque Sombrío',     icon: '🌲', x: .33, y: .68 },
  { name: 'Colinas del Cuervo', icon: '🪶', x: .44, y: .40 },
  { name: 'Fortaleza Gris',     icon: '🏰', x: .55, y: .64 },
  { name: 'Desierto Ardiente',  icon: '🏜️', x: .66, y: .36 },
  { name: 'Paso Helado',        icon: '❄️', x: .76, y: .62 },
  { name: 'Cráter de Fuego',    icon: '🌋', x: .85, y: .35 },
  { name: 'Ciudadela Oscura',   icon: '🌑', x: .93, y: .60 },
  { name: 'Trono del Titán',    icon: '👑', x: .90, y: .18 }
];

/* ---------------- Estado guardado ---------------- */
const SAVE_KEY = 'titanes_save_v1';
let state = loadState();

function defaultState() {
  return {
    gold: 150,
    conquered: 0, // número de territorios conquistados
    levels: { sword: 1, archer: 1, cavalry: 1, titan: 1 }
  };
}
function loadState() {
  try {
    const raw = localStorage.getItem(SAVE_KEY);
    if (raw) {
      const s = JSON.parse(raw);
      const d = defaultState();
      return { ...d, ...s, levels: { ...d.levels, ...(s.levels || {}) } };
    }
  } catch (e) { /* guardado corrupto: se empieza de cero */ }
  return defaultState();
}
function saveState() {
  try { localStorage.setItem(SAVE_KEY, JSON.stringify(state)); } catch (e) {}
}

function upgradeCost(type) {
  return Math.round(80 * Math.pow(1.6, state.levels[type] - 1));
}
function statMult(level) {
  return 1 + LEVEL_BONUS * (level - 1);
}

/* ---------------- Navegación de pantallas ---------------- */
const screens = ['menu', 'map', 'army', 'battle'];
let currentScreen = 'menu';

function show(name) {
  screens.forEach(s => {
    document.getElementById('screen-' + s).classList.toggle('active', s === name);
  });
  currentScreen = name;
  if (name === 'menu') renderMenu();
  if (name === 'map') renderMap();
  if (name === 'army') renderArmy();
}

/* El botón atrás de Android llama a esta función. */
window.handleAndroidBack = function () {
  if (document.getElementById('overlay-result').classList.contains('active')) {
    return 'ok';
  }
  if (currentScreen === 'battle') { endBattle(); show('map'); return 'ok'; }
  if (currentScreen === 'map' || currentScreen === 'army') { show('menu'); return 'ok'; }
  return 'exit';
};

/* ---------------- Menú ---------------- */
function renderMenu() {
  document.getElementById('menu-gold').textContent =
    `🪙 ${state.gold} de oro · ${state.conquered}/${TERRITORIES.length} territorios`;
}

document.getElementById('btn-play').addEventListener('click', () => show('map'));
document.getElementById('btn-army').addEventListener('click', () => show('army'));
document.getElementById('btn-reset').addEventListener('click', () => {
  if (confirm('¿Seguro que quieres borrar todo tu progreso?')) {
    state = defaultState();
    saveState();
    renderMenu();
  }
});
document.querySelectorAll('[data-back]').forEach(b =>
  b.addEventListener('click', () => show('menu')));

/* ---------------- Mapa ---------------- */
function renderMap() {
  document.getElementById('map-gold').textContent = `🪙 ${state.gold}`;
  const area = document.getElementById('map-area');
  area.querySelectorAll('.territory').forEach(n => n.remove());

  const w = area.clientWidth, h = area.clientHeight;
  const svg = document.getElementById('map-paths');
  svg.setAttribute('viewBox', `0 0 ${w} ${h}`);
  let path = '';
  TERRITORIES.forEach((t, i) => {
    path += (i === 0 ? 'M' : 'L') + (t.x * w) + ' ' + (t.y * h) + ' ';
  });
  svg.innerHTML =
    `<path d="${path}" fill="none" stroke="rgba(240,214,122,.35)" stroke-width="4" stroke-dasharray="10 10" stroke-linecap="round"/>`;

  TERRITORIES.forEach((t, i) => {
    const el = document.createElement('div');
    el.className = 'territory ' +
      (i < state.conquered ? 'done' : i === state.conquered ? 'next' : 'locked');
    el.style.left = (t.x * 100) + '%';
    el.style.top = (t.y * 100) + '%';
    el.innerHTML = `<span class="t-icon">${t.icon}</span><span class="t-name">${t.name}</span>`;
    if (i <= state.conquered) {
      el.addEventListener('click', () => startBattle(i));
    }
    area.appendChild(el);
  });
}
window.addEventListener('resize', () => {
  if (currentScreen === 'map') renderMap();
});

/* ---------------- Ejército ---------------- */
function renderArmy() {
  document.getElementById('army-gold').textContent = `🪙 ${state.gold}`;
  const list = document.getElementById('army-list');
  list.innerHTML = '';
  UNIT_ORDER.forEach(type => {
    const u = UNIT_TYPES[type];
    const lvl = state.levels[type];
    const m = statMult(lvl);
    const cost = upgradeCost(type);
    const card = document.createElement('div');
    card.className = 'unit-card';
    card.innerHTML = `
      <span class="u-icon">${u.icon}</span>
      <div class="u-info">
        <div class="u-name">${u.name} <span class="u-level">Nv. ${lvl}</span></div>
        <div class="u-stats">❤️ ${Math.round(u.hp * m)} · 🗡️ ${Math.round(u.dmg * m)} · ${u.desc}</div>
      </div>
      <button class="btn btn-small ${state.gold >= cost ? 'btn-gold' : ''}" ${state.gold < cost ? 'disabled' : ''}>
        Mejorar<br>🪙 ${cost}
      </button>`;
    card.querySelector('button').addEventListener('click', () => {
      if (state.gold >= cost) {
        state.gold -= cost;
        state.levels[type]++;
        saveState();
        renderArmy();
      }
    });
    list.appendChild(card);
  });
}

/* ============================================================
   MOTOR DE BATALLA
   ============================================================ */
const canvas = document.getElementById('battle-canvas');
const ctx = canvas.getContext('2d');

const LANES = 3;
const WORLD_W = 1000;           // ancho lógico del campo
const CASTLE_X_PLAYER = 40;
const CASTLE_X_ENEMY = WORLD_W - 40;
const CASTLE_RANGE = 55;        // distancia a la que se ataca el castillo

let battle = null;
let rafId = 0;
let lastTime = 0;

function laneY(lane, H) {
  // los carriles ocupan la franja central del terreno
  const top = H * 0.34, bottom = H * 0.80;
  return top + (bottom - top) * (lane + 0.5) / LANES;
}

function makeUnit(side, type, lane, level, offset) {
  const base = UNIT_TYPES[type];
  const m = statMult(level);
  return {
    side, type, lane,
    x: side === 1 ? CASTLE_X_PLAYER + 45 + offset : CASTLE_X_ENEMY - 45 - offset,
    hp: base.hp * m, maxHp: base.hp * m,
    dmg: base.dmg * m,
    range: base.range, speed: base.speed,
    atkTime: base.atkTime, cooldown: Math.random() * 0.3,
    radius: base.radius,
    yJitter: (Math.random() - 0.5) * 18,
    walk: Math.random() * Math.PI * 2,
    dead: false
  };
}

function startBattle(index) {
  const t = TERRITORIES[index];
  const enemyLevel = index + 1;
  battle = {
    index,
    enemyLevel,
    firstTime: index === state.conquered,
    units: [],
    arrows: [],
    particles: [],
    playerHp: 1000, playerMaxHp: 1000,
    enemyHp: 750 + 260 * enemyLevel, enemyMaxHp: 750 + 260 * enemyLevel,
    mana: 6, manaMax: 12, manaRegen: 0.6,
    aiMana: 4, aiRegen: 0.34 + 0.07 * enemyLevel,
    aiTimer: 1.5,
    selectedCard: null,
    over: false,
    shake: 0,
    time: 0
  };
  document.getElementById('battle-title').textContent = `${t.icon} ${t.name}`;
  document.getElementById('enemy-label').textContent = `Nv.${enemyLevel} 🏯`;
  buildCards();
  show('battle');
  resizeCanvas();
  showHint('Toca una carta y luego un carril para desplegar');
  lastTime = performance.now();
  cancelAnimationFrame(rafId);
  rafId = requestAnimationFrame(loop);
}

function endBattle() {
  cancelAnimationFrame(rafId);
  battle = null;
}

/* ---------- Cartas de despliegue ---------- */
function buildCards() {
  const wrap = document.getElementById('cards');
  wrap.innerHTML = '';
  UNIT_ORDER.forEach(type => {
    const u = UNIT_TYPES[type];
    const card = document.createElement('div');
    card.className = 'card';
    card.dataset.type = type;
    card.innerHTML = `
      <span class="c-cost">${u.cost}</span>
      <div class="c-icon">${u.icon}</div>
      <div class="c-name">${u.name}</div>`;
    card.addEventListener('click', () => {
      if (!battle || battle.over) return;
      if (battle.mana < u.cost) return;
      battle.selectedCard = battle.selectedCard === type ? null : type;
      updateCards();
    });
    wrap.appendChild(card);
  });
}

function updateCards() {
  if (!battle) return;
  document.querySelectorAll('#cards .card').forEach(card => {
    const u = UNIT_TYPES[card.dataset.type];
    card.classList.toggle('selected', battle.selectedCard === card.dataset.type);
    card.classList.toggle('unaffordable', battle.mana < u.cost);
  });
  document.getElementById('mana-fill').style.width =
    (battle.mana / battle.manaMax * 100) + '%';
  document.getElementById('mana-text').textContent = Math.floor(battle.mana);
}

let hintTimeout = 0;
function showHint(text) {
  const el = document.getElementById('deploy-hint');
  el.textContent = text;
  el.classList.add('show');
  clearTimeout(hintTimeout);
  hintTimeout = setTimeout(() => el.classList.remove('show'), 2600);
}

/* ---------- Despliegue tocando el campo ---------- */
canvas.addEventListener('pointerdown', e => {
  if (!battle || battle.over || !battle.selectedCard) return;
  const rect = canvas.getBoundingClientRect();
  const y = e.clientY - rect.top;
  const H = rect.height;
  if (y < H * 0.28 || y > H * 0.86) return; // fuera del terreno
  let lane = 0, best = Infinity;
  for (let l = 0; l < LANES; l++) {
    const d = Math.abs(y - laneY(l, H));
    if (d < best) { best = d; lane = l; }
  }
  deploy(1, battle.selectedCard, lane);
});

function deploy(side, type, lane) {
  const u = UNIT_TYPES[type];
  if (side === 1) {
    if (battle.mana < u.cost) return;
    battle.mana -= u.cost;
  } else {
    if (battle.aiMana < u.cost) return;
    battle.aiMana -= u.cost;
  }
  const level = side === 1 ? state.levels[type] : battle.enemyLevel;
  for (let i = 0; i < u.squad; i++) {
    battle.units.push(makeUnit(side, type, lane, level, i * 16));
  }
  if (side === 1) updateCards();
}

/* ---------- IA enemiga ---------- */
function updateAI(dt) {
  battle.aiMana = Math.min(battle.manaMax, battle.aiMana + battle.aiRegen * dt);
  battle.aiTimer -= dt;
  if (battle.aiTimer > 0) return;
  battle.aiTimer = 0.8 + Math.random() * 1.4;

  // elige la unidad más cara que pueda pagar (a veces una aleatoria)
  const affordable = UNIT_ORDER.filter(t => UNIT_TYPES[t].cost <= battle.aiMana);
  if (!affordable.length) return;
  let type;
  if (Math.random() < 0.35) {
    type = affordable[Math.floor(Math.random() * affordable.length)];
  } else {
    type = affordable[affordable.length - 1];
  }
  // el titán enemigo solo aparece en niveles altos y con poca frecuencia
  if (type === 'titan' && (battle.enemyLevel < 4 || Math.random() < 0.5)) {
    type = affordable[0];
  }

  // despliega en el carril donde el jugador presiona más
  const pressure = [0, 0, 0];
  battle.units.forEach(u => { if (u.side === 1) pressure[u.lane]++; });
  let lane = 0;
  if (Math.random() < 0.6) {
    lane = pressure.indexOf(Math.max(...pressure));
  } else {
    lane = Math.floor(Math.random() * LANES);
  }
  deploy(-1, type, lane);
}

/* ---------- Simulación ---------- */
function updateUnits(dt) {
  const units = battle.units;
  for (const u of units) {
    if (u.dead) continue;
    u.cooldown -= dt;
    u.walk += dt * 6;

    // objetivo: enemigo más cercano en el mismo carril
    let target = null, targetDist = Infinity;
    for (const o of units) {
      if (o.dead || o.side === u.side || o.lane !== u.lane) continue;
      const d = Math.abs(o.x - u.x) - o.radius;
      if (d < targetDist) { targetDist = d; target = o; }
    }

    const castleX = u.side === 1 ? CASTLE_X_ENEMY : CASTLE_X_PLAYER;
    const castleDist = Math.abs(castleX - u.x);

    if (target && targetDist <= u.range) {
      if (u.cooldown <= 0) {
        u.cooldown = u.atkTime;
        attack(u, target);
      }
    } else if (castleDist <= CASTLE_RANGE + u.range * 0.4) {
      if (u.cooldown <= 0) {
        u.cooldown = u.atkTime;
        attackCastle(u);
      }
    } else {
      // avanzar, sin atravesar al enemigo más próximo
      let step = u.speed * dt * u.side;
      if (target) {
        const gap = targetDist - u.range * 0.85;
        if (gap > 0) step = Math.sign(step) * Math.min(Math.abs(step), gap);
        else step = 0;
      }
      u.x += step;
    }
  }
  battle.units = units.filter(u => !u.dead);
}

function attack(u, target) {
  if (u.type === 'archer') {
    battle.arrows.push({
      x: u.x, lane: u.lane, side: u.side,
      tx: target.x, target, dmg: u.dmg, speed: 420,
      yJitter: u.yJitter
    });
  } else {
    hurt(target, u.dmg);
    spawnParticles(target.x, target.lane, target.yJitter, u.side === 1 ? '#ffd97a' : '#ff8a7a', 4);
    if (u.type === 'titan') battle.shake = Math.max(battle.shake, 5);
  }
}

function attackCastle(u) {
  if (u.side === 1) {
    battle.enemyHp -= u.dmg;
    spawnParticles(WORLD_W - 70, u.lane, 0, '#ffd97a', 6);
  } else {
    battle.playerHp -= u.dmg;
    spawnParticles(70, u.lane, 0, '#ff8a7a', 6);
  }
  if (u.type === 'titan') battle.shake = Math.max(battle.shake, 8);
}

function hurt(target, dmg) {
  target.hp -= dmg;
  if (target.hp <= 0 && !target.dead) {
    target.dead = true;
    spawnParticles(target.x, target.lane, target.yJitter, '#c9cede', 10);
  }
}

function updateArrows(dt) {
  for (const a of battle.arrows) {
    const dir = Math.sign(a.tx - a.x) || a.side;
    a.x += dir * a.speed * dt;
    if ((dir > 0 && a.x >= a.tx) || (dir < 0 && a.x <= a.tx)) {
      a.hit = true;
      if (a.target && !a.target.dead) hurt(a.target, a.dmg);
    }
  }
  battle.arrows = battle.arrows.filter(a => !a.hit);
}

function spawnParticles(x, lane, yJitter, color, n) {
  for (let i = 0; i < n; i++) {
    battle.particles.push({
      x, lane, yJitter,
      vx: (Math.random() - 0.5) * 120,
      vy: -Math.random() * 90 - 20,
      life: 0.5 + Math.random() * 0.3,
      color
    });
  }
}

function updateParticles(dt) {
  for (const p of battle.particles) {
    p.life -= dt;
    p.x += p.vx * dt;
    p.vy += 260 * dt;
    p.yJitter += p.vy * dt;
  }
  battle.particles = battle.particles.filter(p => p.life > 0);
}

/* ---------- Bucle principal ---------- */
function loop(now) {
  if (!battle) return;
  const dt = Math.min(0.05, (now - lastTime) / 1000);
  lastTime = now;
  battle.time += dt;

  if (!battle.over) {
    battle.mana = Math.min(battle.manaMax, battle.mana + battle.manaRegen * dt);
    updateAI(dt);
    updateUnits(dt);
    updateArrows(dt);
    updateParticles(dt);
    updateCards();
    updateHpBars();
    battle.shake = Math.max(0, battle.shake - dt * 20);

    if (battle.enemyHp <= 0) finishBattle(true);
    else if (battle.playerHp <= 0) finishBattle(false);
  } else {
    updateParticles(dt);
  }

  render();
  rafId = requestAnimationFrame(loop);
}

function updateHpBars() {
  document.getElementById('hp-player').style.width =
    Math.max(0, battle.playerHp / battle.playerMaxHp * 100) + '%';
  document.getElementById('hp-enemy').style.width =
    Math.max(0, battle.enemyHp / battle.enemyMaxHp * 100) + '%';
}

function finishBattle(won) {
  battle.over = true;
  const overlay = document.getElementById('overlay-result');
  const title = document.getElementById('result-title');
  const text = document.getElementById('result-text');

  if (won) {
    const base = 100 + 65 * battle.enemyLevel;
    const reward = battle.firstTime ? base : Math.round(base * 0.4);
    state.gold += reward;
    if (battle.firstTime) state.conquered++;
    saveState();
    title.textContent = '🏆 ¡VICTORIA!';
    title.className = 'win';
    text.textContent = `Has conquistado ${TERRITORIES[battle.index].name}. Recompensa: 🪙 ${reward}` +
      (state.conquered === TERRITORIES.length ? ' · ¡Has conquistado TODO el reino! 👑' : '');
  } else {
    title.textContent = '💀 DERROTA';
    title.className = 'lose';
    text.textContent = 'Tu castillo ha caído. Mejora tu ejército e inténtalo de nuevo.';
  }
  document.getElementById('btn-result-retry').style.display = won ? 'none' : '';
  overlay.classList.add('active');
}

document.getElementById('btn-result-continue').addEventListener('click', () => {
  document.getElementById('overlay-result').classList.remove('active');
  endBattle();
  show('map');
});
document.getElementById('btn-result-retry').addEventListener('click', () => {
  const idx = battle.index;
  document.getElementById('overlay-result').classList.remove('active');
  endBattle();
  startBattle(idx);
});
document.getElementById('btn-flee').addEventListener('click', () => {
  endBattle();
  show('map');
});

/* ---------- Renderizado ---------- */
function resizeCanvas() {
  const dpr = Math.min(2, window.devicePixelRatio || 1);
  canvas.width = canvas.clientWidth * dpr;
  canvas.height = canvas.clientHeight * dpr;
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
}
window.addEventListener('resize', () => {
  if (currentScreen === 'battle') resizeCanvas();
});

function wx(x, W) { return x / WORLD_W * W; } // mundo → pantalla

function render() {
  const W = canvas.clientWidth, H = canvas.clientHeight;
  ctx.clearRect(0, 0, W, H);
  ctx.save();
  if (battle.shake > 0) {
    ctx.translate((Math.random() - 0.5) * battle.shake, (Math.random() - 0.5) * battle.shake);
  }

  drawBackground(W, H);
  drawCastle(W, H, 1);
  drawCastle(W, H, -1);

  // unidades ordenadas por carril para solaparse bien
  const sorted = [...battle.units].sort((a, b) => a.lane - b.lane || a.yJitter - b.yJitter);
  for (const u of sorted) drawUnit(u, W, H);

  for (const a of battle.arrows) drawArrow(a, W, H);
  for (const p of battle.particles) {
    ctx.globalAlpha = Math.max(0, p.life * 2);
    ctx.fillStyle = p.color;
    ctx.fillRect(wx(p.x, W) - 2, laneY(p.lane, H) + p.yJitter - 2, 4, 4);
    ctx.globalAlpha = 1;
  }
  ctx.restore();
}

function drawBackground(W, H) {
  // cielo
  const sky = ctx.createLinearGradient(0, 0, 0, H * 0.4);
  sky.addColorStop(0, '#17224a');
  sky.addColorStop(1, '#2c3a68');
  ctx.fillStyle = sky;
  ctx.fillRect(0, 0, W, H * 0.4);

  // montañas lejanas
  ctx.fillStyle = '#1d2748';
  ctx.beginPath();
  ctx.moveTo(0, H * 0.4);
  for (let i = 0; i <= 8; i++) {
    ctx.lineTo(W * i / 8, H * (0.4 - 0.06 - 0.05 * Math.sin(i * 2.7)));
    ctx.lineTo(W * (i + 0.5) / 8, H * 0.4);
  }
  ctx.closePath();
  ctx.fill();

  // terreno
  const ground = ctx.createLinearGradient(0, H * 0.4, 0, H);
  ground.addColorStop(0, '#2e3d33');
  ground.addColorStop(1, '#1a231d');
  ctx.fillStyle = ground;
  ctx.fillRect(0, H * 0.4, W, H * 0.6);

  // franjas de carril
  for (let l = 0; l < LANES; l++) {
    const y = laneY(l, H);
    ctx.fillStyle = l % 2 ? 'rgba(255,255,255,.03)' : 'rgba(0,0,0,.08)';
    ctx.fillRect(0, y - H * 0.07, W, H * 0.14);
  }
}

function drawCastle(W, H, side) {
  const x = wx(side === 1 ? CASTLE_X_PLAYER : CASTLE_X_ENEMY, W);
  const baseY = H * 0.78;
  const h = H * 0.42;
  const w = Math.max(46, W * 0.055);
  const color = side === 1 ? '#5a6d94' : '#8a5560';
  const dark = side === 1 ? '#414f6e' : '#653d46';
  const flag = side === 1 ? '#62d98a' : '#ff7a6e';

  ctx.fillStyle = dark;
  ctx.fillRect(x - w / 2 - 6, baseY - h * 0.55, w + 12, h * 0.55);
  ctx.fillStyle = color;
  ctx.fillRect(x - w / 2, baseY - h, w, h);
  // almenas
  ctx.fillStyle = color;
  const merlonW = w / 5;
  for (let i = 0; i < 3; i++) {
    ctx.fillRect(x - w / 2 + i * 2 * merlonW, baseY - h - merlonW, merlonW, merlonW);
  }
  // puerta
  ctx.fillStyle = '#1a1410';
  ctx.beginPath();
  ctx.ellipse(x, baseY, w * 0.22, h * 0.18, 0, Math.PI, 0);
  ctx.fill();
  ctx.fillRect(x - w * 0.22, baseY - h * 0.001, w * 0.44, 2);
  // bandera
  ctx.strokeStyle = '#ddd';
  ctx.beginPath();
  ctx.moveTo(x, baseY - h - merlonW);
  ctx.lineTo(x, baseY - h - merlonW - 22);
  ctx.stroke();
  ctx.fillStyle = flag;
  ctx.beginPath();
  ctx.moveTo(x, baseY - h - merlonW - 22);
  ctx.lineTo(x + 18 * side, baseY - h - merlonW - 17);
  ctx.lineTo(x, baseY - h - merlonW - 12);
  ctx.closePath();
  ctx.fill();
}

function drawUnit(u, W, H) {
  const x = wx(u.x, W);
  const y = laneY(u.lane, H) + u.yJitter;
  const r = u.radius * Math.min(W / 900, 1.4);
  const bob = Math.sin(u.walk) * r * 0.12;
  const ally = u.side === 1;
  const body = ally ? '#3f6fb5' : '#b54a3f';
  const bodyDark = ally ? '#2c4f85' : '#83352d';
  const skin = '#e8c9a0';

  ctx.save();
  ctx.translate(x, y + bob);

  // sombra
  ctx.fillStyle = 'rgba(0,0,0,.3)';
  ctx.beginPath();
  ctx.ellipse(0, r * 1.15, r * 0.9, r * 0.3, 0, 0, Math.PI * 2);
  ctx.fill();

  if (u.type === 'titan') {
    // cuerpo colosal de piedra
    ctx.fillStyle = ally ? '#6b7fa8' : '#a86b6b';
    ctx.beginPath();
    ctx.roundRect(-r * 0.8, -r * 1.4, r * 1.6, r * 2.2, r * 0.4);
    ctx.fill();
    ctx.fillStyle = ally ? '#54658a' : '#8a5454';
    ctx.beginPath();
    ctx.arc(0, -r * 1.55, r * 0.55, 0, Math.PI * 2); // cabeza
    ctx.fill();
    // ojos brillantes
    ctx.fillStyle = ally ? '#9cf' : '#fc9';
    ctx.fillRect(-r * 0.3, -r * 1.65, r * 0.2, r * 0.14);
    ctx.fillRect(r * 0.1, -r * 1.65, r * 0.2, r * 0.14);
    // brazos
    ctx.fillStyle = ally ? '#54658a' : '#8a5454';
    ctx.fillRect(-r * 1.15, -r * 1.1, r * 0.38, r * 1.6);
    ctx.fillRect(r * 0.77, -r * 1.1, r * 0.38, r * 1.6);
  } else {
    // piernas
    ctx.fillStyle = bodyDark;
    ctx.fillRect(-r * 0.45, r * 0.2, r * 0.35, r * 0.9);
    ctx.fillRect(r * 0.1, r * 0.2, r * 0.35, r * 0.9);
    // cuerpo
    ctx.fillStyle = body;
    ctx.beginPath();
    ctx.roundRect(-r * 0.6, -r * 0.7, r * 1.2, r * 1.1, r * 0.3);
    ctx.fill();
    // cabeza
    ctx.fillStyle = skin;
    ctx.beginPath();
    ctx.arc(0, -r * 1.05, r * 0.42, 0, Math.PI * 2);
    ctx.fill();
    // casco
    ctx.fillStyle = bodyDark;
    ctx.beginPath();
    ctx.arc(0, -r * 1.12, r * 0.44, Math.PI, 0);
    ctx.fill();

    if (u.type === 'sword') {
      ctx.strokeStyle = '#dfe6f2';
      ctx.lineWidth = Math.max(2, r * 0.16);
      ctx.beginPath();
      ctx.moveTo(u.side * r * 0.6, -r * 0.2);
      ctx.lineTo(u.side * r * 1.25, -r * 0.9);
      ctx.stroke();
      // escudo
      ctx.fillStyle = bodyDark;
      ctx.beginPath();
      ctx.arc(-u.side * r * 0.65, -r * 0.15, r * 0.4, 0, Math.PI * 2);
      ctx.fill();
    } else if (u.type === 'archer') {
      ctx.strokeStyle = '#c9a86a';
      ctx.lineWidth = Math.max(2, r * 0.14);
      ctx.beginPath();
      ctx.arc(u.side * r * 0.7, -r * 0.25, r * 0.55, -Math.PI / 2.2, Math.PI / 2.2);
      ctx.stroke();
    } else if (u.type === 'cavalry') {
      // caballo debajo
      ctx.fillStyle = ally ? '#7a5c3d' : '#5c4a3d';
      ctx.beginPath();
      ctx.roundRect(-r * 1.05, r * 0.15, r * 2.1, r * 0.75, r * 0.3);
      ctx.fill();
      ctx.beginPath();
      ctx.arc(u.side * r * 1.0, r * 0.15, r * 0.32, 0, Math.PI * 2);
      ctx.fill();
      // lanza
      ctx.strokeStyle = '#dfe6f2';
      ctx.lineWidth = Math.max(2, r * 0.13);
      ctx.beginPath();
      ctx.moveTo(u.side * r * 0.3, -r * 0.3);
      ctx.lineTo(u.side * r * 1.5, -r * 0.55);
      ctx.stroke();
    }
  }

  // barra de vida
  const bw = r * 2, bh = Math.max(3, r * 0.22);
  const topY = u.type === 'titan' ? -r * 2.3 : -r * 1.75;
  ctx.fillStyle = 'rgba(0,0,0,.6)';
  ctx.fillRect(-bw / 2, topY, bw, bh);
  ctx.fillStyle = ally ? '#62d98a' : '#ff7a6e';
  ctx.fillRect(-bw / 2, topY, bw * Math.max(0, u.hp / u.maxHp), bh);

  ctx.restore();
}

function drawArrow(a, W, H) {
  const x = wx(a.x, W);
  const y = laneY(a.lane, H) + a.yJitter - 14;
  const dir = Math.sign(a.tx - a.x) || a.side;
  ctx.strokeStyle = '#e8d9b0';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(x - 8 * dir, y);
  ctx.lineTo(x + 8 * dir, y);
  ctx.stroke();
  ctx.fillStyle = '#e8d9b0';
  ctx.beginPath();
  ctx.moveTo(x + 8 * dir, y);
  ctx.lineTo(x + 3 * dir, y - 3);
  ctx.lineTo(x + 3 * dir, y + 3);
  ctx.closePath();
  ctx.fill();
}

/* ---------------- Arranque ---------------- */
show('menu');
