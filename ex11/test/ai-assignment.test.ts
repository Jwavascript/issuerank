import { afterEach, expect, it, vi } from 'vitest';
import { aiClient } from '../server/ai';
const meta = { section: 'it', importance: 3, terms: [['연구소'], ['센서']] };
function network(outputs: unknown[]) {
  const net = vi.fn<typeof fetch>().mockImplementation(async () => new Response(JSON.stringify({
    object: 'response', status: 'completed', output: [{ type: 'message', content: [{ type: 'output_text', text: JSON.stringify(outputs.shift()) }] }],
  }), { headers: { 'content-type': 'application/json' } }));
  vi.stubGlobal('fetch', net);
  return net;
}
afterEach(() => vi.unstubAllGlobals());
it('116개 required 배정으로 멀리 떨어진 같은 사건을 조립하고 메타 단계가 멤버를 바꾸지 않는다', async () => {
  const ids = Array.from({ length: 116 }, (_, i) => `a${String(i).padStart(3, '0')}`);
  const assignments = Object.fromEntries(ids.map(n => [n, n]));
  assignments.a115 = 'a000'; assignments.a019 = '#photo';
  const roots = ids.filter(n => !['a019', 'a115'].includes(n));
  const net = network([{ assignments }, { events: Object.fromEntries(roots.map(n => [n, meta])) }]);
  const result = await aiClient('fake-key').organize(ids.map(n => `${n} [it] 센서 발표`));
  expect(result.refusal).toBe(false);
  if (result.refusal) throw Error('refusal');
  const output = result.output as { issues: { articles: string[] }[]; excluded: string[] };
  expect(output.issues[0].articles).toEqual(['a000', 'a115']);
  expect(output.excluded).toEqual(['a019']);
  expect(output.issues).toHaveLength(114);
  const first = JSON.parse(String(net.mock.calls[0][1]?.body));
  expect(first.text.format.schema.properties.assignments.required).toEqual(ids);
  expect(first.text.format.schema.properties.assignments.additionalProperties).toBe(false);
  const second = JSON.parse(String(net.mock.calls[1][1]?.body));
  expect(second.text.format.name).toBe('describe_events');
  expect(second.text.format.schema.properties.events.required).toEqual(roots);
  expect(JSON.stringify(second.text.format.schema)).not.toContain('"articles"');
  expect(net).toHaveBeenCalledTimes(2);
});
it.each([
  { a000: 'a999', a001: 'a001' },
  { a000: 'a001', a001: 'a000' },
  { a000: 'a001', a001: '#photo' },
  { a000: 'a000' },
  { a000: 'a000', a001: 'a001', a002: 'a002' },
  { a000: '#duplicate', a001: 'a001' },
])('잘못된 배정 %j이면 메타 호출 없이 실패한다', async assignments => {
  const net = network([{ assignments }]);
  expect(await aiClient('fake-key').organize(['a000 [it] 센서', 'a001 [it] 센서'])).toEqual({ refusal: false, output: null });
  expect(net).toHaveBeenCalledTimes(1);
});
it('빈 입력·전부 제외는 불필요한 메타 호출을 하지 않는다', async () => {
  const net = network([{ assignments: { a000: '#photo', a001: '#market' } }]);
  expect(await aiClient('fake-key').organize([])).toEqual({ refusal: false, output: { issues: [], excluded: [] } });
  expect(net).not.toHaveBeenCalled();
  expect(await aiClient('fake-key').organize(['a000 [it] 사진', 'a001 [eco] 시세'])).toEqual({ refusal: false, output: { issues: [], excluded: ['a000', 'a001'] } });
  expect(net).toHaveBeenCalledTimes(1);
});
it.each([
  { events: {} },
  { events: { a000: { ...meta, articles: ['a999'] } } },
  { events: { a000: { ...meta, importance: 6 } } },
  { events: { a000: { ...meta, terms: [[' '], ['센서']] } } },
])('잘못된 메타데이터는 부분 배정을 반환하지 않는다', async output => {
  const net = network([{ assignments: { a000: 'a000' } }, output]);
  expect(await aiClient('fake-key').organize(['a000 [it] 센서'])).toEqual({ refusal: false, output: null });
  expect(net).toHaveBeenCalledTimes(2);
});
