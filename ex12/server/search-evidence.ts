import { mkdir, writeFile } from 'node:fs/promises';
import { hide } from './hide';

export type SearchArticle = { title: string; description: string | null; link: string; sourceId: string; publishedAt: string };
export type VerifyRelated = (members: { title: string; description: string | null }[], results: SearchArticle[]) =>
  Promise<{ refusal: true } | { refusal: false; output: { matches: boolean[] } | null }>;
export type SearchEvidence = {
  id: string; keyword: string; totalResults: number; results: SearchArticle[];
  members: { title: string; description: string | null }[]; size: number;
  ruleRelated: number; aiRelated: number | null; matched: boolean[] | null;
  method: 'ai' | 'rule' | 'empty'; failure: string | null; articleCount: number;
};

// API 원문 대신 허용 칸만 저장. 잘못된 제목도 분모에 남기되 일치로 판정하지 않는다.
export function searchArticles(items: unknown): SearchArticle[] {
  if (!Array.isArray(items)) return [];
  const str = (x: unknown, max: number) => typeof x === 'string' ? [...x].slice(0, max).join('') : '';
  return items.slice(0, 10).map(item => {
    const a = item && typeof item === 'object' ? item as Record<string, unknown> : {};
    return { title: str(a.title, 500), description: str(a.description, 300) || null,
      link: str(a.link, 2048), sourceId: str(a.source_id, 200), publishedAt: str(a.pubDate, 100) };
  });
}

export async function saveEvidence(date: string, searches: SearchEvidence[], directory = '.data/ranking-quality') {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) throw new Error('INVALID_DATE');
  await mkdir(directory, { recursive: true });
  await writeFile(`${directory}/${date}.json`, hide(JSON.stringify({ date, searches }, null, 2)), 'utf8');
}
