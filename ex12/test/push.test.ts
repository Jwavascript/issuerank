// 아침 알림 발송(O5 · D30 · AC-14-2 · 14-3 · 14-4): webpush.ts(RFC 8291 aes128gcm · RFC 8292 VAPID) · push-run.ts(시각 창 · 그날 랭킹 · 하루 1통 · 결과 · 요약)를
// 가짜 fetch(가짜 푸시 서비스) · 메모리 저장소 · 가짜 시계 · 가짜 sleep으로. 실제 푸시 서비스 요청 0.
// VAPID 키는 이 파일이 실행마다 만든 일회용 — 코드 상수(VAPID_PUBLIC_KEY)만 이 파일 안에서 그 공개 키로 바꿈(실제 개인 키는 쓰지 않음)
import { createPublicKey, generateKeyPairSync, verify } from 'node:crypto';
import { describe, expect, it, vi } from 'vitest';

const VAPID = await vi.hoisted(async () => {
  const { generateKeyPairSync: gen } = await import('node:crypto');
  const pair = (() => {
    const { privateKey, publicKey } = gen('ec', { namedCurve: 'prime256v1' });
    const pub = publicKey.export({ format: 'jwk' });
    return { privateKey: privateKey.export({ format: 'jwk' }).d as string, publicKey: Buffer.concat([Buffer.from([4]), Buffer.from(pub.x!, 'base64url'), Buffer.from(pub.y!, 'base64url')]).toString('base64url') };
  })();
  return pair;
});
vi.mock('../server/push-config', async importOriginal => ({ ...(await importOriginal<typeof import('../server/push-config')>()), VAPID_PUBLIC_KEY: VAPID.publicKey }));

import { deviceCode, MORNING, TEST, VAPID_PUBLIC_KEY } from '../server/push-config';
import { runPush, type PushDeps, type PushRow } from '../server/push-run';
import { memoryPushStore, memoryStore } from '../server/sample';
import { encrypt } from '../server/webpush';

const subtle = globalThis.crypto.subtle;
const bs = (u: Uint8Array) => u as unknown as BufferSource;
const b64 = (s: string) => Buffer.from(s, 'base64url');
const utf8 = (s: string) => new TextEncoder().encode(s);
const cat = (...parts: Uint8Array[]) => { const out = new Uint8Array(parts.reduce((n, p) => n + p.length, 0)); let at = 0; for (const p of parts) { out.set(p, at); at += p.length; } return out; };
const MORNING_JSON = '{"title":"오늘의 뉴스가 도착했어요","body":"","tag":"issuerank-daily","url":"/p"}';
const TEST_JSON = '{"title":"[시험] 오늘의 뉴스가 도착했어요","body":"시험 알림입니다","tag":"issuerank-test","url":"/p"}';
const kst = (iso: string) => Date.parse(`${iso}+09:00`);

// RFC 8291 5절 예시(부록 A 입력 — 서버 개인 키 · salt 주입)
const RFC = {
  plaintext: 'When I grow up, I want to be a watermelon',
  asPrivate: 'yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw',
  asPublic: 'BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8',
  uaPrivate: 'q1dXpw3UpT5VOmu_cf_v6ih07Aems3njxI-JWgLcM94',
  uaPublic: 'BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4',
  auth: 'BTBZMqHH6r4Tts7J_aSIgg',
  salt: 'DGv6ra1nlYgDCS1FRnbzlw',
  body: 'DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN',
};

