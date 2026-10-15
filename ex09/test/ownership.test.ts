import { describe, expect, it, vi } from 'vitest';
import { betterAuth } from 'better-auth';
import { memoryAdapter, type MemoryDB } from 'better-auth/adapters/memory';
import { authOptions } from '../server/auth';
import { createApp } from '../server/app';
import { memoryStore } from '../server/sample';
const origin='http://localhost:3000';
const password='Fixture-password-987!';
function setup() {
  const db:MemoryDB={user:[],session:[],account:[],verification:[]};
  const auth=betterAuth({...authOptions({BETTER_AUTH_SECRET:'only-fixture-auth-secret-1234567890abcdef',BETTER_AUTH_URL:origin}),database:memoryAdapter(db)});
  const store={...memoryStore,bookmarks:vi.fn(memoryStore.bookmarks),folders:vi.fn(memoryStore.folders),saveBookmark:vi.fn(memoryStore.saveBookmark),deleteBookmark:vi.fn(memoryStore.deleteBookmark),createFolder:vi.fn(memoryStore.createFolder),issue:vi.fn(memoryStore.issue)};
  const app=createApp(store,auth);
  const request=(path:string,method='GET',body?:unknown,cookie?:string,extra:Record<string,string>={})=>app.request(origin+path,{method,headers:{Origin:origin,...(cookie?{Cookie:cookie}:{}),...(body===undefined?{}:{'Content-Type':'application/json'}),...extra},body:body===undefined?undefined:typeof body==='string'?body:JSON.stringify(body)});
  const signup=async(email:string)=>{const res=await request('/api/auth/sign-up/email','POST',{name:'fixture',email,password});expect(res.status).toBe(200);return {id:(await res.json()).user.id,cookie:res.headers.getSetCookie().map(c=>c.split(';')[0]).join('; ')};};
  return {db,store,app,request,signup};
}
const paths=[['/api/bookmarks','GET'],['/api/bookmarks','POST'],['/api/bookmarks/2026-10-07-pol-1','DELETE'],['/api/folders','GET'],['/api/folders','POST']];

describe('계정별 북마크·폴더 세션과 소유권',()=>{
 it('쿠키 없음·변조·만료는 잘못된 본문 검사 전에 개인5경로401, 저장소 접근0',async()=>{
  const {db,store,request,signup}=setup();const a=await signup('expired@example.invalid');db.session[0].expiresAt=new Date('2000-01-01');
  for(const cookie of [undefined,a.cookie+'wrong',a.cookie]) for(const [path,method] of paths){
   const res=await request(path+'?userId=fixture-user',method,method==='POST'?'{':undefined,cookie,{'X-User-Id':'fixture-user'});
   expect(res.status).toBe(401);expect(res.headers.get('cache-control')).toBe('no-store');expect(await res.json()).toEqual({message:'로그인이 필요합니다'});
  }
  for(const key of ['bookmarks','folders','saveBookmark','deleteBookmark','createFolder','issue'] as const)expect(store[key]).not.toHaveBeenCalled();
  expect((await request('/api/rankings')).status).toBe(200);
 });
 it('인증 제공자가 없는 앱도 개인 요청을 기본사용자로 우회하지 않는다',async()=>{
  const app=createApp(memoryStore);expect((await app.request('/api/bookmarks')).status).toBe(401);expect((await app.request('/api/rankings')).status).toBe(200);
 });
 it('A/B 빈 목록·같은 이슈 독립저장·타인폴더 및 위조 userId 거절·삭제 분리',async()=>{
  const {request,signup}=setup();const a=await signup('a@example.invalid'),b=await signup('b@example.invalid');
  for(const u of [a,b])for(const path of ['/api/bookmarks','/api/folders'])expect(await (await request(path,'GET',undefined,u.cookie)).json()).toEqual([]);
  const fa=await (await request('/api/folders','POST',{name:'A',userId:b.id},a.cookie)).json();
  const fb=await (await request('/api/folders','POST',{name:'B'},b.cookie)).json();
  const issue=(await memoryStore.ranking('2026-10-07'))!.sections[0].issues[0];
  for(const [u,f] of [[a,fa],[b,fb]])expect((await request('/api/bookmarks','POST',{issueId:issue.id,folderId:f.id,userId:b.id},u.cookie)).status).toBe(201);
  expect((await request('/api/bookmarks','POST',{issueId:issue.id,folderId:fb.id},a.cookie)).status).toBe(404);
  expect((await request('/api/bookmarks','POST',{issueId:issue.id,folderId:fa.id},a.cookie)).status).toBe(409);
  const aList=await (await request('/api/bookmarks?userId='+b.id,'GET',undefined,a.cookie,{'X-User-Id':b.id})).json();
  expect(aList).toHaveLength(1);expect(aList[0].folderId).toBe(fa.id);expect(aList[0]).not.toHaveProperty('userId');
  expect((await (await request('/api/folders','GET',undefined,a.cookie)).json())[0]).toEqual({...fa,count:1});
  expect((await request('/api/bookmarks/'+issue.id,'DELETE',undefined,a.cookie)).status).toBe(204);
  expect((await request('/api/bookmarks/'+issue.id,'DELETE',undefined,a.cookie)).status).toBe(404);
  expect(await (await request('/api/bookmarks','GET',undefined,a.cookie)).json()).toEqual([]);
  expect(await (await request('/api/bookmarks','GET',undefined,b.cookie)).json()).toHaveLength(1);
 });
 it('같은 계정 동시저장201/409, 동시폴더UUID 고유, 시각동률은 마지막저장 우선',async()=>{
  const {request,signup}=setup();const a=await signup('race@example.invalid');
  const folders=await Promise.all([request('/api/folders','POST',{name:'동일'},a.cookie),request('/api/folders','POST',{name:'동일'},a.cookie)]);
  expect(folders.map(f=>f.status)).toEqual([201,201]);const [one,two]=await Promise.all(folders.map(f=>f.json()));expect(one.id).not.toBe(two.id);
  const [first,second]=(await memoryStore.ranking('2026-10-07'))!.sections[0].issues;
  expect((await Promise.all([request('/api/bookmarks','POST',{issueId:first.id,folderId:one.id},a.cookie),request('/api/bookmarks','POST',{issueId:first.id,folderId:two.id},a.cookie)])).map(r=>r.status).sort()).toEqual([201,409]);
  expect((await request('/api/bookmarks','POST',{issueId:second.id,folderId:two.id},a.cookie)).status).toBe(201);
  const list=await (await request('/api/bookmarks','GET',undefined,a.cookie)).json();expect(list.map((b:{id:string})=>b.id)).toEqual([second.id,first.id]);expect(list[0].savedAt).toBe(list[1].savedAt);
 });
 it('지난 날짜 카드도 계정 소유 폴더에 저장하고 카드 날짜를 유지한다',async()=>{
  const {request,signup}=setup();const a=await signup('past@example.invalid');const f=await (await request('/api/folders','POST',{name:'이전'},a.cookie)).json();
  const issue=(await memoryStore.ranking('2026-10-06'))!.sections[0].issues[0];const res=await request('/api/bookmarks','POST',{issueId:issue.id,folderId:f.id},a.cookie);
  expect(res.status).toBe(201);expect((await res.json()).date).toBe('2026-10-06');
 });
});
