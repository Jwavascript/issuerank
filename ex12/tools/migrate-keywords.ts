// Actions 전용0008 실행. DB Secret은 env로만 받고 stdout/stderr에는 고정 코드·숫자만 쓴다.
import postgres from 'postgres';
import { migrationState, readKeywordMigration, type MigrationHistory } from '../server/keyword-migration';

const quote = (name: string) => '"' + name.replaceAll('"', '""') + '"';
const NEW_TABLES = ['saved_keywords', 'keyword_devices'];
type Sql = ReturnType<typeof postgres>;
type Transaction = postgres.TransactionSql;
const output = (code: string, value?: number | boolean) => console.log('KEYWORD_MIGRATION_' + code + (value === undefined ? '' : '=' + value));

async function schemaReady(tx: Transaction) {
  const relations = await tx<{ relname: string; relrowsecurity: boolean }[]>`select c.relname, c.relrowsecurity from pg_class c join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and c.relname in ('saved_keywords','keyword_devices') and c.relkind='r'`;
  const columns = await tx<{ table_name: string; column_name: string }[]>`select table_name,column_name from information_schema.columns where table_schema='public' and table_name in ('saved_keywords','keyword_devices')`;
  const expected = {
    saved_keywords: ['id', 'user_id', 'name', 'normalized', 'color', 'alerts', 'baseline', 'created_at', 'enabled_at'],
    keyword_devices: ['subscription_id', 'user_id', 'session_id', 'morning', 'seen'],
  };
  const fks = await tx<{ relname: string; target: string; action: string; convalidated: boolean }[]>`select c.relname, target.relname as target, k.confdeltype as action, k.convalidated from pg_constraint k join pg_class c on c.oid=k.conrelid join pg_namespace n on n.oid=c.relnamespace join pg_class target on target.oid=k.confrelid where n.nspname='public' and c.relname in ('saved_keywords','keyword_devices') and k.contype='f'`;
  const expectedFks = ['keyword_devices:push_subscriptions:c', 'keyword_devices:user:c', 'keyword_devices:session:n', 'saved_keywords:user:c'];
  return relations.length === 2 && relations.every(r => r.relrowsecurity)
    && Object.entries(expected).every(([table, names]) => columns.filter(c => c.table_name === table).map(c => c.column_name).sort().join() === [...names].sort().join())
    && fks.length === 4 && expectedFks.every(f => fks.some(k => k.convalidated && `${k.relname}:${k.target}:${k.action}` === f));
}

async function snapshot(tx: Transaction, tables: string[]) {
  const result = new Map<string, string>();
  for (const table of tables) {
    const [row] = await tx.unsafe<{ count: string; digest: string }[]>(`select count(*)::text as count, md5(coalesce(string_agg(md5(row_to_json(t)::text),'' order by md5(row_to_json(t)::text)),'')) as digest from public.${quote(table)} t`);
    result.set(table, row.count + ':' + row.digest);
  }
  return result;
}

async function main() {
  const args = process.argv.slice(2);
  if (args.length !== 1 || !['--check', '--apply'].includes(args[0])) { output('ARGUMENT_INVALID'); return 2; }
  if (!process.env.DATABASE_URL || process.env.GITHUB_ACTIONS !== 'true') { output('ACTIONS_CONFIG_REQUIRED'); return 2; }
  let client: Sql | undefined;
  try {
    const migration = readKeywordMigration();
    client = postgres(process.env.DATABASE_URL, { prepare: false, max: 1, connect_timeout: 15, idle_timeout: 5, onnotice: () => {} });
    const result = await client.begin(async tx => {
      await tx`select pg_advisory_xact_lock(1794700800)`;
      await tx`set local lock_timeout = '15s'`;
      await tx`set local statement_timeout = '60s'`;
      const history = await tx<MigrationHistory[]>`select hash,created_at from drizzle.__drizzle_migrations order by created_at`;
      const relations = await tx<{ tablename: string }[]>`select tablename from pg_tables where schemaname='public' order by tablename`;
      const tables = relations.map(t => t.tablename);
      output('HISTORY_COUNT', history.length);
      const state = migrationState(migration, history, tables);
      output('READY', true);
      if (state === 'applied') {
        if (!await schemaReady(tx)) throw new Error('MIGRATION_SCHEMA_MISMATCH');
        return 'ALREADY_APPLIED';
      }
      if (args[0] === '--check') return 'CHECK_OK';
      // 기존 표 쓰기를 잠시 기다리게 하고 같은 트랜잭션 안에서 내용 보존을 확인한다.
      const existing = tables.filter(t => !NEW_TABLES.includes(t));
      if (existing.length) await tx.unsafe('lock table ' + existing.map(t => 'public.' + quote(t)).join(', ') + ' in share mode');
      const before = await snapshot(tx, existing);
      for (const command of migration.commands) await tx.unsafe(command);
      if (!await schemaReady(tx)) throw new Error('MIGRATION_SCHEMA_MISMATCH');
      for (const table of NEW_TABLES) {
        const [row] = await tx.unsafe<{ count: string }[]>(`select count(*)::text as count from public.${quote(table)}`);
        if (row.count !== '0') throw new Error('MIGRATION_NEW_TABLE_NOT_EMPTY');
      }
      const after = await snapshot(tx, existing);
      if ([...before].some(([table, value]) => after.get(table) !== value)) throw new Error('MIGRATION_PRESERVATION_FAILED');
      await tx`insert into drizzle.__drizzle_migrations (hash,created_at) values (${migration.hash},${migration.entry.when})`;
      output('OLD_TABLES_PRESERVED', true);
      output('NEW_TABLES', 2);
      return 'APPLIED';
    });
    output(result);
    return 0;
  } catch (error) {
    const code = error instanceof Error && /^MIGRATION_[A-Z_]+$/.test(error.message) ? error.message.slice('MIGRATION_'.length) : 'FAILED';
    output(code); return 2;
  } finally {
    await client?.end({ timeout: 5 }).catch(() => {});
  }
}
main().then(code => { process.exitCode = code; }).catch(() => { output('FAILED'); process.exitCode = 2; });
