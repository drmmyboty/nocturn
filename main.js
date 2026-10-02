const { app, BrowserWindow } = require('electron');
function createWindow() {
  const win = new BrowserWindow({ width: 480, height: 900, backgroundColor: '#000000', autoHideMenuBar: true, title: 'Nocturn', icon: __dirname + '/build/icon.ico' });
  win.loadFile('index.html');
}
app.whenReady().then(createWindow);
app.on('window-all-closed', () => app.quit());
