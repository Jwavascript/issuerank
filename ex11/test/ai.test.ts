// 실제 OpenAI SDK + 가짜 HTTP: API 키·외부 네트워크·DB 없이 어댑터 계약을 검증한다.
import { afterEach, describe, expect, it, vi } from 'vitest';
import { aiClient, MODEL } from '../server/ai';

const KEY = 'test-openai-key';
const organized = { issues: [{ section: 'it', articles: ['a000'], importance: 5, terms: [['누리호'], ['발사']] }], excluded: [] };
const assigned = { assignments: { a000: 'a000' } };
const described = { events: { a000: { section: 'it', importance: 5, terms: [['누리호'], ['발사']] } } };
const LINE = 'a000 [it] 누리호 발사';
const summary = { line1: '첫째', line2: '둘째', line3: '셋째' };
const response = (text: string, status = 'completed') => ({
  id: 'resp_test', object: 'response', status,
  output: [{ id: 'msg_test', type: 'message', role: 'assistant', status: 'completed', content: [{ type: 'output_text', text, annotations: [] }] }],
});
const mockHttp = (body: unknown, status = 200) => {
  const mock = vi.fn<typeof fetch>().mockImplementation(async () => new Response(JSON.stringify(body), {
    status, headers: { 'content-type': 'application/json' },
  }));
  vi.stubGlobal('fetch', mock);
  return mock;
};
afterEach(() => vi.unstubAllGlobals());

describe('OpenAI Responses 어댑터', () => {
  it('정리·요약의 endpoint·인증·strict schema·한도·저장 설정과 정상 응답', async () => {
    const net = mockHttp(response(JSON.stringify(assigned)));
    net.mockImplementationOnce(async () => new Response(JSON.stringify(response(JSON.stringify(assigned))), { headers: { 'content-type': 'application/json' } }));
    net.mockImplementationOnce(async () => new Response(JSON.stringify(response(JSON.stringify(described))), { headers: { 'content-type': 'application/json' } }));
    const client = aiClient(KEY);
    expect(await client.organize(['a000 [it] 누리호 발사'])).toEqual({ refusal: false, output: organized });
    const [url, init] = net.mock.calls[0];
    expect(String(url)).toBe('https://api.openai.com/v1/responses');
    expect(new Headers(init?.headers).get('authorization')).toBe(`Bearer ${KEY}`);
    const request = JSON.parse(String(init?.body));
    expect(request).toMatchObject({ model: MODEL, input: 'a000 [it] 누리호 발사', max_output_tokens: 16000, reasoning: { effort: 'medium' }, store: false,
      text: { format: { type: 'json_schema', name: 'assign_articles', strict: true, schema: { additionalProperties: false } } } });
    expect(request.instructions).toContain('한국어 뉴스 편집자');
    expect(request).not.toHaveProperty('stream');
    net.mockImplementation(async () => new Response(JSON.stringify(response(JSON.stringify(summary))), { headers: { 'content-type': 'application/json' } }));
    expect(await client.summarize([{ title: '제목', description: '설명' }])).toEqual({ refusal: false, output: summary });
    const second = JSON.parse(String(net.mock.calls[2][1]?.body));
    expect(JSON.parse(String(net.mock.calls[1][1]?.body)).reasoning).toEqual({ effort: 'low' });
    expect(second).toMatchObject({ max_output_tokens: 4000, reasoning: { effort: 'low' }, input: '1. 제목: 제목\n   설명문: 설명', text: { format: { name: 'summary' } } });
  });

  it('refusal은 거절로 전달한다', async () => {
    mockHttp({ status: 'completed', output: [{ type: 'message', content: [{ type: 'refusal', refusal: '거절' }] }] });
    expect(await aiClient(KEY).organize([LINE])).toEqual({ refusal: true });
  });

  it.each([
    ['incomplete', response(JSON.stringify(assigned), 'incomplete')],
    ['failed', response(JSON.stringify(assigned), 'failed')],
    ['빈 출력', { status: 'completed', output: [] }],
    ['잘린 JSON', response('{"issues":[')],
    ['스키마 불일치', response('{"issues":"wrong"}')],
  ])('%s는 형식 오류용 null로 전달한다', async (_name, body) => {
    mockHttp(body);
    expect(await aiClient(KEY).organize([LINE])).toEqual({ refusal: false, output: null });
  });

  it('HTTP 429 오류는 전달하고 SDK가 추가 요청하지 않는다', async () => {
    const net = mockHttp({ error: { message: 'rate limited', type: 'rate_limit_error', code: 'rate_limit_exceeded' } }, 429);
    await expect(aiClient(KEY).organize([LINE])).rejects.toThrow('rate limited');
    expect(net).toHaveBeenCalledTimes(1);
  });
});