// 받는 쪽(브라우저 역할)을 WebCrypto로 따로 씀: ECDH(받는 기기 개인 키, 머리의 서버 공개 키) → HKDF → AES-GCM 풀기 → 끝 구분자 0x02 확인
async function decryptWeb(body: Uint8Array, ua: { privateKey: CryptoKey; publicRaw: Uint8Array; auth: Uint8Array }) {
  const salt = body.slice(0, 16), rs = new DataView(body.buffer, body.byteOffset).getUint32(16), idlen = body[20];
  const asPublic = body.slice(21, 21 + idlen);
  const peer = await subtle.importKey('raw', bs(asPublic), { name: 'ECDH', namedCurve: 'P-256' }, false, []);
  const secret = new Uint8Array(await subtle.deriveBits({ name: 'ECDH', public: peer }, ua.privateKey, 256));
  const hkdf = async (s: Uint8Array, ikm: Uint8Array, info: Uint8Array, bits: number) =>
    new Uint8Array(await subtle.deriveBits({ name: 'HKDF', hash: 'SHA-256', salt: bs(s), info: bs(info) }, await subtle.importKey('raw', bs(ikm), 'HKDF', false, ['deriveBits']), bits));
  const ikm = await hkdf(ua.auth, secret, cat(utf8('WebPush: info\0'), ua.publicRaw, asPublic), 256);
  const cek = await hkdf(salt, ikm, utf8('Content-Encoding: aes128gcm\0'), 128);
  const nonce = await hkdf(salt, ikm, utf8('Content-Encoding: nonce\0'), 96);
  const padded = new Uint8Array(await subtle.decrypt({ name: 'AES-GCM', iv: bs(nonce) }, await subtle.importKey('raw', bs(cek), 'AES-GCM', false, ['decrypt']), bs(body.slice(21 + idlen))));
  let end = padded.length - 1;
  while (end > 0 && padded[end] === 0) end--;
  return { rs, idlen, asPublic, delimiter: padded[end], text: new TextDecoder().decode(padded.slice(0, end)) };
}

// 브라우저처럼 만든 구독자(P-256 키 쌍 · auth 16B)
async function browserSub(endpoint: string) {
  const pair = await subtle.generateKey({ name: 'ECDH', namedCurve: 'P-256' }, true, ['deriveBits']) as CryptoKeyPair;
  const publicRaw = new Uint8Array(await subtle.exportKey('raw', pair.publicKey));
  const auth = globalThis.crypto.getRandomValues(new Uint8Array(16));
  return { endpoint, privateKey: pair.privateKey, publicRaw, auth, p256dh: Buffer.from(publicRaw).toString('base64url'), authB64: Buffer.from(auth).toString('base64url') };
}
const uaFromRfc = async () => ({
  privateKey: await subtle.importKey('jwk', { kty: 'EC', crv: 'P-256', d: RFC.uaPrivate, x: b64(RFC.uaPublic).subarray(1, 33).toString('base64url'), y: b64(RFC.uaPublic).subarray(33).toString('base64url') }, { name: 'ECDH', namedCurve: 'P-256' }, false, ['deriveBits']),
  publicRaw: new Uint8Array(b64(RFC.uaPublic)), auth: new Uint8Array(b64(RFC.auth)),
});

// 가짜 푸시 서비스: endpoint마다 정한 동작(상태 · 연결 실패 · 응답 없음), 받은 요청을 기록
type Behavior = number | 'network' | 'timeout';
function fakeService(behavior: (endpoint: string) => Behavior = () => 201) {
  const requests: { url: string; init: RequestInit; body: Uint8Array }[] = [];
  const fetchMock = vi.fn(async (url: string | URL | Request, init?: RequestInit) => {
    const u = String(url);
    requests.push({ url: u, init: init!, body: new Uint8Array(init!.body as Uint8Array) });
    const b = behavior(u);
    if (b === 'network') throw new TypeError('fetch failed');
    if (b === 'timeout') throw new DOMException('The operation was aborted due to timeout', 'TimeoutError');
    return new Response(null, { status: b });
  });
  return { requests, fetch: fetchMock as unknown as typeof fetch };
}

// 의존성 묶음: 메모리 구독 저장소 · 최근 수집일 · 가짜 시계(sleep이 시계를 앞당김) · 출력 모음
function deps(over: Partial<PushDeps> & { at?: number; latest?: string | null; actions?: boolean } = {}) {
  let clock = over.at ?? kst('2026-10-07T07:00:00');
  const lines: string[] = [];
  const store = {
    list: vi.fn(memoryPushStore.list), mark: vi.fn(memoryPushStore.mark), revert: vi.fn(memoryPushStore.revert), remove: vi.fn(memoryPushStore.remove),
  };
  const latestDate = vi.fn(over.latest === undefined ? memoryStore.latestDate : async () => over.latest!);
  const sleep = vi.fn(async (ms: number) => { clock += ms; });
  const d: PushDeps = {
    store, latestDate, fetch: over.fetch ?? fakeService().fetch, now: () => clock, sleep,
    env: over.env ?? { VAPID_PRIVATE_KEY: VAPID.privateKey, ...(over.actions === false ? {} : { GITHUB_ACTIONS: 'true' }) },
    log: line => { lines.push(line); },
  };
  return { d, lines, store, latestDate, sleep, setClock: (ms: number) => { clock = ms; } };
}
const addSub = (endpoint: string, p256dh: string, auth: string) => memoryStore.savePushSubscription({ endpoint, p256dh, auth }, true);
const rows = () => memoryPushStore.list();

