'use strict';
const { app, BrowserWindow, ipcMain, session, dialog, shell, safeStorage, nativeImage, Menu, clipboard } = require('electron');
const path = require('path');
const fs = require('fs');
const { spawn } = require('child_process');
const { pathToFileURL } = require('url');
const ads = require('./lib/adblock');

const REPO = 'faliqfaizullah-cloud/storm-browser';
let win = null, data = {}, blocked = 0, lastSent = -1;

// ---------- storage ----------
const dataFile = () => path.join(app.getPath('userData'), 'storm.json');
function loadData() { try { data = JSON.parse(fs.readFileSync(dataFile(), 'utf8')); } catch (e) { data = {}; } }
let saveTimer = null;
function persist() {
  clearTimeout(saveTimer);
  saveTimer = setTimeout(() => { try { fs.writeFileSync(dataFile(), JSON.stringify(data)); } catch (e) { } }, 250);
}
const THEMES = [{ bg: '#F0EEE9', fg: '#111111' }, { bg: '#000000', fg: '#F2F2F2' }, { bg: '#FFFFFF', fg: '#1C1C1E' }];
const themeOf = s => (s.theme != null ? s.theme | 0 : (s.oled ? 1 : 0));
const settings = () => Object.assign({ ads: true, oled: false, acc: 0, se: 0, proxy: '', autoupd: true, startfull: false }, data.settings || {});
const adsOn = () => settings().ads !== false;
const send = (ch, ...a) => { if (win && !win.isDestroyed()) win.webContents.send(ch, ...a); };

// ---------- sessions ----------
let UA = '';
function cleanUA() {
  if (!UA) UA = session.defaultSession.getUserAgent().replace(/\s(Electron|Storm|storm-browser)\/\S+/gi, '');
  return UA;
}
const PARTS = ['persist:storm', 'storm-private'];
function setupSession(name) {
  const ses = session.fromPartition(name);
  ses.setUserAgent(cleanUA());
  ses.webRequest.onBeforeRequest((d, cb) => {
    if (adsOn() && d.resourceType !== 'mainFrame' && ads.isBlocked(d.url)) { blocked++; cb({ cancel: true }); }
    else cb({});
  });
  ses.setPermissionRequestHandler((wc, perm, cb) =>
    cb(['fullscreen', 'media', 'notifications', 'clipboard-sanitized-write', 'pointerLock', 'mediaKeySystem'].includes(perm)));
  ses.on('will-download', (e, item) => {
    item.once('done', (ev, state) => send('toast', state === 'completed' ? 'Downloaded ' + item.getFilename() : 'Download ' + state));
  });
  return ses;
}
async function applyProxy() {
  const p = (settings().proxy || '').trim();
  for (const n of PARTS) {
    try { await session.fromPartition(n).setProxy(p ? { proxyRules: p } : { mode: 'direct' }); } catch (e) { }
  }
}

// ---------- window ----------
function createWindow() {
  const s = settings();
  win = new BrowserWindow({
    width: 1360, height: 860, minWidth: 820, minHeight: 520, title: 'Storm', show: false,
    backgroundColor: THEMES[themeOf(s)].bg,
    icon: path.join(__dirname, 'renderer', 'icon.png'),
    titleBarStyle: 'hidden',
    titleBarOverlay: { color: THEMES[themeOf(s)].bg, symbolColor: THEMES[themeOf(s)].fg, height: 40 },
    webPreferences: {
      preload: path.join(__dirname, 'preload-ui.js'), webviewTag: true, contextIsolation: true,
      nodeIntegration: false, backgroundThrottling: false
    }
  });
  Menu.setApplicationMenu(null);
  win.once('ready-to-show', () => {
    if (settings().startfull) win.setFullScreen(true); else win.maximize();
    win.show();
  });
  win.on('enter-full-screen', () => send('fs', true));
  win.on('leave-full-screen', () => send('fs', false));
  win.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  win.webContents.on('will-navigate', e => e.preventDefault());
  win.webContents.setWindowOpenHandler(({ url }) => { shell.openExternal(url); return { action: 'deny' }; });
  win.on('closed', () => { win = null; });
}

