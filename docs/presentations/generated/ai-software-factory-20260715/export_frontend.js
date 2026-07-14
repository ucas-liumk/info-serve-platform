const fs = require('fs');
const path = require('path');
const { chromium } = require('playwright');
const PptxGenJS = require('pptxgenjs');

const OUT = '/Users/macmini/windows-info-serve-internal-presentation/docs/presentations/generated/ai-software-factory-20260715';
const HTML = path.join(OUT, 'frontend-slides-light-tech.html');
const PNG = path.join(OUT, 'frontend-render');

(async () => {
  fs.mkdirSync(PNG, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, deviceScaleFactor: 1 });
  await page.goto('file://' + HTML);
  await page.waitForTimeout(500);
  const count = await page.locator('.slide').count();
  for (let i = 0; i < count; i++) {
    await page.evaluate((n) => deck.show(n), i);
    await page.waitForTimeout(700);
    await page.screenshot({ path: path.join(PNG, `slide-${String(i + 1).padStart(2, '0')}.png`) });
  }
  await browser.close();
  const pptx = new PptxGenJS();
  pptx.layout = 'LAYOUT_WIDE';
  pptx.author = '内部信息化自主建设能力探索';
  pptx.title = '从项目实践到软件工厂：Frontend Slides 浅色科技版';
  for (let i = 0; i < count; i++) {
    const slide = pptx.addSlide();
    slide.background = { color: 'FDFAE7' };
    slide.addImage({ path: path.join(PNG, `slide-${String(i + 1).padStart(2, '0')}.png`), x: 0, y: 0, w: 13.333, h: 7.5 });
  }
  await pptx.writeFile({ fileName: path.join(OUT, 'frontend-slides-light-tech.pptx') });
  console.log(`Exported ${count} frontend slides.`);
})().catch((e) => { console.error(e); process.exit(1); });