describe('암호화 = RFC 8291 (AC-14-3)', () => {
  it('부록 A 입력(서버 개인 키 · salt 주입) → 5절 본문과 바이트 같음 · 144B · 머리 86B(salt 16 · rs 4096 · idlen 65 · 서버 공개 키)', async () => {
    const body = encrypt(utf8(RFC.plaintext), RFC.uaPublic, RFC.auth, { salt: b64(RFC.salt), serverPrivateKey: b64(RFC.asPrivate) });
    expect(body.toString('base64url')).toBe(RFC.body);
    expect(body.length).toBe(144);
    expect([body.subarray(0, 16).toString('base64url'), body.readUInt32BE(16), body[20], body.subarray(21, 86).toString('base64url')]).toEqual([RFC.salt, 4096, 65, RFC.asPublic]);
    const opened = await decryptWeb(body, await uaFromRfc());
    expect([opened.text, opened.delimiter]).toEqual([RFC.plaintext, 2]);
  });

  it('주입 없이 두 번: 본문 · salt · 서버 키가 다름 · 둘 다 WebCrypto로 풀림', async () => {
    const ua = await uaFromRfc();
    const one = encrypt(utf8(MORNING_JSON), RFC.uaPublic, RFC.auth), two = encrypt(utf8(MORNING_JSON), RFC.uaPublic, RFC.auth);
    expect(Buffer.compare(one, two)).not.toBe(0);
    expect(one.subarray(0, 16).equals(two.subarray(0, 16))).toBe(false);
    expect(one.subarray(21, 86).equals(two.subarray(21, 86))).toBe(false);
    for (const body of [one, two]) expect((await decryptWeb(body, ua)).text).toBe(MORNING_JSON);
  });

  it('평문 3,993B 넘으면 던짐(한 레코드 한도)', () => {
    expect(() => encrypt(new Uint8Array(3994), RFC.uaPublic, RFC.auth)).toThrow('PUSH_PAYLOAD_TOO_LARGE');
    expect(encrypt(new Uint8Array(3993), RFC.uaPublic, RFC.auth).length).toBe(86 + 3993 + 1 + 16);
  });
});

