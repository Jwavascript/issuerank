// 카드 PNG 실제 렌더(O3 · D28 · AC-12-1): next/og + 저장소 글꼴. 전역 fetch를 불리면 실패하는 가짜로 바꿔 바깥 요청 0을 확인
import { ImageResponse } from 'next/og';
import { createElement } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { buildCard, createCardHandler, type Card } from '../server/card';
import { inflateSync } from 'node:zlib';
import { cardFooter, cardPngResponse, renderCard } from '../server/card-image';
import { memoryStore } from '../server/sample';

const PNG_SIGNATURE = [137, 80, 78, 71, 13, 10, 26, 10];
const size = (png: Uint8Array) => {
  const view = new DataView(png.buffer, png.byteOffset, png.byteLength);
  return { chunk: String.fromCharCode(...png.slice(12, 16)), width: view.getUint32(16), height: view.getUint32(20) };
};

// Satori는 내장 yoga.wasm을 data: 주소로 fetch함(네트워크 아님) → data:만 진짜 fetch로 넘기고, 그 밖의 주소는 기록 · 실패
const realFetch = globalThis.fetch;

// PNG → 행마다 "글자 픽셀"(밝기 < 200: 본문 #161616 · 흐린 글자 #6f6b65. 바탕 #f7f7f5 · 구분선 #e7e4df는 아님)이 있는지
function inkRows(png: Uint8Array): boolean[] {
  const view = new DataView(png.buffer, png.byteOffset, png.byteLength);
  const width = view.getUint32(16), height = view.getUint32(20), depth = png[24], color = png[25];
  if (depth !== 8 || ![2, 6].includes(color)) throw new Error(`PNG 형식 ${depth}/${color}`);
  const bpp = color === 6 ? 4 : 3;
  const idat: Uint8Array[] = [];
  for (let at = 8; at < png.length;) {
    const len = view.getUint32(at), type = String.fromCharCode(...png.slice(at + 4, at + 8));
    if (type === 'IDAT') idat.push(png.slice(at + 8, at + 8 + len));
    at += 12 + len;
  }
  const raw = inflateSync(Buffer.concat(idat));
  const stride = width * bpp, rows: boolean[] = [];
  let prev = new Uint8Array(stride);
  for (let y = 0; y < height; y++) {
    const f = raw[y * (stride + 1)], line = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1)), cur = new Uint8Array(stride);
    for (let i = 0; i < stride; i++) {
      const a = i >= bpp ? cur[i - bpp] : 0, b = prev[i], c = i >= bpp ? prev[i - bpp] : 0;
      const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
      cur[i] = (line[i] + (f === 0 ? 0 : f === 1 ? a : f === 2 ? b : f === 3 ? (a + b) >> 1 : pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 255;
    }
    let ink = false;
    for (let x = 0; x < width && !ink; x++) { const i = x * bpp; if (0.299 * cur[i] + 0.587 * cur[i + 1] + 0.114 * cur[i + 2] < 200) ink = true; }
    rows.push(ink);
    prev = cur;
  }
  return rows;
}
// y 범위 안의 글자 줄 덩어리 수(빈 행으로 나뉜 묶음)
const bands = (rows: boolean[], from: number, to: number) => rows.slice(from, to).reduce((n, ink, i, all) => n + (ink && !all[i - 1] ? 1 : 0), 0);

