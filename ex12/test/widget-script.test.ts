// iPhone 위젯(public/widget.js, Scriptable)을 가짜 전역으로 실행(AC-11-1~4). 실제 기기 확인은 검증 18번(사용자 iPhone)
// 가짜 ListWidget · WidgetText · WidgetStack · WidgetSpacer · Color · Font · Request · FileManager · config · Script는
// docs.scriptable.app(2026-10-17 확인)에 있는 속성 · 메서드만 가짐: 문서에 없는 이름을 읽거나 쓰면 실패, 문서에 있지만 가짜에 없는 메서드도 실패
// 파일은 새 VM 컨텍스트에서 async 함수 본문으로 실행(Node 전역 fetch · setTimeout · console 없음). 시각은 테스트마다 고정
import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { describe, expect, it, vi } from 'vitest';

const SOURCE = readFileSync('public/widget.js', 'utf8');
const BASE = 'https://issuerank-kappa.vercel.app';
const LIBRARY = '/var/mobile/Library';
const CACHE = `${LIBRARY}/issuerank-widget.json`;
const FAIL_TEXT = '불러오지 못했습니다';

type Raw = Record<string, any>;
type Spec = { props?: string[]; readonly?: string[]; methods?: string[] };
const FONT_STATICS = ['largeTitle', 'title1', 'title2', 'title3', 'headline', 'subheadline', 'body', 'callout', 'footnote', 'caption1', 'caption2',
  ...['', 'ultraLight', 'thin', 'light', 'regular', 'medium', 'semibold', 'bold', 'heavy', 'black', 'italic'].map(w => (w ? `${w}SystemFont` : 'systemFont')),
  ...['ultraLight', 'thin', 'light', 'regular', 'medium', 'semibold', 'bold', 'heavy', 'black'].flatMap(w => [`${w}MonospacedSystemFont`, `${w}RoundedSystemFont`])];
const DOC: Record<string, Spec> = {
  ListWidget: {
    props: ['backgroundColor', 'backgroundImage', 'backgroundGradient', 'addAccessoryWidgetBackground', 'spacing', 'url', 'refreshAfterDate'],
    methods: ['addText', 'addDate', 'addImage', 'addSpacer', 'addStack', 'setPadding', 'useDefaultPadding', 'presentSmall', 'presentMedium',
      'presentLarge', 'presentExtraLarge', 'presentAccessoryInline', 'presentAccessoryCircular', 'presentAccessoryRectangular'],
  },
  WidgetText: {
    props: ['text', 'textColor', 'font', 'textOpacity', 'lineLimit', 'minimumScaleFactor', 'shadowColor', 'shadowRadius', 'shadowOffset', 'url'],
    methods: ['leftAlignText', 'centerAlignText', 'rightAlignText'],
  },
  WidgetStack: {
    props: ['backgroundColor', 'backgroundImage', 'backgroundGradient', 'spacing', 'size', 'cornerRadius', 'borderWidth', 'borderColor', 'url'],
    methods: ['addText', 'addDate', 'addImage', 'addSpacer', 'addStack', 'setPadding', 'useDefaultPadding', 'topAlignContent', 'centerAlignContent',
      'bottomAlignContent', 'layoutHorizontally', 'layoutVertically'],
  },
  WidgetSpacer: { props: ['length'] },
  Color: { readonly: ['hex', 'red', 'green', 'blue', 'alpha'] },
  ColorStatic: { methods: ['black', 'darkGray', 'lightGray', 'white', 'gray', 'red', 'green', 'blue', 'cyan', 'yellow', 'magenta', 'orange', 'purple', 'brown', 'clear', 'dynamic'] },
  Font: {},
  FontStatic: { methods: FONT_STATICS },
  Request: {
    props: ['url', 'method', 'headers', 'body', 'timeoutInterval', 'onRedirect', 'allowInsecureRequest'], readonly: ['response'],
    methods: ['load', 'loadString', 'loadJSON', 'loadImage', 'addParameterToMultipart', 'addFileDataToMultipart', 'addFileToMultipart', 'addImageToMultipart'],
  },
  FileManager: {
    methods: ['read', 'readString', 'readImage', 'write', 'writeString', 'writeImage', 'remove', 'move', 'copy', 'fileExists', 'isDirectory', 'createDirectory',
      'temporaryDirectory', 'cacheDirectory', 'documentsDirectory', 'libraryDirectory', 'joinPath', 'allTags', 'addTag', 'removeTag', 'readExtendedAttribute',
      'writeExtendedAttribute', 'removeExtendedAttribute', 'allExtendedAttributes', 'getUTI', 'listContents', 'fileName', 'fileExtension', 'bookmarkedPath',
      'bookmarkExists', 'downloadFileFromiCloud', 'isFileStoredIniCloud', 'isFileDownloaded', 'creationDate', 'modificationDate', 'fileSize', 'allFileBookmarks'],
  },
  FileManagerStatic: { methods: ['local', 'iCloud'] },
  config: { readonly: ['runsInApp', 'runsInActionExtension', 'runsWithSiri', 'runsInWidget', 'runsInAccessoryWidget', 'runsInNotification', 'runsFromHomeScreen', 'widgetFamily'] },
  Script: { methods: ['name', 'complete', 'setShortcutOutput', 'setWidget'] },
};