describe('VAPID (RFC 8292)', () => {
  const ENDPOINTS = [
    'https://fcm.googleapis.com/fcm/send/vapid-a',
    'https://updates.push.services.mozilla.com/wpush/v2/vapid-b',
    'https://web.push.apple.com/vapid-c',
    'https://wns2-par02p.notify.windows.com/w/?token=vapid-d',
  ];

  it('엔드포인트 4종: vapid t={JWT}, k={코드 공개 키} · 머리 · aud = origin · exp = 지금 + 43,200초 · sub · 서명 = crypto.verify · subtle.verify 둘 다 참', async () => {
    for (const e of ENDPOINTS) await addSub(e, RFC.uaPublic, RFC.auth);
    const service = fakeService();
    const run = deps({ fetch: service.fetch });
    expect(await runPush([], run.d)).toBe(0);
    expect(service.requests.map(r => r.url)).toEqual(ENDPOINTS);
    const jwk = { kty: 'EC', crv: 'P-256', x: b64(VAPID_PUBLIC_KEY).subarray(1, 33).toString('base64url'), y: b64(VAPID_PUBLIC_KEY).subarray(33).toString('base64url') };
    const nodeKey = createPublicKey({ key: jwk, format: 'jwk' });
    const webKey = await subtle.importKey('jwk', jwk, { name: 'ECDSA', namedCurve: 'P-256' }, false, ['verify']);
    for (const { url, init } of service.requests) {
      const auth = (init.headers as Record<string, string>).Authorization;
      const m = /^vapid t=([\w-]+)\.([\w-]+)\.([\w-]+), k=([\w-]+)$/.exec(auth)!;
      expect(m, url).not.toBeNull();
      expect(m[4]).toBe(VAPID.publicKey);
      expect(b64(m[1]).toString()).toBe('{"typ":"JWT","alg":"ES256"}');
      expect(JSON.parse(b64(m[2]).toString())).toEqual({ aud: new URL(url).origin, exp: kst('2026-10-07T07:00:00') / 1000 + 43200, sub: 'https://issuerank-kappa.vercel.app' });
      const data = Buffer.from(`${m[1]}.${m[2]}`), sig = b64(m[3]);
      expect(sig.length).toBe(64);
      expect(verify('sha256', data, { key: nodeKey, dsaEncoding: 'ieee-p1363' }, sig)).toBe(true);
      expect(await subtle.verify({ name: 'ECDSA', hash: 'SHA-256' }, webKey, sig, data)).toBe(true);
    }
  });

  it('다른 개인 키 → fetch 0 · 종료 2 · "VAPID 키가 코드의 공개 키와 다릅니다" / 키 없음 → "VAPID_PRIVATE_KEY가 없습니다" / 형식 틀림 → 다름 · 출력에 키 값 0', async () => {
    await addSub(ENDPOINTS[0], RFC.uaPublic, RFC.auth);
    const other = generateKeyPairSync('ec', { namedCurve: 'prime256v1' }).privateKey.export({ format: 'jwk' }).d as string;
    for (const [env, want] of [
      [{ VAPID_PRIVATE_KEY: other, GITHUB_ACTIONS: 'true' }, 'VAPID 키가 코드의 공개 키와 다릅니다'],
      [{ GITHUB_ACTIONS: 'true' }, 'VAPID_PRIVATE_KEY가 없습니다'],
      [{ VAPID_PRIVATE_KEY: '', GITHUB_ACTIONS: 'true' }, 'VAPID_PRIVATE_KEY가 없습니다'],
      [{ VAPID_PRIVATE_KEY: 'not-a-key', GITHUB_ACTIONS: 'true' }, 'VAPID 키가 코드의 공개 키와 다릅니다'],
    ] as [Record<string, string>, string][]) {
      const service = fakeService();
      const run = deps({ fetch: service.fetch, env });
      expect(await runPush([], run.d)).toBe(2);
      expect(run.lines).toEqual(['아침 알림 2026-10-07 · 07:00 KST', want]);
      expect([service.requests.length, run.store.list.mock.calls.length, run.store.mark.mock.calls.length]).toEqual([0, 0, 0]);
      expect(run.lines.join('\n')).not.toContain(other);
      expect(run.lines.join('\n')).not.toContain(VAPID.privateKey);
    }
  });
});

describe('요청 · 복호화(가짜 푸시 서비스, AC-14-3)', () => {
  it('구독자 3(브라우저 키) · 그날 샘플 랭킹: POST · 머리 = 표 · redirect error · 10초 신호 · 본문 ≤ 4,096B · 풀린 JSON = 아침 알림 정확히 · 뉴스 내용 0', async () => {
    const subs = await Promise.all(['https://fcm.googleapis.com/fcm/send/r1', 'https://web.push.apple.com/r2', 'https://updates.push.services.mozilla.com/wpush/v2/r3'].map(browserSub));
    for (const s of subs) await addSub(s.endpoint, s.p256dh, s.authB64);
    const service = fakeService();
    const run = deps({ fetch: service.fetch });
    expect(await runPush([], run.d)).toBe(0);
    expect(service.requests).toHaveLength(3);
    const ranking = (await memoryStore.ranking('2026-10-07'))!;
    const news = ranking.sections.flatMap(s => [s.name, ...s.issues.map(i => i.title)]);
    for (const [i, req] of service.requests.entries()) {
      const s = subs[i];
      expect(req.url).toBe(s.endpoint);
      expect(req.init.method).toBe('POST');
      const { Authorization, ...headers } = req.init.headers as Record<string, string>;
      expect(headers).toEqual({ TTL: '21600', Urgency: 'normal', 'Content-Encoding': 'aes128gcm', 'Content-Type': 'application/octet-stream' });
      expect(Authorization.startsWith('vapid t=')).toBe(true);
      expect(req.init.redirect).toBe('error');
      expect(req.init.signal).toBeInstanceOf(AbortSignal);
      expect(req.init.signal!.aborted).toBe(false);
      expect(req.body.length).toBeLessThanOrEqual(4096);
      const opened = await decryptWeb(req.body, { privateKey: s.privateKey, publicRaw: s.publicRaw, auth: s.auth });
      expect([opened.rs, opened.idlen, opened.delimiter]).toEqual([4096, 65, 2]);
      expect(opened.text).toBe(MORNING_JSON);
      expect(JSON.parse(opened.text)).toEqual(MORNING);
      for (const word of news) expect(opened.text).not.toContain(word);
      expect(opened.text).not.toMatch(/\d/); // 날짜 · 연관기사 수 · 순위 숫자 0
    }
    expect(run.lines).toEqual(['아침 알림 2026-10-07 · 07:00 KST', '키 확인 OK', '구독 3 · 이미 보냄 0 · 대상 3', '보냄 3 · 만료 삭제 0 · 실패 0 · 응답 없음 0', '상태: 201 3', '서비스: fcm 1 · apple 1 · mozilla 1']);
  });
});

