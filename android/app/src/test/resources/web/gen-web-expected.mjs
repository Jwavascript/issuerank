// 웹 기대값(O8 데이터 층 JVM 시험): ex12/public/index.html · widget.js의 함수 글자를 그대로 떼어 node:vm으로 돌린 결과 JSON.
// 바깥 패키지 0(node 내장만) · 고정 시각 · 고정 입력 · LF → 다시 돌리면 같은 바이트. DOM에 묶인 drawMap · renderMap · renderContext는
// 아래 작은 가짜 DOM(필요한 속성만)으로 그대로 돌리고, 지도 폭 · 높이는 index.html CSS(137~138 · 251 · 390 · 394)를 옮긴 cssMap*로 줌.
// 실행(아무 곳에서나): node android/app/src/test/resources/web/gen-web-expected.mjs  → 같은 폴더 *.json
process.env.TZ = 'Asia/Seoul'; // parse() · fmtDate의 로컬 시간 Date(브라우저와 같게 한국 시간대로 고정)

import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import vm from 'node:vm';

const HERE = new URL('./', import.meta.url);
const ROOT = new URL('../../../../../../', HERE); // web → resources → test → src → app → android → 저장소
const text = rel => readFileSync(new URL(rel, ROOT), 'utf8').replace(/\r\n/g, '\n');
const INDEX = text('ex12/public/index.html');
const WIDGET = text('ex12/public/widget.js');
const SEED = JSON.parse(readFileSync(new URL('../fixtures/rankings-d0.json', HERE), 'utf8'));

// ── 웹 글자 떼기: 그 접두로 시작하는 줄부터 — 함수 = 맨 앞 칸 `}` 줄까지, 배열 = `];` 줄까지, 그 밖 = 한 줄 ──
const sources = [];
function grab(file, src, start) {
  const lines = src.split('\n');
  const i = lines.findIndex(l => l.startsWith(start));
  if (i < 0) throw new Error(`${file}: "${start}" 없음`);
  let j = i;
  if (lines[i].trimEnd().endsWith('{')) while (lines[j] !== '}') j++;
  else if (lines[i].trimEnd().endsWith('[')) while (lines[j] !== '];') j++;
  const code = lines.slice(i, j + 1).join('\n');
  sources.push({ file, start, lines: `${i + 1}-${j + 1}`, sha256: createHash('sha256').update(code).digest('hex').slice(0, 16) });
  return code;
}
const web = start => grab('ex12/public/index.html', INDEX, start);
const widgetJs = start => grab('ex12/public/widget.js', WIDGET, start);

// 이 파일들이 쓰는 웹 조각(먼저 한 번씩 떼어 sources에 기록)
const CODE = {
  sections: [web('const SECTIONS = ['), web('const SEC = '), web('const secColor = '), web('const secIssues = ')].join('\n'),
  dates: [web('const TODAY = '), web('const iso = '), web('const parse = '), web('const addDays = '), web('const WD = '),
    web('const fmtDate = '), web('const fmtShort = '), web('const fmtUpdated = ')].join('\n'),
  ratio: [web('const L = '), web('const ratio = '), web('const rankMax = ')].join('\n'),
  timeAgo: web('const timeAgo = '),
  issueById: web('const issueById = '),
  iconMark: web('const ICON_MARK = '),
  renderContext: web('function renderContext()'),
  squarify: web('function squarify('),
  map: [web('const GAP = '), web('const NARROW = '), web('const secTotal = '), web('function renderMap()'), web('function drawMap()')].join('\n'),
  authMessage: web('function authMessage('),
  googleStartMessage: web('function googleStartMessage('),
  googleReturn: web('function googleReturn('),
  share: [web('const SHARE_BASE = '), web('const SITE_SHARE = '), web('function issueShare(')].join('\n'),
  nav: [web('const ORDER = '), web('const viewFromHash = ')].join('\n'),
  issueShortcut: web('function issueShortcut()'),
  valid: widgetJs('function valid('),
  refreshAfter: widgetJs('function refreshAfter('),
};

