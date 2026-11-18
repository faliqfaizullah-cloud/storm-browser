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
    if (!t.priv && t.url && !/^(about|data):/.test(t.url)) addHistory(t.title, t.url);
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
  showCurrent(); saveTabs();
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
  S.tabsSaved = tabs.filter(t => !t.priv && t.url).map(t => ({ url: t.url, title: t.title }));
  S.tabCur = Math.max(0, tabs.filter(t => !t.priv && t.url).indexOf(T()));
  save('tabsSaved'); save('tabCur');
}
function renderTabs() {
  $('#tabs').innerHTML = tabs.map((t, i) => `<div class="tab ${i === cur ? 'on' : ''} ${t.priv ? 'priv' : ''}" data-act="tabsel" data-i="${i}">
    <span class="L num">${n2(i + 1)}</span><span class="ttl">${esc(t.priv ? 'Private · ' : '')}${esc(t.url ? (t.title || hostOf(t.url)) : 'New tab')}</span>
    <button class="x" data-act="tabclose" data-i="${i}">✕</button></div>`).join('') +
    '<button class="tabbtn" data-act="newtab" title="New tab (Ctrl+T)">+</button><button class="tabbtn" data-act="newprivate" title="Private tab (Ctrl+Shift+N)">P</button>';
}
function showCurrent() {
  const t = T();
  document.querySelectorAll('webview').forEach(w => w.classList.remove('on'));
  const home = $('#home');
  if (t && t.url && t.wv) { t.wv.classList.add('on'); home.classList.remove('on'); }
  else if (t) { home.innerHTML = homeHTML(t.priv); home.classList.add('on'); home.classList.toggle('priv', t.priv && !S.settings.oled); }
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
    ${row(4, 'AI assistant', '', 'ai')}${row(5, 'Bookmarks', '', 'bookmarks')}${row(6, 'History', '', 'history')}
    ${row(7, 'Add bookmark', '', 'bookmark')}${row(8, 'Find in page', '', 'find')}
    ${row(9, 'Full screen', 'F11', 'fullscreen')}${row(10, 'Ad block', S.settings.ads ? 'On' : 'Off', 'toggleads2')}
    ${row(11, 'Settings', '', 'settings')}${row(12, 'Check for updates', 'v' + appVersion, 'update')}${row(13, 'Clear data', '', 'cleardata')}
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
    <div class="lbl L">Home photo</div><div class="seg"><button data-act="bgpick">Choose photo</button><button data-act="bgreset">Reset</button></div>
    <div class="lbl L">Proxy (http://host:port or socks5://host:port)</div>
    <div style="display:flex;gap:8px"><input class="field" id="proxyin" value="${esc(s.proxy)}" placeholder="Leave empty for direct connection"><button class="tag" data-act="proxysave">Save</button></div>
    <div class="lbl L">Behaviour</div><div class="panel">
      ${row(1, 'Ad block', s.ads ? 'On' : 'Off', 'toggleads2')}
      ${row(2, 'Search engine', ENGINES[s.se][0], 'engine')}
      ${row(3, 'Auto update check', s.autoupd ? 'On' : 'Off', 'autoupd')}
      ${row(4, 'Start in full screen', s.startfull ? 'On' : 'Off', 'startfull')}
      ${row(5, 'AI keys', '', 'aikeys')}
      ${row(6, 'Check for updates', 'v' + appVersion, 'update')}
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
}

// ---------- AI ----------
const aiSel = () => Math.min(AIS.length - 1, S.aiSel || 0);
async function renderAI() {
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
  if (manual) toast('Checking for updates…');
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

// ---------- actions ----------
async function pickImage(name, w) { try { return await storm.pickImage(name, w); } catch (e) { return null; } }
const ACT = {
  tabsel: a => select(+a.dataset.i),
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
    toast('Bookmarked'); if (sheetMode === 'bm') renderBookmarks();
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
  theme: a => { setting('oled', a.dataset.v === '1'); applyTheme(); renderSettings(); },
  accent: a => { setting('acc', +a.dataset.i); applyTheme(); renderSettings(); refreshHome(); },
  engine: () => { setting('se', (S.settings.se + 1) % ENGINES.length); renderSettings(); },
  fullscreen: () => { closeSheet(); storm.fullscreen(); },
  startfull: () => { setting('startfull', !S.settings.startfull); renderSettings(); },
  autoupd: () => { setting('autoupd', !S.settings.autoupd); renderSettings(); },
  bgpick: async () => { const u = await pickImage('home', 1920); if (u) { S.bgimg = u; save('bgimg'); toast('Photo updated'); refreshHome(); } },
  bgreset: () => { S.bgimg = ''; save('bgimg'); toast('Photo reset'); refreshHome(); },
  update: () => checkUpdate(true),
  cleardata: async () => { const r = await modal('Clear browsing data?', 'Cookies, cache and site data will be removed (you will be signed out of websites).', ['Clear', 'Cancel']); if (r.i === 0) { await storm.clearData(); S.history = []; save('history'); toast('Cleared'); } },
  hero: () => { $('#addr').focus(); $('#addr').select(); },
  shortcut: a => load(T(), QUICK[+a.dataset.i][1]),
  pic: async (a, e) => { e.stopPropagation(); const i = +a.dataset.i; const u = await pickImage('sc' + i, 900); if (u) { S.pics = S.pics || {}; S.pics[i] = u; save('pics'); toast('Picture updated'); refreshHome(); } },
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
document.addEventListener('input', e => { if (e.target.id === 'hq') renderHistory(e.target.value); if (e.target.id === 'findin') doFind(true, true); });

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
  else if (c === 'next') select((cur + 1) % tabs.length);
  else if (c === 'prev') select((cur - 1 + tabs.length) % tabs.length);
  else if (c === 'notes') { $('#notes').classList.contains('on') ? closeNotes() : openNotes(); }
  else if (c === 'ai') renderAI();
  else if (c === 'fullscreen') storm.fullscreen();
  else if (c === 'zoomin') zoom(.1); else if (c === 'zoomout') zoom(-.1); else if (c === 'zoom0') zoom(0);
  else if (c === 'esc') { closeSheet(); if ($('#notes').classList.contains('on')) closeNotes(); closeFind(); }
}
document.addEventListener('keydown', e => {
  const c = e.ctrlKey || e.metaKey, k = e.key.toLowerCase(); let cmd = null;
  if (c && (k === '+' || k === '=')) cmd = 'zoomin'; else if (c && k === '-') cmd = 'zoomout'; else if (c && k === '0') cmd = 'zoom0';
  else if (c && !e.shiftKey) cmd = { t: 'new', w: 'close', l: 'focus', r: 'reload', d: 'bookmark', f: 'find', tab: 'next', j: 'notes' }[k];
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
  S = Object.assign({ bookmarks: [], history: [], notes: [], pics: {}, bgimg: '', tabsSaved: [], aiSel: 0, aiModels: {} }, st);
  S.settings = Object.assign({ ads: true, oled: false, acc: 0, se: 0, proxy: '', autoupd: true }, st.settings || {});
  blockedCount = st.blockedTotal || 0;
  applyTheme();
  storm.on('open-tab', url => newTab(url, false));
  storm.on('cmd', command);
  storm.on('toast', toast);
  storm.on('fs', on => { document.body.classList.toggle('fs', on); $('#chrome').classList.remove('show'); });
  storm.on('ask-ai', async text => { await renderAI(); aiSend('Explain selection', 'Explain the following selected text in simple terms.\n\n' + text); });
  storm.on('blocked', n => { blockedCount = n; const b = $('#blk'); if (b) b.textContent = n; });
  const saved = S.tabsSaved || [];
  if (saved.length) {
    saved.forEach(s => tabs.push({ priv: false, url: s.url, title: s.title || s.url, wv: null, ready: false }));
    select(Math.min(S.tabCur || 0, tabs.length - 1));
  } else newTab(null, false);
  if (S.settings.autoupd && Date.now() - (S.lastCheck || 0) > 12 * 3600 * 1000) setTimeout(() => checkUpdate(false), 4000);
})();
