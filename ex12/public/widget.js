// 이슈랭크 홈 화면 위젯 v1 (2026-10-17) · iPhone Scriptable 스크립트
// 설치 안내: https://issuerank-kappa.vercel.app/widget — 새 버전이 나오면 같은 방법으로 내용을 바꿔 붙여넣습니다.
// 헤드라인 순서와 링크는 서버(/api/widget)가 정하고, 이 스크립트는 받은 목록을 크기만큼 그리기만 합니다.
const BASE = 'https://issuerank-kappa.vercel.app';
const CACHE_FILE = 'issuerank-widget.json';

// 크기별 줄 수 · 제목 글자 크기(semibold) · 제목당 줄 · 줄 간격. accessory*는 small 규칙, 앱에서 실행하면(null) medium 미리보기
const SIZES = {
  small: { rows: 3, font: 13, lines: 2, gap: 4 },
  medium: { rows: 5, font: 13, lines: 1, gap: 3 },
  large: { rows: 10, font: 14, lines: 1, gap: 6 },
};
const family = config.widgetFamily;
const size = family === null || family === undefined || family === 'medium' ? 'medium'
  : family === 'large' || family === 'extraLarge' ? 'large' : 'small';
const rule = SIZES[size];

// 사이트 색 토큰(밝게, 어둡게)
function dynamicColor(light, dark) {
  return Color.dynamic(new Color(light), new Color(dark));
}

// 모양 검사: date · updatedLabel 문자열, items 1개 이상, 항목마다 title 문자열 · path는 /로 시작하는 문자열
function valid(data) {
  return !!data && typeof data.date === 'string' && typeof data.updatedLabel === 'string'
    && Array.isArray(data.items) && data.items.length > 0
    && data.items.every(item => !!item && typeof item.title === 'string'
      && typeof item.path === 'string' && item.path.startsWith('/'));
}

// 요청 하나(10초). 성공(200 · 모양 맞음)일 때만 받은 JSON을 라이브러리 폴더에 덮어씀(캐시 폴더는 시스템이 지울 수 있음)
async function load(fm, path) {
  try {
    const req = new Request(BASE + '/api/widget');
    req.timeoutInterval = 10;
    const text = await req.loadString();
    if (req.response.statusCode !== 200) return null;
    const data = JSON.parse(text);
    if (!valid(data)) return null;
    try { fm.writeString(path, text); } catch (e) {}
    return data;
  } catch (e) {
    return null;
  }
}

// 실패하면 마지막으로 받은 내용. 없거나 깨졌으면 null
function cached(fm, path) {
  try {
    if (!fm.fileExists(path)) return null;
    const data = JSON.parse(fm.readString(path));
    return valid(data) ? data : null;
  } catch (e) {
    return null;
  }
}

// 갱신 힌트(보장 아님, iOS가 정함). KST = 기기 시간대와 무관하게 UTC + 9시간
// 보여 주는 date가 KST 오늘이면 3시간 뒤, 아니면 KST 오늘 04:30과 30분 뒤 중 늦은 때(실패 안내 화면도)
function refreshAfter(data) {
  const now = Date.now();
  const today = new Date(now + 9 * 3600e3).toISOString().slice(0, 10);
  if (data && data.date === today) return new Date(now + 3 * 3600e3);
  return new Date(Math.max(Date.parse(today + 'T04:30:00+09:00'), now + 30 * 60e3));
}

let data = null;
try {
  const fm = FileManager.local();
  const path = fm.joinPath(fm.libraryDirectory(), CACHE_FILE);
  data = (await load(fm, path)) || cached(fm, path);
} catch (e) {
  data = null;
}

const widget = new ListWidget();
widget.backgroundColor = dynamicColor('#ffffff', '#1a1a19');
widget.spacing = rule.gap;
const titleFont = Font.semiboldSystemFont(rule.font);
const titleColor = dynamicColor('#161616', '#ecebe8');
if (data) {
  // 글은 제목과 하단뿐. 줄 제한을 넘는 제목은 iOS가 끝을 줄임
  for (const item of data.items.slice(0, rule.rows)) {
    const title = widget.addText(item.title);
    title.font = titleFont;
    title.textColor = titleColor;
    title.lineLimit = rule.lines;
    // 작은 위젯은 누를 곳이 위젯 하나뿐(줄 url은 medium · large만)
    if (size !== 'small') title.url = BASE + item.path;
  }
  widget.addSpacer(null);
  const foot = widget.addText(data.updatedLabel);
  foot.font = Font.regularSystemFont(10);
  foot.textColor = dynamicColor('#6f6b65', '#9b978f');
  widget.url = BASE + (typeof data.morePath === 'string' && data.morePath.startsWith('/') ? data.morePath : '/');
} else {
  const fail = widget.addText('불러오지 못했습니다');
  fail.font = titleFont;
  fail.textColor = titleColor;
  widget.url = BASE + '/';
}
widget.refreshAfterDate = refreshAfter(data);

if (config.runsInWidget) Script.setWidget(widget);
else await widget.presentMedium();
Script.complete();
