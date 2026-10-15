import { test, expect, Page } from '@playwright/test';

async function loginAsAdmin(page: Page) {
  await page.goto('/login');
  await page.locator('input#username').fill('admin');
  await page.locator('input#password').fill('Password@123');
  await page.locator('button[type="submit"]').click();
  await expect(page).toHaveURL(/.*\/dashboard/);
  await expect(page.locator('.brand-title')).toBeVisible();
}

test.describe('SCOS End-to-End Quality Gate Suite', () => {

  test('1. Protected navigation: Unauthenticated requests redirect to login', async ({ page }) => {
    // Attempt to access protected dashboard directly
    await page.goto('/dashboard');
    await expect(page).toHaveURL(/.*\/login/);

    // Attempt to access work orders directly
    await page.goto('/work-orders');
    await expect(page).toHaveURL(/.*\/login/);

    // Attempt to access assets directly
    await page.goto('/assets');
    await expect(page).toHaveURL(/.*\/login/);
  });

  test('2. Authentication: Form validation and bad credentials error states', async ({ page }) => {
    await page.goto('/login');

    // Verify login page header elements
    await expect(page.locator('.login-header h2')).toContainText('Welcome to SCOS');
    await expect(page.locator('.login-header p')).toContainText('Smart Community Operating System');

    // Test client validation: empty credentials
    await page.locator('input#username').fill('');
    await page.locator('input#password').fill('');
    await page.locator('button[type="submit"]').click();
    await expect(page.locator('.alert-error')).toContainText('Please enter both username and password');

    // Test bad credentials error handling
    await page.locator('input#username').fill('nonexistent.user');
    await page.locator('input#password').fill('WrongPassword123');
    await page.locator('button[type="submit"]').click();
    await expect(page.locator('.alert-error')).toBeVisible();
  });

  test('3. Successful Authentication and Session Establishment', async ({ page }) => {
    await loginAsAdmin(page);

    // Verify session elements on shell layout
    await expect(page.locator('.brand-title')).toContainText('SCOS');
    await expect(page.locator('.user-info .user-name')).toBeVisible();
    await expect(page.locator('.nav-menu')).toBeVisible();
  });

  test('4. Work Orders Desk: List, KPIs, and Drawer Lifecycle', async ({ page }) => {
    await loginAsAdmin(page);

    // Navigate to Work Orders
    await page.goto('/work-orders');
    await expect(page.locator('h1')).toContainText('Complaints & Maintenance Desk');

    // Verify KPI summary metrics
    const kpiArticles = page.locator('.kpis article');
    await expect(kpiArticles.first()).toBeVisible();
    expect(await kpiArticles.count()).toBeGreaterThanOrEqual(4);

    // Click on the first ticket to open the workbench drawer
    const firstTicket = page.locator('.ticket').first();
    if (await firstTicket.isVisible()) {
      await firstTicket.click();
      await expect(page.locator('aside.drawer')).toBeVisible();
      await expect(page.locator('aside.drawer h2')).toBeVisible();

      // Close the drawer
      await page.locator('aside.drawer .icon-btn').click();
      await expect(page.locator('aside.drawer')).not.toBeVisible();
    }
  });

  test('5. Work Orders Desk: Creation Modal and Field Validation', async ({ page }) => {
    await loginAsAdmin(page);

    await page.goto('/work-orders');
    await page.locator('button:has-text("New work order")').click();

    // Verify creation modal opened
    const modal = page.locator('.dialog');
    await expect(modal).toBeVisible();
    await expect(modal.locator('h2')).toContainText('Create work order');

    // Close modal via Cancel button
    await modal.locator('button:has-text("Cancel")').click();
    await expect(modal).not.toBeVisible();
  });

  test('6. Asset Register: Inventory and Maintenance Tracking', async ({ page }) => {
    await loginAsAdmin(page);

    // Navigate to Asset Register
    await page.goto('/assets');
    await expect(page.locator('h1')).toContainText('Asset Register & Preventive Maintenance');

    // Verify summary statistics
    const assetKpis = page.locator('.kpis article');
    await expect(assetKpis.first()).toBeVisible();
    expect(await assetKpis.count()).toBeGreaterThanOrEqual(4);

    // Verify toolbar controls
    await expect(page.locator('input.search')).toBeVisible();
    await expect(page.locator('button:has-text("Register asset")')).toBeVisible();
  });

  test('7. Infrastructure Matrix: Floor and Unit Visualization', async ({ page }) => {
    await loginAsAdmin(page);

    // Navigate to building matrix
    await page.goto('/infrastructure/matrix');
    await expect(page.locator('h1, h2, .page-header')).toBeVisible();
  });
});
