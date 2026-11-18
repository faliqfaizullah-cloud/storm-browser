'use strict';
const { contextBridge, ipcRenderer } = require('electron');
contextBridge.exposeInMainWorld('storm', {
  state: () => ipcRenderer.invoke('state:get'),
  set: (k, v) => ipcRenderer.send('state:set', k, v),
  paths: () => ipcRenderer.invoke('paths'),
  fullscreen: () => ipcRenderer.send('win:toggleFull'),
  theme: oled => ipcRenderer.send('theme:set', oled),
  pickImage: (name, w) => ipcRenderer.invoke('img:pick', name, w),
  aiHasKey: id => ipcRenderer.invoke('ai:hasKey', id),
  aiSetKey: (id, key) => ipcRenderer.invoke('ai:setKey', id, key),
  aiAsk: (id, model, hist) => ipcRenderer.invoke('ai:ask', id, model, hist),
  updateCheck: () => ipcRenderer.invoke('update:check'),
  updateInstall: url => ipcRenderer.invoke('update:install', url),
  clearData: () => ipcRenderer.invoke('clear-data'),
  openExternal: url => ipcRenderer.invoke('open-external', url),
  on: (ch, fn) => ipcRenderer.on(ch, (e, ...a) => fn(...a))
});