// 서버 응답(성공 · 실패) 한 가지: 200 등 상태와 본문 / 'throw' = loadString이 바로 던짐 / 'reject' = 시간 초과(거절)
type Reply = { status: number; body: string } | 'throw' | 'reject';

// 가짜 Scriptable 한 벌. files는 실행 사이에 이어 쓰는 라이브러리 폴더(오프라인 캐시)
function scriptable(family: string | null, reply: Reply, files = new Map<string, string>()) {
  const violations: string[] = [], events: string[] = [], dirs: string[] = [], writes: string[] = [];
  const widgets: Raw[] = [], requests: Raw[] = [], shown: Raw[] = [];
  const raw = new WeakMap<object, Raw>();
  let complete = 0;
  // 스크립트의 try가 오류를 삼켜도 테스트가 실패하도록 위반은 모두 기록(run이 비었는지 봄)
  const notFaked = (name: string) => () => { violations.push(`가짜에 없음 ${name}`); throw new Error(`가짜에 없는 메서드: ${name}`); };
  // 문서에 있는 이름만 읽기(props · readonly · methods)와 쓰기(props)를 허용
  const wrap = <T extends object>(name: string, target: T, spec: Spec): T => {
    for (const m of spec.methods ?? []) if (!(m in target)) (target as Raw)[m] = notFaked(`${name}.${m}`);
    const readable = new Set([...(spec.props ?? []), ...(spec.readonly ?? []), ...(spec.methods ?? []), 'prototype']);
    const writable = new Set(spec.props ?? []);
    const proxy = new Proxy(target, {
      get(t, key, receiver) {
        if (typeof key === 'string' && !readable.has(key)) { violations.push(`읽기 ${name}.${key}`); throw new TypeError(`문서에 없는 API ${name}.${key}`); }
        return Reflect.get(t, key, receiver);
      },
      set(t, key, value, receiver) {
        if (typeof key !== 'string' || !writable.has(key)) { violations.push(`쓰기 ${name}.${String(key)}`); throw new TypeError(`쓸 수 없는 속성 ${name}.${String(key)}`); }
        return Reflect.set(t, key, value, receiver);
      },
    });
    raw.set(proxy, target as Raw);
    return proxy;
  };
  // 클래스(정적 메서드도 문서 이름만). Script.name처럼 함수 자체 속성과 겹치는 이름이 있어 defineProperty로 넣음
  const klass = (name: string, make: (...args: any[]) => object, statics: Spec, impl: Raw = {}) => {
    const fn = function (...args: any[]) { return make(...args); };
    for (const [key, value] of Object.entries(impl)) Object.defineProperty(fn, key, { value, writable: true, configurable: true, enumerable: true });
    return wrap(name, fn, statics);
  };
  const notConstructible = (name: string) => () => { violations.push(`new ${name}`); throw new Error(`${name}는 직접 만들지 않음`); };

  // 위젯 · 스택에 붙는 요소
  const container = (kids: Raw[]) => ({
    addText: (text: string) => { const t: Raw = { type: 'text', text }; kids.push(t); return wrap('WidgetText', t, DOC.WidgetText); },
    addSpacer: (length: number | null) => { const s: Raw = { type: 'spacer', length }; kids.push(s); return wrap('WidgetSpacer', s, DOC.WidgetSpacer); },
    addStack: () => { const s: Raw = { type: 'stack', kids: [] }; kids.push(s); return wrap('WidgetStack', Object.assign(s, container(s.kids)), DOC.WidgetStack); },
  });
  const ListWidget = klass('ListWidget', () => {
    const w: Raw = { kids: [], presented: [] };
    widgets.push(w);
    const present = (size: string) => () => { w.presented.push(size); events.push(`present ${size}`); return Promise.resolve(); };
    return wrap('ListWidget', Object.assign(w, container(w.kids), {
      presentSmall: present('small'), presentMedium: present('medium'), presentLarge: present('large'), presentExtraLarge: present('extraLarge'),
    }), DOC.ListWidget);
  }, {});
  const Color = klass('Color', (hex: string, alpha?: number) => wrap('Color', { hex, alpha }, DOC.Color), DOC.ColorStatic, {
    dynamic: (light: object, dark: object) => wrap('Color', { dynamic: true, light: raw.get(light)?.hex, dark: raw.get(dark)?.hex }, DOC.Color),
  });
  const fonts: Raw = {};
  for (const name of FONT_STATICS) fonts[name] = (size?: number) => wrap('Font', { name, size }, DOC.Font);
  const Font = klass('Font', (name: string, size: number) => wrap('Font', { name, size }, DOC.Font), DOC.FontStatic, fonts);
  const Request = klass('Request', (url: string) => {
    const r: Raw = { url, method: 'GET', timeoutInterval: 60, response: undefined };
    requests.push(r);
    r.loadString = () => {
      events.push('request');
      if (reply === 'throw') throw new Error('The Internet connection appears to be offline.');
      if (reply === 'reject') return Promise.reject(new Error('The request timed out.'));
      r.response = { statusCode: reply.status, url: r.url, headers: {} };
      return Promise.resolve(reply.body);
    };
    return wrap('Request', r, DOC.Request);
  }, {});
  const fm = wrap('FileManager', {
    libraryDirectory: () => { dirs.push('library'); return LIBRARY; },
    cacheDirectory: () => { dirs.push('cache'); return '/var/mobile/Caches'; },
    documentsDirectory: () => { dirs.push('documents'); return '/var/mobile/Documents'; },
    temporaryDirectory: () => { dirs.push('temporary'); return '/tmp'; },
    joinPath: (a: string, b: string) => `${a}/${b}`,
    fileExists: (path: string) => files.has(path),
    readString: (path: string) => files.get(path) ?? null,
    writeString: (path: string, content: string) => { writes.push(path); files.set(path, content); },
  }, DOC.FileManager);
  const FileManager = klass('FileManager', notConstructible('FileManager'), DOC.FileManagerStatic, { local: () => fm });
  const config = wrap('config', {
    widgetFamily: family, runsInWidget: family !== null, runsInAccessoryWidget: !!family && family.startsWith('accessory'),
    runsInApp: family === null, runsInActionExtension: false, runsWithSiri: false, runsInNotification: false, runsFromHomeScreen: false,
  }, DOC.config);
  const Script = klass('Script', notConstructible('Script'), DOC.Script, {
    name: () => '이슈랭크',
    setWidget: (w: object) => { shown.push(raw.get(w)!); events.push('setWidget'); },
    complete: () => { complete++; events.push('complete'); },
  });
  const globals = {
    ListWidget, WidgetText: klass('WidgetText', notConstructible('WidgetText'), {}), WidgetStack: klass('WidgetStack', notConstructible('WidgetStack'), {}),
    WidgetSpacer: klass('WidgetSpacer', notConstructible('WidgetSpacer'), {}), Color, Font, Request, FileManager, config, Script,
  };
  return { globals, violations, events, dirs, writes, widgets, requests, shown, raw, files, get complete() { return complete; } };
}

