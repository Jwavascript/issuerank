/* 관심 키워드(O14). 개인 목록·검색 결과는 메모리에만 보관한다. */
(() => {
  const root = document.getElementById('view-keywords');
  if (!root) return;
  const escape = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const norm = s => s.normalize('NFKC').toLocaleLowerCase('ko').replace(/\s+/g, ' ').trim();
  const colors = new Map();
  let nextColor = 5;
  let terms = [], saved = [], items = [], cursor = null, request = 0, busy = false, personal = 0, device = null, currentUser = null;
  const color = name => {
    const key = norm(name);
    const stored = saved.find(k => norm(k.name) === key);
    if (stored) return stored.color;
    if (!colors.has(key)) colors.set(key, nextColor++);
    return colors.get(key);
  };
  const colorStyle = name => color(name) < 8 ? '' : ` style="--kw-color:hsl(${(color(name) * 137.508) % 360} 55% 48%)"`;
  const chip = name => `<span class="kw-chip kw-color-${color(name)}"${colorStyle(name)}>${escape(name)}</span>`;
  root.innerHTML = `<div class="wrap kw-page">
    <h1>관심 키워드 타임라인</h1><p class="kw-note">관심 있는 주제의 주요 보도를 시간순으로 확인하세요.</p>
    <div class="kw-panel"><form class="kw-search" id="kwSearch"><label class="sr-only" for="kwInput">키워드</label><input id="kwInput" name="keyword" minlength="2" maxlength="40" placeholder="기업·인물·주제 검색" autocomplete="off" required><button class="kw-primary" type="submit">키워드 추가</button></form>
    <div class="kw-chips" id="kwSelected" aria-label="선택한 키워드"></div>
    <p class="kw-note">최대 3개를 함께 봅니다. 키워드를 누르면 비교에서 빠지며 관심 저장은 유지됩니다.</p>
    <details class="kw-filters"><summary id="kwFilterLabel">최근 30일 · 주요 보도 · 최신순 설정</summary><div class="kw-options"><label>기간 <select id="kwPeriod"><option value="7">최근 7일</option><option value="30" selected>최근 30일</option><option value="90">최근 90일</option><option value="custom">직접 선택</option></select></label>
    <label>시작 <input type="date" id="kwFrom" max="${TODAY}" value="${addDays(TODAY, -29)}"></label><label>끝 <input type="date" id="kwTo" max="${TODAY}" value="${TODAY}"></label>
    <label>순서 <select id="kwOrder"><option value="desc">최신순</option><option value="asc">오래된 순</option></select></label>
    <label>보기 <select id="kwMode"><option value="major">주요 보도</option><option value="all">관련 보도 전체</option></select></label></div></details>
    <div class="kw-actions"><button class="chip-btn" type="button" id="kwRun">검색</button><button class="chip-btn" type="button" id="kwSave">선택한 키워드 저장</button></div>
    <p class="kw-note">이슈랭크가 수집한 뉴스에서 검색합니다. 보도일 기준이며 실제 사건 발생일과 다를 수 있습니다. 뉴스 제공에는 지연이 있습니다.</p></div>
    <details class="kw-panel" id="kwSettings"><summary>관심 키워드 · 알림 설정</summary><div id="kwAccount"></div><div id="kwSaved"></div>
    <p class="kw-note">저장 최대 5개 · 알림을 켠 뒤 새로 수집한 주요 보도를 아침에 한 번 모아 보냅니다. 의미가 모호한 사건은 별도로 표시합니다.</p>
    <div id="kwDevice"><label><input type="checkbox" id="kwMorning" checked> 일반 아침 뉴스도 함께 받기</label><div class="kw-actions"><button class="chip-btn" id="kwConnect" type="button">이 기기에 키워드 알림 연결</button><button class="chip-btn" id="kwDisconnect" type="button">키워드 알림 연결 해제</button></div><p class="kw-note" id="kwDeviceStatus"></p></div></details>
    <p class="kw-note" id="kwCoverage"></p><div class="kw-status" id="kwStatus" role="status" aria-live="polite"></div>
    <div class="kw-timeline" id="kwResults"></div><button class="kw-more" id="kwMore" type="button" hidden>이전 보도 더 보기</button>
    <p class="kw-note">요약은 AI가 생성했습니다. 출처와 원문을 함께 확인해 주세요. 같은 URL 또는 보도일·제목·설명문이 동일한 보도만 수집 날짜를 넘어 묶습니다.</p></div>`;
  const $ = id => document.getElementById(id);
  const status = (text, error = false) => { $('kwStatus').textContent = text; $('kwStatus').classList.toggle('kw-error', error); };
  function selected() {
    $('kwSelected').innerHTML = terms.map(name => `<button type="button" class="kw-chip kw-color-${color(name)}"${colorStyle(name)} data-kw-remove="${escape(name)}" aria-label="${escape(name)} 비교에서 빼기">${escape(name)} ×</button>`).join('');
    $('kwSaved').innerHTML = saved.map(k => `<div class="kw-saved"><button type="button" class="kw-chip kw-color-${k.color}" data-kw-select="${escape(k.name)}" aria-pressed="${terms.some(t => norm(t) === norm(k.name))}">${escape(k.name)}</button><label><input type="checkbox" data-kw-alert="${k.id}" ${k.alerts ? 'checked' : ''}>아침 알림</label><button type="button" class="kw-danger" data-kw-delete="${k.id}">삭제</button></div>`).join('');
    $('kwAccount').innerHTML = state.loggedIn ? `<p class="kw-note">${escape(state.user.name)}의 관심 키워드 <button type="button" class="chip-btn" id="kwLogout">로그아웃</button></p>` : '<p class="kw-note">관심 키워드를 저장하려면 <button type="button" class="chip-btn" data-login>로그인</button>해 주세요.</p>';
    $('kwDevice').hidden = !state.loggedIn;
  }
  function add(name) {
    name = name.trim();
    if (name.length < 2 || name.length > 40 || /[\p{Cc}\p{Cf}]/u.test(name)) return status('키워드는 2~40자로 입력해 주세요', true);
    if (terms.some(t => norm(t) === norm(name))) return status('이미 선택한 키워드입니다');
    if (terms.length === 3) return status('키워드는 최대 3개까지 함께 볼 수 있습니다', true);
    color(name); terms.push(name); selected(); void search();
  }
  function render() {
    $('kwResults').innerHTML = items.map(item => `<article class="kw-event"><time datetime="${escape(item.publishedAt)}">${escape(item.date)} 보도</time><div class="kw-chips">${item.keywords.map(chip).join('')}</div><h2>${escape(item.title)}</h2><span class="kw-reason">${item.reasons.length ? item.reasons.map(escape).join(' · ') : '관련 보도'}</span>
      ${item.summary?.length ? item.summary.map(s => `<p>${escape(s)}</p>`).join('') : '<p class="kw-note">요약 없음 · 원문을 확인해 주세요</p>'}
      <details><summary>출처 · 원문 ${item.sources.length}건</summary><ul>${item.sources.filter(s => { try { return /^https?:$/.test(new URL(s.url).protocol); } catch { return false; } }).map(s => `<li><a href="${escape(s.url)}" target="_blank" rel="noopener noreferrer">${escape(s.name)} · ${escape(s.title)}</a></li>`).join('')}</ul></details></article>`).join('');
    $('kwMore').hidden = !cursor; $('kwMore').textContent = $('kwOrder').value === 'asc' ? '다음 보도 더 보기' : '이전 보도 더 보기';
  }
  async function search(more = false) {
    const seq = ++request;
    $('kwFilterLabel').textContent = `${$('kwFrom').value} ~ ${$('kwTo').value} · ${$('kwMode').selectedOptions[0].textContent} · ${$('kwOrder').selectedOptions[0].textContent} 설정`;
    if (!more) { items = []; cursor = null; render(); $('kwCoverage').textContent = ''; }
    if (!terms.length) { status('검색할 키워드를 추가해 주세요'); return; }
    const query = new URLSearchParams({ from: $('kwFrom').value, to: $('kwTo').value, order: $('kwOrder').value, mode: $('kwMode').value });
    for (const term of terms) query.append('q', term);
    if (more && cursor) query.set('cursor', cursor);
    $('kwMore').disabled = true; status('뉴스를 찾고 있습니다…');
    try {
      const data = await api('/timeline?' + query);
      if (seq !== request) return;
      items = more ? [...new Map([...items, ...data.items].map(e => [e.id, e])).values()] : data.items;
      cursor = data.nextCursor; render();
      $('kwCoverage').textContent = data.firstDate ? `저장된 보도 ${data.firstDate} ~ ${data.lastDate}${data.updatedAt ? ' · 마지막 갱신 ' + new Date(data.updatedAt).toLocaleString('ko-KR') : ''}` : '아직 저장된 뉴스가 없습니다';
      let message = data.total ? `주요 기준에 따른 ${data.total}건 중 ${items.length}건 표시` : data.relatedCount ? '관련 보도는 있지만 주요 보도 기준에 맞는 항목은 없습니다. 관련 보도 전체로 바꿔 보세요.' : '수집한 뉴스에서 일치하는 결과가 없습니다.';
      if (data.total && $('kwMode').value === 'all') message = `관련 보도 ${data.total}건 중 ${items.length}건 표시`;
      if (data.firstDate && $('kwFrom').value < data.firstDate) message += ' 저장 시작일 이전의 뉴스는 포함되지 않습니다.';
      if (data.failedCollections.length) message += ` 기간 주변 수집 실패(${data.failedCollections.join(', ')})로 일부 뉴스가 빠질 수 있습니다.`;
      status(message);
    } catch (e) { if (seq === request) { status((e.message || '뉴스를 불러오지 못했습니다') + ' 검색 버튼으로 다시 시도해 주세요.', true); } }
    finally { if (seq === request) $('kwMore').disabled = false; }
  }
  async function subscription() {
    if (!('serviceWorker' in navigator)) return null;
    const reg = await navigator.serviceWorker.getRegistration('/');
    return reg ? reg.pushManager.getSubscription() : null;
  }
  async function refreshUser() {
    const version = ++personal;
    const nextUser = state.user?.id ?? null;
    if (currentUser && currentUser !== nextUser) { terms = []; items = []; cursor = null; request++; render(); status('검색할 키워드를 추가해 주세요'); }
    currentUser = nextUser;
    saved = []; device = null; selected(); $('kwDeviceStatus').textContent = '';
    if (!state.loggedIn) return;
    try {
      const list = await api('/keywords');
      if (version !== personal) return;
      saved = list; selected(); render();
      const sub = await subscription();
      if (sub) {
        const d = await api('/keywords/device/status', { method: 'POST', body: JSON.stringify({ endpoint: sub.endpoint }) });
        if (version !== personal) return;
        device = d;
        $('kwMorning').checked = d?.morning ?? true;
        $('kwDeviceStatus').textContent = d?.active ? '이 로그인에서 키워드 알림을 받습니다. 로그아웃·세션 만료 시 개인 알림이 중단됩니다.' : '이 기기에 연결하면 아침에 키워드 알림을 받습니다. 먼저 종 아이콘으로 아침 알림을 켜 주세요.';
      } else $('kwDeviceStatus').textContent = '먼저 화면 위 종 아이콘으로 아침 알림을 켜 주세요.';
      // 알림 링크는 개인 검색어를 URL에 담지 않고 현재 계정에서 복원한다.
      if (new URLSearchParams(location.search).get('timeline') === '1' && !terms.length && saved.length) {
        const link = new URLSearchParams(location.search), focus = link.get('focus')?.split(',') ?? [];
        terms = saved.filter(k => k.alerts && (!focus.length || focus.includes(k.id))).slice(0, 3).map(k => k.name);
        if (link.get('from')) $('kwFrom').value = link.get('from');
        if (link.get('to')) $('kwTo').value = link.get('to');
        $('kwPeriod').value = 'custom'; selected(); void search();
      }
    } catch (e) { if (version === personal && !e.stale) status(e.message || '관심 키워드를 불러오지 못했습니다', true); }
  }
  async function mutate(task) {
    if (busy) return;
    if (!state.loggedIn) return openAuth();
    const version = state.authVersion;
    busy = true;
    try { await task(); if (version === state.authVersion) await refreshUser(); }
    catch (e) { if (!e.stale && !e.handled && version === state.authVersion) status(e.message || '설정을 저장하지 못했습니다', true); }
    finally { busy = false; }
  }
  $('kwSearch').addEventListener('submit', e => { e.preventDefault(); add($('kwInput').value); $('kwInput').value = ''; });
  root.addEventListener('click', e => {
    const b = e.target.closest('button'); if (!b) return;
    if (b.dataset.kwRemove) { terms = terms.filter(t => norm(t) !== norm(b.dataset.kwRemove)); selected(); void search(); }
    else if (b.dataset.kwSelect) {
      const term = b.dataset.kwSelect;
      if (terms.some(t => norm(t) === norm(term))) { terms = terms.filter(t => norm(t) !== norm(term)); selected(); void search(); } else add(term);
    } else if (b.dataset.kwDelete) void mutate(async () => { await api('/keywords/' + b.dataset.kwDelete, { method: 'DELETE' }); status('관심 키워드를 삭제했습니다'); });
    else if (b.id === 'kwRun') void search();
    else if (b.id === 'kwMore') void search(true);
    else if (b.id === 'kwLogout') void signOut();
    else if (b.id === 'kwSave') void mutate(async () => {
      const pending = terms.filter(t => !saved.some(k => norm(k.name) === norm(t)));
      if (!pending.length) return status(terms.length ? '선택한 키워드는 이미 저장되어 있습니다' : '검색할 키워드를 먼저 추가해 주세요');
      if (saved.length + pending.length > 5) return status('관심 키워드는 최대 5개까지 저장할 수 있습니다', true);
      for (const name of pending) await api('/keywords', { method: 'POST', body: JSON.stringify({ name }) });
      status('관심 키워드를 저장했습니다. 설정에서 아침 알림을 켤 수 있습니다.');
    });
    else if (b.id === 'kwConnect' || b.id === 'kwDisconnect') void mutate(async () => {
      const sub = await subscription();
      if (!sub) return status('먼저 화면 위 종 아이콘으로 아침 알림을 켜 주세요', true);
      await api('/keywords/device', { method: b.id === 'kwConnect' ? 'POST' : 'DELETE', body: JSON.stringify({ endpoint: sub.endpoint, auth: sub.toJSON().keys?.auth, morning: $('kwMorning').checked }) });
      status(b.id === 'kwConnect' ? '이 기기에 키워드 알림을 연결했습니다' : '키워드 알림 연결을 해제했습니다. 일반 아침 알림은 종 아이콘으로 관리하세요.');
    });
  });
  root.addEventListener('change', e => {
    if (e.target.dataset.kwAlert) { const alerts = e.target.checked; void mutate(async () => { await api('/keywords/' + e.target.dataset.kwAlert, { method: 'PATCH', body: JSON.stringify({ alerts }) }); status(alerts ? '새로 수집한 주요 보도부터 알립니다. 이 기기 연결 상태도 확인해 주세요.' : '키워드 알림을 껐습니다'); }); }
    else if (e.target.id === 'kwPeriod') { if (e.target.value !== 'custom') { $('kwTo').value = TODAY; $('kwFrom').value = addDays(TODAY, 1 - Number(e.target.value)); void search(); } }
    else if (['kwFrom', 'kwTo'].includes(e.target.id)) $('kwPeriod').value = 'custom';
    else if (['kwOrder', 'kwMode'].includes(e.target.id)) void search();
  });
  window.keywordUI = { refreshUser, entered: () => { selected(); if (!terms.length) status('검색할 키워드를 추가해 주세요'); } };
  selected(); void refreshUser();
})();
