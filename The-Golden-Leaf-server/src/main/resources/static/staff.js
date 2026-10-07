'use strict';
// No localStorage/sessionStorage: a reload intentionally requires a new sign-in.
let token = null;
let assigning = null;
let profile = null;
let menuItems = [];
const el = id => document.getElementById(id);
const statusName = code => ({HOLDING:'Đang giữ chỗ',CONFIRMED:'Đã xác nhận',ASSIGNED:'Đã phân bàn',SEATED:'Đang phục vụ',COMPLETED:'Hoàn tất',CANCELLED:'Đã hủy',EXPIRED:'Hết giữ chỗ',PENDING:'Chờ đối soát',PAID:'Đã nhận tiền',REFUND_REQUIRED:'Chờ hoàn tiền',REFUNDED:'Đã hoàn tiền',DELIVERED:'Đã gửi',FAILED:'Gửi lỗi',SENDING:'Đang gửi'})[code] || code;
const money = amount => new Intl.NumberFormat('vi-VN',{maximumFractionDigits:2}).format(amount||0)+' VND';
const say = value => { el('message').textContent = value; };
async function api(path, body, method) {
  const response = await fetch(path, {method: method || (body ? 'POST' : 'GET'), credentials:'omit', headers:{Authorization:`Bearer ${token}`, ...(body ? {'Content-Type':'application/json'} : {})}, ...(body ? {body:JSON.stringify(body)} : {})});
  const text = await response.text(); const data = text ? JSON.parse(text) : null;
  if (!response.ok) {
    if(response.status===401) logout();
    throw new Error(data?.message || `Yêu cầu bị từ chối (${response.status})`);
  }
  return data;
}
function logout() { token=null;profile=null;assigning=null;menuItems=[];el('assignment-dialog').close();el('workspace').hidden=true;el('login').hidden=false;el('password').value='';el('bookings').replaceChildren();el('audit-content').textContent='';el('booking-detail').textContent=''; }
async function action(task) {
  const buttons=[...document.querySelectorAll('button')];buttons.forEach(b=>b.disabled=true);
  try{await task();}catch(e){say(e.message);}finally{buttons.forEach(b=>b.disabled=false);}
}
function button(label, handler) { const b=document.createElement('button');b.textContent=label;b.onclick=()=>action(handler);return b; }
function cell(row,value) {const td=document.createElement('td');td.textContent=value;row.append(td);return td;}
async function refresh() {
  const query=el('booking-date').value ? `?date=${encodeURIComponent(el('booking-date').value)}` : '';
  const [bookings,tables,areas,refunds,deliveries,allocations]=await Promise.all([`/api/staff/bookings${query}`,'/api/staff/tables','/api/staff/areas','/api/staff/refunds','/api/staff/deliveries','/api/staff/allocations'].map(p=>api(p)));
  el('bookings').replaceChildren();
  bookings.forEach(b=>{
    const row=document.createElement('tr');cell(row,`#${b.id}`);cell(row,`${b.customer_name}\n${b.booking_date} · ${b.slot_label}`);
    cell(row,`${b.guest_count} khách / ${b.reserved_tables} bàn · ${b.preferred_area_name}`);cell(row,statusName(b.status));cell(row,`${b.payment_status?statusName(b.payment_status):'Chưa tạo'}\n${money(b.amount)}`);
    const tools=cell(row,'');
    tools.append(button('Chi tiết',async()=>{el('booking-detail').textContent=JSON.stringify(await api(`/api/staff/bookings/${b.id}`),null,2);el('booking-detail').scrollIntoView({behavior:'smooth'});}));
    if(b.status==='CONFIRMED') tools.append(button('Phân bàn',async()=>{
      assigning=b;el('assignment-title').textContent=`Phân bàn · Đơn #${b.id}`;
      el('assignment-hint').textContent=`${b.guest_count} khách · chọn ${b.reserved_tables} bàn · ${b.booking_date} ${b.slot_label} · ưu tiên ${b.preferred_area_name}`;
      el('assignment-options').replaceChildren();tables.forEach(t=>{
        const occupied=allocations.some(a=>a.table_id===t.id&&a.booking_date===b.booking_date&&a.slot_label===b.slot_label);
        const label=document.createElement('label');label.className='table-card';const input=document.createElement('input');input.type='checkbox';input.value=t.id;input.name='table';input.disabled=occupied||!t.active;
        label.append(input,document.createTextNode(`${t.code} · ${t.area} · ${t.capacity} ghế${occupied?' · đã phân':''}`));el('assignment-options').append(label);
      });el('assignment-dialog').showModal();
    }));
    if(b.status==='ASSIGNED')tools.append(button('Nhận khách',async()=>{await api(`/api/staff/bookings/${b.id}/check-in`,{});await refresh();say('Đã nhận khách.');}));
    if(b.status==='SEATED')tools.append(button('Hoàn tất',async()=>{if(!confirm(`Hoàn tất phục vụ đơn #${b.id}?`))return;await api(`/api/staff/bookings/${b.id}/complete`,{});await refresh();say('Đã hoàn tất phục vụ.');}));
    if(b.payment_status==='PENDING')tools.append(button('Đối soát',async()=>{el('transfer-booking').value=b.id;el('transfer-amount').value=b.amount;el('transfer-kind').value='verify-payment';el('transfer-form').scrollIntoView({behavior:'smooth'});}));
    if(['HOLDING','CONFIRMED','ASSIGNED'].includes(b.status))tools.append(button('Hủy đơn',async()=>{if(!confirm(`Hủy đơn #${b.id}? Tiền đã thu sẽ chuyển sang chờ hoàn.`))return;await api(`/api/staff/bookings/${b.id}/cancel`,{});await refresh();say('Đã hủy đơn và cập nhật đối soát.');}));
    el('bookings').append(row);
  });
  el('tables').replaceChildren();tables.forEach(t=>{const card=document.createElement('div');card.className='table-card';card.textContent=`ID ${t.id} · ${t.code} · ${t.capacity} ghế · ${t.area}${t.active?'':' · ngừng hoạt động'}`;el('tables').append(card);});
  el('area').replaceChildren();areas.forEach(a=>{const option=document.createElement('option');option.value=a.id;option.textContent=a.name;el('area').append(option);});
  el('refunds').textContent=refunds.length ? refunds.map(r=>`Đơn #${r.booking_id} · ${money(r.amount)}`).join(' | ') : 'Không có khoản chờ hoàn.';
  el('deliveries').replaceChildren();deliveries.forEach(d=>{const line=document.createElement('p');line.textContent=`#${d.id} · ${statusName(d.status)} · ${d.attempts} lần · ${d.last_error||''}`;if(d.status==='FAILED')line.append(button('Thử lại',async()=>{await api(`/api/staff/deliveries/${d.id}/retry`,{});await refresh();}));el('deliveries').append(line);});
  if(profile?.roles.includes('ADMIN'))await loadMenu();
}
el('login-form').onsubmit=e=>{e.preventDefault();action(async()=>{
  const config=await fetch('/api/auth/web-config',{credentials:'omit'}).then(r=>r.json());
  if(!config.apiKey)throw Error('Chưa cấu hình FIREBASE_WEB_API_KEY. Liên hệ quản trị triển khai.');
  const response=await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${encodeURIComponent(config.apiKey)}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:el('email').value,password:el('password').value,returnSecureToken:true})});
  el('password').value='';const data=await response.json();if(!response.ok)throw Error('Đăng nhập thất bại. Kiểm tra email và mật khẩu.');
  token=data.idToken;
  try {profile=await api('/api/auth/me');el('admin-panel').hidden=!profile.roles.includes('ADMIN');el('reconcile').hidden=!profile.roles.includes('ADMIN');await refresh();el('login').hidden=true;el('workspace').hidden=false;say('Đã đăng nhập. Phiên hết hạn sẽ yêu cầu đăng nhập lại.');}catch(error){logout();throw error;}
});};
el('logout').onclick=()=>{logout();say('Đã đăng xuất.');};el('refresh').onclick=()=>action(refresh);
el('booking-date').onchange=()=>action(refresh);
el('assignment-close').onclick=()=>{el('assignment-dialog').close();assigning=null;};
el('assignment-form').onsubmit=e=>{e.preventDefault();action(async()=>{
  if(!assigning)return;const ids=[...el('assignment-options').querySelectorAll('input:checked')].map(i=>Number(i.value));
  if(ids.length!==assigning.reserved_tables)throw Error(`Cần chọn đúng ${assigning.reserved_tables} bàn.`);
  await api(`/api/staff/bookings/${assigning.id}/assign`,{tableIds:ids});el('assignment-dialog').close();assigning=null;await refresh();say('Đã phân bàn.');
});};
el('transfer-form').onsubmit=e=>{e.preventDefault();action(async()=>{if(!confirm('Đã kiểm tra giao dịch này trong sao kê ngân hàng thực tế?'))return;await api(`/api/staff/bookings/${Number(el('transfer-booking').value)}/${el('transfer-kind').value}`,{reference:el('transfer-reference').value,amount:Number(el('transfer-amount').value)});await refresh();say('Đã ghi nhận đối soát và lưu audit.');el('transfer-reference').value='';});};
el('role-form').onsubmit=e=>{e.preventDefault();action(async()=>{await api('/api/admin/roles',{uid:el('role-uid').value,role:el('role-code').value,grant:el('role-grant').value==='true'});say('Đã cập nhật quyền; áp dụng từ request kế tiếp.');});};
el('table-form').onsubmit=e=>{e.preventDefault();action(async()=>{await api('/api/admin/tables',{areaId:Number(el('area').value),code:el('table-code').value,capacity:Number(el('table-capacity').value)});await refresh();say('Đã khai báo bàn. Cần đối chiếu sức chứa khung giờ trước khi nhận đơn thực tế.');});};
el('audit').onclick=()=>action(async()=>{el('audit-content').textContent=JSON.stringify(await api('/api/admin/audit'),null,2);});
el('reconcile').onclick=()=>action(async()=>{if(!confirm('Đồng bộ sức chứa 7 ngày với số bàn thực tế? Các chỗ đang giữ sẽ được bảo toàn.'))return;const result=await api('/api/admin/inventory/reconcile',{});say(`Đã đồng bộ ${result.physicalTableCount} bàn thực tế cho mỗi khung giờ.`);});
el('lookup-form').onsubmit=e=>{e.preventDefault();action(async()=>{el('booking-detail').textContent=JSON.stringify(await api(`/api/staff/bookings/${Number(el('lookup-id').value)}`),null,2);});};
el('status-form').onsubmit=e=>{e.preventDefault();action(async()=>{if(!confirm('Đổi trạng thái truy cập của tài khoản này?'))return;await api(`/api/admin/users/${encodeURIComponent(el('status-uid').value)}/status`,{active:el('status-active').value==='true'},'PUT');say('Đã cập nhật trạng thái tài khoản.');});};
async function loadMenu(){
  menuItems=await api('/api/admin/menu');const selection=el('menu-choice').value;el('menu-choice').replaceChildren();
  const fresh=document.createElement('option');fresh.value='';fresh.textContent='Món mới';el('menu-choice').append(fresh);
  menuItems.forEach(m=>{const option=document.createElement('option');option.value=m.idThucDon;option.textContent=`${m.tenMon}${m.active?'':' · ngừng phục vụ'}`;el('menu-choice').append(option);});el('menu-choice').value=selection;
}
el('menu-choice').onchange=()=>{
  const item=menuItems.find(m=>String(m.idThucDon)===el('menu-choice').value);
  el('menu-id').value=item?.idThucDon||'';el('menu-name').value=item?.tenMon||'';el('menu-price').value=item?.gia||'';el('menu-group').value=item?.nhom||'MON_CHINH';el('menu-description').value=item?.moTa||'';el('menu-active').checked=item?.active??true;el('menu-image').value='';
};
el('menu-form').onsubmit=e=>{e.preventDefault();action(async()=>{
  const id=el('menu-id').value;const old=menuItems.find(m=>String(m.idThucDon)===id);let image=old?.anh||null;
  const file=el('menu-image').files[0];if(file){if(file.size>5*1024*1024)throw Error('Ảnh vượt quá 5 MB.');const form=new FormData();form.append('image',file);const response=await fetch('/api/admin/menu/image',{method:'POST',credentials:'omit',headers:{Authorization:`Bearer ${token}`},body:form});const result=await response.json();if(!response.ok)throw Error(result.message||'Không tải được ảnh');image=result.url;}
  await api(id?`/api/admin/menu/${id}`:'/api/admin/menu',{tenMon:el('menu-name').value,gia:Number(el('menu-price').value),moTa:el('menu-description').value,anh:image,nhom:el('menu-group').value,active:el('menu-active').checked},id?'PUT':'POST');
  await loadMenu();say('Đã lưu món. Đơn đã chốt vẫn giữ giá snapshot trước đó.');
});};