type Env = ReturnType<typeof scriptable>;
// 새 VM 컨텍스트(JS 기본 객체 + 지금 고정된 Date만)에서 파일을 async 함수 본문으로 실행. 예외가 밖으로 나오면 테스트 실패
async function run(env: Env) {
  const names = Object.keys(env.globals);
  const fn = runInContext(`(async function (${names.join(', ')}) {\n${SOURCE}\n})`, createContext({ Date }), { filename: 'public/widget.js' });
  await fn(...names.map(n => env.globals[n as keyof typeof env.globals]));
  expect(env.violations).toEqual([]);
  return env;
}

// 그린 위젯을 평범한 값으로(색 = { light, dark } hex, 글꼴 = { name, size })
function drawn(env: Env) {
  expect(env.widgets).toHaveLength(1);
  const w = env.widgets[0];
  const color = (c: object) => { const r = env.raw.get(c)!; return r.dynamic ? { light: r.light, dark: r.dark } : r.hex; };
  const texts = w.kids.filter((k: Raw) => k.type === 'text').map((t: Raw) => ({
    text: t.text, url: t.url, lineLimit: t.lineLimit, font: env.raw.get(t.font), color: t.textColor && color(t.textColor),
  }));
  return { w, texts, types: w.kids.map((k: Raw) => k.type), background: color(w.backgroundColor) };
}

