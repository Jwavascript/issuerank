// npm run push:keys: 아침 알림(O5 · D30) VAPID 키 쌍(P-256)을 한 번 만듦. 개인 키는 .env의 VAPID_PRIVATE_KEY 한 줄로만 덧붙이고(권한 600)
// 화면에는 공개 키만 찍음(코드 상수 server/push-config.ts · public/index.html에 넣는 값). 이미 있으면 바꾸지 않고 멈춤(종료 2)
// 키는 바꾸지 않음: 바꾸면 모든 기기가 알림을 다시 켜야 함(개인 키가 새었을 때만, 옛 구독 정리는 사용자 결정)
import { generateKeyPairSync } from 'node:crypto';
import { appendFileSync, chmodSync, existsSync, readFileSync } from 'node:fs';

const FILE = '.env';
const text = existsSync(FILE) ? readFileSync(FILE, 'utf8') : '';
if (/^VAPID_PRIVATE_KEY=/m.test(text)) {
  console.log('VAPID_PRIVATE_KEY가 이미 .env에 있습니다. 바꾸지 않았습니다');
  process.exitCode = 2;
} else {
  const { privateKey, publicKey } = generateKeyPairSync('ec', { namedCurve: 'prime256v1' });
  const priv = privateKey.export({ format: 'jwk' }), pub = publicKey.export({ format: 'jwk' });
  // 공개 키 = 비압축 점 0x04 ‖ x ‖ y(65B) base64url, 개인 키 = d(32B) base64url
  const point = Buffer.concat([Buffer.from([4]), Buffer.from(pub.x!, 'base64url'), Buffer.from(pub.y!, 'base64url')]).toString('base64url');
  appendFileSync(FILE, `${text && !text.endsWith('\n') ? '\n' : ''}VAPID_PRIVATE_KEY=${priv.d}\n`, { mode: 0o600 });
  chmodSync(FILE, 0o600);
  console.log(`VAPID 공개 키: ${point}`);
  console.log('개인 키는 .env에만 썼습니다');
}
