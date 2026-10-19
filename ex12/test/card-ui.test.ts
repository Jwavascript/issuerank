// /card 페이지(O3 · AC-12-2 · 12-3 · 12-4): 실제 public/card.html 스크립트를 VM에서 가짜 document · fetch · clipboard · Date로 실행
// 캡션 응답은 실제 처리기(메모리 저장소)로 만듦. 시각은 setup.ts가 2026-10-07 12:00 KST로 고정(VM에도 같은 Date를 넘김)
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it, vi } from 'vitest';
import { captionResponse, createCardHandler } from '../server/card';
import { memoryStore } from '../server/sample';

const html = readFileSync('public/card.html', 'utf8');
const SCRIPT = html.slice(html.indexOf('<script>') + 8, html.indexOf('</script>'));
const captionApi = createCardHandler(() => memoryStore, captionResponse);

type El = {
  id: string; textContent: string; innerHTML: string; hidden: boolean; value: string; src: string; alt: string; href: string;
  attrs: Record<string, string>; listeners: Record<string, (e: unknown) => unknown>;
  setAttribute(k: string, v: string): void; addEventListener(type: string, fn: (e: unknown) => unknown): void;
  focus(): void; select(): void; setSelectionRange(a: number, b: number): void; selected: boolean;
};
const element = (id: string): El => ({
  id, textContent: '', innerHTML: '', hidden: ['stale', 'error', 'card', 'hiddenRow'].includes(id), value: '', src: '', alt: '', href: '', attrs: {}, listeners: {},
  selected: false,
  setAttribute(k, v) { this.attrs[k] = v; },
  addEventListener(type, fn) { this.listeners[type] = fn; },
  focus() {}, select() { this.selected = true; }, setSelectionRange() {},
});

// reply: 없으면 실제 처리기, 있으면 그 응답(던지면 네트워크 실패)
async function boot(opts: { reply?: (url: string) => Response | Promise<Response>; clipboard?: (text: string) => Promise<void>; download?: boolean } = {}) {
  const els = new Map<string, El>();
  const $ = (id: string) => { if (!els.has(id)) els.set(id, element(id)); return els.get(id)!; };
  const requests: string[] = [];
  const fetchMock = vi.fn(async (url: string, init: { cache?: string }) => {
    requests.push(`${url} ${init?.cache}`);
    if (opts.reply) return opts.reply(url);
    return captionApi(new Request(`http://localhost:3000${url}`));
  });
  const copied: string[] = [];
  const clipboard = { writeText: opts.clipboard ?? (async (text: string) => { copied.push(text); }) };
  const context = createContext({
    document: { getElementById: $, createElement: () => (opts.download === false ? {} : { download: '' }) },
    fetch: fetchMock, navigator: { clipboard }, Date, URLSearchParams, Promise, Error, String, encodeURIComponent,
  });
  runInContext(SCRIPT, context);
  const settle = () => new Promise(r => setTimeout(r, 0));
  await settle();
  const run = async (code: string) => { await runInContext(code, context); await settle(); };
  const query = (url: string) => new URL(url, 'http://localhost:3000').searchParams;
  const rows = () => [...$('rows').innerHTML.matchAll(/data-hide="([^"]+)"(?: disabled)?/g)].map(m => ({ id: m[1], disabled: m[0].endsWith('disabled') }));
  return { $, requests, copied, run, query, rows };
}

const widget = async () => (await memoryStore.widget('2026-10-07'))!;

