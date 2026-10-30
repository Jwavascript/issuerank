// 서비스 워커 아침 알림(O5 · D30 · AC-14-3 · 회귀 AC-13-2): public/sw.js를 가짜 self(registration · clients · location)로 VM에서 실행 —
// push = waitUntil 안 showNotification 정확히 1(같은 tag는 먼저 닫음) · notificationclick = 같은 출처 창 focus + navigate / openWindow. O4 install · activate · fetch는 pwa.test.ts(v2)
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it, vi } from 'vitest';

const ORIGIN = 'http://localhost:3000';
const MORNING = ['오늘의 뉴스가 도착했어요', { body: '', icon: '/icons/icon-192.png', badge: '/icons/badge-96.png', tag: 'issuerank-daily', data: { url: '/p' }, lang: 'ko' }];
const TEST = ['[시험] 오늘의 뉴스가 도착했어요', { body: '시험 알림입니다', icon: '/icons/icon-192.png', badge: '/icons/badge-96.png', tag: 'issuerank-test', data: { url: '/p' }, lang: 'ko' }];
const plain = <T,>(v: T): T => JSON.parse(JSON.stringify(v));

function loadSw(opts: { shown?: { tag: string }[]; windows?: unknown[] } = {}) {
  const listeners: Record<string, (event: unknown) => void> = {};
  const log: string[] = [];
  const shown = (opts.shown ?? []).map(n => ({ ...n, close: vi.fn(() => { log.push(`close ${n.tag}`); }) }));
  const registration = {
    getNotifications: vi.fn(async (filter: { tag: string }) => { log.push(`get ${filter.tag}`); return shown.filter(n => n.tag === filter.tag); }),
    showNotification: vi.fn(async (title: string, _options?: unknown) => { log.push(`show ${title}`); }),
  };
  const clients = {
    claim: vi.fn(async () => {}),
    matchAll: vi.fn(async () => opts.windows ?? []),
    openWindow: vi.fn(async (url: string) => { log.push(`open ${url}`); return null; }),
  };
  const self = { addEventListener: (type: string, fn: (event: unknown) => void) => { listeners[type] = fn; }, registration, clients, location: { origin: ORIGIN }, skipWaiting: vi.fn() };
  runInContext(readFileSync('public/sw.js', 'utf8'), createContext({ self, caches: {}, fetch: vi.fn(), URL, Promise }));
  // 이벤트 1번: waitUntil 횟수 · 그 약속이 끝났을 때의 기록
  const fire = async (type: string, event: Record<string, unknown>) => {
    const waits: Promise<unknown>[] = [];
    listeners[type]({ ...event, waitUntil: (p: Promise<unknown>) => { waits.push(p); } });
    await Promise.all(waits);
    return waits.length;
  };
  const push = (data: unknown) => fire('push', { data });
  return { listeners, log, shown, registration, clients, fire, push };
}
const json = (value: unknown) => ({ json: () => value, text: () => JSON.stringify(value) });
const notJson = { json: () => { throw new SyntaxError('Unexpected token'); }, text: () => 'hello' };
const win = (url: string, navigate: () => Promise<unknown> = async () => ({})) => ({ url, focus: vi.fn(async () => ({})), navigate: vi.fn(navigate) });

describe('push (AC-14-3)', () => {
  it('아침 JSON · 시험 JSON → waitUntil 1 · showNotification 정확히 1 = 표 값 · renotify · actions · image · silent · vibrate 0', async () => {
    for (const [payload, want] of [
      [{ title: '오늘의 뉴스가 도착했어요', body: '', tag: 'issuerank-daily', url: '/p' }, MORNING],
      [{ title: '[시험] 오늘의 뉴스가 도착했어요', body: '시험 알림입니다', tag: 'issuerank-test', url: '/p' }, TEST],
    ] as const) {
      const sw = loadSw();
      expect(await sw.push(json(payload))).toBe(1);
      expect(plain(sw.registration.showNotification.mock.calls)).toEqual([want]);
      const options = sw.registration.showNotification.mock.calls[0][1] as Record<string, unknown>;
      for (const k of ['renotify', 'actions', 'image', 'silent', 'vibrate', 'requireInteraction']) expect(k in options, k).toBe(false);
    }
  });

  it('데이터 없음 · JSON 아님 · 칸 빠짐 · 문자열 아님 · tag ISSUE! · 바깥 url · // url · 300자 제목 · 빈 제목 · 긴 body → 아침 알림 값(알림 1개는 꼭)', async () => {
    const good = { title: 'x', body: '', tag: 'issuerank-daily', url: '/p' };
    for (const [label, data] of [
      ['없음', null], ['JSON 아님', notJson], ['칸 빠짐', json({ title: 'x', body: '', tag: 'a' })], ['배열', json(['x'])], ['숫자 body', json({ ...good, body: 1 })],
      ['tag ISSUE!', json({ ...good, tag: 'ISSUE!' })], ['tag 65자', json({ ...good, tag: 'a'.repeat(65) })], ['바깥 url', json({ ...good, url: 'https://evil.example/' })],
      ['// url', json({ ...good, url: '//evil.example' })], ['상대 url', json({ ...good, url: 'p' })], ['300자 제목', json({ ...good, title: 'x'.repeat(300) })],
      ['빈 제목', json({ ...good, title: '' })], ['201자 body', json({ ...good, body: 'x'.repeat(201) })], ['null', json(null)],
    ] as [string, unknown][]) {
      const sw = loadSw();
      expect(await sw.push(data), label).toBe(1);
      expect(plain(sw.registration.showNotification.mock.calls), label).toEqual([MORNING]);
    }
  });

  it('맞는 다른 값은 그대로(80자 제목 · 200자 body · /p?x 같은 경로)', async () => {
    const sw = loadSw();
    await sw.push(json({ title: 't'.repeat(80), body: 'b'.repeat(200), tag: 'a-1', url: '/widget#push' }));
    expect(plain(sw.registration.showNotification.mock.calls)).toEqual([['t'.repeat(80), { ...MORNING[1] as object, body: 'b'.repeat(200), tag: 'a-1', data: { url: '/widget#push' } }]]);
  });

  it('같은 tag 알림이 떠 있으면 먼저 close 1 → 그 뒤 show(다른 tag는 그대로) · getNotifications 실패여도 show 1', async () => {
    const sw = loadSw({ shown: [{ tag: 'issuerank-daily' }, { tag: 'issuerank-test' }] });
    await sw.push(json({ title: '오늘의 뉴스가 도착했어요', body: '', tag: 'issuerank-daily', url: '/p' }));
    expect(sw.log).toEqual(['get issuerank-daily', 'close issuerank-daily', 'show 오늘의 뉴스가 도착했어요']);
    expect(sw.shown[1].close).not.toHaveBeenCalled();
    const broken = loadSw();
    broken.registration.getNotifications.mockRejectedValueOnce(new Error('x'));
    expect(await broken.push(null)).toBe(1);
    expect(broken.registration.showNotification).toHaveBeenCalledTimes(1);
  });
});

