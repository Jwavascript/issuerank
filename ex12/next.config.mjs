/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [{ source: '/', destination: '/index.html' }, { source: '/widget', destination: '/widget.html' },
      { source: '/privacy', destination: '/privacy.html' },
      { source: '/terms', destination: '/terms.html' },
      { source: '/card', destination: '/card.html' },
      // 측정 경로(O4 · D29): 설치 앱 시작 주소 /app · 공유 주소 /s(?issue=) — 화면은 / 와 같은 index.html
      { source: '/app', destination: '/index.html' },
      { source: '/s', destination: '/index.html' },
      // 측정 경로(O5 · D30): 아침 알림을 누르면 /p(첫 화면, ?issue= 없음)
      { source: '/p', destination: '/index.html' }];
  },
  // 서비스 워커 · manifest 파일 헤더(O4 · D29, public 파일에도 적용). 다른 경로 헤더는 그대로
  async headers() {
    return [
      { source: '/sw.js', headers: [
        { key: 'Content-Type', value: 'application/javascript; charset=utf-8' },
        { key: 'Cache-Control', value: 'no-cache, no-store, must-revalidate' },
        { key: 'Content-Security-Policy', value: "default-src 'self'; script-src 'self'" },
      ] },
      { source: '/manifest.webmanifest', headers: [{ key: 'Content-Type', value: 'application/manifest+json' }] },
    ];
  },
  // 카드 PNG 함수에만 글꼴 2개(D28). 캡션 · /api/[...path] 함수에는 넣지 않음
  // Turbopack 빌드는 키를 진입점 이름(app/api/card/route)에 "포함" 일치로 맞춤 → '/api/card'는 '/api/card/caption/route'에도 걸려 '/api/card/route'로 씀
  outputFileTracingIncludes: { '/api/card/route': ['./assets/fonts/*'] },
};

export default nextConfig;