describe('/card 페이지 (AC-12-2 · 12-3 · 12-4)', () => {
  it('① 정상: 캡션 API 먼저 → 그 date로 img · 저장 링크, 6줄 · 캡션 상자 · 안내 숨김', async () => {
    const p = await boot();
    expect(p.requests).toEqual(['/api/card/caption no-store']);
    expect(p.$('preview').src).toBe('/api/card?date=2026-10-07');
    expect(p.$('preview').alt).toBe('10월 7일 오늘의 이슈 카드');
    expect(p.$('save').href).toBe('/api/card?date=2026-10-07');
    expect(p.$('save').attrs.download).toBe('issuerank-2026-10-07.png');
    expect(p.$('meta').textContent).toBe(`10월 7일 수요일 · ${(await widget()).updatedLabel}`);
    expect(p.$('stale').hidden).toBe(true);
    expect(p.$('error').hidden).toBe(true);
    expect(p.$('card').hidden).toBe(false);
    expect(p.rows().map(r => r.id)).toEqual((await widget()).items.slice(0, 6).map(it => it.id));
    expect(p.rows().every(r => !r.disabled)).toBe(true);
    expect(p.$('hiddenRow').hidden).toBe(true);
    const body = await (await captionApi(new Request('http://localhost:3000/api/card/caption'))).json();
    expect(p.$('caption').value).toBe(body.caption);
  });

  it('② 빼기 1 → 3 → 되돌리기: 요청 hide = 뺀 id들(같은 date) · img src 같음 · 3개 뒤 비활성 · 되돌리기면 hide 없음', async () => {
    const p = await boot();
    const items = (await widget()).items;
    await p.run(`hideItem(${JSON.stringify(items[0].id)})`);
    let last = p.requests.at(-1)!.split(' ')[0];
    expect(p.query(last).get('date')).toBe('2026-10-07');
    expect(p.query(last).get('hide')).toBe(items[0].id);
    expect(p.$('preview').src).toBe(`/api/card?date=2026-10-07&hide=${items[0].id}`);
    expect(p.query(p.$('preview').src).get('hide')).toBe(p.query(last).get('hide'));
    expect(p.rows().map(r => r.id)).toEqual(items.slice(1, 7).map(it => it.id));
    expect(p.$('hiddenRow').hidden).toBe(false);
    expect(p.$('hiddenCount').textContent).toBe('뺀 이슈 1개 ·');

    await p.run(`hideItem(${JSON.stringify(items[3].id)})`);
    await p.run(`hideItem(${JSON.stringify(items[5].id)})`);
    last = p.requests.at(-1)!.split(' ')[0];
    const hide = [items[0].id, items[3].id, items[5].id];
    expect(p.query(last).get('hide')).toBe(hide.join(','));
    expect(p.query(p.$('preview').src).get('hide')).toBe(hide.join(','));
    expect(p.rows().map(r => r.id)).toEqual(items.filter(it => !hide.includes(it.id)).slice(0, 6).map(it => it.id));
    expect(p.rows().every(r => r.disabled)).toBe(true);
    expect(p.$('hiddenCount').textContent).toBe('뺀 이슈 3개 ·');
    const before = p.requests.length;
    await p.run(`hideItem(${JSON.stringify(items[1].id)})`); // 4개째는 요청 없음
    expect(p.requests.length).toBe(before);

    await p.run('restore()');
    last = p.requests.at(-1)!.split(' ')[0];
    expect(p.query(last).get('hide')).toBeNull();
    expect(p.$('preview').src).toBe('/api/card?date=2026-10-07');
    expect(p.$('hiddenRow').hidden).toBe(true);
    expect(p.rows().map(r => r.id)).toEqual(items.slice(0, 6).map(it => it.id));
  });

  it('③ date ≠ KST 오늘: "아직 오늘 카드가 없습니다. 마지막 카드: …"', async () => {
    const p = await boot({ reply: async () => {
      const res = await captionApi(new Request('http://localhost:3000/api/card/caption?date=2026-10-06'));
      return res;
    } });
    expect(p.$('stale').hidden).toBe(false);
    expect(p.$('stale').textContent).toBe('아직 오늘 카드가 없습니다. 마지막 카드: 10월 6일');
    expect(p.$('preview').src).toBe('/api/card?date=2026-10-06');
    expect(p.$('save').attrs.download).toBe('issuerank-2026-10-06.png');
  });

  it.each([
    ['caption 500', () => Response.json({ message: 'x' }, { status: 500 })],
    ['네트워크 실패', () => { throw new TypeError('Failed to fetch'); }],
  ])('④ %s: "카드를 불러오지 못했습니다" · 다시 시도 → 성공', async (_name, fail) => {
    let broken = true;
    const p = await boot({ reply: url => (broken ? fail() : captionApi(new Request(`http://localhost:3000${url}`))) });
    expect(p.$('error').hidden).toBe(false);
    expect(p.$('card').hidden).toBe(true);
    expect(p.$('retry').listeners.click).toBeTypeOf('function');
    broken = false;
    await p.$('retry').listeners.click({});
    await new Promise(r => setTimeout(r, 0));
    expect(p.$('error').hidden).toBe(true);
    expect(p.$('card').hidden).toBe(false);
    expect(p.$('preview').src).toBe('/api/card?date=2026-10-07');
  });

  it('⑤ 캡션 복사: 성공 "복사했습니다" = 캡션 전문 / clipboard 거절 → 실패 문구 · 글 상자 선택', async () => {
    const ok = await boot();
    await ok.run('copyCaption()');
    expect(ok.copied).toEqual([ok.$('caption').value]);
    expect(ok.$('status').textContent).toBe('복사했습니다');

    const no = await boot({ clipboard: async () => { throw new Error('NotAllowedError'); } });
    await no.run('copyCaption()');
    expect(no.$('status').textContent).toBe('복사하지 못했습니다. 아래 글을 길게 눌러 복사해 주세요');
    expect(no.$('caption').selected).toBe(true);
  });

  it('⑥ download 속성을 모르는 브라우저: 저장 대신 "이미지를 길게 눌러 저장해 주세요"', async () => {
    const p = await boot({ download: false });
    const event = { preventDefault: vi.fn() };
    p.$('save').listeners.click(event);
    expect(event.preventDefault).toHaveBeenCalled();
    expect(p.$('status').textContent).toBe('이미지를 길게 눌러 저장해 주세요');
    const yes = await boot();
    const e2 = { preventDefault: vi.fn() };
    yes.$('save').listeners.click(e2);
    expect(e2.preventDefault).not.toHaveBeenCalled();
  });

  it('⑦ 페이지: noindex · Web Analytics 없음 · 바깥 스크립트 · 글꼴 0', () => {
    expect(html).toContain('<meta name="robots" content="noindex">');
    expect(html).not.toMatch(/_vercel\/insights|window\.va|<script[^>]+src=|<link[^>]+stylesheet|https?:\/\//);
  });
});
