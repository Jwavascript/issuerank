// PWA 아이콘 5개 + 사이트 공통 OG 그림 1개(O4 · D29) + 알림 배지 1개(O5 · D30): next/og ImageResponse(O3와 같음 — 새 패키지 0)로 한 번 그려 저장소에 둠.
// 런타임 라우트가 아니라 정적 파일(public/)이라 배포에서 함수 호출 0. 글꼴 = 저장소 Noto Sans KR(OFL, O3)
// 실행(ex12, 래퍼): npx tsx tools/pwa-images.tsx [출력 폴더 — 기본 public]
// 실행 중 fetch는 data:(Satori 내장 yoga.wasm)만 통과, 그 밖은 기록하고 던짐 → 하나라도 있으면 파일을 쓰지 않고 실패(바깥 요청 0)
import { createHash } from 'node:crypto';
import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, join, resolve } from 'node:path';
import { ImageResponse } from 'next/og';
import type { ReactElement } from 'react';

const FONT = 'Noto Sans KR';
// 사이트 밝은 토큰(index.html :root)
const INK = '#161616';
const BG = '#f7f7f5';
const MUTED = '#6f6b65';
const LINE = '#e7e4df';
const WHITE = '#ffffff';
const SECTIONS: [string, string][] = [
  ['정치', '#6476c9'], ['경제', '#3d9a74'], ['사회', '#c8843a'], ['생활·문화', '#b8638f'], ['세계', '#3f93ad'], ['IT·과학', '#8467c4'],
];

// 사이트 · O3 카드와 같은 3칸 마크(왼쪽 큰 칸 + 오른쪽 위 · 아래, 아래 칸 45%). 비율 = O3 카드 40px 마크(24 · 2 · 14, 모서리 3),
// 칸 경계가 번지지 않게 정수 픽셀로 반올림
function Mark({ size, color }: { size: number; color: string }) {
  const big = Math.round(size * 0.6), gap = Math.max(1, Math.round(size * 0.05)), small = size - big - gap;
  const radius = Math.max(1, Math.round(size * 0.075));
  return (
    <div style={{ display: 'flex', width: size, height: size, gap }}>
      <div style={{ display: 'flex', width: big, height: size, background: color, borderRadius: radius }} />
      <div style={{ display: 'flex', flexDirection: 'column', width: small, height: size, gap }}>
        <div style={{ display: 'flex', width: small, height: big, background: color, borderRadius: radius }} />
        <div style={{ display: 'flex', width: small, height: small, background: color, borderRadius: radius, opacity: 0.45 }} />
      </div>
    </div>
  );
}

// purpose any: 투명 바탕 + #161616 둥근 사각 타일(모서리 22%) + 흰 마크(한 변 50%)
function AnyIcon({ size }: { size: number }) {
  return (
    <div style={{ display: 'flex', width: size, height: size, alignItems: 'center', justifyContent: 'center', background: INK, borderRadius: Math.round(size * 0.22) }}>
      <Mark size={Math.round(size * 0.5)} color={WHITE} />
    </div>
  );
}

// 온 면 #161616 + 흰 마크. maskable: 마크 한 변 40%(꼭짓점이 중심에서 0.28W — 안전 영역 반지름 0.4W 안), apple-touch: 50%(iOS는 모서리만 둥글게 자름)
function FullIcon({ size, mark }: { size: number; mark: number }) {
  return (
    <div style={{ display: 'flex', width: size, height: size, alignItems: 'center', justifyContent: 'center', background: INK }}>
      <Mark size={Math.round(size * mark)} color={WHITE} />
    </div>
  );
}

// 알림 배지(O5 · D30) 96×96: 투명 바탕 + 흰 마크(한 변 75%). Android 상태 표시줄은 알파만 씀(불투명 타일 아이콘을 쓰면 흰 사각형으로만 보임)
function Badge({ size }: { size: number }) {
  return (
    <div style={{ display: 'flex', width: size, height: size, alignItems: 'center', justifyContent: 'center' }}>
      <Mark size={Math.round(size * 0.75)} color={WHITE} />
    </div>
  );
}

