// 카드 그림(D28): 1080×1350 PNG(4:5). next/og ImageResponse(Satori + Resvg, Next에 들어 있음 — 새 패키지 0)
// 글꼴은 저장소의 Noto Sans KR Bold · Regular(OFL, 한자 포함 — 글리프가 없으면 @vercel/og가 Google Fonts로 나감)
// 첫 카드 요청 때 한 번 읽어 모듈 범위에 둠(실패하면 다음 요청이 다시). 배포에는 next.config.mjs outputFileTracingIncludes로 들어감
import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { ImageResponse } from 'next/og';
import { CARD_CACHE, cardTitle, sectionName, type Card } from './card';
import type { SectionKey } from './ranking';

export const CARD_WIDTH = 1080;
export const CARD_HEIGHT = 1350;
const FONT = 'Noto Sans KR';
const RENDER_FAILED = '카드를 만들 수 없습니다';

// 사이트 밝은 토큰(index.html :root)
const BG = '#f7f7f5';
const TEXT = '#161616';
const MUTED = '#6f6b65';
const LINE = '#e7e4df';
const SECTION_COLOR: Record<SectionKey, string> = {
  pol: '#6476c9', eco: '#3d9a74', soc: '#c8843a', cul: '#b8638f', wor: '#3f93ad', it: '#8467c4',
};

type Font = { name: string; data: Buffer; weight: 400 | 700; style: 'normal' };
let fonts: Promise<Font[]> | undefined;
const fontFile = (name: string) => readFile(join(process.cwd(), 'assets/fonts', name));
function loadFonts(): Promise<Font[]> {
  const pending = fonts ??= Promise.all([fontFile('NotoSansKR-Regular.otf'), fontFile('NotoSansKR-Bold.otf')])
    .then(([regular, bold]): Font[] => [
      { name: FONT, data: regular, weight: 400, style: 'normal' },
      { name: FONT, data: bold, weight: 700, style: 'normal' },
    ]);
  pending.catch(() => { if (fonts === pending) fonts = undefined; });
  return pending;
}

// 맨 아래 작은 줄(한 줄에 들어가게 — 사용자 결정 2026-10-19: 끝 "참고 지표"를 뺌, 수집 시각 · 출처 문구는 그대로)
export const cardFooter = (updatedLabel: string) => `${updatedLabel} · 기사 제목 출처: 각 언론사(newsdata.io 수집) · 순위는 연관기사 수 기준`;

// 사이트와 같은 브랜드 마크(3칸 사각형: 왼쪽 큰 칸 + 오른쪽 위 · 아래, 아래는 흐리게). 18px 마크를 40px로
function BrandMark() {
  return (
    <div style={{ display: 'flex', width: 40, height: 40, gap: 2 }}>
      <div style={{ display: 'flex', width: 24, height: 40, background: TEXT, borderRadius: 3 }} />
      <div style={{ display: 'flex', flexDirection: 'column', width: 14, height: 40, gap: 2 }}>
        <div style={{ display: 'flex', width: 14, height: 24, background: TEXT, borderRadius: 3 }} />
        <div style={{ display: 'flex', width: 14, height: 14, background: TEXT, borderRadius: 3, opacity: 0.45 }} />
      </div>
    </div>
  );
}

export function CardView({ card }: { card: Card }) {
  return (
    <div style={{
      display: 'flex', flexDirection: 'column', width: CARD_WIDTH, height: CARD_HEIGHT, padding: 72,
      background: BG, color: TEXT, fontFamily: FONT, fontWeight: 400,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
          <BrandMark />
          <div style={{ display: 'flex', fontSize: 34, fontWeight: 700, letterSpacing: '-0.01em' }}>이슈랭크</div>
        </div>
        <div style={{ display: 'flex', fontSize: 30, color: MUTED }}>{card.dateLabel}</div>
      </div>

      <div style={{ display: 'flex', marginTop: 56, fontSize: 88, fontWeight: 700, lineHeight: 1.15, letterSpacing: '-0.03em' }}>오늘의 이슈</div>
      <div style={{ display: 'flex', marginTop: 12, fontSize: 34, color: MUTED }}>어제 가장 많이 보도된 뉴스</div>

      <div style={{ display: 'flex', flexDirection: 'column', flexGrow: 1, marginTop: 40, borderTop: `2px solid ${TEXT}` }}>
        {card.items.map((it, i) => (
          <div key={it.id} style={{
            display: 'flex', alignItems: 'center', flexGrow: 1, flexBasis: 0, gap: 24,
            borderBottom: `2px solid ${LINE}`,
          }}>
            <div style={{ display: 'flex', width: 44, fontSize: 44, fontWeight: 700, lineHeight: 1 }}>{i + 1}</div>
            <div style={{
              display: 'flex', justifyContent: 'center', width: 140, padding: '8px 0', borderRadius: 10,
              background: SECTION_COLOR[it.section], color: '#ffffff', fontSize: 24, fontWeight: 700, lineHeight: 1.2,
            }}>{sectionName(it.section)}</div>
            <div style={{ display: 'flex', flex: 1, fontSize: 40, fontWeight: 700, lineHeight: 1.3, letterSpacing: '-0.01em' }}>{cardTitle(it.title)}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'flex', alignItems: 'baseline', gap: 20, marginTop: 40, fontSize: 30 }}>
        <div style={{ display: 'flex', fontWeight: 700 }}>issuerank-kappa.vercel.app</div>
        <div style={{ display: 'flex', color: MUTED }}>섹션별 순위 · 3줄 요약 · 원문 링크</div>
      </div>
      <div style={{ display: 'flex', marginTop: 16, fontSize: 22, lineHeight: 1.45, color: MUTED }}>
        {cardFooter(card.updatedLabel)}
      </div>
    </div>
  );
}

// 카드 → PNG 바이트. ImageResponse는 스트림이라 렌더 실패가 200 뒤에 나므로 끝까지 받아 둠(실패하면 던짐)
export async function renderCard(card: Card): Promise<Uint8Array> {
  const image = new ImageResponse(<CardView card={card} />, { width: CARD_WIDTH, height: CARD_HEIGHT, fonts: await loadFonts() });
  return new Uint8Array(await image.arrayBuffer());
}

// GET /api/card 200 = 만든 PNG · CDN 300초. 렌더가 실패하면 500 { message }(캐시 헤더 없음, 로그는 안전 코드만)
export async function cardPngResponse(card: Card, render: (card: Card) => Promise<Uint8Array> = renderCard): Promise<Response> {
  let png: Uint8Array;
  try { png = await render(card); } catch {
    console.error('CARD_RENDER_FAILED');
    return Response.json({ message: RENDER_FAILED }, { status: 500 });
  }
  return new Response(png as Uint8Array<ArrayBuffer>, { status: 200, headers: { 'Content-Type': 'image/png', 'Cache-Control': CARD_CACHE } });
}
