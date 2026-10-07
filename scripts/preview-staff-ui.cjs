// Read-only UI fixture server. No Firebase/bank access; not a replacement for backend security tests.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../The-Golden-Leaf-server/src/main/resources/static');
const fixtures = {
  '/api/auth/web-config': {apiKey: ''},
  '/api/auth/me': {uid: 'ui-fixture', roles: ['ADMIN']},
  '/api/staff/bookings': [
    {id: 101, customer_name: 'Khách demo A', booking_date: '2026-10-08', slot_label: '11:00-15:00', guest_count: 12, reserved_tables: 2, preferred_area_name: 'Trong nhà', status: 'CONFIRMED', payment_status: 'PENDING', amount: 1250000},
    {id: 102, customer_name: 'Khách demo B', booking_date: '2026-10-08', slot_label: '11:00-15:00', guest_count: 4, reserved_tables: 1, preferred_area_name: 'Ven sông', status: 'ASSIGNED', payment_status: 'PAID', amount: 650000},
    {id: 103, customer_name: 'Khách demo C', booking_date: '2026-10-08', slot_label: '19:00-23:00', guest_count: 8, reserved_tables: 1, preferred_area_name: 'Phòng riêng', status: 'SEATED', payment_status: 'PAID', amount: 1850000}
  ],
  '/api/staff/tables': [
    {id: 1, code: 'R01', area: 'Ven sông', capacity: 8, active: true},
    {id: 2, code: 'A01', area: 'Trong nhà', capacity: 8, active: true},
    {id: 3, code: 'A02', area: 'Trong nhà', capacity: 8, active: true},
    {id: 4, code: 'P01', area: 'Phòng riêng', capacity: 8, active: true}
  ],
  '/api/staff/areas': [{id: 1, name: 'Trong nhà'}, {id: 2, name: 'Ngoài trời'}, {id: 3, name: 'Ven sông'}, {id: 4, name: 'Phòng riêng'}],
  '/api/staff/allocations': [{booking_id: 102, table_id: 1, booking_date: '2026-10-08', slot_label: '11:00-15:00'}],
  '/api/staff/refunds': [{booking_id: 99, amount: 500000, status: 'REFUND_REQUIRED'}],
  '/api/staff/deliveries': [{id: 5, status: 'DELIVERED', attempts: 1}, {id: 6, status: 'FAILED', attempts: 5, last_error: 'UNAVAILABLE'}],
  '/api/admin/menu': [{idThucDon: 1, tenMon: 'Súp nấm', gia: 65000, nhom: 'KHAI_VI', active: true, moTa: 'Dữ liệu giao diện minh họa'}]
};
http.createServer((req, res) => {
  const url = new URL(req.url, 'http://127.0.0.1:18086');
  if (req.method !== 'GET') {
    res.writeHead(403, {'Content-Type': 'application/json'});
    res.end(JSON.stringify({message: 'Bản xem trước chỉ đọc, không thực hiện thao tác.'}));
    return;
  }
  if (fixtures[url.pathname]) {
    res.writeHead(200, {'Content-Type': 'application/json'});
    res.end(JSON.stringify(fixtures[url.pathname]));
    return;
  }
  if (url.pathname === '/fixture.js') {
    res.writeHead(200, {'Content-Type': 'text/javascript'});
    res.end("token='UI_FIXTURE_ONLY';profile={roles:['ADMIN']};el('login').hidden=true;el('workspace').hidden=false;say('BẢN XEM TRƯỚC · Dữ liệu giả · Mọi API ghi đều bị chặn');refresh().catch(e=>say(e.message));");
    return;
  }
  const file = url.pathname === '/' ? 'staff.html' : url.pathname.slice(1);
  if (!['staff.html', 'staff.js', 'staff.css'].includes(file)) {res.writeHead(404);res.end();return;}
  res.setHeader('Content-Security-Policy', "default-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'");
  res.setHeader('Content-Type', file.endsWith('.css') ? 'text/css' : file.endsWith('.js') ? 'text/javascript' : 'text/html; charset=utf-8');
  let body = fs.readFileSync(path.join(root, file), 'utf8');
  if (file === 'staff.html' && url.searchParams.get('view') !== 'login') body = body.replace('</head>', '<script defer src="/fixture.js"></script></head>');
  res.end(body);
}).listen(18086, '127.0.0.1', () => process.stdout.write('Read-only staff UI fixture: http://127.0.0.1:18086/staff.html\n'));