// 고정 시각 Date(인자 없는 new Date() · Date.now()만 바뀜)
const fixedDate = now => class extends Date {
  constructor(...a) { super(...(a.length ? a : [now])); }
  static now() { return now; }
};
const T = iso => Date.parse(iso);
// vm 문맥: 기본 = 한국어 숫자 꼴(브라우저 언어 ko-KR 가정 — toLocaleString)
function context(globals, now = T('2026-11-03T03:00:00Z')) {
  const ctx = vm.createContext({ Date: fixedDate(now), URLSearchParams, ...globals });
  vm.runInContext("Number.prototype.toLocaleString = function () { return new Intl.NumberFormat('ko-KR').format(this); };", ctx);
  return ctx;
}
const run = (ctx, ...codes) => { for (const c of codes) vm.runInContext(c, ctx); return ctx; };
const evalIn = (ctx, expr) => vm.runInContext(expr, ctx);

// JSON: NaN · ±Infinity는 글자로(JSON.stringify는 null로 바꿈) · 사례 하나 한 줄
const num = v => (typeof v === 'number' && !Number.isFinite(v) ? String(v) : v);
const replacer = (_, v) => num(v);
function write(name, meta, cases) {
  const body = `{"meta":${JSON.stringify(meta)},\n"cases":[\n${cases.map(c => JSON.stringify(c, replacer)).join(',\n')}\n]}\n`;
  writeFileSync(new URL(name, HERE), body);
  console.log(name, cases.length, 'cases', Buffer.byteLength(body), 'B');
}
const used = (...starts) => ({ sources: sources.filter(s => starts.includes(s.start)) });

// ════════ 1. squarify ════════
{
  const ctx = run(context({}), CODE.squarify);
  const sq = evalIn(ctx, 'squarify');
  const seedLog = SEED.sections.flatMap(s => s.issues.map(it => Math.log1p(it.articleCount)));
  const cases = [
    ['one', [5], 0, 0, 100, 50],
    ['two', [3, 1], 0, 0, 400, 300],
    ['sections-6-wide', [1, 1, 1, 1, 1, 1], 0, 0, 1157, 820],
    ['sections-6-tall', [1, 1, 1, 1, 1, 1], 0, 0, 328, 760],
    ['sections-5-and-0', [1, 1, 1, 1, 1, 0], 0, 0, 705, 820],
    ['ties-5', [3, 3, 3, 3, 3], 0, 22, 190, 250],
    ['ties-mixed-stable', [2, 5, 2, 5, 2], 0, 0, 300, 200],
    ['seed-log1p-30', seedLog, 0, 0, 1157, 820],
    ['descending-10', [10, 9, 8, 7, 6, 5, 4, 3, 2, 1], 5, 7, 300, 200],
    ['ascending-10', [1, 2, 3, 4, 5, 6, 7, 8, 9, 10], 0, 0, 200, 300],
    ['zeros-mixed', [0, 4, 0, 2, 0], 0, 0, 300, 300],
    ['all-zero', [0, 0, 0], 0, 0, 300, 300],
    ['huge', [1e9, 999999999, 1, 2, 3], 0, 0, 1000, 600],
    ['tiny', [0.001, 0.002, 0.0005], 0, 0, 0.5, 0.5],
    ['width-0', [1, 2], 0, 0, 0, 100],
    ['height-negative', [1, 2], 0, 0, 100, -5],
    ['fractional-origin', [5, 3, 2, 1], 1.5, 22, 380.25, 198.75],
    ['negative-value', [2, -1, 3], 0, 0, 100, 100],
    ['empty', [], 0, 0, 100, 100],
    // worst(row + 다음) === worst(row)(정확히 같음)일 때 `<=`가 다음을 같은 줄에 넣음: 넓이 2 · 변 2(S/2)
    ['tie-worst-4x2', [1, 1, 1, 1], 0, 0, 4, 2],
    ['tie-worst-2x2', [1, 1], 0, 0, 2, 2],
  ].map(([name, values, x, y, w, h]) => ({
    name, values, x, y, w, h,
    out: sq(values.map((value, i) => ({ i, value })), x, y, w, h).map(r => [r.i, r.x, r.y, r.w, r.h]),
  }));
  write('squarify.json', used('function squarify('), cases);
}