// guest pages (webviews): new windows -> new tab, shortcuts, context menu
app.on('web-contents-created', (e, wc) => {
  if (wc.getType() !== 'webview') return;
  wc.setWindowOpenHandler(({ url }) => { send('open-tab', url); return { action: 'deny' }; });
  wc.on('before-input-event', (ev, input) => {
    if (input.type !== 'keyDown') return;
    const c = input.control || input.meta, k = String(input.key).toLowerCase();
    let cmd = null;
    if (c && (k === '+' || k === '=')) cmd = 'zoomin';
    else if (c && k === '-') cmd = 'zoomout';
    else if (c && k === '0') cmd = 'zoom0';
    else if (c && !input.shift) {
      cmd = { t: 'new', w: 'close', l: 'focus', r: 'reload', d: 'bookmark', f: 'find', tab: 'next', j: 'notes', m: 'minimize' }[k] || null;
    } else if (c && input.shift) {
      cmd = { n: 'private', a: 'ai', tab: 'prev' }[k] || null;
    } else if (k === 'f5') cmd = 'reload';
    else if (k === 'f11') cmd = 'fullscreen';
    else if (k === 'escape') cmd = 'esc';
    if (cmd) { ev.preventDefault(); send('cmd', cmd); }
  });
  wc.on('context-menu', (ev, p) => {
    const items = [];
    if (p.linkURL) {
      items.push({ label: 'Open link in new tab', click: () => send('open-tab', p.linkURL) });
      items.push({ label: 'Copy link address', click: () => clipboard.writeText(p.linkURL) });
    }
    if (p.mediaType === 'image' && p.srcURL) {
      items.push({ label: 'Open image in new tab', click: () => send('open-tab', p.srcURL) });
      items.push({ label: 'Copy image address', click: () => clipboard.writeText(p.srcURL) });
    }
    if (p.selectionText) {
      items.push({ role: 'copy' });
      items.push({ label: 'Ask AI about selection', click: () => send('ask-ai', p.selectionText) });
    }
    if (p.isEditable) items.push({ role: 'cut' }, { role: 'paste' }, { role: 'selectAll' });
    if (items.length) items.push({ type: 'separator' });
    items.push({ label: 'Back', enabled: wc.canGoBack(), click: () => wc.goBack() },
      { label: 'Forward', enabled: wc.canGoForward(), click: () => wc.goForward() },
      { label: 'Reload', click: () => wc.reload() },
      { type: 'separator' },
      { label: 'Inspect', click: () => wc.inspectElement(p.x, p.y) });
    Menu.buildFromTemplate(items).popup({ window: win });
  });
});

// ---------- IPC: state ----------
ipcMain.handle('state:get', () => Object.assign({}, data, { aiKeys: undefined }));
ipcMain.on('state:set', (e, k, v) => {
  data[k] = v; persist();
  if (k === 'settings') applyProxy();
});
ipcMain.handle('paths', () => ({ webPreload: pathToFileURL(path.join(__dirname, 'preload-web.js')).href, version: app.getVersion() }));
ipcMain.on('adblock:enabled', e => { e.returnValue = adsOn(); });
ipcMain.on('adblock:js', e => { e.returnValue = ads.JS; });
ipcMain.on('theme:set', (e, t) => {
  const c = THEMES[t === true ? 1 : t === false ? 0 : (t | 0)] || THEMES[0];
  try { if (win) win.setTitleBarOverlay({ color: c.bg, symbolColor: c.fg }); } catch (err) { }
  if (win) win.setBackgroundColor(c.bg);
});
ipcMain.on('win:toggleFull', () => { if (win) win.setFullScreen(!win.isFullScreen()); });
ipcMain.handle('open-external', (e, url) => shell.openExternal(url));
ipcMain.handle('clear-data', async () => {
  for (const n of PARTS) { const s = session.fromPartition(n); await s.clearStorageData(); await s.clearCache(); }
  return true;
});