describe('결과 · 하루 1통 (AC-14-2 · 14-4)', () => {
  const PLAN: Behavior[] = [201, 404, 410, 400, 403, 413, 429, 500, 'network', 'timeout'];
  const endpointOf = (i: number) => `https://fcm.googleapis.com/fcm/send/day-${i}`;
  const behavior = (url: string) => PLAN[Number(url.split('-').pop())];

  it('10기기 → 404 · 410 삭제 · 201 · 응답 없음만 그날 표시 · 나머지 6 되돌림 · 종료 1 / 같은 날 두 번째 = 되돌린 6만 / 다음 날 = 남은 8 모두 · 출력에 endpoint · 키 · id · 기기 코드 0', async () => {
    for (let i = 0; i < PLAN.length; i++) await addSub(endpointOf(i), RFC.uaPublic, RFC.auth);
    const before = await rows();
    const all: string[] = [];

    const first = fakeService(behavior);
    const run1 = deps({ fetch: first.fetch });
    expect(await runPush([], run1.d)).toBe(1);
    all.push(...run1.lines);
    expect(first.requests).toHaveLength(10);
    expect(run1.lines.slice(2)).toEqual([
      '구독 10 · 이미 보냄 0 · 대상 10',
      '보냄 1 · 만료 삭제 2 · 실패 6 · 응답 없음 1',
      '상태: 201 1 · 400 1 · 403 1 · 404 1 · 410 1 · 413 1 · 429 1 · 500 1 · 연결 실패 1 · 응답 없음 1',
      '서비스: fcm 10',
    ]);
    const afterFirst = await rows();
    expect(afterFirst.map(r => r.endpoint)).toEqual([0, 3, 4, 5, 6, 7, 8, 9].map(endpointOf));
    expect(Object.fromEntries(afterFirst.map(r => [r.endpoint.split('-').pop(), r.lastSentOn]))).toEqual({
      0: '2026-10-07', 3: null, 4: null, 5: null, 6: null, 7: null, 8: null, 9: '2026-10-07',
    });
    // 보내기 전에 그날 표시 → 실패면 전 값(null)으로
    expect(run1.store.mark.mock.calls).toHaveLength(10);
    expect(run1.store.revert.mock.calls.map(c => [c[1], c[2]])).toEqual(Array(6).fill(['2026-10-07', null]));

    const second = fakeService(behavior);
    const run2 = deps({ fetch: second.fetch });
    expect(await runPush([], run2.d)).toBe(1);
    all.push(...run2.lines);
    expect(second.requests.map(r => r.url)).toEqual([3, 4, 5, 6, 7, 8].map(endpointOf));
    expect(run2.lines[2]).toBe('구독 8 · 이미 보냄 2 · 대상 6');

    const third = fakeService(() => 201);
    const run3 = deps({ fetch: third.fetch, at: kst('2026-10-08T07:00:00'), latest: '2026-10-08' });
    expect(await runPush([], run3.d)).toBe(0);
    all.push(...run3.lines);
    expect(third.requests.map(r => r.url)).toEqual([0, 3, 4, 5, 6, 7, 8, 9].map(endpointOf));
    expect(run3.lines.slice(2)).toEqual(['구독 8 · 이미 보냄 0 · 대상 8', '보냄 8 · 만료 삭제 0 · 실패 0 · 응답 없음 0', '상태: 201 8', '서비스: fcm 8']);
    expect((await rows()).every(r => r.lastSentOn === '2026-10-08')).toBe(true);

    const text = all.join('\n');
    for (const r of before) for (const secret of [r.endpoint, r.endpoint.split('/').pop()!, r.id, r.p256dh, r.auth, deviceCode(r.endpoint)]) expect(text).not.toContain(secret);
  });

  it('보내기 직전 다시 검사: 받지 않는 endpoint · 깨진 키 행은 보내지 않고 삭제(검사 실패 삭제)', async () => {
    await addSub('https://fcm.googleapis.com/fcm/send/ok', RFC.uaPublic, RFC.auth);
    const run = deps();
    // 저장소에 바로 들어간 행(구독 API는 막지만 다시 검사하는지 보려고)
    await memoryStore.savePushSubscription({ endpoint: 'https://evil.example/push', p256dh: RFC.uaPublic, auth: RFC.auth }, true);
    await memoryStore.savePushSubscription({ endpoint: 'https://fcm.googleapis.com/fcm/send/badkey', p256dh: 'BAAA', auth: RFC.auth }, true);
    for (const endpoint of ['https://constructor/x', 'https://__proto__/x']) await memoryStore.savePushSubscription({ endpoint, p256dh: RFC.uaPublic, auth: RFC.auth }, true);
    const service = fakeService();
    expect(await runPush([], { ...run.d, fetch: service.fetch })).toBe(0);
    expect(service.requests.map(r => r.url)).toEqual(['https://fcm.googleapis.com/fcm/send/ok']);
    expect((await rows()).map(r => r.endpoint)).toEqual(['https://fcm.googleapis.com/fcm/send/ok']);
    expect(run.lines[3]).toBe('보냄 1 · 만료 삭제 0 · 실패 0 · 응답 없음 0 · 검사 실패 삭제 4');
  });
});

