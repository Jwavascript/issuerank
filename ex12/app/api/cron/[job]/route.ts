// GET /api/cron/{job}: Vercel Cron 전용(O9 · D37). job = daily · health → GitHub workflow_dispatch. 판단은 server/cron.ts
import { handleCron } from '../../../../server/cron';

export const runtime = 'nodejs';
export const dynamic = 'force-dynamic';

export async function GET(request: Request, { params }: { params: Promise<{ job: string }> }) {
  const { job } = await params;
  return handleCron(job, request, process.env);
}