// ════════ 2. 이슈맵(renderMap + drawMap) ════════
// CSS: .wrap-wide max-width 1240 · 좌우 24(640 이하 16) · border-box → 지도 폭 / .map 높이 clamp(440, 100vh − 260, 820), 640 이하 760
const cssMapWidth = vw => Math.min(1240, vw) - 2 * (vw <= 640 ? 16 : 24);
const cssMapHeight = (vw, vh) => (vw <= 640 ? 760 : Math.max(440, Math.min(vh - 260, 820)));

function el(className = '') {
  const e = { className, dataset: {}, style: {}, children: [], parent: null, attrs: {}, classes: new Set(), html: '', textContent: '' };
  Object.defineProperty(e.style, 'setProperty', { value: (k, v) => { e.style[k] = v; }, enumerable: false });
  e.classList = { toggle: (c, on) => (on ? e.classes.add(c) : e.classes.delete(c)), contains: c => e.classes.has(c) };
  e.appendChild = c => { c.parent = e; e.children.push(c); return c; };
  e.prepend = c => { c.parent = e; e.children.unshift(c); };
  e.remove = () => { if (e.parent) e.parent.children = e.parent.children.filter(x => x !== e); };
  e.setAttribute = (k, v) => { e.attrs[k] = String(v); };
  e.querySelector = sel => find(e, sel)[0] ?? null;
  e.querySelectorAll = sel => find(e, sel);
  Object.defineProperty(e, 'innerHTML', {
    get: () => e.html,
    set: h => {
      e.html = h;
      // 타일 안 칸(805): .t · .foot > .rk · .n · .mark
      if (h.startsWith('<span class="t">')) {
        e.children = [];
        e.appendChild(el('t'));
        const foot = e.appendChild(el('foot'));
        foot.appendChild(el('rk')); foot.appendChild(el('n num')); foot.appendChild(el('mark'));
      }
    },
  });
  return e;
}
// 선택자: .클래스 또는 .클래스[data-키="값"] — 자손 깊이 우선
function find(root, sel) {
  const m = /^\.([\w-]+)(?:\[data-([\w-]+)="([^"]*)"\])?$/.exec(sel);
  if (!m) throw new Error('선택자 ' + sel);
  const out = [];
  const walk = n => { for (const c of n.children) { if (c.className.split(' ').includes(m[1]) && (!m[2] || c.dataset[m[2]] === m[3])) out.push(c); walk(c); } };
  walk(root);
  return out;
}
const px = v => parseFloat(v);

