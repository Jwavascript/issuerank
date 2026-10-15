import { readFileSync } from 'node:fs';
import { createContext, runInContext } from 'node:vm';
import { expect, it, vi } from 'vitest';
const html=readFileSync('public/index.html','utf8');
const between=(start:string,end:string)=>html.slice(html.indexOf(start),html.indexOf(end,html.indexOf(start)));
function setup(){
 const state={loggedIn:true,user:{id:'A'},authVersion:1,bookmarks:[{id:'issue',folderId:'A-folder'}],folders:[{id:'A-folder',name:'A'}],folderFilter:'A-folder'};
 const toast=vi.fn(),sync=vi.fn(),fetchMock=vi.fn();
 const context=createContext({state,fetch:fetchMock,toast,Response,Error,JSON,Promise,openAuth:vi.fn(),closePop:vi.fn(),closeDrawer:vi.fn(),renderUser:vi.fn(),syncBookmarkUI:sync,renderBookmarks:vi.fn(),go:vi.fn(),folderName:()=> 'A',findBookmark:(id:string)=>state.bookmarks.find(b=>b.id===id)});
 runInContext(between('async function api(path','/* ─────────────── Date')+between('async function loadBookmarks()','const allBookmarks')+between('const postBookmark =','function syncBookmarkUI()')+between('async function applyUser(user)','async function restoreSession()'),context);
 const deferred=()=>{let resolve!:(r:Response)=>void;const promise=new Promise<Response>(r=>resolve=r);return {promise,resolve};};
 const switchToB=()=>{state.authVersion++;state.user={id:'B'};state.bookmarks=[];state.folders=[];state.folderFilter='all';};
 return {context,state,toast,sync,fetchMock,deferred,switchToB};
}
it('A의 늦은 목록과401은 B의 데이터나 로그인 상태를 덮지 않는다',async()=>{
 for(const status of [200,401]){
  const {context,state,fetchMock,deferred,switchToB,toast}=setup();const a=deferred(),b=deferred();fetchMock.mockReturnValueOnce(a.promise).mockReturnValueOnce(b.promise);
  const task=runInContext('loadBookmarks()',context);switchToB();a.resolve(Response.json(status===401?{message:'로그인이 필요합니다'}:[{id:'A'}],{status}));b.resolve(Response.json([{id:'A-folder'}]));await task;
  expect(state.user.id).toBe('B');expect(state.loggedIn).toBe(true);expect(state.bookmarks).toEqual([]);expect(state.folders).toEqual([]);expect(toast).not.toHaveBeenCalled();
 }
});
it('현재 세션401이면 목록·폴더필터를 비우고 로그인 안내를 보여준다',async()=>{
 const {context,state,fetchMock,toast}=setup();fetchMock.mockResolvedValue(Response.json({message:'로그인이 필요합니다'},{status:401}));
 await runInContext('loadBookmarks()',context);expect(state.loggedIn).toBe(false);expect(state.user).toBeNull();expect(state.bookmarks).toEqual([]);expect(state.folderFilter).toBe('all');expect(toast).toHaveBeenCalledWith('로그인이 필요합니다','로그인',expect.any(Function));
});
it('계정 전환 뒤 늦은 저장·해제 응답은 목록 재조회나 알림을 만들지 않는다',async()=>{
 for(const expression of ["save('issue','A-folder')","unsave('issue')"]){
  const {context,fetchMock,deferred,switchToB,toast,sync}=setup();const d=deferred();fetchMock.mockReturnValue(d.promise);const task=runInContext(expression,context);switchToB();d.resolve(Response.json({}));await task;
  expect(fetchMock).toHaveBeenCalledTimes(1);expect(toast).not.toHaveBeenCalled();expect(sync).not.toHaveBeenCalled();
 }
});
it('A의 폴더생성이 늦게 끝나도 B로 후속 북마크 저장을 하지 않는다',async()=>{
 const {context,fetchMock,deferred,switchToB}=setup();const d=deferred();fetchMock.mockReturnValue(d.promise);const task=runInContext("createFolderAndSave('A','issue')",context);switchToB();d.resolve(Response.json({id:'A-new'}));await task;expect(fetchMock).toHaveBeenCalledTimes(1);
});
it('A의 되돌리기를 B에서 실행해도 네트워크 쓰기를 하지 않는다',async()=>{
 const {context,fetchMock,switchToB,toast}=setup();fetchMock.mockResolvedValueOnce(new Response(null,{status:204})).mockResolvedValueOnce(Response.json([])).mockResolvedValueOnce(Response.json([]));
 await runInContext("unsave('issue')",context);const undo=toast.mock.calls.find(c=>c[1]==='되돌리기')![2];switchToB();await undo();expect(fetchMock).toHaveBeenCalledTimes(3);
});

it('다른 날짜 카드도 전역 최근 저장 순서와 동률 입력 순서를 유지한다',()=>{
 const entries=[{id:'old-date-new-save',date:'2026-10-01',savedAt:'2026-10-07T04:00:00Z',folderId:'f'}, {id:'today',date:'2026-10-07',savedAt:'2026-10-07T03:00:00Z',folderId:'f'}, {id:'old-date-older-save',date:'2026-10-01',savedAt:'2026-10-07T03:00:00Z',folderId:'f'}];
 const nodes=Object.fromEntries(['bmBadge','folderChips','bmList','bmSub'].map(id=>[id,{textContent:'',innerHTML:''}]));
 const context=createContext({state:{loggedIn:true,folderFilter:'all',folders:[{id:'f',name:'F',count:3}]},document:{getElementById:(id:string)=>nodes[id]},allBookmarks:()=>entries,esc:(s:string)=>s,fmtDate:(s:string)=>s,itemHTML:(item:{id:string})=>`<li data-id="${item.id}"></li>`});
 runInContext(between('function renderBookmarks()','/* ─────────────── Tooltip'),context);runInContext('renderBookmarks()',context);
 const html=nodes.bmList.innerHTML;expect([...html.matchAll(/data-id="([^"]+)"/g)].map(m=>m[1])).toEqual(entries.map(e=>e.id));expect((html.match(/class="group-head"/g)||[]).length).toBe(3);
});
