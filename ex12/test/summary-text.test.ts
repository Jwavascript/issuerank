// 요약 없음 문구(O7 · D32 · AC-04-3): public/index.html summaryHTML(목록 · 북마크 카드 · 이슈맵 툴팁 · 상세창이 함께 씀)을 VM에서 실행 /
// index.html · openapi.yaml 문자열. 요약 있는 카드(3줄 · 이스케이프)는 그대로
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it } from 'vitest';

const html = readFileSync('public/index.html', 'utf8');
const openapi = readFileSync('openapi.yaml', 'utf8');
const between = (source: string, start: string, end: string) => {
  const i = source.indexOf(start), j = source.indexOf(end, i);
  if (i < 0 || j < 0) throw new Error(`없음: ${start}`);
  return source.slice(i, j);
};
const NEW = '요약 없음 · 원문을 확인해 주세요';
const OLD = '요약을 준비 중입니다';

describe('summaryHTML (AC-04-3)', () => {
  const context = createContext({});
  runInContext(between(html, 'const esc =', 'const issueById') + between(html, 'const summaryHTML =', '/* ─────────────── Shared item renderer') + '\nthis.summaryHTML = summaryHTML;', context);
  const summaryHTML = context.summaryHTML as (s: string[] | null) => string;

  it('null → <ul class="summary pending"><li>요약 없음 · 원문을 확인해 주세요</li></ul> 정확히(마크업 · 클래스 그대로)', () => {
    expect(summaryHTML(null)).toBe(`<ul class="summary pending"><li>${NEW}</li></ul>`);
  });

  it('3줄(<b>&"\' 포함) = 이전과 같은 결과(줄마다 li · 이스케이프)', () => {
    expect(summaryHTML(['<b>굵게</b>', 'A & B', `"인용" '작은'`]))
      .toBe('<ul class="summary"><li>&lt;b&gt;굵게&lt;/b&gt;</li><li>A &amp; B</li><li>&quot;인용&quot; &#39;작은&#39;</li></ul>');
  });
});

describe('문구가 있는 곳', () => {
  it('index.html: 옛 문구 0 · 새 문구 1(summaryHTML 안) · summaryHTML( 부르는 곳 3 = 목록 · 북마크 카드(itemHTML) · 툴팁 · 상세창(openDrawer)', () => {
    expect(html.split(OLD).length - 1).toBe(0);
    expect(html.split(NEW).length - 1).toBe(1);
    expect(between(html, 'const summaryHTML =', '/* ─────────────── Shared item renderer')).toContain(NEW);
    expect(html.split('summaryHTML(').length - 1).toBe(3);
    expect(between(html, 'function itemHTML(', '/* ─────────────── Shared date context').split('summaryHTML(').length - 1).toBe(1);
    expect(between(html, '/* ─────────────── Tooltip', '/* ─────────────── Drawer').split('summaryHTML(').length - 1).toBe(1);
    expect(between(html, 'function openDrawer(id)', 'function closeDrawer()').split('summaryHTML(').length - 1).toBe(1);
  });

  it('openapi.yaml: Issue.summary 설명 = 새 문구 · 옛 문구 0', () => {
    expect(openapi).toContain(`          description: AI 3줄 요약. 없으면 null(화면은 "${NEW}")\n`);
    expect(openapi.split(OLD).length - 1).toBe(0);
  });
});
