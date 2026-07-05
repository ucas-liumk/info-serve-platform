#!/usr/bin/env python3
"""UI/UX 评审素材采集：登录后逐页截图（桌面 + 首页移动端）。"""
import os, pathlib
from playwright.sync_api import sync_playwright

BASE = os.environ.get('UI_CAPTURE_BASE', 'http://127.0.0.1:7010')
OUT = pathlib.Path(os.environ.get('UI_CAPTURE_OUT', 'output/ui-capture'))
OUT.mkdir(parents=True, exist_ok=True)

PAGES = [
    ('portal-home', '/portal', 3500),
    ('portal-resources', '/portal/resources', 3000),
    ('portal-tools', '/portal/tools', 3000),
    ('portal-forum', '/portal/forum', 3000),
    ('admin-home', '/admin', 3500),
    ('admin-module-registry', '/admin/portal-apps/module', 2500),
    ('admin-app-manage', '/admin/portal-apps/application', 3000),
]

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    ctx = browser.new_context(viewport={'width': 1440, 'height': 900}, device_scale_factor=2)
    page = ctx.new_page()

    # 登录页先拍（未登录态）
    page.goto(f'{BASE}/login', wait_until='networkidle')
    page.wait_for_timeout(1200)
    page.screenshot(path=str(OUT / 'login.png'))

    # 登录
    page.fill('input[placeholder*="账号"], input[placeholder*="用户名"]', 'admin')
    page.fill('input[type="password"]', 'admin123')
    with page.expect_response(lambda r: '/auth/login' in r.url, timeout=15000):
        page.click('button:has-text("登")')
    page.wait_for_url(lambda u: '/login' not in u, timeout=15000)
    page.wait_for_load_state('networkidle')

    for name, path, wait in PAGES:
        page.goto(f'{BASE}{path}', wait_until='networkidle')
        page.wait_for_timeout(wait)
        page.screenshot(path=str(OUT / f'{name}.png'))
        print('拍摄', name, page.url)

    # 论坛话题详情抽屉（交互态）
    page.goto(f'{BASE}/portal/forum', wait_until='networkidle')
    page.wait_for_timeout(2500)
    cards = page.locator('.topic-card')
    if cards.count() > 0:
        cards.first.click()
        page.wait_for_timeout(2000)
        page.screenshot(path=str(OUT / 'portal-forum-detail.png'))
        print('拍摄 forum-detail')

    # 首页移动端
    mob = browser.new_context(viewport={'width': 390, 'height': 844}, device_scale_factor=2, storage_state=ctx.storage_state())
    mp = mob.new_page()
    mp.goto(f'{BASE}/portal', wait_until='networkidle')
    mp.wait_for_timeout(3000)
    mp.screenshot(path=str(OUT / 'portal-home-mobile.png'), full_page=True)
    print('拍摄 mobile home')

    browser.close()
print('完成 ->', OUT)