// ---------- IPC: pictures ----------
const IMG_FILTERS = [{ name: 'Images', extensions: ['jpg', 'jpeg', 'png', 'webp', 'gif', 'bmp'] }];
function saveImage(src, name, maxW) {
  let img = nativeImage.createFromPath(src);
  if (img.isEmpty()) return null;
  if (img.getSize().width > maxW) img = img.resize({ width: maxW, quality: 'best' });
  const dir = path.join(app.getPath('userData'), 'img');
  fs.mkdirSync(dir, { recursive: true });
  const out = path.join(dir, name + '-' + Date.now() + '-' + Math.random().toString(36).slice(2, 6) + '.jpg');
  fs.writeFileSync(out, img.toJPEG(90));
  return pathToFileURL(out).href;
}
ipcMain.handle('img:pick', async (e, name, maxW) => {
  const r = await dialog.showOpenDialog(win, { properties: ['openFile'], filters: IMG_FILTERS });
  if (r.canceled || !r.filePaths[0]) return null;
  return saveImage(r.filePaths[0], name, maxW);
});
ipcMain.handle('img:pickMany', async (e, name, maxW) => {
  const r = await dialog.showOpenDialog(win, { properties: ['openFile', 'multiSelections'], filters: IMG_FILTERS });
  if (r.canceled) return [];
  return r.filePaths.slice(0, 12).map(p => saveImage(p, name, maxW)).filter(Boolean);
});
ipcMain.handle('img:fromPaths', (e, paths, name, maxW) =>
  (paths || []).filter(p => /\.(jpe?g|png|webp|gif|bmp)$/i.test(p)).slice(0, 12).map(p => saveImage(p, name, maxW)).filter(Boolean));

// ---------- IPC: AI ----------
const PROV = {
  openai: 'https://api.openai.com/v1/chat/completions',
  anthropic: 'https://api.anthropic.com/v1/messages',
  gemini: 'https://generativelanguage.googleapis.com/v1beta/models/',
  xai: 'https://api.x.ai/v1/chat/completions',
  pplx: 'https://api.perplexity.ai/chat/completions'
};
function getKey(id) {
  const v = (data.aiKeys || {})[id];
  if (!v) return '';
  try { return v.startsWith('p:') ? v.slice(2) : safeStorage.decryptString(Buffer.from(v, 'base64')); } catch (e) { return ''; }
}
ipcMain.handle('ai:hasKey', (e, id) => !!getKey(id));
ipcMain.handle('ai:setKey', (e, id, key) => {
  data.aiKeys = data.aiKeys || {};
  if (!key) delete data.aiKeys[id];
  else data.aiKeys[id] = safeStorage.isEncryptionAvailable() ? safeStorage.encryptString(key).toString('base64') : 'p:' + key;
  persist(); return true;
});
ipcMain.handle('ai:ask', async (e, id, model, hist) => {
  const key = getKey(id);
  if (!key) throw new Error('No API key set');
  const sys = 'You are Storm AI, a concise assistant built into a web browser. Answer clearly in plain text; avoid heavy markdown.';
  let url = PROV[id]; const headers = { 'Content-Type': 'application/json' }; let body;
  if (id === 'anthropic') {
    headers['x-api-key'] = key; headers['anthropic-version'] = '2023-06-01';
    body = { model, max_tokens: 1500, system: sys, messages: hist.map(h => ({ role: h.role, content: h.text })) };
  } else if (id === 'gemini') {
    url += model + ':generateContent'; headers['x-goog-api-key'] = key;
    body = { systemInstruction: { parts: [{ text: sys }] }, contents: hist.map(h => ({ role: h.role === 'assistant' ? 'model' : 'user', parts: [{ text: h.text }] })) };
  } else {
    headers.Authorization = 'Bearer ' + key;
    body = { model, messages: [{ role: 'system', content: sys }, ...hist.map(h => ({ role: h.role, content: h.text }))] };
  }
  const r = await fetch(url, { method: 'POST', headers, body: JSON.stringify(body) });
  const raw = await r.text(); let j = null;
  try { j = JSON.parse(raw); } catch (err) { }
  if (!r.ok) {
    const er = j && j.error;
    throw new Error((er && er.message) || (typeof er === 'string' ? er : '') || raw.slice(0, 200) || 'HTTP ' + r.status);
  }
  let out = '';
  if (id === 'anthropic') out = (j.content || []).filter(b => b.type === 'text').map(b => b.text).join('');
  else if (id === 'gemini') out = ((((j.candidates || [])[0] || {}).content || {}).parts || []).map(p => p.text || '').join('');
  else {
    out = (((j.choices || [])[0] || {}).message || {}).content || '';
    if (Array.isArray(j.citations) && j.citations.length) out += '\n\nSources:\n' + j.citations.slice(0, 6).map((c, i) => (i + 1) + '. ' + c).join('\n');
  }
  return out.trim() || '(empty reply)';
});

