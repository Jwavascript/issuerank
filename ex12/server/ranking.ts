// 순위 규칙(AC-03-2)과 랭킹 · 위젯 목록(D22) 조립. 메모리 저장소(sample.ts) · DB 저장소(db/store.ts)가 같이 씀
export type SectionKey = 'pol' | 'eco' | 'soc' | 'cul' | 'wor' | 'it';
export type Issue = {
  id: string; date: string; section: SectionKey; rank: number; title: string;
  summary: string[] | null; articleCount: number; url: string;
};
export type Ranking = { date: string; updatedAt: string; sections: { key: SectionKey; name: string; issues: Issue[] }[] };
// 저장된 이슈 한 행(랭킹 후보). rank는 저장하지 않고 조회 때 계산
export type IssueRow = Omit<Issue, 'rank'> & { latestPublishedAt: string; importance: number };

export const SECTIONS: { key: SectionKey; name: string }[] = [
  { key: 'pol', name: '정치' },
  { key: 'eco', name: '경제' },
  { key: 'soc', name: '사회' },
  { key: 'cul', name: '생활·문화' },
  { key: 'wor', name: '세계' },
  { key: 'it', name: 'IT·과학' },
];

export type Candidate = { id: string; articleCount: number; importance?: number; latestPublishedAt: string };

// D16 순서: 연관기사 수 ↓ → importance ↓(없는 값은 0) → 최근 발행 시각 ↓ → id 자연 순서(숫자 부분은 수로, …-pol-9 < …-pol-10)
// 섹션 Top 5(rankIssues → 화면 · 북마크 카드 · 요약 대상)와 위젯 순서(buildWidget)가 같이 씀
export function compareIssues(a: Candidate, b: Candidate): number {
  return b.articleCount - a.articleCount
    || (b.importance ?? 0) - (a.importance ?? 0)
    || Date.parse(b.latestPublishedAt) - Date.parse(a.latestPublishedAt)
    || a.id.localeCompare(b.id, 'en', { numeric: true });
}

// D16 순서로 앞 5개에 rank 1~5
export function rankIssues<T extends Candidate>(candidates: T[]): (T & { rank: number })[] {
  return [...candidates]
    .sort(compareIssues)
    .slice(0, 5)
    .map((c, i) => ({ ...c, rank: i + 1 }));
}

// 날짜 D의 랭킹(섹션 6개, 카드 8칸, 발행 시각 빼고). D의 행이 없으면 null
// rows = D의 행 + (있으면) D 이전 행. 섹션마다 D 이하에서 그 섹션 행이 있는 가장 늦은 날짜의 행으로 순위를 매김
// → D에 그 섹션 행이 없으면(그날 수집 실패) 직전 날짜의 Top 5, 카드 id · date는 그 날짜(D8, AC-01-2. 행 복사 없음)
// runs = D의 collect_runs. updatedAt = ok 실행 중 가장 늦은 finishedAt을 KST(초 아래 버림)로, 없으면 D 06:00(AC-05-1)
export function buildRanking(date: string, rows: IssueRow[], runs: { status: string; finishedAt: string }[] = []): Ranking | null {
  if (!rows.some(r => r.date === date)) return null;
  const sections = SECTIONS.map(({ key, name }) => {
    const mine = rows.filter(r => r.section === key && r.date <= date);
    const day = mine.reduce((d, r) => (r.date > d ? r.date : d), '');
    const issues = rankIssues(mine.filter(r => r.date === day))
      .map(({ id, date, section, rank, title, summary, articleCount, url }): Issue =>
        ({ id, date, section, rank, title, summary, articleCount, url }));
    return { key, name, issues };
  });
  const ok = runs.filter(r => r.status === 'ok').map(r => Date.parse(r.finishedAt));
  const updatedAt = ok.length
    ? `${new Date(Math.max(...ok) + 9 * 3600e3).toISOString().slice(0, 19)}+09:00`
    : `${date}T06:00:00+09:00`;
  return { date, updatedAt, sections };
}

export type WidgetItem = { id: string; section: SectionKey; title: string; path: string };
export type Widget = { date: string; updatedAt: string; updatedLabel: string; morePath: string; items: WidgetItem[] };

// updatedAt(+09:00) → "10.17 오전 4:31 수집"(화면 날짜 줄 fmtUpdated + " 수집"과 같은 꼴, 0시 → 오전 12시 · 12시 → 오후 12시)
export function updatedLabel(updatedAt: string): string {
  const [day, time] = updatedAt.split('T');
  const [, month, date] = day.split('-').map(Number);
  const [h, m] = time.split(':').map(Number);
  return `${month}.${date} ${h < 12 ? '오전' : '오후'} ${h % 12 || 12}:${String(m).padStart(2, '0')} 수집`;
}

// 위젯 목록(D22 · AC-11-1): 날짜 D의 랭킹 카드 전부(buildRanking과 같은 카드 — 섹션마다 Top 5, D8 대체 섹션 포함, 최대 30)를
// ① 섹션 1위들 → ② 나머지(2~5위) 순으로, 각각 D16(compareIssues). 항목 칸은 id · section · title · path(/?issue={id})뿐. D의 행이 없으면 null
export function buildWidget(date: string, rows: IssueRow[], runs: { status: string; finishedAt: string }[] = []): Widget | null {
  const ranking = buildRanking(date, rows, runs);
  if (!ranking) return null;
  const byId = new Map(rows.map(r => [r.id, r]));
  const cards = ranking.sections.flatMap(s => s.issues.map(c => ({ ...byId.get(c.id)!, rank: c.rank })));
  const items = [...cards.filter(c => c.rank === 1).sort(compareIssues), ...cards.filter(c => c.rank > 1).sort(compareIssues)]
    .map(({ id, section, title }) => ({ id, section, title, path: `/?issue=${encodeURIComponent(id)}` }));
  return { date: ranking.date, updatedAt: ranking.updatedAt, updatedLabel: updatedLabel(ranking.updatedAt), morePath: '/#list', items };
}

// 행들 중 id의 카드. rank는 그 이슈 날짜의 랭킹(GET /api/rankings?date=와 같음)에서 찾음
export function findCard(id: string, rows: IssueRow[]): Issue | null {
  const row = rows.find(r => r.id === id);
  if (!row) return null;
  const ranking = buildRanking(row.date, rows.filter(r => r.date === row.date));
  for (const s of ranking!.sections) { const it = s.issues.find(x => x.id === id); if (it) return it; }
  return null;
}
