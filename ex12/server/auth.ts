import { betterAuth, type BetterAuthOptions } from 'better-auth';
import { APIError, createAuthMiddleware } from 'better-auth/api';
import { drizzleAdapter } from '@better-auth/drizzle-adapter';
import type { Db } from './db/client';
import * as schema from './db/auth-schema';

// Google 로그인(O2, D26). 두 값이 다 있을 때만 켜고 하나라도 비면 끈다 → POST /sign-in/social 404 PROVIDER_NOT_FOUND
// (앱 준비 실패로 만들지 않음: 이메일 로그인 · 랭킹 · 위젯은 그대로). 범위는 openid · email · profile 3개로 고정(추가 없음),
// 본문 idToken 로그인은 닫음. 이름 = 앞뒤 공백을 뺀 Google name, 비면 이메일 @ 앞부분. 사진 주소는 저장하지 않음(image 칸 비움)
function googleProvider(env: Record<string, string | undefined>): BetterAuthOptions['socialProviders'] {
  const clientId = env.GOOGLE_CLIENT_ID, clientSecret = env.GOOGLE_CLIENT_SECRET;
  if (!clientId || !clientSecret) return {};
  return { google: {
    clientId, clientSecret,
    disableDefaultScope: true, scope: ['openid', 'email', 'profile'],
    disableIdTokenSignIn: true,
    mapProfileToUser: profile => ({
      name: (typeof profile.name === 'string' ? profile.name.trim() : '') || String(profile.email ?? '').split('@')[0],
      image: undefined,
    }),
  } };
}

// 생성은 서버 진입점의 try 안에서만 한다. 값이나 라이브러리 원본 오류를 기록하지 않는다.
export function authOptions(env: Record<string, string | undefined>): BetterAuthOptions {
  const secret = env.BETTER_AUTH_SECRET;
  const address = env.BETTER_AUTH_URL;
  if (!secret || secret.length < 32 || !address) throw new Error('AUTH_CONFIG_INVALID');
  let origin: string;
  try {
    const url = new URL(address);
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.pathname !== '/' || url.search || url.hash) throw new Error();
    origin = url.origin;
  } catch { throw new Error('AUTH_CONFIG_INVALID'); }
  return {
    appName: '이슈랭크', secret, baseURL: origin, basePath: '/api/auth',
    trustedOrigins: [origin],
    advanced: { disableOriginCheck: false, disableCSRFCheck: false },
    // better-call의 원본 console.error까지 우회하고 Hono의 안전 오류 처리에 맡긴다.
    // Google 콜백에서 상태를 읽지 못한 실패는 기본 /api/auth/error 페이지 대신 화면 첫 주소로(/?error=state_mismatch 등)
    onAPIError: { throw: true, errorURL: '/' },
    socialProviders: googleProvider(env),
    account: {
      // state · code_verifier · 돌아올 주소 · 만료(10분)는 암호화한 better-auth.oauth_state 쿠키 하나에만. 시작 · 취소 · 상태 실패에서 DB 쓰기 0
      storeStateStrategy: 'cookie',
      // Google 토큰은 저장하지 않음(③): 다시 로그인할 때도 account 행을 고치지 않고, 처음 만들 때는 아래 훅이 토큰 칸을 비움
      updateAccountOnSignIn: false,
      // 같은 이메일의 기존 계정과 자동으로 잇지 않음(①②): 콜백은 error=account_not_linked, 새 행 0
      accountLinking: { disableImplicitLinking: true },
    },
    databaseHooks: { account: { create: { before: async account => ({ data: {
      ...account, accessToken: null, refreshToken: null, idToken: null, accessTokenExpiresAt: null, refreshTokenExpiresAt: null,
    } }) } } },
    // 화면에서 쓰지 않고 ①③을 API로 우회하지 못하게 닫는 경로 → 404
    disabledPaths: ['/link-social', '/unlink-account', '/get-access-token', '/refresh-token', '/account-info'],
    emailAndPassword: { enabled: true, minPasswordLength: 8, maxPasswordLength: 128, autoSignIn: true, requireEmailVerification: false },
    hooks: { before: createAuthMiddleware(async ctx => {
      if (ctx.path === '/sign-up/email' && (typeof ctx.body?.name !== 'string' || !ctx.body.name.trim())) {
        throw new APIError('BAD_REQUEST', { code: 'INVALID_NAME', message: 'Name is required' });
      }
    }) },
    logger: { level: 'error', log: () => { console.error('AUTH_INTERNAL_ERROR'); } },
  };
}

export function createAuth(db: Db, env: Record<string, string | undefined>) {
  return betterAuth({ ...authOptions(env), database: drizzleAdapter(db, { provider: 'pg', schema }) });
}
