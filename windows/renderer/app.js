'use strict';
const $ = (s, r = document) => r.querySelector(s);
const esc = s => String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const n2 = n => String(n).padStart(2, '0');

const ACC = ['#FF5500', '#4A4DFF', '#C8F23C', '#FF4FA3', '#8E6BFF', '#FFC928'];
const QUICK = [['Google', 'https://www.google.com'], ['YouTube', 'https://www.youtube.com'], ['Wikipedia', 'https://en.wikipedia.org'],
  ['GitHub', 'https://github.com'], ['Brave', 'https://search.brave.com'], ['Reddit', 'https://www.reddit.com']];
const QF = ['lime', 'blue', 'ink', 'white', 'orange', 'paper'];
const ENGINES = [['DuckDuckGo', 'https://duckduckgo.com/?q='], ['Google', 'https://www.google.com/search?q='],
  ['Brave', 'https://search.brave.com/search?q='], ['Bing', 'https://www.bing.com/search?q=']];
// name, id, default model, web url, key page
const AIS = [
  ['ChatGPT', 'openai', 'gpt-4o-mini', 'https://chatgpt.com/?q=', 'https://platform.openai.com/api-keys'],
  ['Claude', 'anthropic', 'claude-sonnet-5-5', 'https://claude.ai/new?q=', 'https://console.anthropic.com/settings/keys'],
  ['Gemini', 'gemini', 'gemini-2.5-flash', 'https://gemini.google.com/app', 'https://aistudio.google.com/apikey'],
  ['Grok', 'xai', 'grok-3-mini', 'https://grok.com/?q=', 'https://console.x.ai'],
  ['Perplexity', 'pplx', 'sonar', 'https://www.perplexity.ai/search?q=', 'https://www.perplexity.ai/settings/api']];
const ARROW = '<svg class="arrow" viewBox="0 0 24 24"><path d="M8,3 L21,3 L21,16 L17,12 L5,24 L0,19 L12,7 Z"/></svg>';

let S = {}, paths = {}, tabs = [], cur = -1, blockedCount = 0, aiLog = [], aiBusy = false, aiUsePage = false, sheetMode = '';
let noteCur = null, appVersion = '0';

const save = k => storm.set(k, S[k]);
const T = () => tabs[cur];

// ---------- toast & modal ----------
let toastT;
function toast(msg) { const t = $('#toast'); t.textContent = msg; t.classList.add('on'); clearTimeout(toastT); toastT = setTimeout(() => t.classList.remove('on'), 2400); }
function modal(title, bodyHtml, buttons) {
  return new Promise(res => {
    const m = $('#modal');
    m.innerHTML = `<div class="mbox"><h3 class="D">${esc(title)}</h3><div class="mb">${bodyHtml}</div><div class="mbtn">${
      buttons.map((b, i) => `<button class="tag" data-mbtn="${i}">${esc(b)}</button>`).join('')}</div></div>`;
    m.classList.add('on');
    const done = i => { const get = s => { const e = $(s, m); return e ? e.value : ''; }; const out = { i, get }; m.classList.remove('on'); m.onclick = null; setTimeout(() => { m.innerHTML = ''; }, 50); res(out); };
    m.onclick = e => { const b = e.target.closest('[data-mbtn]'); if (b) done(Number(b.dataset.mbtn)); else if (e.target === m) done(-1); };
    const f = $('input,textarea', m); if (f) f.focus();
  });
}

