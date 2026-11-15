import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    include: ['test/**/*.test.ts'], // .data 검증 사본은 제품 테스트가 아니다.
    setupFiles: ['test/setup.ts'],
  },
});
