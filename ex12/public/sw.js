// 이슈랭크 서비스 워커(O4 · D29). 기기에 저장하는 것은 /offline.html 하나뿐(cache.put 0 = 응답 저장 0).
// 같은 출처 · GET · 화면 탐색(mode navigate)이고 /api/ · /_vercel/ 밖일 때만 네트워크 → 실패하면 오프라인 안내. 그 밖 요청은 손대지 않음
// 새 배포에서 이 파일이나 offline.html이 바뀌면 CACHE 판을 올림(activate가 옛 캐시를 지움) — O5(아침 알림 push · notificationclick)에서 v2
// 같은 주소에 fetch 처리 없는 판으로 바꿀 때도 push · notificationclick은 남김(빼면 아침 알림이 깨짐)
const CACHE = 'issuerank-v2';
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

// 아침 알림(O5 · D30): 모든 push에 알림 1개(보이지 않는 푸시 금지 — Chrome · Edge userVisibleOnly, Safari는 안 보이면 권한 회수).
// payload = { title, body, tag, url }(서버 push-config.ts). 없거나 틀리면 아침 알림 값. 같은 tag 알림은 먼저 닫고 새로 띄움(어제 알림 대체)
const MORNING = { title: '오늘의 뉴스가 도착했어요', body: '', tag: 'issuerank-daily', url: '/p' };
const pushUrl = url => typeof url === 'string' && url.startsWith('/') && !url.startsWith('//');
function readPush(data) {
  let p = null;
  try { p = data ? data.json() : null; } catch { p = null; }
  const ok = !!p && typeof p === 'object' && ['title', 'body', 'tag', 'url'].every(k => typeof p[k] === 'string')
    && p.title.length >= 1 && p.title.length <= 80 && p.body.length <= 200 && /^[a-z0-9-]{1,64}$/.test(p.tag) && pushUrl(p.url);
  return ok ? { title: p.title, body: p.body, tag: p.tag, url: p.url } : MORNING;
}

self.addEventListener('push', event => {
  const n = readPush(event.data);
  event.waitUntil(self.registration.getNotifications({ tag: n.tag })
    .then(list => { for (const shown of list) shown.close(); })
    .catch(() => {})
    .then(() => self.registration.showNotification(n.title, {
      body: n.body, icon: '/icons/icon-192.png', badge: '/icons/badge-96.png', tag: n.tag, data: { url: n.url }, lang: 'ko',
    })));
});

// 누르면: 같은 출처 창이 있으면 앞으로 + 그 주소로(안 되면 새 창), 없으면 새 창. 주소 = 이 출처 + data.url(기본 /p = 첫 화면)
self.addEventListener('notificationclick', event => {
  event.notification.close();
  const data = event.notification.data;
  const url = self.location.origin + (pushUrl(data && data.url) ? data.url : '/p');
  event.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
    const win = list.find(c => { try { return new URL(c.url).origin === self.location.origin; } catch { return false; } });
    if (!win) return self.clients.openWindow(url);
    return win.focus().then(() => win.navigate(url)).catch(() => self.clients.openWindow(url));
  }));
});