describe('notificationclick', () => {
  const click = (sw: ReturnType<typeof loadSw>, data: unknown = { url: '/p' }) => {
    const notification = { data, close: vi.fn(() => { sw.log.push('close notification'); }) };
    return { notification, done: sw.fire('notificationclick', { notification }) };
  };

  it('같은 출처 창 → close 1 · focus → navigate(http://localhost:3000/p) · openWindow 0 · matchAll(window · includeUncontrolled)', async () => {
    const w = win(`${ORIGIN}/#list`);
    const sw = loadSw({ windows: [w] });
    const { notification, done } = click(sw);
    expect(await done).toBe(1);
    expect(notification.close).toHaveBeenCalledTimes(1);
    expect(sw.clients.matchAll.mock.calls).toEqual([[{ type: 'window', includeUncontrolled: true }]]);
    expect([w.focus.mock.calls.length, w.navigate.mock.calls]).toEqual([1, [[`${ORIGIN}/p`]]]);
    expect(sw.clients.openWindow).not.toHaveBeenCalled();
  });

  it('navigate 실패 → openWindow(/p) · 창 없음 → openWindow · 다른 출처 창만 → 그 창 건드리지 않고 openWindow', async () => {
    const failing = win(`${ORIGIN}/`, async () => { throw new TypeError('not controlled'); });
    const one = loadSw({ windows: [failing] });
    await click(one).done;
    expect([failing.focus.mock.calls.length, failing.navigate.mock.calls.length, one.clients.openWindow.mock.calls]).toEqual([1, 1, [[`${ORIGIN}/p`]]]);

    const none = loadSw({ windows: [] });
    await click(none).done;
    expect(none.clients.openWindow.mock.calls).toEqual([[`${ORIGIN}/p`]]);

    const other = win('https://evil.example/');
    const foreign = loadSw({ windows: [other] });
    await click(foreign).done;
    expect([other.focus.mock.calls.length, other.navigate.mock.calls.length, foreign.clients.openWindow.mock.calls]).toEqual([0, 0, [[`${ORIGIN}/p`]]]);
  });

  it('data.url 없음 · 틀림 → /p 기본', async () => {
    for (const data of [undefined, null, {}, { url: 'https://evil.example/' }, { url: '//evil.example' }, { url: 3 }]) {
      const sw = loadSw({ windows: [] });
      await click(sw, data).done;
      expect(sw.clients.openWindow.mock.calls, JSON.stringify(data)).toEqual([[`${ORIGIN}/p`]]);
    }
  });
});

it('캐시 판 = issuerank-v3 · O4 install · activate · fetch 처리 그대로(HEAD 문장 3개가 글자 그대로 있음)', () => {
  const sw = readFileSync('public/sw.js', 'utf8');
  expect(sw).toContain("const CACHE = 'issuerank-v3';");
  expect(sw).not.toContain("'issuerank-v2'");
  expect(sw).not.toContain("'issuerank-v1'");
  for (const line of [
    "  event.waitUntil(caches.open(CACHE).then(cache => cache.add(OFFLINE)).then(() => self.skipWaiting()));",
    "    .then(keys => Promise.all(keys.filter(key => key !== CACHE).map(key => caches.delete(key))))",
    "  if (url.origin !== self.location.origin || url.pathname.startsWith('/api/') || url.pathname.startsWith('/_vercel/')) return;",
    "  event.respondWith(fetch(request).catch(() => caches.match(OFFLINE)));",
  ]) expect(sw).toContain(line);
  expect(sw.replace(/^\s*\/\/.*$/gm, '')).not.toMatch(/cache\.put|renotify|importScripts/); // 주석 밖 코드에서
});
