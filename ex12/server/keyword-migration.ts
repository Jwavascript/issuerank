import { createHash } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

export type MigrationEntry = { idx: number; when: number; tag: string };
export type MigrationHistory = { hash: string; created_at: string | number };
export function readKeywordMigration(folder = 'drizzle') {
  const journal = JSON.parse(readFileSync(join(folder, 'meta/_journal.json'), 'utf8')) as { entries: MigrationEntry[] };
  const entry = journal.entries.at(-1);
  if (journal.entries.length !== 9 || entry?.idx !== 8 || entry.tag !== '0008_keyword_timeline') throw new Error('MIGRATION_MANIFEST_INVALID');
  const content = readFileSync(join(folder, entry.tag + '.sql'), 'utf8');
  const commands = content.split('--> statement-breakpoint').map(s => s.trim()).filter(Boolean);
  if (commands.length !== 8 || commands.some(s => !/^(?:CREATE|ALTER) TABLE "(?:saved_keywords|keyword_devices)"\s/.test(s))) throw new Error('MIGRATION_SQL_INVALID');
  const hash = (s: string) => createHash('sha256').update(s).digest('hex');
  return { entries: journal.entries, entry, commands, hash: hash(content), lfHash: hash(content.replace(/\r\n/g, '\n')) };
}

export function migrationState(migration: ReturnType<typeof readKeywordMigration>, history: MigrationHistory[], tables: string[]): 'pending' | 'applied' {
  const numbers = history.map(r => Number(r.created_at)).sort((a, b) => a - b);
  const expected = migration.entries.map(e => e.when).sort((a, b) => a - b);
  const existing = tables.filter(t => ['saved_keywords', 'keyword_devices'].includes(t)).length;
  const same = (a: number[], b: number[]) => a.length === b.length && a.every((n, i) => n === b[i]);
  if (same(numbers, expected.slice(0, -1)) && existing === 0) return 'pending';
  const target = history.find(r => Number(r.created_at) === migration.entry.when);
  if (same(numbers, expected) && existing === 2 && target && [migration.hash, migration.lfHash].includes(target.hash)) return 'applied';
  throw new Error('MIGRATION_BASELINE_MISMATCH');
}
