import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { expect, it } from 'vitest';

it('임시 검색을 여러 번 교체해도 색상을 재사용하지 않고 저장 슬롯과 충돌하지 않는다', () => {
  const script = readFileSync('public/keywords.js', 'utf8');
  const start = script.indexOf('  const norm ='), end = script.indexOf('  const chip =');
  const context = createContext({});
  runInContext(script.slice(start, end), context);
  const evaluate = (code: string) => runInContext(code, context);
  const first = evaluate(`color('반도체')`);
  evaluate(`terms=['반도체','실적','기술']; terms.forEach(color); terms=terms.filter(t=>t!=='반도체'); color('공동'); terms=['실적','기술','공동']; terms=terms.filter(t=>t!=='실적'); terms.push('반도체');`);
  expect(evaluate(`color('반도체')`)).toBe(first);
  expect(evaluate(`new Set(terms.map(color)).size`)).toBe(3);
  expect(evaluate(`colorStyle('공동')`)).toContain('hsl(');
  evaluate(`saved=[{name:'삼성전자',color:0},{name:'SK하이닉스',color:1}];`);
  expect(evaluate(`color('삼성전자')`)).toBe(0);
  expect(evaluate(`color('SK하이닉스')`)).toBe(1);
  expect(evaluate(`color('반도체')`)).toBeGreaterThanOrEqual(5);
  evaluate(`saved.reverse();`);
  expect(evaluate(`color('삼성전자')`)).toBe(0);
});
