import { betterAuth, type BetterAuthOptions } from 'better-auth';
import { APIError, createAuthMiddleware } from 'better-auth/api';
import { drizzleAdapter } from '@better-auth/drizzle-adapter';
import type { Db } from './db/client';
import * as schema from './db/auth-schema';

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
    onAPIError: { throw: true },
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
