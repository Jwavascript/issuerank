// 하루 실행 규칙(steps.ts): 가짜 단계 실행 함수로, 자식 프로세스 · 네트워크 · DB 없이
import { describe, expect, it } from 'vitest';
import { runDaily, type Step } from '../server/steps';

// 단계별 종료 코드(또는 던질 오류)를 정해 두고 부른 단계 · 인수 · 출력 줄을 기록
function fake(codes: Partial<Record<Step, number | Error>>) {
  const calls: { step: Step; args: string[] }[] = [];
  const lines: string[] = [];
  const run = async (step: Step, args: string[]) => {
    calls.push({ step, args });
    const c = codes[step] ?? 0;
    if (c instanceof Error) throw c;
    return c;
  };
  return { calls, lines, run, log: (line: string) => { lines.push(line); } };
}

describe('순서 · 멈춤 · 종료 코드', () => {
  const cases: [string, Partial<Record<Step, number>>, Step[], number, string][] = [
    ['모두 정상', { collect: 0, group: 0, summarize: 0 }, ['collect', 'group', 'summarize'], 0,
      '하루 실행 끝 · collect 0 · group 0 · summarize 0'],
    ['collect 일부 실패여도 묶기 · 요약 진행(AC-01-2)', { collect: 1, group: 0, summarize: 0 }, ['collect', 'group', 'summarize'], 1,
      '하루 실행 끝 · collect 1 · group 0 · summarize 0'],
    ['collect 2면 나머지 건너뜀', { collect: 2 }, ['collect'], 2,
      '하루 실행 끝 · collect 2 · group 건너뜀 · summarize 건너뜀'],
    ['group 2면 summarize 건너뜀', { collect: 0, group: 2 }, ['collect', 'group'], 2,
      '하루 실행 끝 · collect 0 · group 2 · summarize 건너뜀'],
    ['group · summarize 1', { collect: 0, group: 1, summarize: 1 }, ['collect', 'group', 'summarize'], 1,
      '하루 실행 끝 · collect 0 · group 1 · summarize 1'],
  ];
  it.each(cases)('%s', async (_, codes, ran, exit, end) => {
    const f = fake(codes);
    expect(await runDaily([], f.run, f.log)).toBe(exit);
    expect(f.calls.map(c => c.step)).toEqual(ran);
    expect(f.lines.filter(l => l.startsWith('== '))).toEqual(ran.map(s => `== ${s} ==`));
    expect(f.lines.at(-1)).toBe(end);
  });

  it('단계 실행 함수가 던지면 실행 오류 · 2 · 나머지 건너뜀, 오류 내용은 찍지 않음', async () => {
    const f = fake({ group: new Error('postgres://u:FAKE-SECRET@host/db FAKE-KEY') });
    expect(await runDaily([], f.run, f.log)).toBe(2);
    expect(f.calls.map(c => c.step)).toEqual(['collect', 'group']);
    expect(f.lines).toEqual([
      '== collect ==', '== group ==', '실행 오류: group', '하루 실행 끝 · collect 0 · group 2 · summarize 건너뜀',
    ]);
    expect(f.lines.join('\n')).not.toMatch(/FAKE/);
  });
});

describe('옵션 넘김', () => {
  it('각 단계가 자기 옵션만 받음', async () => {
    const f = fake({});
    expect(await runDaily(['--pages', '1', '--search', '1', '--max', '3'], f.run, f.log)).toBe(0);
    expect(f.calls).toEqual([
      { step: 'collect', args: ['--pages', '1'] },
      { step: 'group', args: ['--search', '1'] },
      { step: 'summarize', args: ['--max', '3'] },
    ]);
  });

  it('옵션이 없으면 인수 없이(각 명령의 기본값)', async () => {
    const f = fake({});
    await runDaily([], f.run, f.log);
    expect(f.calls.map(c => c.args)).toEqual([[], [], []]);
  });

  it('값 검사는 각 명령에 맡김(그대로 넘김)', async () => {
    const f = fake({ collect: 2 });
    expect(await runDaily(['--pages', 'x'], f.run, f.log)).toBe(2);
    expect(f.calls).toEqual([{ step: 'collect', args: ['--pages', 'x'] }]);
  });

  it.each([[['--ai', '0']], [['--x', '1']], [['--pages', '1', '--ai', '1']]])('다른 인수 %j는 단계 실행 없이 2', async args => {
    const f = fake({});
    expect(await runDaily(args, f.run, f.log)).toBe(2);
    expect(f.calls).toEqual([]);
    expect(f.lines).toEqual(['--pages · --search · --max만 받습니다']);
  });
});
