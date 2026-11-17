'use strict';
// Runs inside every page (webview) before the page's own scripts: ad / tracker cosmetic blocking + YouTube ad removal.
const { webFrame, ipcRenderer } = require('electron');
try {
  if (ipcRenderer.sendSync('adblock:enabled')) webFrame.executeJavaScript(ipcRenderer.sendSync('adblock:js'));
} catch (e) { }
