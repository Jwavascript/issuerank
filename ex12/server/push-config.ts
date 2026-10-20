// 아침 알림(O5 · D30) 설정 · 검사 한 곳: VAPID 공개 키 · subject · 알림 문구 · tag · url · 받는 푸시 서비스(SSRF 방지) · 상한 · 구독 키 검사 · 기기 코드
// 구독 API(routes/push.ts — Vercel 함수)와 발송 명령(push-run.ts — Actions)이 같이 씀. 암호화 · 서명은 webpush.ts(명령만 import)
import { createHash, createPublicKey } from 'node:crypto';

// 공개 키(P-256 비압축 65B, base64url 87자) — 비밀 아님(브라우저 applicationServerKey · 요청마다 k=). public/index.html PUSH_KEY와 같은 값(테스트가 비교)
// 짝인 개인 키는 ex12/.env · GitHub Actions Secret VAPID_PRIVATE_KEY에만(Vercel env에는 넣지 않음). npm run push:keys로 한 번 만듦
export const VAPID_PUBLIC_KEY = 'BH8iVJ-iq1b6TqxcnK4kkV0R7ZuD_qeF0zHKgA864mWlRbhi3qlBuQD8d-N95LThbBc-OuraowXCbrzLfWS-AN0';
// JWT sub(RFC 8292 "mailto: 또는 https:", Apple은 URL · mailto만) — 이메일은 쓰지 않음(D26)
export const VAPID_SUBJECT = 'https://issuerank-kappa.vercel.app';

// 알림 = 고정 문구만(뉴스 내용 · 숫자 · 날짜 0, AC-14-3). 같은 tag는 서비스 워커가 앞 알림을 닫고 새로 띄움. 누르면 /p(측정 경로 = 첫 화면)
export type PushPayload = { title: string; body: string; tag: string; url: string };
export const MORNING: PushPayload = { title: '오늘의 뉴스가 도착했어요', body: '', tag: 'issuerank-daily', url: '/p' };
export const TEST: PushPayload = { title: '[시험] 오늘의 뉴스가 도착했어요', body: '시험 알림입니다', tag: 'issuerank-test', url: '/p' };

export const MAX_SUBSCRIPTIONS = 5000; // 전체 행 상한(새 endpoint만 막고 기존 갱신은 됨)
export const BODY_LIMIT = 4096;        // 구독 API 본문 상한(바이트)
const ENDPOINT_MAX = 2048;

// 받는 푸시 서비스(이 목록 밖은 400 · 보내기 전에도 다시 검사). 정확히 같은 호스트 2 + 점부터 맞추는 접미사 2
export type PushService = 'fcm' | 'mozilla' | 'apple' | 'windows';
const EXACT: Record<string, PushService> = { 'fcm.googleapis.com': 'fcm', 'updates.push.services.mozilla.com': 'mozilla' };
const SUFFIX: [string, PushService][] = [['.push.apple.com', 'apple'], ['.notify.windows.com', 'windows']];

// endpoint 검사: https: · ≤ 2,048자 · 공백 · 제어 문자 · # · \ 없음 · 주소 부분에 포트 · 사용자 정보(: · @) 없음 · IP · localhost 아님 ·
// 호스트(소문자)가 위 목록. 맞으면 서비스 이름, 아니면 null
export function pushService(endpoint: unknown): PushService | null {
  if (typeof endpoint !== 'string' || endpoint.length > ENDPOINT_MAX || /[\s#\\\u0000-\u001f\u007f]/.test(endpoint)) return null;
  if (!/^https:\/\//i.test(endpoint)) return null;
  const authority = endpoint.slice(8).split(/[/?]/)[0];
  if (!authority || /[:@]/.test(authority)) return null;
  let host: string;
  try {
    const url = new URL(endpoint);
    if (url.protocol !== 'https:' || url.username || url.password || url.port) return null;
    host = url.hostname;
  } catch { return null; }
  if (host === 'localhost' || /^\d+(\.\d+){3}$/.test(host) || host.startsWith('[')) return null;
  if (Object.hasOwn(EXACT, host)) return EXACT[host];
  for (const [suffix, name] of SUFFIX) if (host.endsWith(suffix) && host.length > suffix.length) return name;
  return null;
}

// base64url(패딩 = 허용) → 바이트, 형식이 틀리면 null
function bytes(value: unknown): Buffer | null {
  if (typeof value !== 'string' || !/^[A-Za-z0-9_-]+={0,2}$/.test(value)) return null;
  return Buffer.from(value, 'base64url');
}

// 구독 키 검사: p256dh = 65B · 첫 바이트 0x04 · P-256 곡선 위의 점, auth = 16B
export function validKeys(p256dh: unknown, auth: unknown): boolean {
  const point = bytes(p256dh), secret = bytes(auth);
  if (!point || point.length !== 65 || point[0] !== 4 || !secret || secret.length !== 16) return false;
  try {
    createPublicKey({ key: { kty: 'EC', crv: 'P-256', x: point.subarray(1, 33).toString('base64url'), y: point.subarray(33).toString('base64url') }, format: 'jwk' });
    return true;
  } catch { return false; }
}

// 기기 코드 = SHA-256(endpoint) 16진수 앞 8자(/widget#push에 보임 · 시험 알림 수동 실행에 넣음). 비밀 아님
export const deviceCode = (endpoint: string) => createHash('sha256').update(endpoint).digest('hex').slice(0, 8);
