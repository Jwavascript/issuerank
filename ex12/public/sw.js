// 이슈랭크 서비스 워커(O4 · D29). 기기에 저장하는 것은 /offline.html 하나뿐(cache.put 0 = 응답 저장 0).
// 같은 출처 · GET · 화면 탐색(mode navigate)이고 /api/ · /_vercel/ 밖일 때만 네트워크 → 실패하면 오프라인 안내. 그 밖 요청은 손대지 않음
// 새 배포에서 이 파일이나 offline.html이 바뀌면 CACHE를 issuerank-v2로 올림(activate가 옛 캐시를 지움)
const CACHE = 'issuerank-v1';
const OFFLINE = '/offline.html';

self.addEventListener('install', event => {
  event.waitUntil(caches.open(CACHE).then(cache => cache.add(OFFLINE)).then(() => self.skipWaiting()));
});

self.addEventListener('activate', event => {
  event.waitUntil(caches.keys()
    .then(keys => Promise.all(keys.filter(key => key !== CACHE).map(key => caches.delete(key))))
    .then(() => self.clients.claim()));
});

self.addEventListener('fetch', event => {
  const request = event.request;
  if (request.method !== 'GET' || request.mode !== 'navigate') return;
  const url = new URL(request.url);
  if (url.origin !== self.location.origin || url.pathname.startsWith('/api/') || url.pathname.startsWith('/_vercel/')) return;
  event.respondWith(fetch(request).catch(() => caches.match(OFFLINE)));
});
