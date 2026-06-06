const http = require('http');
const fs = require('fs');
const path = require('path');

const port = Number(process.env.FRONTEND_PORT || 3000);
const host = process.env.FRONTEND_HOST || '0.0.0.0';
const publicDir = path.join(__dirname, 'public');

const types = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8'
};

const server = http.createServer((req, res) => {
  if (req.url === '/config.js') {
    res.writeHead(200, { 'Content-Type': 'application/javascript; charset=utf-8' });
    res.end(`window.MIO_CONFIG=${JSON.stringify({
      gatewayUrl: process.env.MIO_GATEWAY_URL || 'http://192.168.131.40:8080',
      googleMapsApiKey: process.env.GOOGLE_MAPS_API_KEY || ''
    })};`);
    return;
  }

  const safePath = req.url === '/' ? '/index.html' : decodeURIComponent(req.url.split('?')[0]);
  const filePath = path.normalize(path.join(publicDir, safePath));
  if (!filePath.startsWith(publicDir)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404);
      res.end('Not found');
      return;
    }
    res.writeHead(200, { 'Content-Type': types[path.extname(filePath)] || 'application/octet-stream' });
    res.end(data);
  });
});

server.listen(port, host, () => {
  console.log(`Frontend listo en http://${host}:${port}`);
});
