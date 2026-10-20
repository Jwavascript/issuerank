// 웹 푸시 보내기(O5 · D30): RFC 8291 aes128gcm 암호화(한 레코드) + RFC 8292 VAPID 서명(ES256) + 요청 1건. node:crypto만(새 패키지 0)
// 발송 명령(push-run.ts → push.ts, GitHub Actions)만 import — 구독 API(Vercel 함수)는 push-config.ts 검사 함수만 씀(함수 번들에 이 파일 0)
import { createCipheriv, createECDH, createPrivateKey, hkdfSync, randomBytes, sign, type KeyObject } from 'node:crypto';
import { VAPID_PUBLIC_KEY, VAPID_SUBJECT } from './push-config';

const b64u = (buf: Uint8Array) => Buffer.from(buf).toString('base64url');
const RECORD_SIZE = 4096;     // rs(머리에 적는 레코드 크기)
const MAX_PLAINTEXT = 3993;   // RFC 8291 4: 한 레코드 평문 상한
export const TTL = 21600;     // 6시간(꺼진 휴대폰이 점심 전에 켜지면 받음, RFC 8030 필수)
export const TIMEOUT_MS = 10_000;

// 본문 = salt(16) ‖ rs(4) ‖ idlen(1) ‖ 서버 임시 공개 키(65) ‖ AES-128-GCM(평문 ‖ 0x02) + 태그(16).
// p256dh(65B) · auth(16B) = 브라우저 구독 키(base64url). fixed(salt · 서버 개인 키)는 RFC 8291 5절 예시를 맞춰 보는 시험에서만 넣음 — 평소는 매번 새로 뽑음
export function encrypt(plaintext: Uint8Array, p256dh: string, auth: string, fixed?: { salt: Uint8Array; serverPrivateKey: Uint8Array }): Buffer {
  if (plaintext.length > MAX_PLAINTEXT) throw new Error('PUSH_PAYLOAD_TOO_LARGE');
  const uaPublic = Buffer.from(p256dh, 'base64url'), authSecret = Buffer.from(auth, 'base64url');
  const ecdh = createECDH('prime256v1');
  if (fixed) ecdh.setPrivateKey(fixed.serverPrivateKey); else ecdh.generateKeys();
  const asPublic = ecdh.getPublicKey();
  const ecdhSecret = ecdh.computeSecret(uaPublic); // 곡선 밖 점이면 던짐
  const salt = Buffer.from(fixed?.salt ?? randomBytes(16));
  // IKM = HKDF(salt = auth, ECDH 비밀, "WebPush: info" ‖ 0 ‖ ua_public ‖ as_public, 32) → CEK 16 · NONCE 12(salt = 위 salt)
  const ikm = hkdfSync('sha256', ecdhSecret, authSecret, Buffer.concat([Buffer.from('WebPush: info\0'), uaPublic, asPublic]), 32);
  const cek = Buffer.from(hkdfSync('sha256', ikm, salt, Buffer.from('Content-Encoding: aes128gcm\0'), 16));
  const nonce = Buffer.from(hkdfSync('sha256', ikm, salt, Buffer.from('Content-Encoding: nonce\0'), 12));
  const cipher = createCipheriv('aes-128-gcm', cek, nonce);
  const sealed = Buffer.concat([cipher.update(Buffer.concat([plaintext, Buffer.from([2])])), cipher.final(), cipher.getAuthTag()]);
  const head = Buffer.alloc(21);
  salt.copy(head, 0);
  head.writeUInt32BE(RECORD_SIZE, 16);
  head[20] = asPublic.length;
  return Buffer.concat([head, asPublic, sealed]);
}

// VAPID 서명 키: 개인 키(32B base64url 43자)로 계산한 공개 키가 코드 상수와 같을 때만, 아니면 null
// (다른 키로 보내면 Mozilla는 404를 줘 구독을 지우게 됨 — 그래서 보내기 전에 막음)
export function vapidKey(privateKey: string): KeyObject | null {
  if (!/^[A-Za-z0-9_-]{43}$/.test(privateKey)) return null;
  try {
    const ecdh = createECDH('prime256v1');
    ecdh.setPrivateKey(Buffer.from(privateKey, 'base64url'));
    const point = ecdh.getPublicKey();
    if (b64u(point) !== VAPID_PUBLIC_KEY) return null;
    return createPrivateKey({ key: { kty: 'EC', crv: 'P-256', d: privateKey, x: b64u(point.subarray(1, 33)), y: b64u(point.subarray(33)) }, format: 'jwk' });
  } catch { return null; }
}

// Authorization 머리: vapid t={JWT}, k={공개 키}. JWT = {"typ":"JWT","alg":"ES256"} · { aud = endpoint origin, exp = 지금 + 12시간, sub } · ES256(r ‖ s 64B)
export function vapidAuthorization(key: KeyObject, origin: string, nowMs: number): string {
  const part = (value: object) => b64u(Buffer.from(JSON.stringify(value)));
  const unsigned = `${part({ typ: 'JWT', alg: 'ES256' })}.${part({ aud: origin, exp: Math.floor(nowMs / 1000) + 43200, sub: VAPID_SUBJECT })}`;
  const signature = sign('sha256', Buffer.from(unsigned), { key, dsaEncoding: 'ieee-p1363' });
  return `vapid t=${unsigned}.${b64u(signature)}, k=${VAPID_PUBLIC_KEY}`;
}

// 요청 1건의 결과: ok = 2xx · gone = 404 · 410(구독 만료 — 행 삭제) · timeout = 10초 안에 응답 없음(받았을 수 있음) · failed = 그 밖(상태 · 연결 실패)
export type SendResult = { kind: 'ok' | 'gone' | 'timeout' | 'failed'; label: string };

export async function sendPush(fetchFn: typeof fetch, endpoint: string, body: Uint8Array, authorization: string): Promise<SendResult> {
  try {
    const res = await fetchFn(endpoint, {
      method: 'POST',
      headers: {
        TTL: String(TTL), Urgency: 'normal', 'Content-Encoding': 'aes128gcm', 'Content-Type': 'application/octet-stream', Authorization: authorization,
      },
      body: new Uint8Array(body),
      redirect: 'error',
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    await res.body?.cancel().catch(() => {});
    const label = String(res.status);
    if (res.status >= 200 && res.status < 300) return { kind: 'ok', label };
    if (res.status === 404 || res.status === 410) return { kind: 'gone', label };
    return { kind: 'failed', label };
  } catch (err) {
    if ((err as { name?: unknown } | null)?.name === 'TimeoutError') return { kind: 'timeout', label: '응답 없음' };
    return { kind: 'failed', label: '연결 실패' };
  }
}
