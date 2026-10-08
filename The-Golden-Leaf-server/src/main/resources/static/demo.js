'use strict';
// Same-origin, in-memory tokens only. No Firebase, financial network or persistent browser storage.
const el = id => document.getElementById(id);
const money = value => new Intl.NumberFormat('vi-VN').format(value) + ' VND';
let token = null, booking = null, payment = null, menu = [], slots = [], config = null;
let busy = false;
const names = {HOLDING:'Đang giữ chỗ',CONFIRMED:'Đã xác nhận',ASSIGNED:'Đã phân bàn',SEATED:'Đang phục vụ',COMPLETED:'Hoàn tất',CANCELLED:'Đã hủy',EXPIRED:'Hết giữ chỗ',PENDING:'Chờ đối soát',PAID:'Đã đối soát demo',REFUND_REQUIRED:'Chờ hoàn demo',REFUNDED:'Đã hoàn demo'};
async function api(path, body, method='GET', extra={}) {
  const response = await fetch(path, {method, credentials:'omit', headers:{...(token ? {Authorization:`Bearer ${token}`} : {}), ...(body !== undefined ? {'Content-Type':'application/json'} : {}), ...extra}, ...(body !== undefined ? {body:JSON.stringify(body)} : {})});
  const text = await response.text(); let data;
  try { data = text ? JSON.parse(text) : null; } catch { throw Error('Backend trả dữ liệu không hợp lệ.'); }
  if (!response.ok) throw Error(`${data?.code || response.status}: ${data?.message || 'Yêu cầu bị từ chối'}`);
  return data;
}
function controls() {
  el('create').disabled=busy||!token||!!booking;
  el('confirm').disabled=busy||booking?.status!=='HOLDING';
  el('payment').disabled=busy||!['CONFIRMED','ASSIGNED'].includes(booking?.status)||!!payment;
  el('cancel').disabled=busy||!['HOLDING','CONFIRMED','ASSIGNED'].includes(booking?.status);
  el('refresh').disabled=busy||!booking;
}
async function action(task) {
  if(busy)return;busy=true;controls();
  try {await task();} catch(error) {el('message').textContent=error.message;}
  finally {busy=false;controls();}
}
function summary() {
  const values = booking ? [['Mã đơn',`#${booking.idDat}`],['Lịch',`${booking.ngay} · ${booking.khungGio}`],['Sức chứa',`${booking.soLuong} khách · ${booking.reservedTables} bàn`],['Đặt bàn',names[booking.status]||booking.status],...(payment ? [['Thanh toán',names[payment.status]||payment.status],['Tổng do server tính',money(payment.amount)],['Nội dung demo',payment.reference]] : [])] : [['Đơn đặt bàn','Chưa tạo']];
  el('booking-summary').replaceChildren();
  for (const [label,value] of values) {const row=document.createElement('div'),dt=document.createElement('dt'),dd=document.createElement('dd');dt.textContent=label;dd.textContent=value;row.append(dt,dd);el('booking-summary').append(row);}
}
async function inbox() {
  const items=await api('/api/notifications');el('inbox').replaceChildren();
  for(const item of items){const li=document.createElement('li');li.textContent=item.message;el('inbox').append(li);}
}
function selectSlots() {
  el('slot').replaceChildren();
  slots.filter(s=>s.ngay===el('date').value&&s.soBanConLai>0).forEach(s=>{const option=document.createElement('option');option.value=s.khungGio;option.textContent=`${s.khungGio} · ${s.soBanConLai} bàn`;el('slot').append(option);});
}
async function refresh() {
  booking=await api(`/api/datban/${booking.idDat}`);
  if(payment)payment=await api(`/api/payments/bookings/${booking.idDat}`);
  summary();await inbox();
}
el('date').onchange=selectSlots;
el('booking-form').onsubmit=e=>{e.preventDefault();action(async()=>{
  const payload={email:'customer@example.invalid',ten:'Khách demo',ngay:el('date').value,khungGio:el('slot').value,soLuong:Number(el('guests').value),viTriBan:'Trong nhà',ghiChu:'Portfolio walkthrough — không nhận tiền thật'};
  // Reuse the exact payload/key after an uncertain network response; never create a duplicate silently.
  const pending=el('booking-form').pending || {payload,key:crypto.randomUUID()};
  el('booking-form').pending=pending;
  booking=await api('/api/datban/save',pending.payload,'POST',{'Idempotency-Key':pending.key});
  const items=menu.map(m=>({...m,quantity:Number(el(`qty-${m.idThucDon}`).value)})).filter(m=>m.quantity>0).map(m=>({idDat:booking.idDat,idThucDon:m.idThucDon,tenMon:m.tenMon,soLuong:m.quantity,giaMon:m.gia}));
  try {await api(`/api/giohang/${booking.idDat}`,items,'PUT');el('message').textContent='Đã giữ chỗ và lưu giỏ hàng. Hãy xác nhận đặt bàn.';}
  catch(error){el('message').textContent=`Đã tạo đơn #${booking.idDat}, nhưng giỏ hàng chưa lưu: ${error.message}. Hủy và khởi động lại demo để thử lại.`;}
  summary();await inbox();
});};
el('confirm').onclick=()=>action(async()=>{booking=await api(`/api/datban/${booking.idDat}/confirm`,{},'POST');summary();await inbox();el('message').textContent='Đã xác nhận. Tạo thanh toán demo để xem hóa đơn do server tính.';});
el('payment').onclick=()=>action(async()=>{payment=await api(`/api/payments/bookings/${booking.idDat}`,{},'POST');await refresh();el('message').textContent='Thanh toán giả lập đã tạo. Mở dashboard → vào vai nhân viên → đối soát. Không chuyển tiền thật.';});
el('cancel').onclick=()=>action(async()=>{booking=await api(`/api/datban/${booking.idDat}/cancel`,{},'POST');await refresh();el('message').textContent='Đã hủy đơn; nếu đã đối soát, khoản tiền demo chuyển sang chờ hoàn.';});
el('refresh').onclick=()=>action(refresh);
action(async()=>{
  config=await api('/api/demo/config');if(config.demo!==true)throw Error('Đây không phải demo sandbox.');
  const session=await api('/api/demo/session',{persona:'CUSTOMER'},'POST');token=session.token;
  [menu,slots]=await Promise.all([api('/api/thucdon'),api('/api/ban-slot')]);
  // Tomorrow avoids already-started slots and makes the walkthrough reproducible at any hour.
  [...new Set(slots.filter(s=>s.ngay>config.today).map(s=>s.ngay))].sort().forEach(date=>{const option=document.createElement('option');option.value=date;option.textContent=date;el('date').append(option);});selectSlots();
  menu.forEach(m=>{const row=document.createElement('div');row.className='menu-item';const detail=document.createElement('div'),name=document.createElement('p'),price=document.createElement('small'),label=document.createElement('label'),input=document.createElement('input');name.textContent=m.tenMon;price.textContent=money(m.gia);detail.append(name,price);input.type='number';input.min='0';input.max='10';input.value='0';input.id=`qty-${m.idThucDon}`;input.setAttribute('aria-label',`Số lượng ${m.tenMon}`);label.append(input);row.append(detail,label);el('menu').append(row);});
  el('mode').textContent='● LOCAL DEMO';el('message').textContent='Sandbox sẵn sàng. Chọn lịch, số khách và món để thử luồng đặt bàn.';await inbox();
});