// ---------- helpers ----------
function hostOf(u) { try { return new URL(u).hostname.replace(/^www\./, ''); } catch (e) { return u; } }
function fix(s) {
  s = s.trim();
  if (/^[a-z][a-z0-9+.-]*:\/\//i.test(s) || /^(about|file|data):/i.test(s)) return s;
  if (/^localhost(:\d+)?(\/|$)/i.test(s)) return 'http://' + s;
  if (s.includes('.') && !/\s/.test(s)) return 'https://' + s;
  return ENGINES[S.settings.se][1] + encodeURIComponent(s);
}
function applyTheme() {
  document.body.classList.toggle('oled', !!S.settings.oled);
  document.documentElement.style.setProperty('--acc', ACC[S.settings.acc] || ACC[0]);
  storm.theme(!!S.settings.oled);
}
const setting = (k, v) => { S.settings[k] = v; save('settings'); };

// ---------- tabs ----------
function newTab(url, priv) {
  const t = { priv: !!priv, url: '', title: 'New tab', wv: null, ready: false };
  tabs.push(t); cur = tabs.length - 1;
  if (t.priv) ach('private'); achSet('tabs3', tabs.length);
  if (url) { t.url = fix(url); ensureWv(t, t.url); }
  showCurrent();
  if (!url) { $('#addr').focus(); }
  return t;
}
function ensureWv(t, url) {
  if (t.wv) return t.wv;
  const wv = document.createElement('webview');
  wv.setAttribute('partition', t.priv ? 'storm-private' : 'persist:storm');
  wv.setAttribute('preload', paths.webPreload);
  wv.setAttribute('allowpopups', '');
  wv.setAttribute('webpreferences', 'backgroundThrottling=no');
  const upd = () => { if (T() === t) { updateNav(); updateAddress(); } };
  wv.addEventListener('dom-ready', () => { t.ready = true; upd(); });
  wv.addEventListener('did-start-loading', () => { t.loading = true; upd(); });
  wv.addEventListener('did-stop-loading', () => {
    t.loading = false;
    try { t.url = wv.getURL() || t.url; t.title = wv.getTitle() || t.title; } catch (e) { }
    if (!t.priv && t.url && !/^(about|data):/.test(t.url)) { addHistory(t.title, t.url); ach('site'); ach('sites5'); ach('sites25'); }
    renderTabs(); upd(); saveTabs();
  });
  wv.addEventListener('did-navigate', e => { t.url = e.url; upd(); saveTabs(); });
  wv.addEventListener('did-navigate-in-page', e => { if (e.isMainFrame) { t.url = e.url; upd(); } });
  wv.addEventListener('page-title-updated', e => { t.title = e.title; renderTabs(); });
  wv.addEventListener('did-fail-load', e => { if (e.isMainFrame && e.errorCode !== -3 && e.errorCode !== 0) toast('Couldn\'t load ' + hostOf(e.validatedURL || t.url)); });
  wv.setAttribute('src', url);
  $('#views').appendChild(wv);
  t.wv = wv; return wv;
}
function select(i) {
  if (i < 0 || i >= tabs.length) return;
  cur = i; const t = tabs[i];
  if (t.url && !t.wv) ensureWv(t, t.url);
  showCurrent();
}
function closeTab(i) {
  const t = tabs[i]; if (!t) return;
  if (t.wv) t.wv.remove();
  tabs.splice(i, 1);
  if (!tabs.length) { newTab(null, false); return; }
  if (i < cur) cur--; if (cur >= tabs.length) cur = tabs.length - 1;
  if (tabs[cur].min) { const n = visibleIdx(cur); if (n >= 0) cur = n; else { newTab(null, false); renderBubbles(); return; } }
  showCurrent(); saveTabs(); renderBubbles();
}
function load(t, url) {
  t.url = url;
  if (!t.wv) ensureWv(t, url);
  else if (t.ready) t.wv.loadURL(url).catch(() => { });
  else t.wv.setAttribute('src', url);
  showCurrent();
}
function go(text) {
  const t = T(); if (!t || !text.trim()) return;
  load(t, fix(text)); $('#addr').blur();
}
function goBack() { const t = T(); if (t.wv && t.ready && t.wv.canGoBack()) t.wv.goBack(); else { t.url = ''; showCurrent(); } }
function goFwd() { const t = T(); if (t.wv && t.ready && t.wv.canGoForward()) t.wv.goForward(); }
function saveTabs() {
  S.tabsSaved = tabs.filter(t => !t.priv && t.url).map(t => ({ url: t.url, title: t.title, min: !!t.min }));
  S.tabCur = Math.max(0, tabs.filter(t => !t.priv && t.url).indexOf(T()));
  save('tabsSaved'); save('tabCur');
}
function renderTabs() {
  $('#tabs').innerHTML = tabs.map((t, i) => ({ t, i })).filter(o => !o.t.min).map((o, k) => {
    const t = o.t, i = o.i;
    return `<div class="tab ${i === cur ? 'on' : ''} ${t.priv ? 'priv' : ''}" data-act="tabsel" data-i="${i}">
    <span class="L num">${n2(k + 1)}</span><span class="ttl">${esc(t.priv ? 'Private · ' : '')}${esc(t.url ? (t.title || hostOf(t.url)) : 'New tab')}</span>
    ${t.url ? `<button class="x mn" data-act="tabmin" data-i="${i}" title="Minimize (Ctrl+M)">–</button>` : ''}<button class="x" data-act="tabclose" data-i="${i}">✕</button></div>`;
  }).join('') +
    '<button class="tabbtn" data-act="newtab" title="New tab (Ctrl+T)">+</button><button class="tabbtn" data-act="newprivate" title="Private tab (Ctrl+Shift+N)">P</button>';
}
// ---------- minimize: floating tab bubbles ----------
function visibleIdx(from) {
  for (let d = 1; d <= tabs.length; d++) for (const j of [from - d, from + d]) if (j >= 0 && j < tabs.length && !tabs[j].min) return j;
  return -1;
}
function minimizeTab(t) {
  t = t || T();
  if (!t || !t.url) { toast('Open a page first, then minimize it'); return; }
  const i = tabs.indexOf(t); t.min = true; t.bx = null; ach('minimize');
  if (T() === t) { const n = visibleIdx(i); if (n >= 0) select(n); else newTab(null, false); }
  renderTabs(); renderBubbles(); saveTabs();
  toast('Tab minimized — click its bubble to bring it back');
}
async function restoreTab(t) {
  const c = T();
  const show = () => { t.min = false; select(tabs.indexOf(t)); renderBubbles(); saveTabs(); };
  if (!c || c === t || !c.url || c.min) { show(); return; }
  const mode = S.settings.minMode | 0;
  if (mode === 2) { show(); return; }
  const both = () => { c.min = true; c.bx = null; t.min = false; cur = tabs.indexOf(t); showCurrent(); renderBubbles(); saveTabs(); };
  if (mode === 1) { both(); return; }
  const name = c.title || hostOf(c.url);
  const r = await modal('Float your current tab?', 'Minimize “' + esc(name.slice(0, 40)) + '” into a floating bubble so you can jump back to it?', ['Float it', 'Just switch', 'Cancel']);
  if (r.i === 0) both(); else if (r.i === 1) show();
}
function renderBubbles() {
  const box = $('#bubbles'); box.innerHTML = ''; let n = 0;
  tabs.forEach(t => {
    if (!t.min) return;
    if (t.bx == null) { t.bx = innerWidth - 64 - 18; t.by = 140 + n * 76; }
    n++;
    const b = document.createElement('div'); b.className = 'bubble' + (t.priv ? ' priv' : '');
    const host = hostOf(t.url) || '?';
    b.innerHTML = `<div class="D l">${esc(host[0] || '?')}</div><div class="L h">${esc(host.slice(0, 10))}</div><button class="bx" title="Close tab">✕</button>`;
    b.title = (t.title || host) + ' — click to restore';
    b.style.left = t.bx + 'px'; b.style.top = t.by + 'px';
    attachDrag(b, t); box.appendChild(b);
  });
}
function attachDrag(b, t) {
  let sx = 0, sy = 0, ox = 0, oy = 0, moved = false;
  b.addEventListener('pointerdown', e => {
    if (e.target.classList.contains('bx')) return;
    b.setPointerCapture(e.pointerId); sx = e.clientX; sy = e.clientY; ox = t.bx; oy = t.by; moved = false; b.classList.add('drag');
  });
  b.addEventListener('pointermove', e => {
    if (!b.hasPointerCapture(e.pointerId)) return;
    const dx = e.clientX - sx, dy = e.clientY - sy;
    if (!moved && Math.hypot(dx, dy) > 5) moved = true;
    if (moved) {
      t.bx = Math.max(0, Math.min(innerWidth - 64, ox + dx)); t.by = Math.max(0, Math.min(innerHeight - 64, oy + dy));
      b.style.left = t.bx + 'px'; b.style.top = t.by + 'px';
    }
  });
  b.addEventListener('pointerup', e => {
    if (e.target.classList.contains('bx')) return;
    b.classList.remove('drag');
    if (!moved) restoreTab(t);
    else { t.bx = t.bx + 32 < innerWidth / 2 ? 14 : innerWidth - 64 - 14; b.style.left = t.bx + 'px'; }
  });
  b.querySelector('.bx').addEventListener('click', e => { e.stopPropagation(); closeTab(tabs.indexOf(t)); });
}
function stepTab(dir) {
  const vis = tabs.map((t, i) => i).filter(i => !tabs[i].min); if (!vis.length) return;
  const k = vis.indexOf(cur); select(vis[(k + dir + vis.length) % vis.length]);
}
const wallList = () => S.walls || [];
function wallFor(t) {
  const l = wallList(); if (!l.length || !t || t.priv) return null;
  if (S.settings.wallShuffle && l.length > 1) { if (t.wi == null) t.wi = Math.floor(Math.random() * l.length); return l[t.wi % l.length]; }
  return S.wallSel >= 0 && S.wallSel < l.length ? l[S.wallSel] : null;
}
function setWallVars() {
  const s = S.settings;
  $('#wall').style.filter = 'blur(' + (s.wallBlur || 0) + 'px)';
  $('#shade').style.background = 'rgba(0,0,0,' + ((s.wallDim == null ? 35 : s.wallDim) / 100) + ')';
}
function applyWall(t, show) {
  const u = show ? wallFor(t) : null, w = $('#wall'), sh = $('#shade'), h = $('#home');
  if (u) { w.style.display = 'block'; w.style.backgroundImage = 'url("' + u + '")'; sh.style.display = 'block'; h.classList.add('has-wall'); setWallVars(); }
  else { w.style.display = 'none'; sh.style.display = 'none'; h.classList.remove('has-wall'); }
}
function showCurrent() {
  const t = T();
  document.querySelectorAll('webview').forEach(w => w.classList.remove('on'));
  const home = $('#home');
  if (t && t.url && t.wv) { t.wv.classList.add('on'); home.classList.remove('on'); applyWall(t, false); }
  else if (t) { home.innerHTML = homeHTML(t.priv); home.classList.add('on'); home.classList.toggle('priv', t.priv && !S.settings.oled); applyWall(t, true); }
  $('#toolbar').classList.toggle('priv', !!(t && t.priv));
  updateNav(); updateAddress(); renderTabs();
}
function updateNav() {
  const t = T(); let b = false, f = false;
  try { b = !!(t.wv && t.ready && t.wv.canGoBack()); f = !!(t.wv && t.ready && t.wv.canGoForward()); } catch (e) { }
  $('#bBack').disabled = !(b || (t && t.url)); $('#bFwd').disabled = !f; $('#bHome').disabled = !(t && t.url);
  $('#bReload').textContent = t && t.loading ? '✕' : '↻';
}
function updateAddress() {
  const t = T(); const a = $('#addr');
  if (document.activeElement !== a) a.value = t && t.url ? t.url : '';
  $('#addrLabel').textContent = (t && t.priv ? 'Private · ' : '') + (t && t.url ? (t.url.startsWith('https') ? 'Secure · ' + hostOf(t.url) : 'Address · ' + hostOf(t.url)) : 'Address');
}
function addHistory(title, url) {
  S.history = S.history.filter(h => h.u !== url);
  S.history.unshift({ t: title || url, u: url, ts: Date.now() });
  if (S.history.length > 400) S.history.length = 400;
  save('history');
}

// ---------- home ----------
function homeHTML(priv) {
  const acc = ACC[S.settings.acc] || ACC[0], ads = S.settings.ads, proxy = !!S.settings.proxy;
  const cards = QUICK.map((q, i) => {
    const pic = S.pics && S.pics[i];
    return `<div class="card ${QF[i]} ${pic ? 'pic' : ''}" data-act="shortcut" data-i="${i}" ${pic ? `style="--pic:url('${pic}')"` : ''}>
      <span class="num L">${n2(i + 2)}</span><button class="mini" data-act="pic" data-i="${i}" title="Change picture (right-click to remove)">▣</button>
      <div class="D name">${esc(q[0])}</div></div>`;
  }).join('');
  const hero = `<div class="hero ${S.bgimg && !priv ? 'photo' : ''}" data-act="hero" ${S.bgimg && !priv ? `style="--bgimg:url('${S.bgimg}')"` : ''}>
    <div class="D n">01</div>${ARROW}<div class="t"><div class="D">Search</div><div class="L" style="margin-top:6px">Type an address or a question</div></div></div>`;
  return `<div class="hwrap">
    <div class="hhead"><span class="D brand wide">Storm</span><span class="tag" data-act="menu">Menu</span></div>
    <h1 class="D big">${priv ? 'Private<br>Session' : 'Quick<br>Access'}</h1>
    <div class="grid2">
      <div class="banner ${ads ? '' : 'off'}" data-act="toggleads"><div class="bl"><div class="L">Ad block · <span id="blk">${blockedCount}</span> blocked</div><div class="D b34">${ads ? 'Protected' : 'Exposed'}</div></div><div class="onbox D">${ads ? 'On' : 'Off'}</div></div>
      <div class="banner ${proxy ? 'on2' : 'off'}" data-act="proxy"><div class="bl"><div class="L">Proxy · ${proxy ? esc(S.settings.proxy.slice(0, 28)) : 'tap to set up'}</div><div class="D b34">${proxy ? 'Connected' : 'Proxy off'}</div></div><div class="onbox D">${proxy ? 'On' : 'Off'}</div></div>
    </div>
    ${hero}
    <div class="sec L"><span>Shortcuts</span><span>${n2(QUICK.length)}</span></div>
    <div class="cards">${cards}</div>
    <div class="info">
      <div data-act="menu-tabs"><div class="L">Tabs</div><div class="D">${n2(tabs.length)}</div></div>
      <div data-act="bookmarks"><div class="L">Saved</div><div class="D">${n2(S.bookmarks.length)}</div></div>
      <div data-act="history"><div class="L">History</div><div class="D">${n2(S.history.length)}</div></div>
      <div data-act="notes"><div class="L">Notes</div><div class="D">${n2(S.notes.length)}</div></div>
    </div>
    <div class="wide2">
      <div class="blockcell" data-act="google"><div class="l"><div class="L">Google account</div><div class="D">Sign in</div></div><div class="g D">G</div></div>
      <div class="blockcell" data-act="games"><div class="l"><div class="L">Mini games · offline</div><div class="D">Play now</div></div><div class="g D" style="font-size:22px">Play</div></div>
      <div class="blockcell" data-act="awards"><div class="l"><div class="L">Achievements · easy to earn</div><div class="D">Awards</div></div><div class="g D" style="font-size:22px">${achDoneCount()}/${ACH.length}</div></div>
      <div class="blockcell" data-act="ai"><div class="l"><div class="L">AI assistant · ${esc(AIS[S.aiSel || 0][0])}</div><div class="D">Ask anything</div></div><div class="g D" style="background:${acc};color:#111">AI</div></div>
    </div>
    <div class="L foot">No trackers · No clutter · ${new Date().getFullYear()}</div>
  </div>`;
}
function refreshHome() { const t = T(); if (t && !t.url) showCurrent(); }

// ---------- sheets ----------
function openSheet(mode, html, wide, flex) {
  const s = $('#sheet'); sheetMode = mode;
  s.className = 'sheet on' + (wide ? ' wide' : '') + (flex ? ' flex' : '');
  s.innerHTML = `<div class="shead"><span class="D brand wide">Storm</span><button class="tag" data-act="closesheet">Done</button></div>${html}`;
  s.scrollTop = 0;
}
function closeSheet() { $('#sheet').className = 'sheet'; sheetMode = ''; }
const row = (n, name, state, act, extra = '') => `<div class="row" data-act="${act}" ${extra}><span class="rn L">${n2(n)}</span><span class="rt D">${esc(name)}</span>${
  state ? `<span class="rs L ${state === 'On' ? 'on' : ''}">${esc(state)}</span>` : ''}</div>`;

function renderMenu() {
  const t = T();
  openSheet('menu', `<h2 class="D stitle">Menu</h2><div class="panel">
    ${row(1, 'New tab', '', 'newtab')}${row(2, 'Private tab', '', 'newprivate')}${row(3, 'Notes', '', 'notes')}
    ${row(4, 'AI assistant', '', 'ai')}${row(5, 'Minimize tab', 'Ctrl+M', 'minimize')}${row(6, 'Games', 'Offline', 'games')}${row(7, 'Awards', achDoneCount() + '/' + ACH.length, 'awards')}${row(8, 'Bookmarks', '', 'bookmarks')}${row(9, 'History', '', 'history')}
    ${row(10, 'Add bookmark', '', 'bookmark')}${row(11, 'Find in page', '', 'find')}
    ${row(12, 'Full screen', 'F11', 'fullscreen')}${row(13, 'Ad block', S.settings.ads ? 'On' : 'Off', 'toggleads2')}
    ${row(14, 'Settings', '', 'settings')}${row(15, 'Check for updates', 'v' + appVersion, 'update')}${row(16, 'Clear data', '', 'cleardata')}
  </div>`);
}
function renderBookmarks() {
  const l = S.bookmarks;
  openSheet('bm', `<h2 class="D stitle">Bookmarks</h2>
    <div class="panel" style="margin-bottom:14px"><div class="row" data-act="bookmark"><span class="rn L">＋</span><span class="rt D">Add this page</span></div></div>
    ${l.length ? `<div class="panel">${l.map((b, i) => `<div class="row" data-act="bm-open" data-i="${i}"><span class="rn L">${n2(i + 1)}</span><div class="mid"><div class="D">${esc(b.t)}</div><div class="L">${esc(hostOf(b.u))}</div></div><button class="rx" data-act="bm-del" data-i="${i}">✕</button></div>`).join('')}</div>` : '<div class="D empty">Nothing<br>saved yet</div>'}`);
}
function renderHistory(q = '') {
  const f = q.toLowerCase(); const d0 = new Date(); d0.setHours(0, 0, 0, 0); const today = d0.getTime(), yest = today - 864e5;
  let last = '', html = '', n = 0;
  S.history.forEach((h, i) => {
    if (f && !(h.t.toLowerCase().includes(f) || h.u.toLowerCase().includes(f))) return;
    const g = h.ts >= today ? 'Today' : h.ts >= yest ? 'Yesterday' : 'Earlier';
    if (g !== last) { html += `<div class="hist-h L">${g}</div>`; last = g; }
    n++;
    html += `<div class="row" data-act="hist-open" data-i="${i}"><span class="rn L">${n2(n)}</span><div class="mid"><div class="D">${esc(h.t)}</div><div class="L">${esc(hostOf(h.u))}</div></div><span class="rn L" style="width:70px">${new Date(h.ts).toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' })}</span></div>`;
  });
  if (sheetMode !== 'hist') {
    openSheet('hist', `<h2 class="D stitle">History</h2><div style="display:flex;gap:8px"><input class="field" id="hq" placeholder="Search history"><button class="tag" data-act="clear-hist">Clear</button></div><div id="hlist"></div>`);
  }
  $('#hlist').innerHTML = html || '<div class="D empty">Nothing<br>here yet</div>';
}
function renderSettings() {
  const s = S.settings;
  openSheet('settings', `<h2 class="D stitle">Settings</h2>
    <div class="lbl L">Theme</div><div class="seg"><button class="${s.oled ? '' : 'on'}" data-act="theme" data-v="0">Light</button><button class="${s.oled ? 'on' : ''}" data-act="theme" data-v="1">OLED dark</button></div>
    <div class="lbl L">Accent colour</div><div class="swatches">${ACC.map((c, i) => `<button class="sw ${i === s.acc ? 'sel' : ''}" style="background:${c}" data-act="accent" data-i="${i}">${i === s.acc ? '✓' : ''}</button>`).join('')}</div>
    <div class="lbl L">Wallpaper (home page)</div>
    <div class="walls">${wallList().map((u, i) => `<div class="wt ${i === S.wallSel && !s.wallShuffle ? 'sel' : ''}" style="background-image:url('${u}')" data-act="wallsel" data-i="${i}"><button class="wx" data-act="walldel" data-i="${i}">✕</button></div>`).join('')}<div class="wt add" data-act="wallpick" title="Add pictures — or drop image files onto the home page">＋</div></div>
    <div class="seg" style="margin-top:8px"><button data-act="wallpick">Add pictures</button><button data-act="wallnone">No wallpaper</button></div>
    <div class="lbl L">Darken · <span id="wdv">${s.wallDim == null ? 35 : s.wallDim}</span>%</div><input type="range" class="range" id="wdim" min="0" max="85" value="${s.wallDim == null ? 35 : s.wallDim}">
    <div class="lbl L">Blur · <span id="wbv">${s.wallBlur || 0}</span>px</div><input type="range" class="range" id="wblur" min="0" max="24" value="${s.wallBlur || 0}">
    <div class="panel" style="margin-top:12px">${row(1, 'Shuffle wallpapers', s.wallShuffle ? 'On' : 'Off', 'wallshuffle')}</div>
    <div class="lbl L">Hero card photo</div><div class="seg"><button data-act="bgpick">Choose photo</button><button data-act="bgreset">Reset</button></div>
    <div class="lbl L">Proxy (http://host:port or socks5://host:port)</div>
    <div style="display:flex;gap:8px"><input class="field" id="proxyin" value="${esc(s.proxy)}" placeholder="Leave empty for direct connection"><button class="tag" data-act="proxysave">Save</button></div>
    <div class="lbl L">Behaviour</div><div class="panel">
      ${row(1, 'Ad block', s.ads ? 'On' : 'Off', 'toggleads2')}
      ${row(2, 'Search engine', ENGINES[s.se][0], 'engine')}
      ${row(3, 'Auto update check', s.autoupd ? 'On' : 'Off', 'autoupd')}
      ${row(4, 'Start in full screen', s.startfull ? 'On' : 'Off', 'startfull')}
      ${row(5, 'Float on restore', ['Ask', 'Always', 'Never'][(s.minMode | 0) % 3], 'minmode')}
      ${row(6, 'AI keys', '', 'aikeys')}
      ${row(7, 'Check for updates', 'v' + appVersion, 'update')}
      ${row(8, 'Replay intro', '', 'intro')}
    </div>
    <div class="help">Storm identifies as Chrome so sites (including Google sign-in) work normally. The proxy applies to all tabs.</div>`);
}

// ---------- notes (left drawer) ----------
const NC = 6;
function openNotes() { renderNotesList(); $('#notes').classList.add('on'); }
function closeNotes() { saveNote(); $('#notes').classList.remove('on'); noteCur = null; }
function renderNotesList() {
  noteCur = null;
  const l = [...S.notes].sort((a, b) => (b.pin | 0) - (a.pin | 0) || b.ts - a.ts);
  $('#notes').innerHTML = `<div class="shead"><span class="D brand wide">Storm</span><button class="tag" data-act="closenotes">Close</button></div>
    <div style="display:flex;align-items:flex-end;justify-content:space-between"><h2 class="D stitle" style="margin:10px 0 0">Notes</h2><div class="D stitle" style="margin:10px 0 0;color:var(--acc)">${n2(l.length)}</div></div>
    <div class="seg" style="margin-top:12px"><button data-act="note-new">+ New note</button><button data-act="note-page">+ This page</button></div>
    <div class="nlist">${l.length ? l.map(n => `<div class="note c${n.color % NC}" data-act="note-open" data-id="${n.id}"><div class="L" style="display:flex;justify-content:space-between"><span>${new Date(n.ts).toLocaleString([], { month: 'short', day: 'numeric', hour: 'numeric', minute: '2-digit' })}</span><span>${n.pin ? '★ Pinned' : ''}</span></div><div class="D nt">${esc(n.title || 'Untitled')}</div>${n.body ? `<div class="nb">${esc(n.body)}</div>` : ''}</div>`).join('') : '<div class="D empty">No notes<br>yet</div><div class="help">Press Ctrl+J any time to open your notes</div>'}</div>`;
}
function openEditor(n) {
  const isNew = !n;
  noteCur = n || { id: String(Date.now()), title: '', body: '', color: 0, pin: 0, ts: Date.now() };
  const c = noteCur.color % NC;
  $('#notes').innerHTML = `<div class="neditor c${c}">
    <div class="shead"><span class="L">${new Date(noteCur.ts).toLocaleString()}${noteCur.pin ? ' · ★ pinned' : ''}</span><button class="tag" data-act="note-done">Done</button></div>
    <textarea class="ntitle" id="ntitle" rows="2" placeholder="TITLE">${esc(noteCur.title)}</textarea>
    <textarea class="nbody" id="nbody" placeholder="Write something…">${esc(noteCur.body)}</textarea>
    <div class="nbar"><button data-act="note-pin">${noteCur.pin ? 'Unpin' : 'Pin'}</button><button data-act="note-color">Colour</button><button data-act="note-copy">Copy</button><button data-act="note-del">Delete</button></div></div>`;
  let tm; const sch = () => { clearTimeout(tm); tm = setTimeout(saveNote, 500); };
  $('#ntitle').oninput = sch; $('#nbody').oninput = sch;
  if (isNew) $('#ntitle').focus();
}
function saveNote() {
  if (!noteCur || !$('#ntitle')) return;
  const t = $('#ntitle').value.trim(), b = $('#nbody').value;
  if (t !== noteCur.title || b !== noteCur.body) noteCur.ts = Date.now();
  noteCur.title = t; noteCur.body = b;
  const at = S.notes.findIndex(x => x.id === noteCur.id);
  if (!t && !b.trim()) { if (at >= 0) S.notes.splice(at, 1); }
  else if (at >= 0) S.notes[at] = noteCur; else S.notes.unshift(noteCur);
  save('notes');
  if (t || b.trim()) ach('note');
}

// ---------- AI ----------
const aiSel = () => Math.min(AIS.length - 1, S.aiSel || 0);
async function renderAI() {
  ach('ai');
  const pi = aiSel(); const keys = await Promise.all(AIS.map(a => storm.aiHasKey(a[1])));
  openSheet('ai', `<h2 class="D stitle" style="font-size:56px">Ask AI</h2>
    <div class="chips">${AIS.map((a, i) => `<button class="${i === pi ? 'on' : ''}" data-act="aisel" data-i="${i}">${esc(a[0])}${keys[i] ? '' : ' · no key'}</button>`).join('')}</div>
    <div class="chips">${['Summarize page', 'Key points', 'Explain selection', 'Translate page', 'Ask on web', 'Clear chat', 'API keys'].map((q, i) => `<button data-act="aiq" data-i="${i}">${q}</button>`).join('')}</div>
    <div id="aitx"></div>
    <div class="aiin"><button class="${aiUsePage ? 'on' : ''}" data-act="aipage" title="Send the current page along with your question">Page</button><textarea id="aiin" placeholder="Ask ${esc(AIS[pi][0])}…"></textarea><button class="send" data-act="aisend">Send</button></div>`, true, true);
  renderAILog();
  const ta = $('#aiin'); ta.focus();
  ta.onkeydown = e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); aiSubmit(); } };
}
function renderAILog() {
  const tx = $('#aitx'); if (!tx) return;
  const pi = aiSel();
  tx.innerHTML = (!aiLog.length && !aiBusy ? '<div class="D empty">Ask<br>anything</div><div class="help">Pick a provider, then type a question or use a quick action. Add your API key under API keys, or use Ask on web.</div>' : '') +
    aiLog.map((m, i) => m.role === 'user' ? `<div class="msg user">${esc(m.shown)}</div>` :
      m.role === 'error' ? `<div class="msg err">${esc(m.shown)}</div>` :
      `<div class="msg ai"><div class="who L">${esc(AIS[pi][0])}</div><div class="txt">${esc(m.shown)}</div><div class="acts"><button data-act="aicopy" data-i="${i}">Copy</button><button data-act="ainote" data-i="${i}">Save note</button></div></div>`).join('') +
    (aiBusy ? '<div class="msg ai"><div class="txt">Thinking…</div></div>' : '');
  tx.scrollTop = tx.scrollHeight;
}
async function aiSend(shown, full) {
  if (aiBusy) { toast('Still thinking…'); return; }
  const pi = aiSel(), id = AIS[pi][1];
  if (!(await storm.aiHasKey(id))) { toast('Add your ' + AIS[pi][0] + ' API key first'); return renderAIKeys(); }
  if (sheetMode !== 'ai') await renderAI();
  const entry = { role: 'user', shown, full }; aiLog.push(entry); aiBusy = true; renderAILog();
  let hist = aiLog.filter(m => m.role === 'user' || m.role === 'assistant').slice(-10).map(m => ({ role: m.role, text: m.full }));
  while (hist.length && hist[0].role !== 'user') hist.shift();
  try {
    const out = await storm.aiAsk(id, (S.aiModels || {})[id] || AIS[pi][2], hist);
    aiLog.push({ role: 'assistant', shown: out, full: out });
  } catch (e) {
    aiLog.splice(aiLog.indexOf(entry), 1);
    aiLog.push({ role: 'error', shown: '⚠ ' + String(e.message || e).replace(/^Error invoking remote method '[^']*': (Error: )?/, '') });
  }
  aiBusy = false; if (sheetMode === 'ai') renderAILog();
}
async function pageInfo() {
  const t = T(); if (!t || !t.url || !t.wv || !t.ready) return null;
  try {
    return JSON.parse(await t.wv.executeJavaScript(`JSON.stringify({s:String(window.getSelection()),t:document.title,u:location.href,b:(document.body?document.body.innerText:'').slice(0,12000)})`));
  } catch (e) { return null; }
}
async function aiPageAction(instr, selection) {
  const p = await pageInfo(); if (!p) { toast('Open a web page first'); return; }
  if (selection && !p.s.trim()) { toast('Select some text on the page first'); return; }
  aiSend(instr, instr + '\n\nPage: ' + p.t + '\n' + p.u + '\n\n' + (selection ? p.s : p.b));
}
function aiSubmit() {
  const ta = $('#aiin'); const q = ta ? ta.value.trim() : ''; if (!q) return; ta.value = '';
  if (aiUsePage) aiPageAction(q, false); else aiSend(q, q);
}
function aiWeb() {
  const pi = aiSel(); const ta = $('#aiin'); let q = ta ? ta.value.trim() : '';
  if (!q) { const u = aiLog.filter(m => m.role === 'user').pop(); if (u) q = u.shown; }
  if (q) navigator.clipboard.writeText(q).catch(() => { });
  const base = AIS[pi][3]; closeSheet();
  newTab(base.endsWith('=') ? (q ? base + encodeURIComponent(q) : base.slice(0, base.indexOf('?'))) : base, false);
  if (q) toast('Prompt copied — paste it if the site doesn\'t fill it in');
}
async function renderAIKeys() {
  const keys = await Promise.all(AIS.map(a => storm.aiHasKey(a[1])));
  openSheet('aikeys', `<h2 class="D stitle">AI keys</h2><div class="panel">${AIS.map((a, i) => row(i + 1, a[0], keys[i] ? 'Key set' : 'No key', 'aikey', `data-i="${i}"`)).join('')}</div>
    <div class="help">Storm talks directly to each provider with your own API key — nothing goes through our servers. Keys are encrypted with Windows. Each provider bills usage to your account. Click a provider to add or change its key and model.</div>
    <div class="seg" style="margin-top:14px"><button data-act="ai">Back to chat</button></div>`);
}
async function keyDialog(i) {
  const a = AIS[i], has = await storm.aiHasKey(a[1]);
  const r = await modal(a[0], `<input class="field" id="kk" type="password" placeholder="${has ? 'API key (leave blank to keep)' : 'API key'}"><div style="height:8px"></div><input class="field" id="km" placeholder="Model" value="${esc((S.aiModels || {})[a[1]] || a[2])}">`, ['Save', 'Remove key', 'Get key', 'Cancel']);
  if (r.i === 0) {
    const k = r.get('#kk').trim(); if (k) await storm.aiSetKey(a[1], k);
    S.aiModels = S.aiModels || {}; S.aiModels[a[1]] = r.get('#km').trim() || a[2]; save('aiModels'); toast('Saved');
  } else if (r.i === 1) { await storm.aiSetKey(a[1], ''); toast('Key removed'); }
  else if (r.i === 2) { closeSheet(); newTab(a[4], false); return; }
  renderAIKeys();
}

// ---------- updates ----------
const vparts = v => String(v || '').replace(/[^0-9.]/g, '').split('.').map(x => parseInt(x, 10) || 0);
function newer(a, b) { const x = vparts(a), y = vparts(b); for (let i = 0; i < 3; i++) if ((x[i] || 0) !== (y[i] || 0)) return (x[i] || 0) > (y[i] || 0); return false; }
async function checkUpdate(manual) {
  if (manual) { ach('update_check'); toast('Checking for updates…'); }
  try {
    const u = await storm.updateCheck();
    S.lastCheck = Date.now(); save('lastCheck');
    if (newer(u.tag, u.current) && u.url) {
      if (!manual && S.skipVer === u.tag) return;
      const r = await modal('Update available', esc('Version ' + u.tag + ' is ready (you have v' + u.current + ').' + (u.notes ? '\n\n' + u.notes.slice(0, 400) : '')), ['Update now', 'Later']);
      if (r.i === 0) {
        toast('Downloading update…');
        storm.on('update-progress', p => toast('Downloading update… ' + p + '%'));
        try { await storm.updateInstall(u.url); toast('Installing — Storm will restart'); } catch (e) { toast('Update failed'); }
      } else if (!manual) { S.skipVer = u.tag; save('skipVer'); }
    } else if (manual) toast(newer(u.tag, u.current) ? 'That release has no Windows installer yet' : 'You\'re up to date · v' + u.current);
  } catch (e) { if (manual) toast('Couldn\'t check for updates'); }
}

// ---------- intro tour (shown once per version) ----------
const TOUR = [
  ['ADS', 'Block', 'No ads.\nNo trackers.', 'Ad servers, banners and YouTube ads are removed before they load. Click the black banner on Home to switch it on or off.'],
  ['FULL', 'Immersive', 'Every pixel.\nYour page.', 'Press F11 for true full screen. Touch the top edge with the mouse and the toolbar slides back in.'],
  ['HIDE', 'Private', 'Private tabs.\nNo traces.', 'Ctrl+Shift+N opens a tab that keeps no history, cookies or cache. Purple means off the record.'],
  ['FLOAT', 'Minimize', 'Minimize into\nbubbles.', 'Press Ctrl+M or the – on a tab. Drag the floating bubble anywhere and click it to jump back.'],
  ['NOTES', 'Notes', 'Notes on tap.\nCtrl+J.', 'Pin, colour and copy notes. Save any page or AI answer with a single click.'],
  ['AI', 'Assistants', 'Five AIs.\nOne place.', 'ChatGPT, Claude, Gemini, Grok and Perplexity — summarize the page you are on, using your own keys.'],
  ['PROXY', 'Protect', 'Route it\nyour way.', 'Send every tab through your own HTTP or SOCKS5 proxy from Settings.'],
  ['SKIN', 'Make it yours', 'Wallpapers.\nThemes. Colours.', 'Drop your own pictures onto Home, switch to pure OLED black and pick one of six accent colours.'],
  ['PLAY', 'Background', 'Keep it\nplaying.', 'Music and video keep going when the window is minimized or covered.'],
  ['GO', 'Everything else', 'Shortcuts with\nyour pictures.', 'Bookmarks, history, find in page, zoom, Google sign-in and updates inside the app.']];
const QUOTES = [
  ['The journey of a thousand miles begins with a single step.', 'Lao Tzu'],
  ['It does not matter how slowly you go, as long as you do not stop.', 'Confucius'],
  ['The unexamined life is not worth living.', 'Socrates'],
  ['He who knows he has enough is rich.', 'Lao Tzu'],
  ['Knowing yourself is the beginning of all wisdom.', 'Aristotle'],
  ['What we think, we become.', 'Buddha']];
const TOURC = ['#FF5500', '#4A4DFF', '#C8F23C', '#FF4FA3', '#8E6BFF', '#FFC928'];
const TOURT = ['#111', '#fff', '#111', '#111', '#fff', '#111'];
let introIdx = 0, introTimer = null, introTyper = null, introFirst = false, introQuote = QUOTES[0];

// what changed, newest first: version, glyph, title, body
const CHANGES = [
  ['3.13', 'AWARDS', 'Achievements.\nEasy to earn.', 'Collect badges as you use Storm. Every one says exactly how to get it, and clicking it takes you there. Find them on Home or Menu → Awards.'],
  ['3.12', 'GAMES', 'Mini games.\nAll offline.', 'Snake, 2048, Flap, Memory and Tic-Tac-Toe — no internet needed. Open Games from Home or the menu.'],
  ['3.10', 'TOUR', 'Updates now\nstart here.', 'After every update this tour begins with what\'s new, then a quick look at everything Storm can do.'],
  ['3.9', 'FLOAT', 'Minimize into\nbubbles.', 'Press Ctrl+M to float a tab as a draggable bubble. Click the bubble to jump back.'],
  ['3.7', 'SKIN', 'Your own\nwallpapers.', 'Add several pictures behind Home — or drop image files onto it — then darken, blur or shuffle them.'],
  ['3.3', 'FULL', 'True full\nscreen.', 'Press F11 and the toolbar slides away until you touch the top edge of the screen.'],
  ['3.1', 'AI', 'Five AI\nassistants.', 'ChatGPT, Claude, Gemini, Grok and Perplexity — summarize any page with your own keys.']
];
let introList = [];
function buildIntroList(first) {
  const l = [['STORM', first ? 'WELCOME' : 'UPDATED', first ? 'Welcome' : 'Updated · v' + appVersion, first ? 'Meet Storm.\nFast. Private.\nYours.' : 'Storm just\nlevelled up.',
    first ? 'A quick tour of everything inside. Click or press → to go faster, ← to go back, Esc to skip.' : 'First, what\'s new in this update. Then a quick look at everything Storm can do.', '-1']];
  if (!first) {
    const last = S.introVer || ''; let n = 0;
    for (const c of CHANGES) {
      if (n >= 6) break;
      if (newer(c[0], last) && !newer(c[0], appVersion)) l.push([c[1], 'NEW', 'New in v' + c[0], c[2], c[3], String(n++ % 6)]);
    }
  }
  TOUR.forEach((t, k) => l.push([t[0], n2(k + 1) + ' / ' + n2(TOUR.length), t[1], t[2], t[3], String(k % 6)]));
  return l;
}

function maybeIntro() { if (S.introVer !== appVersion) showIntro(!S.introVer); }
function showIntro(first) {
  if ($('#intro')) return;
  introFirst = first; introQuote = QUOTES[Math.floor(Math.random() * QUOTES.length)]; introList = buildIntroList(first);
  const total = introList.length + 1, el = document.createElement('div'); el.id = 'intro';
  el.innerHTML = `<div class="isegs">${Array.from({ length: total }, () => '<div class="seg"><i></i></div>').join('')}</div><button class="tag iskip" data-iskip="1">Skip</button><div id="islide"></div>`;
  document.body.appendChild(el);
  el.addEventListener('click', e => {
    if (e.target.closest('[data-iskip],[data-istart]')) { finishIntro(); return; }
    gotoSlide(introIdx + (e.clientX / innerWidth < .18 ? -1 : 1));
  });
  requestAnimationFrame(() => el.classList.add('on'));
  gotoSlide(0);
}
function gotoSlide(i) {
  const el = $('#intro'); if (!el) return;
  clearTimeout(introTimer); clearInterval(introTyper);
  const total = introList.length + 1;
  if (i < 0) i = 0; if (i >= total) { finishIntro(); return; }
  introIdx = i;
  const dur = i === 0 ? 3200 : 3400;
  el.querySelectorAll('.seg').forEach((s, k) => { s.className = 'seg' + (k < i ? ' done' : ''); s.firstChild.style.removeProperty('animation'); });
  const cur = el.querySelectorAll('.seg')[i];
  if (i < total - 1) { void cur.offsetWidth; cur.firstChild.style.animation = `segfill ${dur}ms linear forwards`; introTimer = setTimeout(() => gotoSlide(introIdx + 1), dur); }
  else cur.classList.add('done');
  const sl = $('#islide');
  if (i === total - 1) { sl.innerHTML = quoteHTML(); startTyping(); }
  else {
    const e = introList[i], t = [e[0], e[2], e[3], e[4]], ci = +e[5];
    const c = ci < 0 ? (ACC[S.settings.acc] || ACC[0]) : TOURC[ci % TOURC.length];
    const tc = ci < 0 ? ([1, 4].includes(S.settings.acc | 0) ? '#fff' : '#111') : TOURT[ci % TOURT.length];
    const num = e[1];
    sl.innerHTML = `<div class="sl" style="--c:${c};--t:${tc}">
      <div class="blk"><i class="ring"></i><i class="ring r2"></i>${ARROW}<div class="D num">${esc(num)}</div><div class="D gl">${esc(t[0])}</div></div>
      <div class="itxt"><div class="L kick">${esc(t[1])}</div><h2 class="D">${t[2].split('\n').map((l, k) => `<span style="animation-delay:${0.35 + k * 0.12}s">${esc(l)}</span>`).join('')}</h2>
      <p style="animation-delay:${0.35 + t[2].split('\n').length * 0.12 + 0.2}s">${esc(t[3])}</p><button class="tag next" data-inext="1">Next →</button></div></div>`;
  }
}
function quoteHTML() {
  return `<div class="qs"><div class="D qm">“</div><div class="D qt" id="qt"></div><div class="L qa" id="qa">— ${esc(introQuote[1])}</div>
    <button class="D qcta" data-istart="1">Start browsing →</button><div class="L qhint">Wise words to start with</div></div>`;
}
function startTyping() {
  const words = introQuote[0].split(' '); let n = 0;
  setTimeout(() => {
    introTyper = setInterval(() => {
      n++; const q = $('#qt'); if (!q) { clearInterval(introTyper); return; }
      q.textContent = words.slice(0, n).join(' ');
      if (n >= words.length) { clearInterval(introTyper); const a = $('#qa'); if (a) a.classList.add('show'); }
    }, 130);
  }, 650);
}
function finishIntro() {
  const el = $('#intro'); if (!el) return;
  clearTimeout(introTimer); clearInterval(introTyper);
  S.introVer = appVersion; save('introVer'); ach('tour');
  el.classList.add('out'); setTimeout(() => el.remove(), 520);
}

// ---------- offline mini-games ----------
function openGames() {
  closeSheet();
  $('#gframe').src = 'games/index.html?oled=' + (S.settings.oled ? 1 : 0) + '&acc=' + encodeURIComponent(ACC[S.settings.acc] || ACC[0]);
  $('#games').classList.add('on'); setTimeout(() => { try { $('#gframe').focus(); } catch (e) { } }, 100);
}
function closeGames() { $('#games').classList.remove('on'); $('#gframe').src = 'about:blank'; }
window.addEventListener('message', e => {
  const d = e.data; if (!d) return;
  if (d.stormGames === 'close') closeGames();
  else if (d.stormGames === 'event') { if (d.name === 'play') ach('games'); else if (d.name === 'score') ach('game_score'); }
});

// ---------- achievements ----------
// id, glyph, title, how to get it, goal, action
const ACH = [
  ['site', 'GO', 'First steps', 'Open any website. Type an address in the bar, or click a shortcut on Home.', 1, 'site'],
  ['bookmark', 'SAVE', 'Bookworm', 'Save a bookmark: open a page, then press Ctrl+D.', 1, 'bookmarks'],
  ['note', 'NOTE', 'Note taker', 'Write a note. Press Ctrl+J, click + New note and type anything.', 1, 'notes'],
  ['tabs3', 'TABS', 'Multitasker', 'Have 3 tabs open at once. Press Ctrl+T twice.', 3, 'newtab'],
  ['ads10', 'ADS', 'Ad dodger', 'Let Storm block 10 ads or trackers. Just browse with Ad block on.', 10, 'site'],
  ['ai', 'AI', 'Curious mind', 'Open the AI assistant (Ctrl+Shift+A). That\'s all.', 1, 'ai'],
  ['games', 'PLAY', 'Player one', 'Open Games and start any game.', 1, 'games'],
  ['game_score', 'WIN', 'High scorer', 'Do well in a game: Snake 5, a 128 tile in 2048, 3 pipes in Flap, finish Memory, or draw Tic-Tac-Toe.', 1, 'games'],
  ['private', 'HIDE', 'Incognito', 'Open a private tab: Ctrl+Shift+N.', 1, 'private'],
  ['minimize', 'MIN', 'Out of the way', 'Minimize a page: open one, then press Ctrl+M.', 1, 'minimize'],
  ['wallpaper', 'SKIN', 'Interior designer', 'Add a wallpaper: Settings → Wallpaper, or drop a picture onto Home.', 1, 'settings'],
  ['accent', 'HUE', 'Colour me in', 'Pick a new accent colour in Settings.', 1, 'settings'],
  ['theme', 'DARK', 'Dark side', 'Switch the theme to OLED dark in Settings.', 1, 'settings'],
  ['shortcut_pic', 'PIC', 'Personal touch', 'Change a shortcut\'s picture: click the small ▣ button on a card on Home.', 1, 'home'],
  ['sites5', 'SITES', 'Explorer', 'Visit 5 pages. Any pages count.', 5, 'site'],
  ['update_check', 'NEW', 'Up to date', 'Click Check for updates in the menu.', 1, 'update'],
  ['tour', 'TOUR', 'Well informed', 'Watch the intro tour (Settings → Replay intro) and finish or skip it.', 1, 'tour'],
  ['sites25', 'LOTS', 'Surfer', 'Visit 25 pages.', 25, 'site'],
  ['ads1000', 'SHLD', 'Shield up', 'Let Storm block 1000 ads and trackers.', 1000, 'site'],
  ['collector', 'ALL', 'Collector', 'Unlock 10 achievements.', 10, '']
];
const achStored = id => (S.ach && S.ach[id]) || 0;
const achProg = a => { const s = achStored(a[0]); return (a[0] === 'ads10' || a[0] === 'ads1000') ? Math.max(s, blockedCount) : s; };
const achDone = a => achProg(a) >= a[4];
const achDoneCount = () => ACH.filter(achDone).length;
function ach(id, add) {
  const a = ACH.find(x => x[0] === id); if (!a || achDone(a)) return;
  const p = Math.min(a[4], achStored(id) + (add || 1)); S.ach = S.ach || {}; S.ach[id] = p; save('ach');
  if (p >= a[4]) achUnlocked(a);
}
function achSet(id, v) {
  const a = ACH.find(x => x[0] === id); if (!a || achDone(a) || achStored(id) >= v) return;
  const p = Math.min(a[4], v); S.ach = S.ach || {}; S.ach[id] = p; save('ach');
  if (p >= a[4]) achUnlocked(a);
}
function achSyncAds() {
  for (const id of ['ads10', 'ads1000']) {
    const a = ACH.find(x => x[0] === id);
    if (blockedCount >= a[4] && achStored(id) < a[4]) { S.ach = S.ach || {}; S.ach[id] = a[4]; save('ach'); achUnlocked(a); }
  }
}
let achQueue = [], achShowing = false;
function achUnlocked(a) {
  achQueue.push(a); if (!achShowing) nextBanner();
  if (a[0] !== 'collector') achSet('collector', achDoneCount());
}
function nextBanner() {
  if (!achQueue.length) { achShowing = false; return; }
  achShowing = true; const a = achQueue.shift(), b = document.createElement('div'); b.className = 'achb';
  b.innerHTML = `<div class="g D">${esc(a[1])}</div><div class="t"><div class="L">Achievement unlocked</div><div class="D">${esc(a[2])}</div></div>`;
  document.body.appendChild(b); requestAnimationFrame(() => b.classList.add('on'));
  setTimeout(() => { b.classList.remove('on'); setTimeout(() => { b.remove(); nextBanner(); }, 450); }, 3200);
}
function achAction(act) {
  const t = T();
  if (act === 'site') { $('#addr').focus(); $('#addr').select(); }
  else if (act === 'home') { if (t) { t.url = ''; showCurrent(); } }
  else if (act === 'bookmarks') renderBookmarks();
  else if (act === 'notes') openNotes();
  else if (act === 'private') newTab(null, true);
  else if (act === 'newtab') newTab(null, false);
  else if (act === 'minimize') { if (t && t.url) minimizeTab(t); else toast('Open a page first, then press Ctrl+M'); }
  else if (act === 'settings') renderSettings();
  else if (act === 'ai') renderAI();
  else if (act === 'games') openGames();
  else if (act === 'update') checkUpdate(true);
  else if (act === 'tour') showIntro(false);
}
function renderAch() {
  const done = achDoneCount();
  openSheet('ach', `<h2 class="D stitle">Awards</h2><div class="L" style="opacity:.65">Achievements · each one tells you how to get it</div>
    <div class="panel" style="margin-top:14px;padding:12px 14px"><div style="display:flex;justify-content:space-between;align-items:flex-end"><span class="L">Unlocked</span><span class="D" style="font-size:40px;color:var(--acc)">${n2(done)} / ${n2(ACH.length)}</span></div><div class="abar"><i style="width:${done / ACH.length * 100}%"></i></div></div>
    <div class="panel" style="margin-top:14px">${ACH.map(a => {
      const d = achDone(a), g = Math.min(a[4], achProg(a)), go = !d && a[5];
      return `<div class="arow ${d ? 'done' : ''}" ${go ? `data-act="achgo" data-a="${a[5]}"` : ''}><div class="ag D">${esc(a[1])}</div><div class="am"><div class="D at">${esc(a[2])}</div><div class="ah">${esc(a[3])}</div>${a[4] > 1 ? `<div class="abar"><i style="width:${g / a[4] * 100}%"></i></div>` : ''}${go ? '<div class="L ago">Click to try it →</div>' : ''}</div><div class="L as">${d ? '✓ Done' : g + '/' + a[4]}</div></div>`;
    }).join('')}</div>`, true);
}

// ---------- actions ----------
async function pickImage(name, w) { try { return await storm.pickImage(name, w); } catch (e) { return null; } }
const ACT = {
  tabsel: a => select(+a.dataset.i),
  games: openGames,
  awards: renderAch,
  achgo: a => { closeSheet(); setTimeout(() => achAction(a.dataset.a), 300); },
  tabmin: (a, e) => { e.stopPropagation(); minimizeTab(tabs[+a.dataset.i]); },
  minimize: () => { closeSheet(); minimizeTab(T()); },
  minmode: () => { setting('minMode', ((S.settings.minMode | 0) + 1) % 3); renderSettings(); },
  tabclose: (a, e) => { e.stopPropagation(); closeTab(+a.dataset.i); },
  newtab: () => { closeSheet(); newTab(null, false); },
  newprivate: () => { closeSheet(); newTab(null, true); },
  back: goBack, fwd: goFwd,
  reload: () => { const t = T(); if (!t || !t.wv || !t.url) return; if (t.loading) t.wv.stop(); else t.wv.reload(); },
  home: () => { const t = T(); if (t) { t.url = ''; showCurrent(); } },
  go: () => go($('#addr').value),
  menu: renderMenu, 'menu-tabs': renderMenu,
  closesheet: closeSheet,
  notes: () => { closeSheet(); openNotes(); }, closenotes: closeNotes,
  ai: () => { renderAI(); },
  bookmarks: renderBookmarks, history: () => { sheetMode = ''; renderHistory(); },
  bookmark: () => {
    const t = T(); if (!t || !t.url || t.priv) { toast('Open a normal page first'); return; }
    S.bookmarks = S.bookmarks.filter(b => b.u !== t.url); S.bookmarks.unshift({ t: t.title || t.url, u: t.url }); save('bookmarks');
    toast('Bookmarked'); ach('bookmark'); if (sheetMode === 'bm') renderBookmarks();
  },
  'bm-open': a => { closeSheet(); load(T(), S.bookmarks[+a.dataset.i].u); },
  'bm-del': (a, e) => { e.stopPropagation(); S.bookmarks.splice(+a.dataset.i, 1); save('bookmarks'); renderBookmarks(); },
  'hist-open': a => { closeSheet(); load(T(), S.history[+a.dataset.i].u); },
  'clear-hist': async () => { const r = await modal('Clear history?', 'This removes all browsing history.', ['Clear', 'Cancel']); if (r.i === 0) { S.history = []; save('history'); renderHistory(); } },
  settings: renderSettings,
  toggleads: () => { setting('ads', !S.settings.ads); refreshHome(); },
  toggleads2: () => { setting('ads', !S.settings.ads); if (sheetMode === 'menu') renderMenu(); else renderSettings(); },
  proxy: renderSettings,
  proxysave: () => { setting('proxy', $('#proxyin').value.trim()); toast(S.settings.proxy ? 'Proxy saved' : 'Proxy off'); refreshHome(); },
  theme: a => { if (a.dataset.v === '1') ach('theme'); setting('oled', a.dataset.v === '1'); applyTheme(); renderSettings(); },
  accent: a => { ach('accent'); setting('acc', +a.dataset.i); applyTheme(); renderSettings(); refreshHome(); },
  engine: () => { setting('se', (S.settings.se + 1) % ENGINES.length); renderSettings(); },
  fullscreen: () => { closeSheet(); storm.fullscreen(); },
  startfull: () => { setting('startfull', !S.settings.startfull); renderSettings(); },
  intro: () => { closeSheet(); setTimeout(() => showIntro(false), 300); },
  autoupd: () => { setting('autoupd', !S.settings.autoupd); renderSettings(); },
  wallpick: async () => {
    const urls = await storm.pickImages('wall', 2560); if (!urls.length) return;
    S.walls = wallList().concat(urls); S.wallSel = S.walls.length - 1; save('walls'); save('wallSel'); ach('wallpaper');
    toast(urls.length > 1 ? urls.length + ' wallpapers added' : 'Wallpaper set'); if (sheetMode === 'settings') renderSettings(); refreshHome();
  },
  wallsel: a => { S.wallSel = +a.dataset.i; save('wallSel'); setting('wallShuffle', false); renderSettings(); refreshHome(); },
  walldel: (a, e) => {
    e.stopPropagation(); const i = +a.dataset.i; S.walls.splice(i, 1);
    if (S.wallSel === i) S.wallSel = -1; else if (S.wallSel > i) S.wallSel--;
    save('walls'); save('wallSel'); renderSettings(); refreshHome();
  },
  wallnone: () => { S.wallSel = -1; save('wallSel'); setting('wallShuffle', false); toast('Wallpaper off'); renderSettings(); refreshHome(); },
  wallshuffle: () => { setting('wallShuffle', !S.settings.wallShuffle); renderSettings(); refreshHome(); },
  bgpick: async () => { const u = await pickImage('home', 1920); if (u) { S.bgimg = u; save('bgimg'); toast('Photo updated'); refreshHome(); } },
  bgreset: () => { S.bgimg = ''; save('bgimg'); toast('Photo reset'); refreshHome(); },
  update: () => checkUpdate(true),
  cleardata: async () => { const r = await modal('Clear browsing data?', 'Cookies, cache and site data will be removed (you will be signed out of websites).', ['Clear', 'Cancel']); if (r.i === 0) { await storm.clearData(); S.history = []; save('history'); toast('Cleared'); } },
  hero: () => { $('#addr').focus(); $('#addr').select(); },
  shortcut: a => load(T(), QUICK[+a.dataset.i][1]),
  pic: async (a, e) => { e.stopPropagation(); const i = +a.dataset.i; const u = await pickImage('sc' + i, 900); if (u) { S.pics = S.pics || {}; S.pics[i] = u; save('pics'); ach('shortcut_pic'); toast('Picture updated'); refreshHome(); } },
  google: () => load(T(), 'https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fwww.google.com%2F&hl=en'),
  find: () => { closeSheet(); openFind(); },
  findnext: () => doFind(true), findprev: () => doFind(false), findclose: closeFind,
  // notes
  'note-new': () => openEditor(null),
  'note-open': a => openEditor(S.notes.find(n => n.id === a.dataset.id)),
  'note-page': () => {
    const t = T(); if (!t || !t.url) { toast('Open a page first'); return; }
    S.notes.unshift({ id: String(Date.now()), title: t.title || t.url, body: t.url, color: 0, pin: 0, ts: Date.now() }); save('notes'); renderNotesList();
  },
  'note-done': () => { saveNote(); renderNotesList(); },
  'note-pin': () => { saveNote(); noteCur.pin = noteCur.pin ? 0 : 1; if (!S.notes.includes(noteCur) && S.notes.findIndex(x => x.id === noteCur.id) < 0) S.notes.unshift(noteCur); save('notes'); openEditor(noteCur); },
  'note-color': () => { saveNote(); noteCur.color = (noteCur.color + 1) % NC; if (S.notes.findIndex(x => x.id === noteCur.id) < 0) S.notes.unshift(noteCur); save('notes'); openEditor(noteCur); },
  'note-copy': () => { navigator.clipboard.writeText((noteCur.title ? noteCur.title + '\n\n' : '') + $('#nbody').value).then(() => toast('Copied')); },
  'note-del': async () => { const r = await modal('Delete note?', 'This cannot be undone.', ['Delete', 'Cancel']); if (r.i === 0) { S.notes = S.notes.filter(n => n.id !== noteCur.id); save('notes'); noteCur = null; renderNotesList(); } },
  // AI
  aisel: a => { S.aiSel = +a.dataset.i; save('aiSel'); renderAI(); },
  aiq: a => {
    const i = +a.dataset.i;
    if (i === 0) aiPageAction('Summarize this web page in 5 short bullet points.', false);
    else if (i === 1) aiPageAction('List the key facts, names, numbers and dates from this page.', false);
    else if (i === 2) aiPageAction('Explain the following selected text in simple terms.', true);
    else if (i === 3) aiPageAction('Translate the main content of this page into English, keeping it brief.', false);
    else if (i === 4) aiWeb(); else if (i === 5) { aiLog = []; renderAILog(); } else renderAIKeys();
  },
  aipage: a => { aiUsePage = !aiUsePage; a.classList.toggle('on', aiUsePage); }, aisend: aiSubmit,
  aicopy: a => navigator.clipboard.writeText(aiLog[+a.dataset.i].shown).then(() => toast('Copied')),
  ainote: a => { const m = aiLog[+a.dataset.i]; S.notes.unshift({ id: String(Date.now()), title: 'AI · ' + AIS[aiSel()][0], body: m.shown, color: 0, pin: 0, ts: Date.now() }); save('notes'); toast('Saved to Notes'); },
  aikeys: renderAIKeys, aikey: a => keyDialog(+a.dataset.i)
};
document.addEventListener('click', e => {
  const a = e.target.closest('[data-act]'); if (!a) return;
  const f = ACT[a.dataset.act]; if (f) f(a, e);
});
document.addEventListener('contextmenu', e => {
  const m = e.target.closest('[data-act="pic"], .card.pic');
  if (m) { const c = m.closest('.card'); if (c) { e.preventDefault(); const i = +c.dataset.i; if (S.pics && S.pics[i]) { delete S.pics[i]; save('pics'); toast('Picture removed'); refreshHome(); } } }
});
document.addEventListener('input', e => {
  const id = e.target.id;
  if (id === 'hq') renderHistory(e.target.value);
  else if (id === 'findin') doFind(true, true);
  else if (id === 'wdim') { setting('wallDim', +e.target.value); $('#wdv').textContent = e.target.value; setWallVars(); }
  else if (id === 'wblur') { setting('wallBlur', +e.target.value); $('#wbv').textContent = e.target.value; setWallVars(); }
});
// drop image files onto the window to add them as wallpapers
window.addEventListener('message', async e => {
  const p = e.data && e.data.stormDrop; if (!Array.isArray(p)) return;
  const urls = await storm.importImages(p, 'wall', 2560);
  if (!urls.length) { toast('Drop an image file (JPG, PNG, WebP)'); return; }
  S.walls = wallList().concat(urls); S.wallSel = S.walls.length - 1; save('walls'); save('wallSel'); ach('wallpaper');
  toast(urls.length > 1 ? urls.length + ' wallpapers added' : 'Wallpaper set'); if (sheetMode === 'settings') renderSettings(); refreshHome();
});

// ---------- find in page ----------
function openFind() { $('#findbar').classList.add('on'); $('#findin').focus(); $('#findin').select(); }
function closeFind() { $('#findbar').classList.remove('on'); const t = T(); if (t && t.wv) try { t.wv.stopFindInPage('clearSelection'); } catch (e) { } }
function doFind(fwd, fresh) {
  const t = T(), q = $('#findin').value; if (!t || !t.wv || !q) return;
  try { t.wv.findInPage(q, { forward: fwd, findNext: !fresh }); } catch (e) { }
}
$('#findin').addEventListener('keydown', e => { if (e.key === 'Enter') doFind(!e.shiftKey); if (e.key === 'Escape') closeFind(); });

// ---------- address bar ----------
const addr = $('#addr');
addr.addEventListener('keydown', e => { if (e.key === 'Enter') go(addr.value); if (e.key === 'Escape') { addr.blur(); updateAddress(); } });
addr.addEventListener('focus', () => addr.select());
addr.addEventListener('blur', updateAddress);
$('#addrcell').addEventListener('click', () => addr.focus());

// ---------- zoom + commands from guest pages ----------
function zoom(d) {
  const t = T(); if (!t || !t.wv) return;
  try { const z = d === 0 ? 1 : Math.max(.25, Math.min(5, t.wv.getZoomFactor() + d)); t.wv.setZoomFactor(z); toast('Zoom ' + Math.round(z * 100) + '%'); } catch (e) { }
}
function command(c) {
  if (c === 'new') newTab(null, false);
  else if (c === 'private') newTab(null, true);
  else if (c === 'close') closeTab(cur);
  else if (c === 'focus') { addr.focus(); addr.select(); }
  else if (c === 'reload') ACT.reload();
  else if (c === 'bookmark') ACT.bookmark();
  else if (c === 'find') openFind();
  else if (c === 'next') stepTab(1);
  else if (c === 'prev') stepTab(-1);
  else if (c === 'minimize') minimizeTab(T());
  else if (c === 'notes') { $('#notes').classList.contains('on') ? closeNotes() : openNotes(); }
  else if (c === 'ai') renderAI();
  else if (c === 'fullscreen') storm.fullscreen();
  else if (c === 'zoomin') zoom(.1); else if (c === 'zoomout') zoom(-.1); else if (c === 'zoom0') zoom(0);
  else if (c === 'esc') { if ($('#games').classList.contains('on')) { closeGames(); return; } closeSheet(); if ($('#notes').classList.contains('on')) closeNotes(); closeFind(); }
}
document.addEventListener('keydown', e => {
  if ($('#intro')) {
    if (e.key === 'Escape') finishIntro(); else if (e.key === 'ArrowRight' || e.key === ' ' || e.key === 'Enter') gotoSlide(introIdx + 1); else if (e.key === 'ArrowLeft') gotoSlide(introIdx - 1);
    e.preventDefault(); return;
  }
  const c = e.ctrlKey || e.metaKey, k = e.key.toLowerCase(); let cmd = null;
  if (c && (k === '+' || k === '=')) cmd = 'zoomin'; else if (c && k === '-') cmd = 'zoomout'; else if (c && k === '0') cmd = 'zoom0';
  else if (c && !e.shiftKey) cmd = { t: 'new', w: 'close', l: 'focus', r: 'reload', d: 'bookmark', f: 'find', tab: 'next', j: 'notes', m: 'minimize' }[k];
  else if (c && e.shiftKey) cmd = { n: 'private', a: 'ai', tab: 'prev' }[k];
  else if (k === 'f5') cmd = 'reload'; else if (k === 'f11') cmd = 'fullscreen'; else if (k === 'escape') cmd = 'esc';
  else if (e.altKey && k === 'arrowleft') cmd = 'goback'; else if (e.altKey && k === 'arrowright') cmd = 'gofwd';
  if (cmd === 'goback') goBack(); else if (cmd === 'gofwd') goFwd(); else if (cmd) { e.preventDefault(); command(cmd); }
});

// full-screen: reveal the toolbar when the mouse touches the top edge
(function () {
  const chrome = $('#chrome'); let hideT;
  const hide = () => { if (document.body.classList.contains('fs') && document.activeElement !== $('#addr') && !$('#findbar').classList.contains('on')) chrome.classList.remove('show'); };
  $('#hot').addEventListener('mouseenter', () => { clearTimeout(hideT); chrome.classList.add('show'); });
  chrome.addEventListener('mouseenter', () => clearTimeout(hideT));
  chrome.addEventListener('mouseleave', () => { clearTimeout(hideT); hideT = setTimeout(hide, 700); });
  $('#addr').addEventListener('blur', () => { clearTimeout(hideT); hideT = setTimeout(hide, 700); });
})();

// ---------- init ----------
(async function init() {
  const st = await storm.state(); paths = await storm.paths(); appVersion = paths.version;
  S = Object.assign({ ach: {}, bookmarks: [], history: [], notes: [], pics: {}, bgimg: '', tabsSaved: [], aiSel: 0, aiModels: {}, walls: [], wallSel: -1 }, st);
  S.settings = Object.assign({ ads: true, oled: false, acc: 0, se: 0, proxy: '', autoupd: true, wallDim: 35, wallBlur: 0, wallShuffle: false }, st.settings || {});
  blockedCount = st.blockedTotal || 0;
  applyTheme();
  storm.on('open-tab', url => newTab(url, false));
  storm.on('cmd', command);
  storm.on('toast', toast);
  setTimeout(maybeIntro, 900);
  storm.on('fs', on => { document.body.classList.toggle('fs', on); $('#chrome').classList.remove('show'); });
  storm.on('ask-ai', async text => { await renderAI(); aiSend('Explain selection', 'Explain the following selected text in simple terms.\n\n' + text); });
  storm.on('blocked', n => { blockedCount = n; const b = $('#blk'); if (b) b.textContent = n; if (n >= 10) achSyncAds(); });
  const saved = S.tabsSaved || [];
  if (saved.length) {
    saved.forEach(s => tabs.push({ priv: false, url: s.url, title: s.title || s.url, wv: null, ready: false, min: !!s.min }));
    let ci = Math.min(S.tabCur || 0, tabs.length - 1);
    if (tabs[ci].min) ci = visibleIdx(ci);
    if (ci < 0) newTab(null, false); else select(ci);
    renderBubbles();
  } else newTab(null, false);
  if (S.settings.autoupd && Date.now() - (S.lastCheck || 0) > 12 * 3600 * 1000) setTimeout(() => checkUpdate(false), 4000);
})();
