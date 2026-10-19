// 인스타 "오늘의 이슈" 카드(O3 · D28 · AC-12-1~4): 그날 위젯 목록(D22 · AC-11-1)의 앞 6개 → 카드 데이터 · 캡션
// GET /api/card(PNG, app/api/card/route.tsx)와 GET /api/card/caption(JSON)이 같은 함수를 씀. 그림은 card-image.tsx
// 이 파일은 DB · 글꼴을 읽지 않음(저장소는 처리기에 넘겨받음) → 캡션 함수에는 글꼴이 들어가지 않음
import type { Store } from './app';
import { SECTIONS, type SectionKey, type Widget } from './ranking';
import { BAD_REQUEST, rankingsQuery } from './schemas';

export const CARD_ROWS = 6;      // 카드 줄 수
export const MAX_HIDE = 3;       // 운영자가 뺄 수 있는 줄 수
export const CARD_CACHE = 'public, max-age=0, s-maxage=300'; // 200만, CDN 300초(위젯과 같음)
const TITLE_MAX = 34;            // 카드 제목 코드 포인트 수(넘으면 앞 33자 + …)
const CAPTION_TITLE_MAX = 100;   // 캡션 제목 코드 포인트 수(넘으면 앞 99자 + …)
const UNAVAILABLE = 'API 서버에 연결할 수 없습니다';
const NO_DATA = '해당 날짜의 랭킹 데이터가 없습니다';

export type CardItem = { id: string; section: SectionKey; title: string };
export type Card = { date: string; dateLabel: string; updatedLabel: string; items: CardItem[]; hidden: string[] };

// 그림 문자(이모지) 제거. @vercel/og는 그림 문자를 jsdelivr에서 받으러 나가므로 카드 · 캡션 모두에서 뺌
// Extended_Pictographic + 국기(지역 표시 문자) · 피부색 · 변형 선택자 · ZWJ · 키캡(그림 문자를 이루는 나머지 조각)
const PICTOGRAPHS = /[\p{Extended_Pictographic}\p{Regional_Indicator}\u{1F3FB}-\u{1F3FF}\u{FE0F}\u{200D}\u{20E3}]/gu;
const tidy = (title: string) => title.replace(PICTOGRAPHS, '').replace(/\s+/g, ' ').trim();
// max자 이하는 그대로, 넘으면 앞 max-1자에서 끝 공백을 지우고 …(사용자 결정 2026-10-19: "입장 …" → "입장…")
const clip = (text: string, max: number) => {
  const points = [...text];
  return points.length > max ? `${points.slice(0, max - 1).join('').trimEnd()}…` : text;
};

// 카드에 그리는 제목: 그림 문자 제거 · 공백 정리 → 34자 넘으면 33자(끝 공백 지움) + …(낱말은 원문 그대로)
export const cardTitle = (title: string) => clip(tidy(title), TITLE_MAX);

// 2026-10-19 → "10월 19일 월요일"
export function dateLabel(date: string): string {
  const [y, m, d] = date.split('-').map(Number);
  return `${m}월 ${d}일 ${'일월화수목금토'[new Date(Date.UTC(y, m - 1, d)).getUTCDay()]}요일`;
}

// ?date=&hide=id,id 검사: date = 기존 rankingsQuery(형식 → 미래), hide = 최대 3개 · 중복 없음. 위젯에 있는지는 buildCard에서
export function parseCardQuery(params: URLSearchParams): { ok: true; date?: string; hide: string[] } | { ok: false; message: string } {
  const parsed = rankingsQuery.safeParse({ date: params.get('date') ?? undefined });
  if (!parsed.success) return { ok: false, message: parsed.error.issues[0].message };
  const raw = params.get('hide');
  const hide = raw ? raw.split(',') : [];
  if (hide.length > MAX_HIDE || new Set(hide).size !== hide.length) return { ok: false, message: BAD_REQUEST };
  return { ok: true, date: parsed.data.date, hide };
}

// 위젯 목록 → 카드: 뺀 id를 건너뛰고 앞에서 6개(적으면 있는 만큼). 그날 목록에 없는 id를 빼려 하거나 남는 줄이 없으면 null(400)
export function buildCard(widget: Widget, hide: string[]): Card | null {
  if (hide.some(id => !widget.items.some(it => it.id === id))) return null;
  const items = widget.items.filter(it => !hide.includes(it.id)).slice(0, CARD_ROWS)
    .map(({ id, section, title }) => ({ id, section, title }));
  if (items.length === 0) return null;
  return { date: widget.date, dateLabel: dateLabel(widget.date), updatedLabel: widget.updatedLabel, items, hidden: [...hide] };
}

export const sectionName = (key: SectionKey) => SECTIONS.find(s => s.key === key)!.name;

// 캡션(D28 꼴 그대로). 제목은 원문(그림 문자만 빼고 공백 정리), 100자 넘으면 99자(끝 공백 지움) + …
export function cardCaption(card: Card): string {
  const [, m, d] = card.date.split('-').map(Number);
  const lines = card.items.map((it, i) => `${i + 1}. [${sectionName(it.section)}] ${clip(tidy(it.title), CAPTION_TITLE_MAX)}`);
  return [
    `${m}월 ${d}일 오늘의 이슈 — 어제 가장 많이 보도된 뉴스`,
    '',
    ...lines,
    '',
    '섹션별 순위 · 3줄 요약 · 원문 링크는 프로필 링크의 이슈랭크에서 볼 수 있어요.',
    '기사 제목 출처: 각 언론사(newsdata.io 수집). 순위는 연관기사 수 기준 참고 지표입니다.',
    '',
    '#오늘의이슈 #뉴스 #헤드라인 #시사 #이슈랭크',
  ].join('\n');
}

// GET /api/card/caption 200 본문(items = 카드와 같은 6개 · 같은 순서 · 원문 제목)
export const captionBody = (card: Card) => ({ ...card, caption: cardCaption(card) });
export const captionResponse = (card: Card) => Response.json(captionBody(card), { headers: { 'Cache-Control': CARD_CACHE } });

const fail = (status: number, message: string) => Response.json({ message }, { status });

// 두 라우트 공통 처리기: 쿼리 검사(DB 전) → 저장소 준비(첫 요청 때 한 번, 실패하면 다음 요청이 다시) → 날짜(생략 = 최근 수집일) → 위젯 → 카드 → respond
// 저장소 준비 · 조회가 실패하면 502(postgres는 첫 조회 때 접속). 실패 응답에는 캐시 헤더 · Set-Cookie 없음. 로그는 안전 코드만(오류 객체 · 스택 · env 출력 0)
export function createCardHandler(load: () => Store | Promise<Store>, respond: (card: Card) => Response | Promise<Response>) {
  let ready: Promise<Store> | undefined;
  return async function handle(req: Request): Promise<Response> {
    const query = parseCardQuery(new URL(req.url).searchParams);
    if (!query.ok) return fail(400, query.message);
    let widget: Widget | null;
    try {
      const pending = ready ??= Promise.resolve().then(load);
      let store: Store;
      try { store = await pending; } catch (err) {
        if (ready === pending) ready = undefined;
        throw err;
      }
      const date = query.date ?? await store.latestDate();
      widget = date ? await store.widget(date) : null;
    } catch {
      console.error('CARD_START_FAILED');
      return fail(502, UNAVAILABLE);
    }
    if (!widget) return fail(404, NO_DATA);
    const card = buildCard(widget, query.hide);
    if (!card) return fail(400, BAD_REQUEST);
    return respond(card);
  };
}