function mapCase(name, ranking, vw, vh, zoom) {
  const W = cssMapWidth(vw);
  const map = el('map');
  Object.defineProperty(map, 'clientWidth', { get: () => W });
  Object.defineProperty(map, 'clientHeight', { get: () => (map.style.height ? px(map.style.height) : cssMapHeight(vw, vh)) });
  const body = el(), crumbs = el(), legend = el();
  body.appendChild(map);
  const byId = { map, mapBody: body, crumbs, legend };
  const document = { getElementById: id => byId[id] ?? null, createElement: () => el() };
  const matchMedia = q => ({ matches: vw <= Number(/max-width:\s*(\d+)px/.exec(q)[1]) });
  const ctx = context({ document, matchMedia, ranking, state: { zoom, selected: null }, findBookmark: () => null });
  run(ctx, CODE.sections, CODE.ratio, CODE.iconMark, CODE.squarify, CODE.map);
  evalIn(ctx, 'renderMap()');
  const label = box => box.children.find(c => c.className === 'group-label');
  return {
    name, vw, vh, zoom, narrow: vw <= 640, width: map.clientWidth, height: map.clientHeight,
    crumb: /<span class="num">([^<]*)<\/span>/.exec(crumbs.innerHTML)[1],
    boxes: map.children.map(box => ({
      s: box.dataset.s, x: px(box.style.left), y: px(box.style.top), w: px(box.style.width), h: px(box.style.height),
      hidden: box.style.opacity === 0, zoom: label(box).dataset.zoom,
      total: Number(/<span class="cnt num">(\d+)<\/span>/.exec(label(box).innerHTML)[1]),
      arrow: /<span class="arrow">([^<]*)<\/span>/.exec(label(box).innerHTML)[1],
      tiles: box.children.filter(c => c.className === 'tile').map(t => {
        const te = t.querySelector('.t');
        return {
          id: t.dataset.id, x: px(t.style.left), y: px(t.style.top), w: px(t.style.width), h: px(t.style.height),
          l: t.style['--l'], fs: px(te.style.fontSize), lines: te.style.webkitLineClamp,
          xs: t.classes.has('xs'), xxs: t.classes.has('xxs'),
          rk: t.querySelector('.rk').textContent, n: t.querySelector('.n').textContent, aria: t.attrs['aria-label'],
        };
      }),
    })),
  };
}
{
  const clone = () => structuredClone(SEED);
  const ties = clone(); ties.sections.forEach(s => s.issues.forEach(it => { it.articleCount = 50; }));
  const zero = clone(); zero.sections[0].issues = []; [5, 0, 0, 3, 1].forEach((n, i) => { zero.sections[1].issues[i].articleCount = n; });
  const huge = clone(); [1000000000, 999999999, 5, 2, 1].forEach((n, i) => { huge.sections[0].issues[i].articleCount = n; });
  const partial = clone(); [1, 2, 3, 1, 5, 4].forEach((n, i) => { partial.sections[i].issues = partial.sections[i].issues.slice(0, n); });
  const all = [[360, 800], [640, 800], [641, 800], [753, 1205], [1205, 753], [1400, 900], [1205, 600]];
  const few = [[360, 800], [753, 1205], [1205, 753]];
  const cases = [];
  for (const [rname, r, views] of [['seed-d0', SEED, all], ['ties', ties, few], ['zero', zero, few], ['huge', huge, few], ['partial', partial, few]]) {
    for (const [vw, vh] of views) for (const zoom of [null, 'eco']) cases.push({ ranking: rname, ...mapCase(rname, r, vw, vh, zoom) });
  }
  write('issuemap.json', { ...used('const GAP = ', 'const NARROW = ', 'const secTotal = ', 'function renderMap()', 'function drawMap()', 'function squarify(', 'const L = ', 'const ratio = ', 'const rankMax = '),
    rankings: 'seed-d0 = ../fixtures/rankings-d0.json · ties = 연관기사 모두 50 · zero = 정치 0개 + 경제 [5,0,0,3,1] · huge = 정치 [1e9, 999999999, 5, 2, 1] · partial = 섹션별 [1,2,3,1,5,4]개',
    css: 'width = min(1240, vw) − 2·(vw ≤ 640 ? 16 : 24) · height = vw ≤ 640 ? 760 : max(440, min(vh − 260, 820)) · 쌓기(640 이하 · 확대 아님) = JS가 블록 수 × boxH' }, cases);
}

