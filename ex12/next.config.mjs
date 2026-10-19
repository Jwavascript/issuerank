/** @type {import('next').NextConfig} */
const nextConfig = {
  async rewrites() {
    return [{ source: '/', destination: '/index.html' }, { source: '/widget', destination: '/widget.html' },
      { source: '/privacy', destination: '/privacy.html' },
      { source: '/terms', destination: '/terms.html' },
      { source: '/card', destination: '/card.html' }];
  },
  // 카드 PNG 함수에만 글꼴 2개(D28). 캡션 · /api/[...path] 함수에는 넣지 않음
  // Turbopack 빌드는 키를 진입점 이름(app/api/card/route)에 "포함" 일치로 맞춤 → '/api/card'는 '/api/card/caption/route'에도 걸려 '/api/card/route'로 씀
  outputFileTracingIncludes: { '/api/card/route': ['./assets/fonts/*'] },
};

export default nextConfig;
