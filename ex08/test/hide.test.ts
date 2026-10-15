import { afterEach, expect, it, vi } from 'vitest';
import { hide } from '../server/hide';

afterEach(() => vi.unstubAllEnvs());

it('OpenAI·newsdata 키와 DB 접속 문자열·인코딩 전후 비밀번호를 숨긴다', () => {
  vi.stubEnv('OPENAI_API_KEY', 'fake-openai-secret');
  vi.stubEnv('NEWSDATA_API_KEY', 'fake-news-secret');
  vi.stubEnv('DATABASE_URL', 'postgres://u:fake%40pass@localhost/db');
  expect(hide('fake-openai-secret fake-news-secret postgres://u:fake%40pass@localhost/db fake%40pass fake@pass'))
    .toBe('*** *** *** *** ***');
});
