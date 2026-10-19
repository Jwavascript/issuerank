// PWA 파일(O4 · D29 · AC-13-1 · 13-2 · 13-4): manifest · 아이콘/OG PNG(서명 · IHDR · node:zlib로 풀고 필터를 되돌린 픽셀 — O3 심사 방식)
// · 서비스 워커(public/sw.js를 가짜 self · caches · fetch · clients로 VM에서 실행) · offline.html · index.html · widget.html 원문
import { existsSync, readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { inflateSync } from 'node:zlib';
import { describe, expect, it, vi } from 'vitest';

const read = (path: string) => readFileSync(path, 'utf8');
const BASE = 'https://issuerank-kappa.vercel.app';

// PNG → 폭 · 높이 · RGBA(8비트 · 비월 없음 · 색 형식 2/6만)
function decodePng(file: string) {
  const png = readFileSync(file);
  expect([...png.subarray(0, 8)], file).toEqual([137, 80, 78, 71, 13, 10, 26, 10]);
  expect(png.toString('latin1', 12, 16), file).toBe('IHDR');
  const width = png.readUInt32BE(16), height = png.readUInt32BE(20), depth = png[24], color = png[25], interlace = png[28];
  if (depth !== 8 || ![2, 6].includes(color) || interlace !== 0) throw new Error(`PNG 형식 ${file} ${depth}/${color}/${interlace}`);
  const channels = color === 6 ? 4 : 3;
  const idat: Buffer[] = [];
  for (let at = 8; at < png.length;) {
    const len = png.readUInt32BE(at), type = png.toString('latin1', at + 4, at + 8);
    if (type === 'IDAT') idat.push(png.subarray(at + 8, at + 8 + len));
    at += 12 + len;
  }
  const raw = inflateSync(Buffer.concat(idat));
  const stride = width * channels, rgba = new Uint8Array(width * height * 4);
  let prev = new Uint8Array(stride);
  for (let y = 0; y < height; y++) {
    const f = raw[y * (stride + 1)], line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)), cur = new Uint8Array(stride);
    for (let i = 0; i < stride; i++) {
      const a = i >= channels ? cur[i - channels] : 0, b = prev[i], c = i >= channels ? prev[i - channels] : 0;
      const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
      cur[i] = (line[i] + (f === 0 ? 0 : f === 1 ? a : f === 2 ? b : f === 3 ? (a + b) >> 1 : pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 255;
    }
    for (let x = 0; x < width; x++) {
      const o = (y * width + x) * 4, s = x * channels;
      rgba[o] = cur[s]; rgba[o + 1] = cur[s + 1]; rgba[o + 2] = cur[s + 2]; rgba[o + 3] = channels === 4 ? cur[s + 3] : 255;
    }
    prev = cur;
  }
  const at = (x: number, y: number) => [...rgba.subarray((y * width + x) * 4, (y * width + x) * 4 + 4)];
  return { width, height, rgba, bytes: png.length, at };
}
const INK = [22, 22, 22, 255]; // #161616

describe('manifest · 아이콘 · OG 그림 (AC-13-1 · 13-4)', () => {
  const manifest = JSON.parse(read('public/manifest.webmanifest'));
  const icons = [
    { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any' },
    { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any' },
    { src: '/icons/maskable-192.png', sizes: '192x192', type: 'image/png', purpose: 'maskable' },
    { src: '/icons/maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
  ];

  it('manifest 칸 = 표 11개뿐 · 값 같음 · prefer_related_applications 0', () => {
    expect(Object.keys(manifest)).toEqual(['id', 'name', 'short_name', 'description', 'lang', 'start_url', 'scope', 'display', 'background_color', 'theme_color', 'icons']);
    expect(manifest).toEqual({
      id: '/', name: '이슈랭크', short_name: '이슈랭크', description: '어제 가장 많이 보도된 뉴스 — 섹션별 순위 · 3줄 요약 · 원문 링크',
      lang: 'ko', start_url: '/app', scope: '/', display: 'standalone', background_color: '#f7f7f5', theme_color: '#f7f7f5', icons,
    });
    expect(read('public/manifest.webmanifest')).not.toContain('prefer_related_applications');
  });

  it('icons 4 = 파일 있음 · IHDR = sizes · any 2 · maskable 2(합친 항목 0)', () => {
    for (const icon of manifest.icons as typeof icons) {
      const file = `public${icon.src}`;
      expect(existsSync(file), file).toBe(true);
      const png = decodePng(file);
      expect(`${png.width}x${png.height}`, file).toBe(icon.sizes);
    }
    const purposes = (manifest.icons as typeof icons).map(i => i.purpose);
    expect(purposes.filter(p => p === 'any')).toHaveLength(2);
    expect(purposes.filter(p => p === 'maskable')).toHaveLength(2);
    expect(purposes.some(p => p.includes(' '))).toBe(false);
  });

  it('any 아이콘: 투명 바탕(모서리 alpha 0) · 타일 #161616 · 흰 마크', () => {
    for (const W of [192, 512]) {
      const png = decodePng(`public/icons/icon-${W}.png`);
      for (const [x, y] of [[0, 0], [W - 1, 0], [0, W - 1], [W - 1, W - 1]]) expect(png.at(x, y)[3], `${W} 모서리`).toBe(0);
      expect(png.at(W / 2, Math.round(W * 0.1)), `${W} 타일`).toEqual(INK);
      expect(png.at(Math.round(W * 0.35), W / 2), `${W} 마크 왼쪽 칸`).toEqual([255, 255, 255, 255]);
    }
  });

  it('maskable: 중심에서 0.4W 밖 픽셀이 모두 불투명 #161616 · 안쪽에 흰 마크', () => {
    for (const W of [192, 512]) {
      const png = decodePng(`public/icons/maskable-${W}.png`);
      let outside = 0, bad = 0, white = 0;
      for (let y = 0; y < W; y++) for (let x = 0; x < W; x++) {
        const px = png.at(x, y);
        if (Math.hypot(x + 0.5 - W / 2, y + 0.5 - W / 2) > 0.4 * W) { outside++; if (px.join() !== INK.join()) bad++; }
        else if (px.join() === '255,255,255,255') white++;
      }
      expect([bad, outside > W * W * 0.4, white > 0], `maskable-${W}`).toEqual([0, true, true]);
    }
  });

  it('apple-touch-icon 180×180 전부 불투명(온 면 #161616 바탕)', () => {
    const png = decodePng('public/icons/apple-touch-icon.png');
    expect([png.width, png.height]).toEqual([180, 180]);
    let transparent = 0;
    for (let i = 3; i < png.rgba.length; i += 4) if (png.rgba[i] !== 255) transparent++;
    expect(transparent).toBe(0);
    expect([png.at(0, 0), png.at(179, 179)]).toEqual([INK, INK]);
  });

  it('og.png 1200×630 · 200×200 이상 · ≤ 5MB(카카오 한도) · 바탕 #f7f7f5', () => {
    const png = decodePng('public/og.png');
    expect([png.width, png.height]).toEqual([1200, 630]);
    expect(png.width >= 200 && png.height >= 200).toBe(true);
    expect(png.bytes).toBeLessThanOrEqual(5 * 1024 * 1024);
    expect([png.at(0, 0), png.at(1199, 629)]).toEqual([[247, 247, 245, 255], [247, 247, 245, 255]]);
  });
});

// ── 서비스 워커(AC-13-2): sw.js를 VM에서 가짜 self · caches · fetch · clients로 ──
function loadSw() {
  const listeners: Record<string, (event: unknown) => void> = {};
  const log: string[] = [];
  const stored = new Map<string, string[]>([['issuerank-v0', ['/old.html']], ['other', ['/x']]]);
  const cacheOf = (name: string) => ({
    add: vi.fn(async (url: string) => { log.push(`add ${name} ${url}`); stored.get(name)!.push(url); }),
    addAll: vi.fn(async (urls: string[]) => { log.push(`addAll ${name} ${urls.join(',')}`); stored.get(name)!.push(...urls); }),
    put: vi.fn(async () => { log.push(`put ${name}`); }),
  });
  const offline = new Response('offline page');
  const caches = {
    open: vi.fn(async (name: string) => { log.push(`open ${name}`); if (!stored.has(name)) stored.set(name, []); return cacheOf(name); }),
    keys: vi.fn(async () => [...stored.keys()]),
    delete: vi.fn(async (name: string) => { log.push(`delete ${name}`); return stored.delete(name); }),
    match: vi.fn(async (url: string) => { log.push(`match ${url}`); return offline; }),
  };
  const self = {
    addEventListener: (type: string, fn: (event: unknown) => void) => { listeners[type] = fn; },
    skipWaiting: vi.fn(async () => { log.push('skipWaiting'); }),
    clients: { claim: vi.fn(async () => { log.push('claim'); }) },
    location: { origin: 'http://localhost:3000' },
  };
  const net = { up: true };
  const page = new Response('<!doctype html>');
  const fetchMock = vi.fn(async (request: { url: string }) => {
    log.push(`fetch ${request.url}`);
    if (!net.up) throw new TypeError('Failed to fetch');
    return page;
  });
  runInContext(read('public/sw.js'), createContext({ self, caches, fetch: fetchMock, URL, Promise }));
  const lifecycle = async (type: 'install' | 'activate') => {
    const waits: Promise<unknown>[] = [];
    listeners[type]({ waitUntil: (p: Promise<unknown>) => waits.push(p) });
    await Promise.all(waits);
  };
  const request = (url: string, mode = 'navigate', method = 'GET') => ({ url: new URL(url, 'http://localhost:3000').href, mode, method });
  // fetch 이벤트 1번: respondWith 횟수 · 받은 응답
  const dispatch = async (req: ReturnType<typeof request>) => {
    const respondWith = vi.fn();
    listeners.fetch({ request: req, respondWith });
    return { calls: respondWith.mock.calls.length, response: respondWith.mock.calls.length ? await respondWith.mock.calls[0][0] : null };
  };
  return { log, stored, caches, self, net, page, offline, lifecycle, request, dispatch };
}

describe('서비스 워커 sw.js (AC-13-2)', () => {
  it('install: issuerank-v1에 /offline.html 1개 · skipWaiting / activate: v1만 남음 · claim', async () => {
    const sw = loadSw();
    await sw.lifecycle('install');
    expect(sw.log).toEqual(['open issuerank-v1', 'add issuerank-v1 /offline.html', 'skipWaiting']);
    expect(sw.stored.get('issuerank-v1')).toEqual(['/offline.html']);
    await sw.lifecycle('activate');
    expect(sw.log.slice(3)).toEqual(['delete issuerank-v0', 'delete other', 'claim']);
    expect([...sw.stored.keys()]).toEqual(['issuerank-v1']);
  });

  it('같은 출처 HTML 탐색 4곳만 respondWith(성공 = 그 응답 그대로 · 실패 = offline.html), 나머지 0 · cache.put 0 · caches.match는 실패 대체에서만', async () => {
    const sw = loadSw();
    await sw.lifecycle('install');
    await sw.lifecycle('activate');
    sw.log.length = 0;
    const handled = ['/', '/app', '/s?issue=x', '/privacy'];
    for (const path of handled) {
      sw.net.up = true;
      const ok = await sw.dispatch(sw.request(path));
      expect([ok.calls, ok.response === sw.page], path).toEqual([1, true]);
      sw.net.up = false;
      const down = await sw.dispatch(sw.request(path));
      expect([down.calls, down.response === sw.offline], path).toEqual([1, true]);
    }
    expect(sw.log).toEqual(handled.flatMap(path => {
      const url = new URL(path, 'http://localhost:3000').href;
      return [`fetch ${url}`, `fetch ${url}`, 'match /offline.html'];
    }));
    sw.log.length = 0;
    const untouched = [
      sw.request('/api/auth/callback/google?code=c&state=s'), sw.request('/_vercel/insights/x'), sw.request('https://accounts.google.com/o/oauth2/v2/auth?x=1'),
      sw.request('/api/rankings', 'cors'), sw.request('/api/bookmarks', 'cors'), sw.request('/api/card', 'cors'),
      sw.request('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css', 'no-cors'),
      sw.request('/', 'navigate', 'POST'), sw.request('/api/bookmarks', 'cors', 'POST'),
    ];
    for (const net of [true, false]) {
      sw.net.up = net;
      for (const req of untouched) expect((await sw.dispatch(req)).calls, `${req.method} ${req.mode} ${req.url}`).toBe(0);
    }
    expect(sw.log).toEqual([]); // fetch · match · put 0
    expect([...sw.stored.keys()]).toEqual(['issuerank-v1']);
    expect(sw.stored.get('issuerank-v1')).toEqual(['/offline.html']);
  });
});

// ── head · 오프라인 화면 · 위젯 안내(AC-13-2 · 13-4) ──
const tagsOf = (page: string) => {
  const head = page.slice(page.indexOf('<head>'), page.indexOf('</head>'));
  return [...head.matchAll(/<(meta|link)\b([^>]*)>/g)].map(m => ({ tag: m[1], ...Object.fromEntries([...m[2].matchAll(/([a-z:-]+)="([^"]*)"/g)].map(a => [a[1], a[2]])) } as Record<string, string>));
};
const ogOf = (page: string) => tagsOf(page).filter(t => t.property?.startsWith('og:') || t.name?.startsWith('twitter:')).map(t => [t.property ?? t.name, t.content]);
const pwaTags = (page: string) => {
  const tags = tagsOf(page);
  return {
    manifest: tags.filter(t => t.rel === 'manifest').map(t => t.href),
    theme: tags.filter(t => t.name === 'theme-color').map(t => [t.content, t.media]),
    icon: tags.filter(t => t.rel === 'icon').map(t => [t.href, t.type]),
    apple: tags.filter(t => t.rel === 'apple-touch-icon').map(t => t.href),
  };
};
const PWA_TAGS = {
  manifest: ['/manifest.webmanifest'],
  theme: [['#f7f7f5', '(prefers-color-scheme: light)'], ['#121211', '(prefers-color-scheme: dark)']],
  icon: [['/icons/icon-192.png', 'image/png']],
  apple: ['/icons/apple-touch-icon.png'],
};
const ogTable = (title: string, description: string) => [
  ['og:type', 'website'], ['og:site_name', '이슈랭크'], ['og:title', title], ['og:description', description],
  ['og:image', `${BASE}/og.png`], ['og:image:width', '1200'], ['og:image:height', '630'], ['og:image:alt', title],
  ['og:locale', 'ko_KR'], ['twitter:card', 'summary_large_image'],
];
const text = (fragment: string) => fragment.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim();

describe('offline.html · index.html · widget.html (AC-13-2 · 13-4)', () => {
  it('offline: http(s) 주소 · 바깥 파일 0 · noindex · Analytics 0 · 세 문구 · 다시 시도 = 새로 고침 · 다크 토큰 = widget.html', () => {
    const page = read('public/offline.html');
    expect(page).not.toMatch(/https?:\/\//);
    expect(page).not.toMatch(/<link\b|<script[^>]+src=|<img\b|@import|url\(/);
    expect(page).not.toMatch(/_vercel\/insights|window\.va/);
    expect(page).toContain('<meta name="robots" content="noindex">');
    const body = text(page.slice(page.indexOf('<body>'), page.indexOf('<script>')));
    expect(body).toBe('이슈랭크 인터넷에 연결되어 있지 않습니다 연결되면 다시 시도해 주세요. 랭킹은 매일 새벽 갱신됩니다. 다시 시도');
    expect(page).toContain('<button type="button" class="btn" id="retry">다시 시도</button>');
    const listeners: Record<string, () => void> = {};
    const location = { reload: vi.fn() };
    runInContext(page.slice(page.indexOf('<script>') + 8, page.indexOf('</script>')), createContext({
      location, document: { getElementById: (id: string) => (id === 'retry' ? { addEventListener: (type: string, fn: () => void) => { listeners[type] = fn; } } : null) },
    }));
    expect(location.reload).not.toHaveBeenCalled();
    listeners.click();
    expect(location.reload).toHaveBeenCalledTimes(1);
    // 밝게 · 어둡게 토큰 두 줄씩이 widget.html과 글자 그대로 같음
    const widget = read('public/widget.html');
    const tokens = [...page.matchAll(/--bg: #[0-9a-f]{6};[^}]*?--ink-text: #[0-9a-f]{6};/g)].map(m => m[0]);
    expect(tokens).toHaveLength(2);
    for (const block of tokens) expect(widget).toContain(block);
    expect(page).toContain('@media (prefers-color-scheme: dark)');
  });

  it('index head: manifest · theme-color 2 · icon · apple-touch-icon · description · OG 10개 = 표 · og:url · canonical · apple-mobile-web-app-capable 0 · SW 등록은 start(); 뒤', () => {
    const page = read('public/index.html');
    expect(pwaTags(page)).toEqual(PWA_TAGS);
    expect(tagsOf(page).filter(t => t.name === 'description').map(t => t.content)).toEqual(['어제 가장 많이 보도된 뉴스를 섹션별 순위와 AI 3줄 요약으로 봅니다']);
    expect(ogOf(page)).toEqual(ogTable('이슈랭크 — 어제 가장 많이 보도된 뉴스', '섹션별 순위 · AI 3줄 요약 · 원문 링크'));
    expect(page).not.toMatch(/og:url|rel="canonical"|apple-mobile-web-app/);
    expect(page.indexOf('<link rel="manifest"')).toBeGreaterThan(page.indexOf('<title>이슈랭크</title>'));
    expect(page).toContain('\nstart();\nregisterServiceWorker();\n</script>');
    expect(page.match(/registerServiceWorker\(\)/g)).toHaveLength(2); // 정의 1 · 호출 1
    expect(page.match(/serviceWorker\.register\(/g)).toHaveLength(1);
    expect(page).toContain("navigator.serviceWorker.register('/sw.js', { scope: '/', updateViaCache: 'none' })");
  });

  it('widget: manifest · theme-color 2 · icon · apple-touch-icon · 자기 OG · id="app" 절 · Android 7번 새 문구 · "바로가기 만들기" 0', () => {
    const page = read('public/widget.html');
    expect(pwaTags(page)).toEqual(PWA_TAGS);
    expect(ogOf(page)).toEqual(ogTable('이슈랭크 — 홈 화면 앱 · 위젯 설치', 'iPhone · Android 홈 화면에서 오늘의 헤드라인을 봅니다'));
    const app = page.indexOf('<h2 id="app">앱으로 설치</h2>'), iphone = page.indexOf('<h2>iPhone</h2>');
    expect(app).toBeGreaterThan(page.indexOf('<p class="note">'));
    expect(iphone).toBeGreaterThan(app);
    expect(text(page.slice(app, iphone))).toBe('앱으로 설치 '
      + 'Android(Chrome): Chrome으로 이슈랭크(https://issuerank-kappa.vercel.app)를 열고 오른쪽 위 설치 아이콘(또는 맨 아래 앱 설치)을 누른 뒤 설치를 누릅니다. 보이지 않으면 더보기(⋮)에서 앱 설치 또는 홈 화면에 추가를 고릅니다. 카카오톡 등 앱 안 브라우저에는 설치 메뉴가 없을 수 있으니 Chrome으로 여세요. '
      + 'iPhone(Safari): Safari로 이슈랭크를 열고 공유(□↑) › 홈 화면에 추가 › 추가를 누릅니다. iOS 16.4 이상은 Chrome 등 다른 브라우저의 공유 메뉴에서도 됩니다. 홈 화면 앱은 Safari와 로그인이 따로라 앱에서 다시 로그인해야 할 수 있습니다.');
    const android = page.slice(page.indexOf('<h2>Android</h2>'), page.indexOf('</main>'));
    expect(text(android)).toBe('Android Android 홈 화면 위젯은 준비 중입니다. 대신 위 앱으로 설치로 홈 화면 아이콘을 만들 수 있습니다.');
    expect(android).toContain('<ol start="7">');
    expect(page).not.toContain('바로가기 만들기');
  });
});