// ════════ 3. 날짜 · 날짜 줄 · timeAgo ════════
{
  const todayOf = now => evalIn(run(context({}, T(now)), CODE.dates), 'TODAY');
  const today = ['2026-11-02T14:59:59.999Z', '2026-11-02T15:00:00.000Z', '2026-11-03T05:30:00.000Z', '2026-12-31T14:59:59.999Z',
    '2026-12-31T15:00:00.000Z', '2028-02-28T15:00:00.000Z', '2028-02-29T14:59:59.999Z'].map(now => ({ kind: 'today', now, today: todayOf(now) }));
  const d = run(context({}), CODE.dates);
  const fmt = ['2026-11-03', '2026-11-02', '2026-11-01', '2026-01-01', '2026-12-31', '2028-02-29', '2026-03-01', '2027-07-04', '2026-10-07']
    .map(date => ({ kind: 'fmt', date, fmtDate: evalIn(d, `fmtDate('${date}')`), fmtShort: evalIn(d, `fmtShort('${date}')`) }));
  const updated = ['2026-11-03T04:31:27+09:00', '2026-11-03T00:00:00+09:00', '2026-11-03T00:05:59+09:00', '2026-11-03T11:59:00+09:00',
    '2026-11-03T12:00:00+09:00', '2026-11-03T12:30:00+09:00', '2026-11-03T13:07:00+09:00', '2026-11-03T23:59:59+09:00', '2026-11-01T06:00:00+09:00']
    .map(at => ({ kind: 'updated', at, text: evalIn(d, `fmtUpdated('${at}')`) }));
  const add = [['2026-03-01', -1], ['2028-03-01', -1], ['2026-12-31', 1], ['2026-11-03', -1], ['2026-11-02', 1], ['2026-01-31', 1]]
    .map(([date, n]) => ({ kind: 'addDays', date, n, out: evalIn(d, `addDays('${date}', ${n})`) }));

  // renderContext(698~711): 가짜 요소에 쓴 값
  const ctxCase = (now, date, view, rk) => {
    const ids = ['dateText', 'dateInput', 'nextDay', 'todayBtn', 'rankTitle', 'rankDesc', 'updated', 'context'];
    const els = Object.fromEntries(ids.map(id => [id, { textContent: '', value: '', max: '', disabled: false, hidden: false, classList: { toggle() {} } }]));
    const ranking = rk && { date: rk.date, updatedAt: rk.updatedAt, sections: [{ issues: Array(rk.count).fill({}) }] };
    const c = run(context({ document: { getElementById: id => els[id] }, ranking, state: { date, view } }, T(now)), CODE.dates, CODE.renderContext);
    evalIn(c, 'renderContext()');
    return { kind: 'context', now, date, view, ranking: rk, today: evalIn(c, 'TODAY'),
      dateText: els.dateText.textContent, value: els.dateInput.value, max: els.dateInput.max, nextDisabled: els.nextDay.disabled,
      todayHidden: els.todayBtn.hidden, title: els.rankTitle.textContent, desc: els.rankDesc.textContent, updated: els.updated.textContent };
  };
  const rk30 = { date: '2026-11-03', updatedAt: '2026-11-03T04:31:27+09:00', count: 30 };
  const contexts = [
    ctxCase('2026-11-03T03:00:00Z', '2026-11-03', 'map', rk30),
    ctxCase('2026-11-03T03:00:00Z', '2026-11-03', 'list', rk30),
    ctxCase('2026-11-03T03:00:00Z', '2026-11-02', 'map', { date: '2026-11-02', updatedAt: '2026-11-02T04:28:05+09:00', count: 30 }),
    ctxCase('2026-11-03T03:00:00Z', '2026-10-31', 'list', null),
    ctxCase('2026-11-03T03:00:00Z', '2026-11-01', 'bookmarks', { date: '2026-11-01', updatedAt: '2026-11-01T06:00:00+09:00', count: 25 }),
    // KST 자정 경계: 11-02 23:59:59.999 KST에는 11-02가 오늘, 11-03 00:00 KST에는 지난 날
    ctxCase('2026-11-02T14:59:59.999Z', '2026-11-02', 'map', null),
    ctxCase('2026-11-02T15:00:00.000Z', '2026-11-02', 'map', null),
    ctxCase('2026-12-31T15:00:00.000Z', '2026-12-31', 'map', { date: '2026-12-31', updatedAt: '2026-12-31T12:00:00+09:00', count: 1 }),
  ];
  const ago = [['2026-11-03T03:00:00Z', '2026-11-03T03:00:00.000Z'], ['2026-11-03T03:00:00Z', '2026-11-03T02:59:31.000Z'],
    ['2026-11-03T03:00:00Z', '2026-11-03T02:59:30.000Z'], ['2026-11-03T03:00:00Z', '2026-11-03T02:01:00.000Z'],
    ['2026-11-03T03:00:00Z', '2026-11-03T02:00:31.000Z'], ['2026-11-03T03:00:00Z', '2026-11-03T01:31:00.000Z'],
    ['2026-11-03T03:00:00Z', '2026-11-02T03:31:00.000Z'], ['2026-11-03T03:00:00Z', '2026-11-02T03:30:00.000Z'],
    ['2026-11-03T03:00:00Z', '2026-10-31T03:00:00.000Z'], ['2026-11-03T03:00:00Z', '2026-11-03T12:05:00+09:00'],
    ['2026-11-03T03:00:00Z', '2026-11-04T00:00:00.000Z'], ['2026-11-03T03:00:00Z', '2026-11-02T21:40:00+09:00']]
    .map(([now, savedAt]) => ({ kind: 'timeAgo', now, savedAt, text: evalIn(run(context({}, T(now)), CODE.timeAgo), `timeAgo('${savedAt}')`) }));
  write('dates.json', used('const TODAY = ', 'const iso = ', 'const parse = ', 'const addDays = ', 'const WD = ', 'const fmtDate = ',
    'const fmtShort = ', 'const fmtUpdated = ', 'function renderContext()', 'const timeAgo = '), [...today, ...fmt, ...updated, ...add, ...contexts, ...ago]);
}