describe('날 · 시각 (AC-14-2)', () => {
  const ENDPOINT = 'https://fcm.googleapis.com/fcm/send/clock';
  const go = async (at: string, latest: string | null = '2026-10-07') => {
    await addSub(ENDPOINT, RFC.uaPublic, RFC.auth);
    const service = fakeService();
    const run = deps({ fetch: service.fetch, at: kst(at), latest });
    const code = await runPush([], run.d);
    return { code, run, requests: service.requests.length };
  };

  it('07:00 · 09:59 보냄 / 10:00 "10:00 지남" · 03:29 "너무 이른 시각" · 어제 랭킹 "새 데이터 없음(최근 수집일 …)" — 안 보내면 fetch · 표시 0', async () => {
    for (const at of ['2026-10-07T07:00:00', '2026-10-07T09:59:59']) {
      const r = await go(at);
      expect([r.code, r.requests, (await rows())[0].lastSentOn], at).toEqual([0, 1, '2026-10-07']);
      await memoryStore.deletePushSubscription(ENDPOINT);
    }
    for (const [at, latest, reason] of [
      ['2026-10-07T10:00:00', '2026-10-07', '10:00 지남'],
      ['2026-10-07T23:59:00', '2026-10-07', '10:00 지남'],
      ['2026-10-07T03:29:59', '2026-10-07', '너무 이른 시각'],
      ['2026-10-07T00:10:00', '2026-10-06', '너무 이른 시각'],
      ['2026-10-07T07:00:00', '2026-10-06', '새 데이터 없음(최근 수집일 2026-10-06)'],
      ['2026-10-07T07:00:00', null, '새 데이터 없음(최근 수집일 없음)'],
    ] as [string, string | null, string][]) {
      const r = await go(at, latest);
      expect([r.code, r.requests, r.run.lines.at(-1)], at).toEqual([0, 0, `보내지 않음: ${reason}`]);
      expect(r.run.lines.slice(1, 3), at).toEqual(['키 확인 OK', '구독 1 · 이미 보냄 0 · 대상 1']);
      expect([r.run.store.mark.mock.calls.length, r.run.store.remove.mock.calls.length, r.run.sleep.mock.calls.length], at).toEqual([0, 0, 0]);
      await memoryStore.deletePushSubscription(ENDPOINT);
    }
  });

  it('05:10 → sleep 80분 1번 → 최근 수집일 다시 확인 → 보냄 / 03:30 → 180분 / 기다린 뒤 랭킹이 없으면 건너뜀', async () => {
    const r = await go('2026-10-07T05:10:00');
    expect(r.run.sleep.mock.calls).toEqual([[80 * 60e3]]);
    expect(r.run.latestDate.mock.calls).toHaveLength(2);
    expect([r.code, r.requests]).toEqual([0, 1]);
    expect(r.run.lines.slice(0, 4)).toEqual(['아침 알림 2026-10-07 · 05:10 KST', '키 확인 OK', '06:30까지 기다림(약 80분)', '구독 1 · 이미 보냄 0 · 대상 1']);
    await memoryStore.deletePushSubscription(ENDPOINT);

    const early = await go('2026-10-07T03:30:00');
    expect([early.run.sleep.mock.calls, early.requests]).toEqual([[[180 * 60e3]], 1]);
    await memoryStore.deletePushSubscription(ENDPOINT);

    // 기다리는 동안 그날 랭킹이 사라진 경우(두 번째 확인이 다름)
    await addSub(ENDPOINT, RFC.uaPublic, RFC.auth);
    const service = fakeService();
    const run = deps({ fetch: service.fetch, at: kst('2026-10-07T06:00:00') });
    run.latestDate.mockResolvedValueOnce('2026-10-07').mockResolvedValueOnce('2026-10-06');
    expect(await runPush([], run.d)).toBe(0);
    expect([service.requests.length, run.sleep.mock.calls, run.lines.at(-1)]).toEqual([0, [[30 * 60e3]], '보내지 않음: 새 데이터 없음(최근 수집일 2026-10-06)']);
  });

  it('그날 일부 섹션만 있음(D8 대체) → 그날 랭킹이 있으므로 보냄', async () => {
    // 최근 수집일 = 그날 행이 한 섹션이라도 있으면 그날(dbStore.latestDate와 같은 규칙: 오늘 이하 가장 늦은 date)
    const dates = [{ section: 'pol', date: '2026-10-07' }, { section: 'eco', date: '2026-10-06' }, { section: 'it', date: '2026-10-05' }];
    const latest = dates.map(d => d.date).filter(d => d <= '2026-10-07').sort().at(-1)!;
    const r = await go('2026-10-07T07:00:00', latest);
    expect([r.code, r.requests]).toEqual([0, 1]);
  });

  it('로컬(GITHUB_ACTIONS 없음): 결정 · 대상 수만 · "로컬 실행: 보내지 않음" · fetch · 표시 · 삭제 · sleep 0', async () => {
    for (const [at, lines] of [
      ['2026-10-07T07:00:00', ['구독 1 · 이미 보냄 0 · 대상 1', '로컬 실행: 보내지 않음']],
      ['2026-10-07T05:10:00', ['06:30까지 기다림(약 80분)', '구독 1 · 이미 보냄 0 · 대상 1', '로컬 실행: 보내지 않음']],
      ['2026-10-07T11:00:00', ['구독 1 · 이미 보냄 0 · 대상 1', '보내지 않음: 10:00 지남', '로컬 실행: 보내지 않음']],
    ] as [string, string[]][]) {
      for (const env of [{ VAPID_PRIVATE_KEY: VAPID.privateKey }, { VAPID_PRIVATE_KEY: VAPID.privateKey, GITHUB_ACTIONS: 'false' }]) {
        await addSub(ENDPOINT, RFC.uaPublic, RFC.auth);
        const service = fakeService();
        const run = deps({ fetch: service.fetch, at: kst(at), latest: '2026-10-07', env });
        expect(await runPush([], run.d), at).toBe(0);
        expect(run.lines.slice(1), at).toEqual(['키 확인 OK', ...lines]);
        expect([service.requests.length, run.store.mark.mock.calls.length, run.store.remove.mock.calls.length, run.store.revert.mock.calls.length, run.sleep.mock.calls.length], at).toEqual([0, 0, 0, 0, 0]);
        await memoryStore.deletePushSubscription(ENDPOINT);
      }
    }
  });
});