// ---------- IPC: updates ----------
ipcMain.handle('update:check', async () => {
  const r = await fetch('https://api.github.com/repos/' + REPO + '/releases/latest', { headers: { 'User-Agent': 'Storm-Browser', Accept: 'application/vnd.github+json' } });
  if (!r.ok) throw new Error('HTTP ' + r.status);
  const j = await r.json();
  const asset = (j.assets || []).find(a => /\.exe$/i.test(a.name));
  return { tag: j.tag_name, notes: j.body || '', url: asset ? asset.browser_download_url : null, current: app.getVersion() };
});
ipcMain.handle('update:install', async (e, url) => {
  const dest = path.join(app.getPath('temp'), 'Storm-Update.exe');
  const r = await fetch(url, { redirect: 'follow', headers: { 'User-Agent': 'Storm-Browser' } });
  if (!r.ok) throw new Error('Download failed (' + r.status + ')');
  const total = Number(r.headers.get('content-length')) || 0;
  const out = fs.createWriteStream(dest); const reader = r.body.getReader(); let got = 0, last = -1;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    out.write(Buffer.from(value)); got += value.length;
    if (total) { const pc = Math.floor(got * 100 / total); if (pc !== last) { last = pc; send('update-progress', pc); } }
  }
  await new Promise(res => out.end(res));
  if (got < 1e6) throw new Error('Bad download');
  spawn(dest, [], { detached: true, stdio: 'ignore' }).unref();
  setTimeout(() => app.quit(), 400);
  return true;
});

// ---------- app ----------
if (!app.requestSingleInstanceLock()) { app.quit(); }
else {
  app.on('second-instance', (e, argv) => {
    if (win) { if (win.isMinimized()) win.restore(); win.focus(); }
    const u = argv.find(a => /^https?:\/\//i.test(a)); if (u) send('open-tab', u);
  });
  app.setAppUserModelId('com.storm.browser.desktop');
  app.whenReady().then(async () => {
    loadData(); blocked = data.blockedTotal || 0;
    PARTS.forEach(setupSession);
    await applyProxy();
    createWindow();
    const u = process.argv.find(a => /^https?:\/\//i.test(a));
    if (u) win.webContents.once('did-finish-load', () => send('open-tab', u));
    setInterval(() => {
      if (blocked !== lastSent) { lastSent = blocked; send('blocked', blocked); data.blockedTotal = blocked; if (blocked % 25 === 0) persist(); }
    }, 1000);
  });
  app.on('before-quit', () => { data.blockedTotal = blocked; try { fs.writeFileSync(dataFile(), JSON.stringify(data)); } catch (e) { } });
  app.on('window-all-closed', () => app.quit());
}