// ════════ 4. 위젯(widget.js valid · refreshAfter) ════════
{
  const w = run(context({}), CODE.valid);
  const item = { id: '2026-11-03-pol-21', section: 'pol', title: '제목', path: '/?issue=2026-11-03-pol-21' };
  const ok = { date: '2026-11-03', updatedAt: '2026-11-03T04:31:27+09:00', updatedLabel: '11.3 오전 4:31 수집', morePath: '/#list', items: [item] };
  const without = k => { const o = { ...ok }; delete o[k]; return o; };
  const valids = [
    ['ok', ok], ['ok-31-items', { ...ok, items: Array(31).fill(item) }], ['ok-no-morePath', without('morePath')],
    ['ok-bad-morePath', { ...ok, morePath: 'https://evil.example/' }], ['ok-morePath-number', { ...ok, morePath: 7 }],
    ['ok-empty-date', { ...ok, date: '' }], ['ok-item-extra', { ...ok, items: [{ ...item, rank: 1, articleCount: 9 }] }],
    ['ok-no-updatedAt', without('updatedAt')], ['ok-item-only-title-path', { ...ok, items: [{ title: 't', path: '/' }] }],
    ['null', null], ['array', [ok]], ['string', 'x'], ['no-date', without('date')], ['date-number', { ...ok, date: 20261103 }],
    ['no-updatedLabel', without('updatedLabel')], ['updatedLabel-null', { ...ok, updatedLabel: null }],
    ['items-empty', { ...ok, items: [] }], ['items-object', { ...ok, items: { 0: item } }], ['no-items', without('items')],
    ['item-null', { ...ok, items: [item, null] }], ['item-title-number', { ...ok, items: [{ ...item, title: 3 }] }],
    ['item-no-title', { ...ok, items: [{ path: '/' }] }], ['item-path-relative', { ...ok, items: [{ ...item, path: '?issue=x' }] }],
    ['item-path-absolute-url', { ...ok, items: [{ ...item, path: 'https://evil.example/' }] }], ['item-path-null', { ...ok, items: [{ ...item, path: null }] }],
    ['item-array', { ...ok, items: [[]] }],
  ].map(([name, data]) => ({ kind: 'valid', name, data, valid: evalIn(w, `valid(${JSON.stringify(data)})`) }));
  const refresh = [
    ['2026-11-03T03:00:00Z', '2026-11-03'], ['2026-11-03T03:00:00Z', '2026-11-02'], ['2026-11-03T03:00:00Z', null],
    ['2026-11-02T18:00:00Z', '2026-11-02'], ['2026-11-02T19:00:00Z', '2026-11-02'], ['2026-11-02T19:29:59Z', '2026-11-02'],
    ['2026-11-02T19:30:00Z', '2026-11-02'], ['2026-11-02T15:00:00Z', '2026-11-03'], ['2026-11-02T14:59:59.999Z', '2026-11-02'],
    ['2026-11-02T15:00:00Z', '2026-11-02'], ['2026-11-02T19:10:00Z', null],
  ].map(([now, date]) => {
    const c = run(context({}, T(now)), CODE.refreshAfter);
    return { kind: 'refreshAfter', now, date, at: evalIn(c, `refreshAfter(${date === null ? 'null' : JSON.stringify({ date })}).toISOString()`) };
  });
  write('widget.json', used('function valid(', 'function refreshAfter('), [...valids, ...refresh]);
}

