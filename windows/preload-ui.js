'use strict';
const { contextBridge, ipcRenderer, webUtils } = require('electron');

// dropped image files -> real paths (File objects cannot cross into the page, so post the paths)
window.addEventListener('dragover', e => e.preventDefault());
window.addEventListener('drop', e => {
  e.preventDefault();
  try {
    const paths = Array.from(e.dataTransfer.files).map(f => webUtils.getPathForFile(f)).filter(Boolean);
    if (paths.length) window.postMessage({ stormDrop: paths }, '*');
  } catch (err) { }
});

contextBridge.exposeInMainWorld('storm', {
  state: () => ipcRenderer.invoke('state:get'),
  set: (k, v) => ipcRenderer.send('state:set', k, v),
  paths: () => ipcRenderer.invoke('paths'),
  fullscreen: () => ipcRenderer.send('win:toggleFull'),
  theme: oled => ipcRenderer.send('theme:set', oled),
  pickImage: (name, w) => ipcRenderer.invoke('img:pick', name, w),
  pickImages: (name, w) => ipcRenderer.invoke('img:pickMany', name, w),
  saveImageData: (buf, name, w) => ipcRenderer.invoke('img:fromBuffer', buf, name, w),
  importImages: (paths, name, w) => ipcRenderer.invoke('img:fromPaths', paths, name, w),
  aiHasKey: id => ipcRenderer.invoke('ai:hasKey', id),
  aiSetKey: (id, key) => ipcRenderer.invoke('ai:setKey', id, key),
  aiAsk: (id, model, hist) => ipcRenderer.invoke('ai:ask', id, model, hist),
  updateCheck: () => ipcRenderer.invoke('update:check'),
  updateInstall: url => ipcRenderer.invoke('update:install', url),
  clearData: () => ipcRenderer.invoke('clear-data'),
  openExternal: url => ipcRenderer.invoke('open-external', url),
  on: (ch, fn) => ipcRenderer.on(ch, (e, ...a) => fn(...a))
});
