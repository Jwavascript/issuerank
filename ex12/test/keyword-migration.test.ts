import { readFileSync } from 'node:fs';
import { generateDrizzleJson, generateMigration } from 'drizzle-kit/api';
import { describe, expect, it } from 'vitest';
import * as schema from '../server/db/schema';
const read = (file: string) => readFileSync(file, 'utf8').replace(/\r\n/g, '\n');
describe('관심 키워드 추가 마이그레이션', () => {
  it('기존 테이블은 유지하고 새 표2개·RLS·소유권FK만 추가한다', async () => {
    const before = JSON.parse(read('drizzle/meta/0007_snapshot.json'));
    const after = JSON.parse(read('drizzle/meta/0008_snapshot.json'));
    for (const [table, definition] of Object.entries(before.tables)) expect(after.tables[table]).toEqual(definition);
    expect(Object.keys(after.tables).length - Object.keys(before.tables).length).toBe(2);
    const commands = await generateMigration(before, after);
    expect(commands.join('\n--> statement-breakpoint\n').trim()).toBe(read('drizzle/0008_keyword_timeline.sql').trim());
    expect(commands.some(sql => /DROP|DELETE|TRUNCATE/.test(sql.replace(/ON DELETE (?:cascade|set null)/g, '')))).toBe(false);
    expect(after.tables['public.keyword_devices'].isRLSEnabled).toBe(true);
    expect(after.tables['public.saved_keywords'].isRLSEnabled).toBe(true);
  });
  it('현재 Drizzle 스키마와0008 snapshot 사이에 빠진 변경이 없다', async () => {
    const after = JSON.parse(read('drizzle/meta/0008_snapshot.json'));
    expect(await generateMigration(after, generateDrizzleJson(schema, after.id))).toEqual([]);
  });
});
