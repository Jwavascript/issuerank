import { describe, expect, it } from 'vitest';
import { migrationState, readKeywordMigration } from '../server/keyword-migration';
const migration = readKeywordMigration();
const baseline = migration.entries.slice(0, -1).map(e => ({ created_at: String(e.when), hash: 'legacy' }));
describe('운영0008 전용 적용 가드', () => {
  it('기존0007까지의 이력·새 표 없음만 pending으로 허용한다', () => {
    expect(migration.commands).toHaveLength(8);
    expect(migrationState(migration, baseline, ['articles', 'issues'])).toBe('pending');
  });
  it('이력 누락·새 표 일부 생성·모르는 추가 이력은 거절한다', () => {
    for (const [history, tables] of [[baseline.slice(1), []], [baseline, ['saved_keywords']], [[...baseline, { created_at: '1', hash: 'unknown' }], []]] as const) {
      expect(() => migrationState(migration, [...history], [...tables])).toThrow('MIGRATION_BASELINE_MISMATCH');
    }
  });
  it('같은0008 적용은 이력·SQL 해시·두 표가 모두 맞을 때만 재실행을 건너뛴다', () => {
    const history = [...baseline, { created_at: String(migration.entry.when), hash: migration.hash }];
    expect(migrationState(migration, history, ['saved_keywords', 'keyword_devices'])).toBe('applied');
    expect(() => migrationState(migration, [...baseline, { created_at: String(migration.entry.when), hash: 'wrong' }], ['saved_keywords', 'keyword_devices'])).toThrow('MIGRATION_BASELINE_MISMATCH');
  });
});
