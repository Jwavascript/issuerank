// 운영 경보 판단(O9 · D37 · AC-01-4): 그날(KST) 이슈 · 수집 실행 행을 받아 문제 코드 목록을 돌려줌. DB · env · 네트워크를 읽지 않음
// 명령은 health.ts, DB 읽기는 db/store.ts의 healthStore. 출력에는 숫자 · 코드 · 섹션 키 · 시각만(기사 제목 · 링크 · 비밀값 0)
import { rankIssues, SECTIONS, type SectionKey } from './ranking';

export type HealthIssue = { id: string; section: SectionKey; articleCount: number; importance: number; latestPublishedAt: string; summary: string[] | null };
export type HealthRun = { section: SectionKey; status: string; finishedAt: string };
export type HealthCode = 'NO_RANKING' | 'RUN_FAILED' | 'COLLECT_FAILED' | 'AI_FALLBACK' | 'SUMMARY_LOW' | 'ALL_ONES' | 'LATE' | 'CHECK_ERROR';
export type Problem = { code: HealthCode; text: string };
export type HealthMode = 'after' | 'morning';

export const SUMMARY_LOW_AT = 8; // Top 5(최대 30) 중 요약 없음이 이만큼 이상이면 문제
const LATE_AFTER = 7 * 60; // 07:00 KST(분). 07:01부터 늦음

// ISO 시각 → KST "HH:MM"
const kstTime = (iso: string) => new Date(Date.parse(iso) + 9 * 3600e3).toISOString().slice(11, 16);

// mode morning = 랭킹 없음만(같은 날 실행 뒤 검사와 겹치지 않게). run = daily 결론(workflow_run에서만 넘김)
export function checkHealth(day: { issues: HealthIssue[]; runs: HealthRun[] }, options: { mode: HealthMode; run?: string }): Problem[] {
  const problems: Problem[] = [];
  if (day.issues.length === 0) problems.push({ code: 'NO_RANKING', text: '그날 랭킹 없음' });
  if (options.mode === 'morning') return problems;

  if (options.run !== undefined && options.run !== 'success') problems.push({ code: 'RUN_FAILED', text: `하루 실행 결론 ${options.run || '없음'}` });

  if (day.runs.length > 0) {
    const last = Math.max(...day.runs.map(r => Date.parse(r.finishedAt)));
    const failed = day.runs.filter(r => Date.parse(r.finishedAt) === last && r.status !== 'ok').map(r => r.section);
    if (failed.length) problems.push({ code: 'COLLECT_FAILED', text: `수집 실패 섹션 ${failed.join(' · ')}` });
  }

  if (day.issues.length > 0) {
    if (day.issues.every(i => i.importance === 0)) problems.push({ code: 'AI_FALLBACK', text: '이슈 전부 importance 0 — AI 정리 실패로 규칙 묶기' });
    const tops = SECTIONS.map(({ key }) => ({ key, top: rankIssues(day.issues.filter(i => i.section === key)) }));
    const all = tops.flatMap(t => t.top);
    const missing = all.filter(i => i.summary === null || i.summary.length === 0).length;
    if (missing >= SUMMARY_LOW_AT) problems.push({ code: 'SUMMARY_LOW', text: `요약 없음 ${missing}/${all.length}` });
    const ones = tops.filter(t => t.top.length === 5 && t.top.every(i => i.articleCount === 1)).map(t => t.key);
    if (ones.length) problems.push({ code: 'ALL_ONES', text: `Top 5 전부 1건 섹션 ${ones.join(' · ')}` });
  }

  const ok = day.runs.filter(r => r.status === 'ok').map(r => Date.parse(r.finishedAt));
  if (ok.length) {
    const time = kstTime(new Date(Math.max(...ok)).toISOString());
    const [h, m] = time.split(':').map(Number);
    if (h * 60 + m > LATE_AFTER) problems.push({ code: 'LATE', text: `갱신 ${time} KST(07:00 뒤)` });
  }
  return problems;
}

// npm run today(daily.yml 오늘 확인 단계): 그날 이슈 행 수 → 종료 코드(0 있음 · 1 없음)와 한 줄
export const todayResult = (date: string, issueCount: number) =>
  issueCount > 0 ? { code: 0, line: `오늘 랭킹 있음 ${date}` } : { code: 1, line: `오늘 랭킹 없음 ${date}` };

// health 명령 인수: --run <결론>(선택) · --mode after|morning(기본 after). 틀리면 null
export function parseHealthArgs(args: string[]): { run?: string; mode: HealthMode } | null {
  const out: { run?: string; mode: HealthMode } = { mode: 'after' };
  for (let i = 0; i < args.length; i += 2) {
    const value = args[i + 1];
    if (args[i] === '--run' && value !== undefined && /^[a-z_]*$/.test(value)) { if (value !== '') out.run = value; }
    else if (args[i] === '--mode' && (value === 'after' || value === 'morning')) out.mode = value;
    else return null;
  }
  return out;
}

// 출력 줄: 첫 줄 `상태 OK {날짜}` 또는 `상태 문제 {n} · {날짜}`, 그 뒤 문제마다 `- {코드} {문구}`
export function formatHealth(date: string, problems: Problem[]): string[] {
  if (problems.length === 0) return [`상태 OK ${date}`];
  return [`상태 문제 ${problems.length} · ${date}`, ...problems.map(p => `- ${p.code} ${p.text}`)];
}