describe('시험 알림 (--test)', () => {
  const A = 'https://fcm.googleapis.com/fcm/send/test-a', B = 'https://web.push.apple.com/test-b';
  // 기기 코드 앞 8자가 같은 두 endpoint(SHA-256 앞 32비트 충돌을 찾음 — 평균 약 8만 번)
  const collide = (() => {
    const seen = new Map<string, string>();
    for (let i = 0; ; i++) {
      const e = `https://fcm.googleapis.com/fcm/send/c-${i}`, code = deviceCode(e);
      if (seen.has(code)) return { code, endpoints: [seen.get(code)!, e] };
      seen.set(code, e);
    }
  })();

  it('맞는 기기 1 · 11:00 · 최근 수집일 = 어제: 그 기기만 요청 1 · JSON = 시험 알림 · 그날 표시 그대로 · 종료 0', async () => {
    const a = await browserSub(A);
    await addSub(A, a.p256dh, a.authB64);
    await addSub(B, RFC.uaPublic, RFC.auth);
    const service = fakeService();
    const run = deps({ fetch: service.fetch, at: kst('2026-10-07T11:00:00'), latest: '2026-10-06' });
    expect(await runPush(['--test', deviceCode(A).toUpperCase()], run.d)).toBe(0);
    expect(service.requests.map(r => r.url)).toEqual([A]);
    const opened = await decryptWeb(service.requests[0].body, { privateKey: a.privateKey, publicRaw: a.publicRaw, auth: a.auth });
    expect(opened.text).toBe(TEST_JSON);
    expect(JSON.parse(opened.text)).toEqual(TEST);
    expect((await rows()).map(r => r.lastSentOn)).toEqual([null, null]);
    expect([run.store.mark.mock.calls.length, run.latestDate.mock.calls.length, run.sleep.mock.calls.length]).toEqual([0, 0, 0]);
    expect(run.lines).toEqual(['아침 알림 2026-10-07 · 11:00 KST', '키 확인 OK', '시험 알림 · 구독 2 · 코드에 맞는 기기 1', '보냄 1 · 만료 삭제 0 · 실패 0 · 응답 없음 0', '상태: 201 1', '서비스: fcm 1']);
    expect(run.lines.join('\n')).not.toContain(deviceCode(A));
  });

  it('0개 · 2개(앞 8자 같은 가짜) · 형식 틀림(xyz · 7자 · 9자) → 요청 0 · 종료 2 · 각 문구 / 로컬은 보내지 않음', async () => {
    await addSub(A, RFC.uaPublic, RFC.auth);
    for (const e of collide.endpoints) await addSub(e, RFC.uaPublic, RFC.auth);
    expect(deviceCode(collide.endpoints[0])).toBe(deviceCode(collide.endpoints[1]));
    const missing = ['00000000', '11111111', 'ffffffff'].find(c => ![A, ...collide.endpoints].some(e => deviceCode(e) === c))!;
    for (const [code, want] of [
      [missing, '보내지 않음: 코드에 맞는 기기가 없습니다'],
      [collide.code, '보내지 않음: 코드에 맞는 기기가 둘 이상입니다'],
      ['xyz', '보내지 않음: 시험 코드는 8자 16진수입니다'],
      [deviceCode(A).slice(0, 7), '보내지 않음: 시험 코드는 8자 16진수입니다'],
      [`${deviceCode(A)}0`, '보내지 않음: 시험 코드는 8자 16진수입니다'],
    ] as [string, string][]) {
      const service = fakeService();
      const run = deps({ fetch: service.fetch });
      expect(await runPush(['--test', code], run.d), code).toBe(2);
      expect([service.requests.length, run.lines.at(-1)], code).toEqual([0, want]);
    }
    const local = deps({ actions: false });
    expect(await runPush(['--test', deviceCode(A)], local.d)).toBe(0);
    expect(local.lines.slice(2)).toEqual(['시험 알림 · 구독 3 · 코드에 맞는 기기 1', '로컬 실행: 보내지 않음']);
  });

  it('다른 인수 → 종료 2 · 키 확인 전', async () => {
    for (const args of [['--pages', '1'], ['--test'], ['--test', 'a', 'b'], ['x']]) {
      const run = deps();
      expect(await runPush(args, run.d)).toBe(2);
      expect(run.lines).toEqual(['--test <기기 코드>만 받습니다']);
    }
  });
});

it('메모리 구독 저장소: 같은 endpoint = 키만 갱신 · 그날 표시는 한 번만 · 되돌림은 아직 그날일 때만', async () => {
  await addSub('https://fcm.googleapis.com/fcm/send/m', RFC.uaPublic, RFC.auth);
  await memoryStore.savePushSubscription({ endpoint: 'https://fcm.googleapis.com/fcm/send/m', p256dh: RFC.asPublic, auth: 'AAAAAAAAAAAAAAAAAAAAAA' }, false);
  const [row] = await rows() as PushRow[];
  expect([row.p256dh, row.auth, (await rows()).length]).toEqual([RFC.asPublic, 'AAAAAAAAAAAAAAAAAAAAAA', 1]);
  expect(await memoryPushStore.mark(row.id, '2026-10-07')).toBe(true);
  expect(await memoryPushStore.mark(row.id, '2026-10-07')).toBe(false);
  await memoryPushStore.revert(row.id, '2026-10-06', null);
  expect((await rows())[0].lastSentOn).toBe('2026-10-07');
  await memoryPushStore.revert(row.id, '2026-10-07', null);
  expect((await rows())[0].lastSentOn).toBe(null);
});
