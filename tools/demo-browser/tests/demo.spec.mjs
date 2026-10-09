import { test, expect } from '@playwright/test';
import { fileURLToPath } from 'node:url';

async function capture(page, filename) {
  if (process.env.CAPTURE_DEMO === 'true') {
    await page.screenshot({ path: fileURLToPath(new URL(`../../../docs/assets/${filename}`, import.meta.url)), fullPage: filename !== 'demo-overview.png' });
  }
}
async function dashboard(page, persona='STAFF') {
  await page.goto('/staff.html');
  await expect(page.locator('#demo-login')).toBeVisible();
  await page.locator('#demo-persona').selectOption(persona);
  await page.getByRole('button', { name:'Vào dashboard demo' }).click();
  await expect(page.locator('#workspace')).toBeVisible();
}

test('customer → staff receipt → assignment → cancellation → refund → admin audit', async ({ page, context }) => {
  const errors=[];page.on('pageerror', error=>errors.push(error.message));
  await page.goto('/demo.html');
  await expect(page.locator('#mode')).toHaveText('● LOCAL DEMO');
  await expect(page.locator('.menu-item')).toHaveCount(12);
  await capture(page,'demo-overview.png');
  await page.getByLabel('Số lượng Salad vườn xanh').fill('2');
  await page.getByRole('button',{name:'Tạo đơn & lưu giỏ hàng'}).click();
  await expect(page.locator('#booking-summary')).toContainText('Đang giữ chỗ');
  const id=(await page.locator('#booking-summary').innerText()).match(/#(\d+)/)[1];
  await page.getByRole('button',{name:'Xác nhận đặt bàn',exact:true}).click();
  await expect(page.locator('#booking-summary')).toContainText('Đã xác nhận');
  await page.getByRole('button',{name:'Tạo thanh toán demo'}).click();
  await expect(page.locator('#booking-summary')).toContainText('340.000 VND');
  await expect(page.locator('#booking-summary')).toContainText('Chờ đối soát');
  await capture(page,'demo-booking.png');

  const staff=await context.newPage();staff.on('dialog', dialog=>dialog.accept());
  await dashboard(staff);
  const row=staff.locator('#bookings tr').filter({has:staff.getByText(`#${id}`,{exact:true})});
  await row.getByRole('button',{name:'Đối soát',exact:true}).click();
  await expect(staff.locator('#transfer-amount')).toHaveValue('340000');
  await staff.locator('#transfer-reference').fill(`DEMO-RECEIPT-${id}`);
  await staff.getByRole('button',{name:'Ghi nhận giao dịch thực tế'}).click();
  await expect(row).toContainText('Đã nhận tiền');
  await row.getByRole('button',{name:'Phân bàn',exact:true}).click();
  await expect(staff.locator('#assignment-dialog')).toBeVisible();
  await staff.locator('#assignment-options input:not(:disabled)').first().check();
  await staff.getByRole('button',{name:'Phân bàn đã chọn'}).click();
  await expect(row).toContainText('Đã phân bàn');
  await capture(staff,'demo-dashboard.png');

  await page.getByRole('button',{name:'Làm mới đơn'}).click();
  await expect(page.locator('#booking-summary')).toContainText('Đã phân bàn');
  await page.getByRole('button',{name:'Hủy đơn',exact:true}).click();
  await expect(page.locator('#booking-summary')).toContainText('Chờ hoàn demo');
  await staff.locator('#transfer-booking').fill(id);
  await staff.locator('#transfer-kind').selectOption('verify-refund');
  await staff.locator('#transfer-reference').fill(`DEMO-REFUND-${id}`);
  await staff.getByRole('button',{name:'Ghi nhận giao dịch thực tế'}).click();
  await expect(staff.locator('#message')).toContainText('Đã ghi nhận đối soát');
  await page.getByRole('button',{name:'Làm mới đơn'}).click();
  await expect(page.locator('#booking-summary')).toContainText('Đã hoàn demo');

  const admin=await context.newPage();await dashboard(admin,'ADMIN');
  await admin.locator('#admin-panel summary').click();
  await admin.getByRole('button',{name:'Xem audit gần nhất'}).click();
  await expect(admin.locator('#audit-content')).toContainText('VERIFY_REFUND');
  expect(await page.evaluate(()=>({local:localStorage.length,session:sessionStorage.length}))).toEqual({local:0,session:0});
  expect(errors).toEqual([]);
});

test('seeded today booking supports staff check-in and completion', async ({ page }) => {
  page.on('dialog',dialog=>dialog.accept());await dashboard(page);
  const row=page.locator('#bookings tr').filter({has:page.getByRole('button',{name:'Nhận khách',exact:true})}).first();
  const id=(await row.innerText()).match(/#(\d+)/)[1];
  await row.getByRole('button',{name:'Nhận khách',exact:true}).click();
  const seated=page.locator('#bookings tr').filter({has:page.getByText(`#${id}`,{exact:true})});
  await expect(seated).toContainText('Đang phục vụ');
  await seated.getByRole('button',{name:'Hoàn tất',exact:true}).click();
  await expect(page.locator('#bookings tr').filter({has:page.getByText(`#${id}`,{exact:true})})).toHaveCount(0);
  await page.locator('#lookup-id').fill(id);await page.getByRole('button',{name:'Tra cứu chi tiết'}).click();
  await expect(page.locator('#booking-detail')).toContainText('COMPLETED');
});

test('mobile-width demo renders without horizontal overflow or browser errors', async ({ page }) => {
  await page.setViewportSize({width:390,height:844});
  const errors=[];page.on('pageerror',error=>errors.push(error.message));
  await page.goto('/demo.html');await expect(page.locator('#mode')).toHaveText('● LOCAL DEMO');
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
  await capture(page,'demo-mobile.png');expect(errors).toEqual([]);
});
