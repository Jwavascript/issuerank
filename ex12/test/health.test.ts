// 운영 경보 판단(O9 · D37 · AC-01-4): server/health-check.ts — DB 없이 행으로
import { describe, expect, it } from 'vitest';
import { checkHealth, formatHealth, parseHealthArgs, todayResult, type HealthIssue, type HealthRun } from '../server/health-check';
import { SECTIONS, type SectionKey } from '../server/ranking';

const D = '2026-11-13';
// KST 시각 → ISO(UTC)
const at = (hhmm: string) => new Date(Date.parse(`${D}T${hhmm}:00+09:00`)).toISOString();
// 섹션마다 n개, 연관기사 수 = 10 - i, importance 3, 요약 있음
function issues(n = 6, over: (i: HealthIssue, k: number) => Partial<HealthIssue> = () => ({})): HealthIssue[] {
  return SECTIONS.flatMap(({ key }) => Array.from({ length: n }, (_, i) => {
    const base: HealthIssue = { id: `${D}-${key}-${i + 1}`, section: key, articleCount: 10 - i, importance: 3, latestPublishedAt: at('01:00'), summary: ['a', 'b', 'c'] };
    return { ...base, ...over(base, i) };
  }));
}
const runs = (time = '04:40', failed: SectionKey[] = []): HealthRun[] =>
  SECTIONS.map(({ key }) => ({ section: key, status: failed.includes(key) ? 'failed' : 'ok', finishedAt: at(time) }));
const codes = (day: { issues: HealthIssue[]; runs: HealthRun[] }, run?: string, mode: 'after' | 'morning' = 'after') =>
  checkHealth(day, { mode, run }).map(p => p.code);

describe('checkHealth', () => {
  it('정상 하루 = 문제 0', () => {
    expect(codes({ issues: issues(), runs: runs() }, 'success')).toEqual([]);
  });
  it('NO_RANKING: 이슈 0행(실행 기록도 없음)', () => {
    expect(codes({ issues: [], runs: [] })).toEqual(['NO_RANKING']);
  });
  it('morning은 NO_RANKING만 본다', () => {
    expect(codes({ issues: [], runs: [] }, undefined, 'morning')).toEqual(['NO_RANKING']);
    const bad = issues(6, () => ({ importance: 0, summary: null }));
    expect(codes({ issues: bad, runs: runs('09:19') }, 'failure', 'morning')).toEqual([]);
  });
  it('RUN_FAILED: 결론이 success가 아닐 때만, 결론을 안 받으면 안 봄', () => {
    for (const run of ['failure', 'cancelled', 'timed_out']) expect(codes({ issues: issues(), runs: runs() }, run)).toEqual(['RUN_FAILED']);
    expect(codes({ issues: issues(), runs: runs() }, undefined)).toEqual([]);
  });
  it('COLLECT_FAILED: 가장 늦은 수집 실행의 failed 섹션만(앞 실행의 실패는 안 셈)', () => {
    const p = checkHealth({ issues: issues(), runs: runs('04:40', ['soc', 'it']) }, { mode: 'after' });
    expect(p).toEqual([{ code: 'COLLECT_FAILED', text: '수집 실패 섹션 soc · it' }]);
    expect(codes({ issues: issues(), runs: [...runs('04:20', ['soc']), ...runs('04:40')] })).toEqual([]);
  });
  it('AI_FALLBACK: importance 전부 0 · 하나라도 1이면 아님', () => {
    expect(codes({ issues: issues(6, () => ({ importance: 0 })), runs: runs() })).toEqual(['AI_FALLBACK']);
    expect(codes({ issues: issues(6, (b, i) => ({ importance: b.section === 'it' && i === 5 ? 1 : 0 })), runs: runs() })).toEqual([]);
  });
  it('SUMMARY_LOW: Top 5(30개) 중 요약 없음 7은 아님 · 8부터 · Top 5 밖 요약 없음은 안 셈', () => {
    const missing = (n: number) => issues(6, (b, i) => {
      const idx = SECTIONS.findIndex(s => s.key === b.section) * 5 + i;
      return { summary: i < 5 && idx < n ? null : ['a', 'b', 'c'] };
    });
    expect(codes({ issues: missing(7), runs: runs() })).toEqual([]);
    const p = checkHealth({ issues: missing(8), runs: runs() }, { mode: 'after' });
    expect(p).toEqual([{ code: 'SUMMARY_LOW', text: '요약 없음 8/30' }]);
    expect(codes({ issues: issues(6, (_, i) => ({ summary: i === 5 ? null : ['a', 'b', 'c'] })), runs: runs() })).toEqual([]);
  });
  it('ALL_ONES: 한 섹션 Top 5가 모두 1건(5개 미만 섹션은 안 셈)', () => {
    const p = checkHealth({ issues: issues(6, b => (b.section === 'cul' ? { articleCount: 1 } : {})), runs: runs() }, { mode: 'after' });
    expect(p).toEqual([{ code: 'ALL_ONES', text: 'Top 5 전부 1건 섹션 cul' }]);
    expect(codes({ issues: issues(4, () => ({ articleCount: 1 })), runs: runs() })).toEqual([]);
  });
  it('LATE: ok 수집 완료 07:00은 아님 · 07:01부터 · 실패 실행 시각은 안 셈', () => {
    expect(codes({ issues: issues(), runs: runs('07:00') })).toEqual([]);
    expect(checkHealth({ issues: issues(), runs: runs('07:01') }, { mode: 'after' })).toEqual([{ code: 'LATE', text: '갱신 07:01 KST(07:00 뒤)' }]);
    const failedLate: HealthRun[] = [...runs('04:40'), { section: 'pol', status: 'failed', finishedAt: at('09:00') }];
    expect(codes({ issues: issues(), runs: failedLate })).toEqual(['COLLECT_FAILED']);
  });
  it('11-13 재현: AI 실패 · 요약 30개 없음 · 09:19 · 실행 실패', () => {
    const day = { issues: issues(6, () => ({ importance: 0, summary: null })), runs: runs('09:19') };
    expect(formatHealth(D, checkHealth(day, { mode: 'after', run: 'failure' }))).toEqual([
      `상태 문제 4 · ${D}`,
      '- RUN_FAILED 하루 실행 결론 failure',
      '- AI_FALLBACK 이슈 전부 importance 0 — AI 정리 실패로 규칙 묶기',
      '- SUMMARY_LOW 요약 없음 30/30',
      '- LATE 갱신 09:19 KST(07:00 뒤)',
    ]);
  });
});

describe('출력 · 인수 · today', () => {
  it('formatHealth OK 한 줄', () => {
    expect(formatHealth(D, [])).toEqual([`상태 OK ${D}`]);
  });
  it('parseHealthArgs', () => {
    expect(parseHealthArgs([])).toEqual({ mode: 'after' });
    expect(parseHealthArgs(['--mode', 'morning'])).toEqual({ mode: 'morning' });
    expect(parseHealthArgs(['--mode', 'after', '--run', 'failure'])).toEqual({ mode: 'after', run: 'failure' });
    expect(parseHealthArgs(['--run', ''])).toEqual({ mode: 'after' });
    for (const bad of [['--mode', 'x'], ['--run', 'a;b'], ['--run'], ['--x', '1']]) expect(parseHealthArgs(bad)).toBeNull();
  });
  it('todayResult: 있음 0 · 없음 1', () => {
    expect(todayResult(D, 30)).toEqual({ code: 0, line: `오늘 랭킹 있음 ${D}` });
    expect(todayResult(D, 0)).toEqual({ code: 1, line: `오늘 랭킹 없음 ${D}` });
  });
});
