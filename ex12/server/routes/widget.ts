import { Hono } from 'hono';
import type { Store } from '../app';

// 홈 화면 위젯(D22): 공개(로그인 · 쿠키 없이), 날짜 = 최근 수집일(D3). 쿼리는 보지 않음(위젯은 붙이지 않음 — CDN 캐시 키가 갈림)
// 200만 CDN 캐시(Vercel 지역마다 300초). 404 · 500(onError) · 502(BFF)에는 캐시 헤더 없음
export const widgetRoute = (store: Store) => {
  const route = new Hono();

  route.get('/', async c => {
    const date = await store.latestDate();
    const widget = date ? await store.widget(date) : null;
    if (!widget) return c.json({ message: '해당 날짜의 랭킹 데이터가 없습니다' }, 404);
    return c.json(widget, 200, { 'Cache-Control': 'public, max-age=0, s-maxage=300' });
  });
  return route;
};