// ════════ 5. 문구 · 공유 · 바로가기 ════════
{
  const a = run(context({}), CODE.authMessage, CODE.googleStartMessage);
  const codes = [null, 'PASSWORD_TOO_SHORT', 'PASSWORD_TOO_LONG', 'INVALID_EMAIL', 'INVALID_EMAIL_OR_PASSWORD', 'USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL',
    'INVALID_ORIGIN', 'MISSING_OR_NULL_ORIGIN', 'PROVIDER_NOT_FOUND'];
  const statuses = [null, 400, 401, 403, 404, 422, 429, 500, 502];
  const auth = [];
  for (const code of codes) for (const status of statuses) for (const signingIn of [true, false]) {
    auth.push({ kind: 'authMessage', code, status, signingIn, text: evalIn(a, `authMessage(${JSON.stringify(code ?? undefined) ?? 'undefined'}, ${status ?? 'undefined'}, ${signingIn})`) });
  }
  const start = [];
  for (const code of codes) for (const status of statuses) {
    start.push({ kind: 'googleStartMessage', code, status, text: evalIn(a, `googleStartMessage(${status ?? 'undefined'}, ${JSON.stringify(code ?? undefined) ?? 'undefined'})`) });
  }
  const back = ['access_denied', 'account_not_linked', 'state_mismatch', 'invalid_code', 'unable_to_create_user', ''].map(error => {
    const errEl = { textContent: '' };
    const toasts = [];
    const c = run(context({ state: { loggedIn: false }, openAuth: () => {}, toast: m => toasts.push(m), document: { getElementById: () => errEl } }), CODE.googleReturn);
    evalIn(c, `googleReturn({ google: true, error: ${JSON.stringify(error)} })`);
    return { kind: 'googleReturn', error, text: errEl.textContent };
  });
  const signedIn = (() => {
    const toasts = [];
    const c = run(context({ state: { loggedIn: true }, toast: m => toasts.push(m) }), CODE.googleReturn);
    evalIn(c, 'googleReturn({ google: true, error: null })');
    return { kind: 'googleReturnSignedIn', toast: toasts[0] };
  })();

  const bookmark = { ...SEED.sections[3].issues[0], id: '2026-10-30-wor-3', section: 'wor', title: '랭킹에 없는 북마크 카드 [괄호] & 기호', folderId: 'f1', savedAt: '2026-10-30T00:00:00.000Z' };
  const s = run(context({ ranking: SEED, findBookmark: id => (id === bookmark.id ? bookmark : null) }), CODE.sections, CODE.issueById, CODE.share);
  const share = ['2026-11-03-pol-21', '2026-11-03-eco-20', '2026-11-02-cul-5', '2026-11-03-it-5', bookmark.id, '2026-11-03-none-1']
    .map(id => ({ kind: 'issueShare', id, data: evalIn(s, `issueShare(${JSON.stringify(id)})`) }));
  const site = { kind: 'siteShare', data: evalIn(s, 'SITE_SHARE') };

  const shortcut = ['/?issue=2026-11-03-pol-21', '/?issue=2026-11-02-cul-5#list', '/?issue=2026-11-04-pol-1', '/?issue=2026-02-30-pol-1',
    '/?issue=2026-11-03-xyz-1', '/?issue=2026-11-03-POL-1', '/?issue=2026-11-03-pol-', '/?issue=2026-11-03-pol-1x', '/?issue=',
    '/?issue=0999-01-01-pol-1', '/?issue=0099-01-01-pol-1', '/?issue=1000-01-01-pol-1', '/?issue=2026-11-03%2Dpol%2D21',
    '/?issue=a&issue=2026-11-03-pol-21', '/?issue=2026-11-03-pol-21&login=google', '/?login=google', '/?error=access_denied',
    '/', '/#list', '/#bookmarks', '/#map', '/#nope', '/?x=1#list', '/#list?issue=2026-11-03-pol-21', '/?issue=2026+11', '/?issue=%E0%A4%A']
    .map(path => {
      const loc = new URL(path, 'https://issuerank-kappa.vercel.app');
      const c = run(context({ location: { search: loc.search, pathname: loc.pathname, hash: loc.hash }, history: { replaceState() {} } }), CODE.sections, CODE.dates, CODE.issueShortcut, CODE.nav);
      return { kind: 'shortcut', path, today: evalIn(c, 'TODAY'), back: evalIn(c, 'issueShortcut()'), view: evalIn(c, 'viewFromHash()') };
    });
  write('text.json', used('function authMessage(', 'function googleStartMessage(', 'function googleReturn(', 'const SHARE_BASE = ', 'const SITE_SHARE = ',
    'function issueShare(', 'function issueShortcut()', 'const ORDER = ', 'const viewFromHash = '), [...auth, ...start, ...back, signedIn, ...share, site, ...shortcut]);
}