const items = (n: number, date = '2026-10-17') => Array.from({ length: n }, (_, i) => ({
  id: `${date}-${['pol', 'eco', 'soc', 'cul', 'wor', 'it'][i % 6]}-${i + 1}`, section: ['pol', 'eco', 'soc', 'cul', 'wor', 'it'][i % 6],
  title: `헤드라인 ${i + 1} — 아주 긴 제목이면 iOS가 줄 제한에서 끝을 줄입니다`, path: `/?issue=${date}-${['pol', 'eco', 'soc', 'cul', 'wor', 'it'][i % 6]}-${i + 1}`,
}));
const widgetJson = (n: number, extra: Raw = {}) => JSON.stringify({
  date: '2026-10-17', updatedAt: '2026-10-17T04:31:12+09:00', updatedLabel: '10.17 오전 4:31 수집', morePath: '/#list', items: items(n), ...extra,
});
const ok = (body: string): Reply => ({ status: 200, body });

// 크기별 규칙(표): 줄 수 · 글자 크기 · 제목당 줄 · 줄 간격 · 줄 링크
const RULES: [string | null, { rows: number; size: number; lines: number; gap: number; links: boolean }][] = [
  ['small', { rows: 3, size: 13, lines: 2, gap: 4, links: false }],
  ['medium', { rows: 5, size: 13, lines: 1, gap: 3, links: true }],
  ['large', { rows: 10, size: 14, lines: 1, gap: 6, links: true }],
  ['extraLarge', { rows: 10, size: 14, lines: 1, gap: 6, links: true }],
  ['accessoryRectangular', { rows: 3, size: 13, lines: 2, gap: 4, links: false }],
  ['accessoryInline', { rows: 3, size: 13, lines: 2, gap: 4, links: false }],
  ['accessoryCircular', { rows: 3, size: 13, lines: 2, gap: 4, links: false }],
  [null, { rows: 5, size: 13, lines: 1, gap: 3, links: true }],
];
const TITLE = { light: '#161616', dark: '#ecebe8' };
const FOOT = { light: '#6f6b65', dark: '#9b978f' };
const BACKGROUND = { light: '#ffffff', dark: '#1a1a19' };

