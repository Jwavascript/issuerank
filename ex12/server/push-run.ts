// 아침 알림 발송 규칙(O5 · D30, AC-14-2 · 14-3 · 14-4): 시각 창 06:30~10:00 KST · KST 오늘 랭킹 · 기기별 그날 표시(하루 1통) · 결과 처리 · 실행 요약 · 종료 코드.
// 저장소 · 최근 수집일 · fetch · 시계 · sleep · env · 출력을 받아 씀(process.env · DB · 네트워크를 직접 읽지 않음). 명령은 push.ts, 워크플로는 push.yml
// 출력(= Actions 실행 요약, 공개 저장소라 누구나 봄)은 숫자 · 상태 코드 · 문구뿐 — endpoint · 키 · 행 id · 기기 코드 0
// 종료 코드: 0 보냄 또는 규칙대로 건너뜀(실패 0) · 1 일부 실패(되돌림 · 응답 없음) · 2 설정 오류(키 없음 · 키가 코드와 다름 · 시험 코드 틀림). DB 오류는 push.ts가 2
import { deviceCode, MORNING, pushService, TEST, validKeys, type PushPayload, type PushService } from './push-config';
import { encrypt, sendPush, vapidAuthorization, vapidKey, type SendResult } from './webpush';

export type PushRow = { id: string; endpoint: string; p256dh: string; auth: string; lastSentOn: string | null };
export type PushStore = {
  list(): Promise<PushRow[]>;                                          // 구독 전부
  mark(id: string, day: string): Promise<boolean>;                     // 그날 표시(보내기 전) — 이미 그날이면 false
  revert(id: string, day: string, previous: string | null): Promise<void>; // 그날 표시를 전 값으로(실패 → 같은 날 다시 실행하면 그 기기만 다시)
  remove(id: string): Promise<void>;                                   // 행 삭제(404 · 410 · 검사 실패)
};
export type PushDeps = {
  store: PushStore;
  latestDate: () => Promise<string | null>; // 하루 실행 쪽 DB 읽기(KST 오늘 이하 가장 늦은 랭킹 날짜)
  fetch: typeof fetch;
  now: () => number;
  sleep: (ms: number) => Promise<void>;
  env: Record<string, string | undefined>;  // VAPID_PRIVATE_KEY · GITHUB_ACTIONS
  log: (line: string) => void;
};

const KST = 9 * 3600e3, MIN = 60e3;
const kstDay = (ms: number) => new Date(ms + KST).toISOString().slice(0, 10);
const kstClock = (ms: number) => new Date(ms + KST).toISOString().slice(11, 16);
const kstMinutes = (ms: number) => { const d = new Date(ms + KST); return d.getUTCHours() * 60 + d.getUTCMinutes(); };
const START = 6 * 60 + 30, END = 10 * 60, EARLIEST = START - 180; // 06:30 · 10:00 · 03:30(이보다 이르면 기다리지 않고 건너뜀)
const POOL = 10;
const SERVICES: PushService[] = ['fcm', 'apple', 'mozilla', 'windows'];

// args: 없음(아침 규칙) 또는 --test <기기 코드 8자 16진수>
export async function runPush(args: string[], deps: PushDeps): Promise<number> {
  const { store, log } = deps;
  if (!(args.length === 0 || (args.length === 2 && args[0] === '--test'))) { log('--test <기기 코드>만 받습니다'); return 2; }
  const test = args.length === 2 ? args[1].trim().toLowerCase() : null;
  let at = deps.now();
  log(`아침 알림 ${kstDay(at)} · ${kstClock(at)} KST`);

  // 키 확인: 개인 키로 계산한 공개 키 = 코드 상수일 때만(다르면 fetch 0)
  const privateKey = deps.env.VAPID_PRIVATE_KEY ?? '';
  if (!privateKey) { log('VAPID_PRIVATE_KEY가 없습니다'); return 2; }
  const key = vapidKey(privateKey);
  if (!key) { log('VAPID 키가 코드의 공개 키와 다릅니다'); return 2; }
  log('키 확인 OK');
  const actions = deps.env.GITHUB_ACTIONS === 'true';

  // 시험 알림: 코드가 정확히 한 기기와 맞을 때만 그 기기에 [시험] 1통(시각 창 · 그날 표시 · 새 랭킹 여부 무시)
  if (test !== null) {
    if (!/^[0-9a-f]{8}$/.test(test)) { log('보내지 않음: 시험 코드는 8자 16진수입니다'); return 2; }
    const subs = await store.list();
    const hits = subs.filter(s => deviceCode(s.endpoint) === test);
    log(`시험 알림 · 구독 ${subs.length} · 코드에 맞는 기기 ${hits.length}`);
    if (hits.length === 0) { log('보내지 않음: 코드에 맞는 기기가 없습니다'); return 2; }
    if (hits.length > 1) { log('보내지 않음: 코드에 맞는 기기가 둘 이상입니다'); return 2; }
    if (!actions) { log('로컬 실행: 보내지 않음'); return 0; }
    return report(await sendAll(hits, TEST, null, key, deps), log);
  }

  // 아침 규칙: 10:00부터 · 03:30 전 = 건너뜀, KST 오늘 랭킹이 없으면 건너뜀, 06:30 전이면 06:30까지 기다린 뒤 다시 확인
  let skip: string | null = null;
  const minutes = kstMinutes(at);
  if (minutes >= END) skip = '10:00 지남';
  else if (minutes < EARLIEST) skip = '너무 이른 시각';
  else {
    skip = await noData(deps, at);
    if (!skip && minutes < START) {
      const wait = START * MIN - ((at + KST) % 864e5); // 지금(KST 하루 안의 ms) → 06:30:00까지
      log(`06:30까지 기다림(약 ${Math.ceil(wait / MIN)}분)`);
      if (actions) {
        await deps.sleep(wait);
        at = deps.now();
        skip = kstMinutes(at) >= END ? '10:00 지남' : await noData(deps, at);
      }
    }
  }
  const day = kstDay(at);
  const subs = await store.list();
  const already = subs.filter(s => s.lastSentOn === day).length;
  log(`구독 ${subs.length} · 이미 보냄 ${already} · 대상 ${subs.length - already}`);
  if (skip) log(`보내지 않음: ${skip}`);
  if (!actions) { log('로컬 실행: 보내지 않음'); return 0; }
  if (skip) return 0;
  return report(await sendAll(subs.filter(s => s.lastSentOn !== day), MORNING, day, key, deps), log);
}