// 사이트 공통 OG 1200×630: 날짜 · 기사 제목 · 숫자 없음(정적이라 날마다 같은 그림)
function OgImage() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', width: 1200, height: 630, padding: '64px 72px 56px', background: BG, color: INK, fontFamily: FONT, fontWeight: 400 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <Mark size={40} color={INK} />
        <div style={{ display: 'flex', fontSize: 36, fontWeight: 700, letterSpacing: '-0.01em' }}>이슈랭크</div>
      </div>
      <div style={{ display: 'flex', marginTop: 68, fontSize: 76, fontWeight: 700, lineHeight: 1.15, letterSpacing: '-0.03em' }}>어제 가장 많이 보도된 뉴스</div>
      <div style={{ display: 'flex', marginTop: 18, fontSize: 36, color: MUTED }}>섹션별 순위 · 3줄 요약 · 원문 링크</div>
      <div style={{ display: 'flex', gap: 12, marginTop: 40 }}>
        {SECTIONS.map(([name, color]) => (
          <div key={name} style={{ display: 'flex', padding: '8px 20px', borderRadius: 10, background: color, color: WHITE, fontSize: 26, fontWeight: 700, lineHeight: 1.2 }}>{name}</div>
        ))}
      </div>
      <div style={{ display: 'flex', flexGrow: 1 }} />
      <div style={{ display: 'flex', paddingTop: 22, borderTop: `2px solid ${LINE}`, fontSize: 30, fontWeight: 700 }}>issuerank-kappa.vercel.app</div>
    </div>
  );
}

const IMAGES: { file: string; size: [number, number]; node: ReactElement }[] = [
  { file: 'icons/icon-192.png', size: [192, 192], node: <AnyIcon size={192} /> },
  { file: 'icons/icon-512.png', size: [512, 512], node: <AnyIcon size={512} /> },
  { file: 'icons/maskable-192.png', size: [192, 192], node: <FullIcon size={192} mark={0.4} /> },
  { file: 'icons/maskable-512.png', size: [512, 512], node: <FullIcon size={512} mark={0.4} /> },
  { file: 'icons/apple-touch-icon.png', size: [180, 180], node: <FullIcon size={180} mark={0.5} /> },
  { file: 'og.png', size: [1200, 630], node: <OgImage /> },
  { file: 'icons/badge-96.png', size: [96, 96], node: <Badge size={96} /> },
];

async function main() {
  const out = resolve(process.argv[2] ?? 'public');
  const realFetch = globalThis.fetch;
  const blocked: string[] = [];
  let dataFetches = 0;
  globalThis.fetch = (async (input: string | URL | Request, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url;
    if (url.startsWith('data:')) { dataFetches++; return realFetch(input, init); }
    blocked.push(url.slice(0, 80));
    throw new Error('바깥 요청 금지');
  }) as typeof fetch;

  const font = (name: string) => readFile(join(process.cwd(), 'assets/fonts', name));
  const [regular, bold] = await Promise.all([font('NotoSansKR-Regular.otf'), font('NotoSansKR-Bold.otf')]);
  const fonts = [
    { name: FONT, data: regular, weight: 400 as const, style: 'normal' as const },
    { name: FONT, data: bold, weight: 700 as const, style: 'normal' as const },
  ];

  const rendered: { file: string; size: [number, number]; png: Buffer }[] = [];
  for (const image of IMAGES) {
    const [width, height] = image.size;
    const png = Buffer.from(await new ImageResponse(image.node, { width, height, fonts }).arrayBuffer());
    rendered.push({ file: image.file, size: image.size, png });
  }
  if (blocked.length) {
    console.error(JSON.stringify({ written: 0, dataFetches, blocked }));
    process.exitCode = 1;
    return;
  }
  for (const { file, size, png } of rendered) {
    const path = join(out, file);
    await mkdir(dirname(path), { recursive: true });
    await writeFile(path, png);
    console.log(`${file} ${size[0]}x${size[1]} ${png.length} B sha256 ${createHash('sha256').update(png).digest('hex')}`);
  }
  console.log(JSON.stringify({ written: rendered.length, out, dataFetches, blocked: blocked.length }));
}

main().catch((error: unknown) => {
  console.error('PWA_IMAGES_FAILED', error instanceof Error ? error.message : String(error));
  process.exitCode = 1;
});
