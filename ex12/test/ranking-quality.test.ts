import { describe, expect, it, vi } from 'vitest';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { group, type GroupArticle, type GroupStore } from '../server/grouping';
import { newsFetch } from '../server/news-rate';
import { SECTIONS } from '../server/ranking';
import { saveEvidence, searchArticles, type VerifyRelated } from '../server/search-evidence';

const member: GroupArticle = { section: 'it', title: '누리호 발사 성공 발표', description: '오늘 세 번째 발사 성공',
  link: 'https://example.com/original', publishedAt: '2026-10-07T00:00:00Z' };
const results = [
  { title: '한국형 발사체 세 번째 비행 성공', description: '누리호가 오늘 궤도에 올랐다', source_id: 'a', link: 'https://example.com/a' },
  { title: '누리호 후속 발사 일정 발표', description: '내년 일정 확정', source_id: 'b' },
];
async function run(verifyRelated?: VerifyRelated, items: unknown[] = results) {
  const store: GroupStore = { load: async () => ({ articles: [{ ...member, issueId: null }], issues: [], bookmarked: [] }), save: vi.fn() };
  const verify = verifyRelated && vi.fn(verifyRelated);
  const result = await group({ fetch: async () => new Response(JSON.stringify({ status: 'success', totalResults: 20, results: items })),
    store, key: 'fixture-key', search: 1, waitMs: 0, verifyRelated: verify,
    organize: async () => ({ refusal: false, output: { issues: [{ section: 'it', articles: ['a000'], importance: 5, terms: [['누리호'], ['발사']] }], excluded: [] } }) });
  if (result.empty) throw new Error('unexpected empty');
  return { result, verify, store };
}

describe('O11·O12 검색 품질', () => {
  it('섹션별 후보6개만 검색·AI 판정하고 전부 불일치여도 수집 묶음보다 작아지지 않는다', async () => {
    const articles = SECTIONS.flatMap(({ key: section }) => Array.from({ length: 7 }, (_, i) => ({
      ...member, section, link: `https://example.com/${section}/${i}`, issueId: null,
    })));
    const fetch = vi.fn(async () => new Response(JSON.stringify({ status: 'success', totalResults: 50, results })));
    const verifyRelated = vi.fn<VerifyRelated>(async () => ({ refusal: false, output: { matches: [false, false] } }));
    const result = await group({ fetch, verifyRelated, key: 'key', search: 6, waitMs: 0,
      store: { load: async () => ({ articles, issues: [], bookmarked: [] }), save: async () => {} },
      organize: async lines => ({ refusal: false, output: { excluded: [], issues: lines.map(line => ({
        section: /\[([a-z]+)\]/.exec(line)![1], articles: [line.split(' ')[0]], importance: 3, terms: [['누리호'], ['발사']],
      })) } }),
    });
    if (result.empty) throw new Error('unexpected empty');
    expect(result.issues).toBe(42);
    expect(fetch).toHaveBeenCalledTimes(36);
    expect(verifyRelated).toHaveBeenCalledTimes(36);
    expect(result.evidence).toHaveLength(36);
    expect(result.evidence.every(e => e.articleCount === 1 && e.aiRelated === 0)).toBe(true);
  });

  it('다른 표현의 동일 사건만 인정한 AI 결과를 추정과 근거에 사용한다', async () => {
    const { result, verify } = await run(async () => ({ refusal: false, output: { matches: [true, false] } }));
    expect(verify).toHaveBeenCalledWith([member], searchArticles(results));
    expect(result.code).toBe(0);
    expect(result.evidence[0]).toMatchObject({ totalResults: 20, aiRelated: 1, matched: [true, false], method: 'ai', articleCount: 10 });
    expect(result.sections.find(s => s.section === 'it')).toMatchObject({ candidates: [{ articleCount: 10 }] });
  });

  it.each<VerifyRelated>([
    async () => ({ refusal: true }),
    async () => ({ refusal: false, output: null }),
    async () => ({ refusal: false, output: { matches: [true] } }),
    async () => { throw new Error('private-error'); },
  ])('실패·거절·잘린 배열은 낱말 규칙으로 대체하며 종료1', async verify => {
    const { result } = await run(verify);
    expect(result.code).toBe(1);
    expect(result.verificationFailed).toBe(1);
    expect(result.evidence[0]).toMatchObject({ method: 'rule', aiRelated: null, failure: 'AI_VERIFICATION_FAILED' });
    expect(result.evidence[0].articleCount).toBe(Math.max(result.evidence[0].ruleRelated * 10, 1));
    expect(JSON.stringify(result.evidence)).not.toContain('private-error');
  });

  it('빈 결과는 AI 호출0·묶음 크기, AI 없으면 규칙 유지, 잘못된 제목은 분모에 남되 불일치', async () => {
    const empty = await run(async () => ({ refusal: true }), []);
    expect(empty.verify).not.toHaveBeenCalled();
    expect(empty.result.evidence[0]).toMatchObject({ method: 'empty', articleCount: 1 });
    expect((await run()).result.evidence[0].method).toBe('rule');
    const invalid = await run(async () => ({ refusal: false, output: { matches: [true, true] } }), [null, results[0]]);
    expect(invalid.result.evidence[0]).toMatchObject({ matched: [false, true], articleCount: 10 });
  });

  it('본문 없이 허용 칸·상한만 저장하고 비밀값을 가린다', async () => {
    const directory = await mkdtemp(join(tmpdir(), 'issuerank-evidence-'));
    vi.stubEnv('NEWSDATA_API_KEY', 'fixture-secret');
    try {
      const sanitized = searchArticles([{ ...results[0], content: '본문', description: '나'.repeat(400), title: 'fixture-secret' }]);
      expect([...sanitized[0].description!]).toHaveLength(300);
      expect(sanitized[0]).not.toHaveProperty('content');
      const { result } = await run();
      result.evidence[0].results = sanitized;
      await saveEvidence(result.date, result.evidence, directory);
      const saved = await readFile(join(directory, `${result.date}.json`), 'utf8');
      expect(saved).not.toContain('fixture-secret');
      expect(JSON.parse(saved).searches[0].results[0].title).toBe('***');
      await expect(saveEvidence('../escape', [], directory)).rejects.toThrow('INVALID_DATE');
    } finally {
      vi.unstubAllEnvs();
      await rm(directory, { recursive: true });
    }
  });

  it('첫 호출과 collect→group 새 래퍼 모두 31초 대기한다', async () => {
    vi.useFakeTimers();
    const send = vi.fn(async () => new Response('{}'));
    const collect = newsFetch(send);
    const groupFetch = newsFetch(send);
    const one = collect('https://example.com', { headers: {} });
    await vi.advanceTimersByTimeAsync(30_999);
    expect(send).not.toHaveBeenCalled();
    await vi.advanceTimersByTimeAsync(1);
    await one;
    const two = groupFetch('https://example.com', { headers: {} });
    await vi.advanceTimersByTimeAsync(30_999);
    expect(send).toHaveBeenCalledTimes(1);
    await vi.advanceTimersByTimeAsync(1);
    await two;
    expect(send).toHaveBeenCalledTimes(2);
  });
});