// KST 오늘 랭킹이 없으면 건너뛸 이유(최근 수집일), 있으면 null
async function noData(deps: PushDeps, at: number): Promise<string | null> {
  const latest = await deps.latestDate();
  return latest === kstDay(at) ? null : `새 데이터 없음(최근 수집일 ${latest ?? '없음'})`;
}

type Tally = { sent: number; gone: number; failed: number; timeout: number; invalid: number; skipped: number; status: Map<string, number>; service: Map<PushService, number> };
const bump = <K,>(map: Map<K, number>, k: K) => map.set(k, (map.get(k) ?? 0) + 1);

// 대상마다(10개씩 동시에): 보내기 직전 같은 검사(못 지나면 보내지 않고 삭제) → 그날 표시(day가 있을 때 · 보내기 전) → 암호화 → 요청 →
// 2xx 그대로 · 404/410 삭제 · 응답 없음 그대로(이미 받았을 수 있음) · 그 밖 전 값으로 되돌림. JWT는 푸시 서비스 origin마다 한 번
async function sendAll(targets: PushRow[], payload: PushPayload, day: string | null, key: NonNullable<ReturnType<typeof vapidKey>>, deps: PushDeps): Promise<Tally> {
  const tally: Tally = { sent: 0, gone: 0, failed: 0, timeout: 0, invalid: 0, skipped: 0, status: new Map(), service: new Map() };
  const plaintext = Buffer.from(JSON.stringify(payload));
  const jwt = new Map<string, string>();
  const now = deps.now();
  const one = async (row: PushRow) => {
    const service = pushService(row.endpoint);
    if (!service || !validKeys(row.p256dh, row.auth)) { await deps.store.remove(row.id); tally.invalid++; return; }
    if (day && !(await deps.store.mark(row.id, day))) { tally.skipped++; return; }
    const origin = new URL(row.endpoint).origin;
    if (!jwt.has(origin)) jwt.set(origin, vapidAuthorization(key, origin, now));
    const result: SendResult = await sendPush(deps.fetch, row.endpoint, encrypt(plaintext, row.p256dh, row.auth), jwt.get(origin)!);
    bump(tally.status, result.label);
    bump(tally.service, service);
    if (result.kind === 'ok') tally.sent++;
    else if (result.kind === 'timeout') tally.timeout++;
    else if (result.kind === 'gone') { tally.gone++; await deps.store.remove(row.id); }
    else { tally.failed++; if (day) await deps.store.revert(row.id, day, row.lastSentOn); }
  };
  let next = 0;
  await Promise.all(Array.from({ length: Math.min(POOL, targets.length) }, async () => { while (next < targets.length) await one(targets[next++]); }));
  return tally;
}

// 요약 줄: 결과 수 · 상태별 수 · 서비스별 수 → 종료 코드(실패 · 응답 없음이 있으면 1)
function report(t: Tally, log: (line: string) => void): number {
  log(`보냄 ${t.sent} · 만료 삭제 ${t.gone} · 실패 ${t.failed} · 응답 없음 ${t.timeout}`
    + (t.invalid ? ` · 검사 실패 삭제 ${t.invalid}` : '') + (t.skipped ? ` · 다른 실행이 먼저 표시 ${t.skipped}` : ''));
  if (t.status.size) log(`상태: ${[...t.status].sort(([a], [b]) => statusOrder(a) - statusOrder(b) || a.localeCompare(b)).map(([k, n]) => `${k} ${n}`).join(' · ')}`);
  const services = SERVICES.filter(s => t.service.has(s));
  if (services.length) log(`서비스: ${services.map(s => `${s} ${t.service.get(s)}`).join(' · ')}`);
  return t.failed || t.timeout ? 1 : 0;
}
const statusOrder = (label: string) => /^\d+$/.test(label) ? Number(label) : 1000;
