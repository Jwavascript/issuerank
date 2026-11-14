// Vercel Cron → GitHub workflow_dispatch(O9 · D37 · AC-01-3 · 01-4). DB · 쿠키 없이 GitHub API 한 번만 부름
// 판단 순서: job 이름(404) → CRON_SECRET 있음(503) → Authorization 같음(401) → GITHUB_DISPATCH_TOKEN 있음(503) → 호출(202 · 502)
// 응답 · 로그에 토큰 · 비밀값 · GitHub 응답 본문을 넣지 않음. 로그는 `CRON_DISPATCH_FAILED {상태}` 한 줄뿐
import { createHash, timingSafeEqual } from 'node:crypto';

const REPO = 'Jwavascript/issuerank';
const TIMEOUT_MS = 10_000;

// job → 실행할 워크플로 · 넘길 입력(그 밖 이름은 404)
export const JOBS: Record<string, { workflow: string; inputs: Record<string, string> }> = {
  daily: { workflow: 'daily.yml', inputs: { trigger: 'cron' } },
  health: { workflow: 'health.yml', inputs: { mode: 'morning' } },
};

type Env = Record<string, string | undefined>;
type Fetch = (url: string, init: RequestInit) => Promise<Response>;

const json = (body: unknown, status: number) =>
  Response.json(body, { status, headers: { 'Cache-Control': 'no-store' } });

// 길이가 달라도 시간이 같도록 sha256으로 맞춘 뒤 비교
const sameSecret = (a: string, b: string) =>
  timingSafeEqual(createHash('sha256').update(a).digest(), createHash('sha256').update(b).digest());

export async function handleCron(job: string, request: Request, env: Env, fetcher: Fetch = fetch,
  log: (line: string) => void = line => console.error(line)): Promise<Response> {
  const target = Object.hasOwn(JOBS, job) ? JOBS[job] : undefined;
  if (!target) return json({ message: 'NOT_FOUND' }, 404);
  const secret = env.CRON_SECRET;
  if (!secret) return json({ message: 'CRON_NOT_CONFIGURED' }, 503);
  if (!sameSecret(request.headers.get('authorization') ?? '', `Bearer ${secret}`)) return json({ message: 'UNAUTHORIZED' }, 401);
  const token = env.GITHUB_DISPATCH_TOKEN;
  if (!token) return json({ message: 'CRON_NOT_CONFIGURED' }, 503);

  let status = 0;
  try {
    const res = await fetcher(`https://api.github.com/repos/${REPO}/actions/workflows/${target.workflow}/dispatches`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        Accept: 'application/vnd.github+json',
        'X-GitHub-Api-Version': '2022-11-28',
        'User-Agent': 'issuerank-cron',
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ ref: 'main', inputs: target.inputs }),
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    status = res.status;
    await res.body?.cancel().catch(() => {});
  } catch {
    status = 0;
  }
  if (status === 200 || status === 204) return json({ dispatched: job }, 202);
  log(`CRON_DISPATCH_FAILED ${status}`);
  return json({ message: 'DISPATCH_FAILED', status }, 502);
}