describe('위젯 크기 · 줄 수 · 링크 (AC-11-1 · AC-11-2)', () => {
  it('8가지 크기 × 항목 30 · 10 · 5 · 3 · 1: 제목 = 앞에서부터 크기만큼(제목 + 하단뿐), 줄 · 위젯 링크, 요청 1개, setWidget · 미리보기 · complete', async () => {
    vi.setSystemTime('2026-10-17T07:00:00+09:00');
    for (const [family, rule] of RULES) {
      for (const n of [30, 10, 5, 3, 1]) {
        const env = await run(scriptable(family, ok(widgetJson(n))));
        const { w, texts, types } = drawn(env);
        const rows = Math.min(n, rule.rows);
        const want = items(n).slice(0, rows);
        const label = `${family} × ${n}`;
        expect(types, label).toEqual([...Array(rows).fill('text'), 'spacer', 'text']);
        expect(texts.map((t: Raw) => t.text), label).toEqual([...want.map(i => i.title), '10.17 오전 4:31 수집']);
        expect(texts.slice(0, rows).map((t: Raw) => t.url), label).toEqual(want.map(i => (rule.links ? BASE + i.path : undefined)));
        expect(texts[rows].url, label).toBeUndefined();
        expect(w.url, label).toBe(`${BASE}/#list`);
        expect(w.kids.find((k: Raw) => k.type === 'spacer').length, label).toBeNull();
        expect(env.requests.map(r => [r.url, r.method, r.timeoutInterval]), label).toEqual([[`${BASE}/api/widget`, 'GET', 10]]);
        expect(new URL(env.requests[0].url).search, label).toBe('');
        expect(env.shown, label).toEqual(family === null ? [] : [w]);
        expect(w.presented, label).toEqual(family === null ? ['medium'] : []);
        expect(env.complete, label).toBe(1);
        expect(env.events.at(-1), label).toBe('complete');
      }
    }
  });

  it('morePath가 /로 시작하지 않거나 없으면 위젯 url = BASE + /', async () => {
    for (const extra of [{ morePath: 'https://example.invalid/' }, { morePath: undefined }, { morePath: 3 }]) {
      const { w } = drawn(await run(scriptable('small', ok(widgetJson(5, extra)))));
      expect(w.url).toBe(`${BASE}/`);
    }
  });
});

describe('글자 · 색 · 하단 · 갱신 힌트 (AC-11-3)', () => {
  it('크기별 제목 semibold 13 · 13 · 14 · 줄 제한 2 · 1 · 1 · 줄 간격 4 · 3 · 6, 하단 regular 10 = updatedLabel, 색 = 사이트 토큰(밝게 · 어둡게)', async () => {
    vi.setSystemTime('2026-10-17T07:00:00+09:00');
    for (const [family, rule] of RULES) {
      const { w, texts, background } = drawn(await run(scriptable(family, ok(widgetJson(30)))));
      for (const t of texts.slice(0, -1)) expect(t).toMatchObject({ font: { name: 'semiboldSystemFont', size: rule.size }, lineLimit: rule.lines, color: TITLE });
      expect(texts.at(-1)).toEqual({ text: '10.17 오전 4:31 수집', url: undefined, lineLimit: undefined, font: { name: 'regularSystemFont', size: 10 }, color: FOOT });
      expect(background).toEqual(BACKGROUND);
      expect(w.spacing).toBe(rule.gap);
    }
  });

  // [KST 지금, 보여 주는 데이터(date 또는 실패), 기대 refreshAfterDate(KST)]
  const CASES: [string, string, string][] = [
    ['2026-10-17T12:00:00+09:00', '2026-10-17', '2026-10-17T15:00:00+09:00'],
    ['2026-10-17T23:00:00+09:00', '2026-10-17', '2026-10-18T02:00:00+09:00'],
    ['2026-10-17T12:00:00+09:00', '2026-10-16', '2026-10-17T12:30:00+09:00'],
    ['2026-10-17T00:10:00+09:00', '2026-10-16', '2026-10-17T04:30:00+09:00'],
    ['2026-10-17T03:00:00+09:00', '2026-10-16', '2026-10-17T04:30:00+09:00'],
    ['2026-10-17T04:20:00+09:00', '2026-10-16', '2026-10-17T04:50:00+09:00'],
    ['2026-10-17T05:10:00+09:00', '실패 · 캐시 없음', '2026-10-17T05:40:00+09:00'],
    ['2026-10-17T05:10:00+09:00', '실패 · 오늘 캐시', '2026-10-17T08:10:00+09:00'],
  ];
  it(`갱신 힌트: 데이터 date가 KST 오늘이면 3시간 뒤, 아니면 KST 오늘 04:30과 30분 뒤 중 늦은 때(실패 화면도) · TZ=${process.env.TZ ?? '(기본)'}`, async () => {
    for (const [now, shown, expected] of CASES) {
      vi.setSystemTime(now);
      const files = new Map<string, string>();
      if (shown === '실패 · 오늘 캐시') files.set(CACHE, widgetJson(5));
      const reply = shown.startsWith('실패') ? { status: 500, body: 'Internal Server Error' } : ok(widgetJson(5, { date: shown }));
      const { w, texts } = drawn(await run(scriptable('medium', reply, files)));
      expect(w.refreshAfterDate.getTime(), `${now} ${shown}`).toBe(Date.parse(expected));
      expect(texts[0].text === FAIL_TEXT, `${now} ${shown}`).toBe(shown === '실패 · 캐시 없음');
    }
  });
});

