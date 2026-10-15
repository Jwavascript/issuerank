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

  it('메타데이터 지시문은 terms를 한 낱말 · 표기 변형 동의어로 요구한다(D19 · D19-2)', async () => {
    const net = mockHttp(response(JSON.stringify(assigned)));
    net.mockImplementationOnce(async () => new Response(JSON.stringify(response(JSON.stringify(assigned))), { headers: { 'content-type': 'application/json' } }));
    net.mockImplementationOnce(async () => new Response(JSON.stringify(response(JSON.stringify(described))), { headers: { 'content-type': 'application/json' } }));
    await aiClient(KEY).organize([LINE]);
    const { instructions } = JSON.parse(String(net.mock.calls[1][1]?.body));
    expect(instructions).toContain('띄어쓰기 없는 한 낱말');
    expect(instructions).toContain('고유 명사');
    expect(instructions).toContain('핵심 행동/대상 한 낱말');
    expect(instructions).toContain('표기 변형을 동의어');
    expect(instructions).toContain('["미국","美"]');
    expect(instructions).toContain('美·中·日 같은 약칭도');
    expect(instructions).toContain('흔한 말만으로 된 그룹');
    expect(instructions).not.toContain('한자 약칭(美·中·日) 대신 한글');
    expect(instructions).not.toContain('1~2낱말');
    expect(instructions).not.toContain('가장 구체적인 표현 하나');
  });

  it('제목이 [사진]·[포토]·[화보]로 시작하는 기사는 AI에 보내지 않고 제외한다(D19-2)', async () => {
    const lines = ['a000 [it] 누리호 발사', 'a001 [cul] [사진]가수 공연 | 장면', 'a002 [it] 누리호 발사 성공', 'a003 [soc] [포토] 행사 장면', 'a004 [wor] [화보] 풍경', 'a005 [cul] 공연 [사진] 전시'];
    const assign = { assignments: { a000: 'a000', a002: 'a000', a005: 'a005' } };
    const meta = { events: { a000: described.events.a000, a005: { section: 'cul', importance: 1, terms: [['공연'], ['전시']] } } };
    const net = mockHttp(response(JSON.stringify(assign)));
    net.mockImplementationOnce(async () => new Response(JSON.stringify(response(JSON.stringify(assign))), { headers: { 'content-type': 'application/json' } }));
    net.mockImplementationOnce(async () => new Response(JSON.stringify(response(JSON.stringify(meta))), { headers: { 'content-type': 'application/json' } }));
    const result = await aiClient(KEY).organize(lines);
    expect(net).toHaveBeenCalledTimes(2);
    const first = JSON.parse(String(net.mock.calls[0][1]?.body));
    for (const n of ['a001', 'a003', 'a004']) {
      expect(first.input).not.toContain(n);
      expect(first.text.format.schema.properties.assignments.properties).not.toHaveProperty(n);
    }
    expect(first.input.startsWith('a000 [it] 누리호 발사\na002 [it] 누리호 발사 성공\na005 [cul] 공연 [사진] 전시')).toBe(true);
    expect(JSON.parse(String(net.mock.calls[1][1]?.body)).input).not.toContain('[사진]가수');
    expect(result.refusal).toBe(false);
    const output = (result as { output: { issues: { articles: string[] }[]; excluded: string[] } }).output;
    expect(output.excluded).toEqual(['a001', 'a003', 'a004']);
    expect([...output.issues.flatMap(i => i.articles), ...output.excluded].sort()).toEqual(lines.map(l => l.split(' ')[0]));
  });

  it('모든 기사가 사진 접두면 AI를 호출하지 않는다(D19-2)', async () => {
    const net = mockHttp(response(JSON.stringify(assigned)));
    expect(await aiClient(KEY).organize(['a000 [cul] [사진]장면', 'a001 [soc] [포토]장면'])).toEqual({ refusal: false, output: { issues: [], excluded: ['a000', 'a001'] } });
    expect(net).not.toHaveBeenCalled();
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
