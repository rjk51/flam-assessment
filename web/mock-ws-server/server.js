const WebSocket = require('ws');
const wss = new WebSocket.Server({ port: 8080 });
console.log('Mock WS server listening on ws://localhost:8080');

wss.on('connection', (ws) => {
  console.log('Client connected');
  const iv = setInterval(() => {
    const payload = JSON.stringify({
      fps: 28 + Math.floor(Math.random() * 6),
      resolution: '1920x1080',
      mode: Math.random() > 0.5 ? 'edges' : 'raw'
    });
    ws.send(payload);
  }, 1000);

  ws.on('close', () => {
    clearInterval(iv);
    console.log('Client disconnected');
  });
});
