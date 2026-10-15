import { serve } from '@hono/node-server';
import { createApp } from './app';
import { authOptions, createAuth } from './auth';
import { connectDb } from './db/client';
import { dbStore } from './db/store';

let connection: ReturnType<typeof connectDb> | undefined;
function fatal() { console.error('API_START_FAILED'); process.exit(2); }
process.on('uncaughtException', fatal);
process.on('unhandledRejection', fatal);
try {
  if (!process.env.DATABASE_URL) throw new Error('DB_CONFIG_MISSING');
  authOptions(process.env);
  connection = connectDb();
  const app = createApp(dbStore(connection.db), createAuth(connection.db, process.env));
  const port = Number(process.env.API_PORT || 8787);
  const server = serve({ fetch: app.fetch, port }, info => console.log(`API 서버: http://localhost:${info.port}`));
  const shutdown = () => server.close(() => { void connection?.client.end().finally(() => process.exit(0)); });
  process.on('SIGTERM', shutdown);
  process.on('SIGINT', shutdown);
} catch {
  console.error('API_START_FAILED');
  void connection?.client.end();
  process.exitCode = 2;
}