describe('실패 · 오프라인 캐시 (AC-11-4)', () => {
  it('① 성공만 라이브러리 폴더에 저장 → ②~⑥ 실패는 그 캐시 그대로(덮지 않음) → ⑦ ⑧ 캐시 없음 · 깨짐은 안내 한 줄', async () => {
    vi.setSystemTime('2026-10-17T07:00:00+09:00');
    const files = new Map<string, string>();
    const body = widgetJson(7);
    // ① 성공
    const first = await run(scriptable('medium', ok(body), files));
    expect([...files.keys()]).toEqual([CACHE]);
    expect(JSON.parse(files.get(CACHE)!)).toEqual(JSON.parse(body));
    expect(first.writes).toEqual([CACHE]);
    expect(first.dirs).toEqual(['library']);
    const expected = drawn(first);
    const snapshot = (d: ReturnType<typeof drawn>) => ({ texts: d.texts, url: d.w.url, types: d.types });

    // ②~⑥ 실패: 캐시의 제목 · 순서 · 링크 · 하단 그대로, 캐시 파일 안 바뀜
    const failures: [string, Reply][] = [
      ['② Request가 던짐', 'throw'],
      ['③ 시간 초과(거절)', 'reject'],
      ['④ 500', { status: 500, body: 'Internal Server Error' }],
      ['⑤ 404', { status: 404, body: JSON.stringify({ message: '해당 날짜의 랭킹 데이터가 없습니다' }) }],
      ['⑥ 200 · items 빈 배열', ok(widgetJson(0))],
      ['⑥ 200 · title 숫자', ok(JSON.stringify({ ...JSON.parse(widgetJson(3)), items: [{ id: 'x', section: 'pol', title: 3, path: '/?issue=x' }] }))],
      ['⑥ 200 · path가 http로 시작', ok(JSON.stringify({ ...JSON.parse(widgetJson(3)), items: [{ id: 'x', section: 'pol', title: 'x', path: 'https://example.invalid/' }] }))],
      ['⑥ 200 · JSON 아님', ok('<html>점검 중</html>')],
    ];
    for (const [label, reply] of failures) {
      const env = await run(scriptable('medium', reply, files));
      expect(snapshot(drawn(env)), label).toEqual(snapshot(expected));
      expect(env.writes, label).toEqual([]);
      expect(files.get(CACHE), label).toBe(body);
      expect(env.dirs, label).toEqual(['library']);
      expect([env.shown.length, env.complete], label).toEqual([1, 1]);
    }

    // ⑦ 캐시 없음 + 실패 · ⑧ 캐시가 JSON 아님 · 모양 틀림 + 실패: 안내 한 줄뿐, 위젯 url = BASE + /
    for (const [label, cache] of [['⑦ 캐시 없음', null], ['⑧ JSON 아님', '{broken'], ['⑧ 모양 틀림', widgetJson(0)]] as const) {
      for (const family of ['small', 'medium', 'large', null]) {
        const own = new Map<string, string>(cache === null ? [] : [[CACHE, cache]]);
        const env = await run(scriptable(family, 'reject', own));
        const { w, texts, types } = drawn(env);
        expect(types, label).toEqual(['text']);
        expect(texts[0].text, label).toBe(FAIL_TEXT);
        expect(texts[0].url, label).toBeUndefined();
        expect(w.url, label).toBe(`${BASE}/`);
        expect([env.shown.length, w.presented.length, env.complete], label).toEqual(family === null ? [0, 1, 1] : [1, 0, 1]);
        expect(own.get(CACHE) ?? null, label).toBe(cache);
      }
    }
  });
});