let fetchMock: ReturnType<typeof vi.fn<(url: string) => Promise<Response>>>;
let localFetches = 0;
beforeEach(() => {
  localFetches = 0;
  fetchMock = vi.fn(async (_url: string): Promise<Response> => { throw new Error('바깥 요청 금지'); });
  vi.stubGlobal('fetch', async (input: string | URL | Request, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
    if (url.startsWith('data:')) { localFetches++; return realFetch(input, init); }
    return fetchMock(url);
  });
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

const expectPng = async (res: Response) => {
  expect(res.status).toBe(200);
  expect(res.headers.get('content-type')).toBe('image/png');
  const png = new Uint8Array(await res.arrayBuffer());
  expect([...png.slice(0, 8)]).toEqual(PNG_SIGNATURE);
  expect(size(png)).toEqual({ chunk: 'IHDR', width: 1080, height: 1350 });
  expect(png.byteLength).toBeLessThanOrEqual(4.5 * 1024 * 1024);
  console.info(`card png ${png.byteLength} B`);
  return png;
};

describe('카드 PNG 렌더 (AC-12-1)', () => {
  it('샘플 날짜(2026-10-07) 카드: 1080×1350 PNG · ≤ 4.5MB · fetch 0', async () => {
    const handle = createCardHandler(() => memoryStore, card => cardPngResponse(card));
    await expectPng(await handle(new Request('http://localhost:3000/api/card?date=2026-10-07')));
    expect(fetchMock).not.toHaveBeenCalled();
  }, 120_000);

  it('美中日 · 따옴표 · 긴 제목 · 그림 문자 · 섹션 6개 섞은 가짜 카드: fetch 0', async () => {
    const card: Card = {
      date: '2026-10-19', dateLabel: '10월 19일 월요일', updatedLabel: '10.19 오전 6:34 수집', hidden: [],
      items: [
        { id: '2026-10-19-pol-1', section: 'pol', title: '“與野 대치” 尹 전 대통령 재판 증인 출석…李 대표 입장 발표 예정이라고 밝혀' },
        { id: '2026-10-19-wor-2', section: 'wor', title: '美·中 정상회담 앞두고 日 총리 방한 🇯🇵🔥 北 미사일 대응 공조 논의' },
        { id: '2026-10-19-eco-3', section: 'eco', title: '코스피 2,600 회복 ‘외국인 순매수’ 9일째' },
        { id: '2026-10-19-cul-4', section: 'cul', title: '가을 단풍 절정 👍' },
        { id: '2026-10-19-soc-5', section: 'soc', title: 'ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789' },
        { id: '2026-10-19-it-6', section: 'it', title: 'AI 반도체 수출 증가… HBM4 양산 본격화 — 삼성·SK하이닉스 경쟁' },
      ],
    };
    await expectPng(await cardPngResponse(card));
    expect(fetchMock).not.toHaveBeenCalled();
  }, 120_000);

  it('하단 작은 줄 = 수집 시각 · 출처 문구 · 순위 기준, 가장 긴 수집 시각으로도 한 줄 (사용자 결정 2026-10-19)', async () => {
    expect(cardFooter('10.19 오전 6:34 수집')).toBe('10.19 오전 6:34 수집 · 기사 제목 출처: 각 언론사(newsdata.io 수집) · 순위는 연관기사 수 기준');
    const w = (await memoryStore.widget('2026-10-07'))!;
    for (const label of ['10.19 오전 6:34 수집', '12.31 오후 12:59 수집']) {
      const png = await renderCard({ ...buildCard(w, [])!, updatedLabel: label });
      // 마지막 줄 구분선 아래(y 1120~) 글자 덩어리 = 주소 줄 1 + 하단 줄 1. 두 줄로 넘치면 3
      expect(bands(inkRows(png), 1120, 1350)).toBe(2);
    }
    expect(fetchMock).not.toHaveBeenCalled();
  }, 120_000);

  it('줄이 3개뿐인 카드도 그림', async () => {
    const w = (await memoryStore.widget('2026-10-07'))!;
    const png = await renderCard(buildCard({ ...w, items: w.items.slice(0, 3) }, [])!);
    expect(size(png)).toEqual({ chunk: 'IHDR', width: 1080, height: 1350 });
    expect(fetchMock).not.toHaveBeenCalled();
  }, 120_000);

  it('렌더가 던지면 500 { message } (200 뒤 끊김 아님) · 캐시 헤더 없음', async () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    const card = buildCard((await memoryStore.widget('2026-10-07'))!, [])!;
    const res = await cardPngResponse(card, async () => { throw new Error('boom'); });
    expect(res.status).toBe(500);
    expect(await res.json()).toEqual({ message: '카드를 만들 수 없습니다' });
    expect(res.headers.get('cache-control')).toBeNull();
  });

  it('ImageResponse 스트림 안에서 Satori가 실패해도 버퍼로 받아 500 (실제 next/og)', async () => {
    const log = vi.spyOn(console, 'error').mockImplementation(() => {});
    const card = buildCard((await memoryStore.widget('2026-10-07'))!, [])!;
    // display: flex 없는 div에 자식 2개 → Satori가 던짐(스트림 start 안)
    const broken = async () => new Uint8Array(await new ImageResponse(createElement('div', null, createElement('span', null, 'a'), createElement('span', null, 'b'))).arrayBuffer());
    const res = await cardPngResponse(card, broken);
    expect(res.status).toBe(500);
    expect(await res.json()).toEqual({ message: '카드를 만들 수 없습니다' });
    expect(log.mock.calls).toEqual([['CARD_RENDER_FAILED']]);
  }, 120_000);
});
