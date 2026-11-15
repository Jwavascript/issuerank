import { describe, expect, it } from 'vitest';
import { keywordNotification, type SavedKeyword } from '../server/keyword-store';
import { timelineEvents } from '../server/timeline';
import { article, snapshot } from './helpers/keyword-fixture';
const day = '2026-10-07';
const keyword = (over: Partial<SavedKeyword> = {}): SavedKeyword => ({ id: 'one', name: '삼성전자', color: 0, alerts: true, baseline: [], createdAt: '2026-10-06T09:00:00Z', enabledAt: '2026-10-06T09:00:00Z', ...over });
describe('키워드 아침 알림 선택', () => {
  it('등록 전 결과 baseline과 동일 기사의 재수집은 발송하지 않는다', () => {
    const data = snapshot([article(1)]);
    const baseline = timelineEvents(data, { terms: ['삼성전자'], from: '2026-10-01', to: day, mode: 'major', order: 'desc' }).flatMap(e => e.fingerprints);
    expect(keywordNotification([keyword({ baseline })], data, [], false, true, day).payload).toBeNull();
  });
  it('새 주요 보도만 한 통으로 묶고 키워드와 검색어는 잠금화면/URL에 담지 않는다', () => {
    const data = snapshot([article(1), article(2, { title: '삼성전자 SK하이닉스 공동 발표' })]);
    const result = keywordNotification([keyword(), keyword({ id: 'two', name: 'SK하이닉스' })], data, [], true, true, day);
    expect(result.payload?.body).toBe('주요 소식 2건 · 아침 뉴스도 준비됐어요');
    expect(JSON.stringify(result.payload)).not.toContain('삼성전자'); expect(JSON.stringify(result.payload)).not.toContain('SK하이닉스');
    const target = new URL(result.payload!.url, 'https://issuerank.example');
    expect(target.hash).toBe('#keywords'); expect(target.searchParams.get('focus')?.split(',').sort()).toEqual(['one', 'two']);
    expect(target.searchParams.get('from')).toBe('2026-10-05'); expect(target.searchParams.get('to')).toBe(day);
    expect(keywordNotification([keyword(), keyword({ id: 'two', name: 'SK하이닉스' })], data, result.seen, false, true, day).payload).toBeNull();
  });
  it('로그아웃·만료·알림끔은 개인발송0이며 일반 아침 설정은 유지한다', () => {
    const data = snapshot([article(1)]);
    expect(keywordNotification([keyword()], data, [], false, false, day).payload).toBeNull();
    expect(keywordNotification([keyword()], data, [], true, false, day).payload?.title).toBe('오늘의 뉴스가 도착했어요');
    expect(keywordNotification([keyword({ alerts: false })], data, [], false, true, day).payload).toBeNull();
  });
  it('저장 순서가 뒤인 키워드만 새 소식이 있어도 해당 ID를 가리킨다', () => {
    const data = snapshot([article(1, { title: 'SK하이닉스 실적 발표' })]);
    const result = keywordNotification([keyword(), keyword({ id: 'second', name: 'SK하이닉스' })], data, [], false, true, day);
    expect(new URL(result.payload!.url, 'https://issuerank.example').searchParams.get('focus')).toBe('second');
  });
  it('같은 기사 수 변화·오래된 보도의 재수집·기준 미달은 개인알림 대상이 아니다', () => {
    const data = snapshot([article(1, { publishedAt: '2026-10-01T00:00:00Z' }), article(2, { title: '삼성전자 출시 전망' })]);
    expect(keywordNotification([keyword()], data, [], false, true, day).payload).toBeNull();
  });
});
