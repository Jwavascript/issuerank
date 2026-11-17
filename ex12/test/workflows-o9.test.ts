// O9 워크플로 · 설정 글자 검사(D37 · AC-01-3 · 01-4): daily.yml 안전망 · 오늘 확인 · health.yml 트리거 · 권한 · vercel.json crons
import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const read = (path: string) => readFileSync(path, 'utf8').replace(/\r\n/g, '\n');
const daily = read('../.github/workflows/daily.yml');
const health = read('../.github/workflows/health.yml');
const push = read('../.github/workflows/push.yml');

describe('daily.yml', () => {
  it('예약은 05:17 KST 안전망 하나 · 수동 실행 입력 trigger(기본 manual)', () => {
    expect(daily.match(/- cron: '([^']+)'/g)).toEqual(["- cron: '17 20 * * *'"]);
    expect(daily).toContain('  workflow_dispatch:\n    inputs:\n      trigger:\n');
    expect(daily).toMatch(/trigger:[\s\S]*?default: manual\n        type: string\n/);
  });
  it('오늘 확인은 예약 · cron 실행에서만, npm ci 뒤 · daily 앞, 종료 0이면 skip', () => {
    const ci = daily.indexOf('npm ci'), guard = daily.indexOf('- name: 오늘 확인'), run = daily.indexOf('- name: npm run daily');
    expect(ci).toBeGreaterThan(0);
    expect(guard).toBeGreaterThan(ci);
    expect(run).toBeGreaterThan(guard);
    // Re-run(재시도 2번째부터)은 사람이 누른 것 — 확인 없이 돈다(D5 보충)
    expect(daily).toContain("if: (github.event_name == 'schedule' || inputs.trigger == 'cron') && github.run_attempt == 1\n");
    // 실행 요약에는 today의 표준 출력 한 줄만(stderr는 작업 로그로)
    expect(daily).toContain('out=$(npm run --silent today 2>"$RUNNER_TEMP/today.err")');
    expect(daily).toContain('if [ "$code" -eq 0 ]; then\n            echo "skip=true" >> "$GITHUB_OUTPUT"');
    expect(daily).toContain("        if: steps.guard.outputs.skip != 'true'\n");
    // 확인 단계는 DATABASE_URL만 받음(뉴스 · OpenAI 키 0)
    const step = daily.slice(guard, run);
    expect(step).toContain('DATABASE_URL: ${{ secrets.DATABASE_URL }}');
    expect(step).not.toMatch(/NEWSDATA|OPENAI/);
  });
  it('daily 실행 단계 · 동시 실행 · 권한 유지, O11·O12 대기 시간과 근거 보관', () => {
    expect(daily).toContain('  group: daily\n  cancel-in-progress: false\n');
    expect(daily).toContain('permissions:\n  contents: read\n');
    expect(daily).toContain('timeout-minutes: 60\n'); // O11·O12: 뉴스 간격 최대34.1분 + AI 여유
    expect(daily).toContain('npm run daily 2>&1 | tee "$RUNNER_TEMP/daily.log"');
    expect(daily).toContain('uses: actions/upload-artifact@v7');
    expect(daily).toContain('path: ex12/.data/ranking-quality/*.json');
    expect(daily).toContain('include-hidden-files: true');
    expect(daily).toContain('retention-days: 14');
  });
});

describe('health.yml', () => {
  it('트리거 = daily 끝 + 수동(mode · test), 권한 = contents read · issues write뿐', () => {
    expect(health).toContain('  workflow_run:\n    workflows: [daily]\n    types: [completed]\n');
    expect(health).toContain('        options: [after, morning]\n');
    expect(health).toContain('      test:\n');
    expect(health).toContain('permissions:\n  contents: read\n  issues: write\n\njobs:');
  });
  it('이벤트 · 입력 값은 env로만(run 줄에 ${{ }} 0)', () => {
    const run = health.slice(health.indexOf('        run: |'));
    expect(run).not.toContain('${{');
    for (const name of ['CONCLUSION: ${{ github.event.workflow_run.conclusion }}', 'MODE: ${{ inputs.mode }}', 'TEST: ${{ inputs.test }}', 'GH_TOKEN: ${{ github.token }}']) {
      expect(health).toContain(name);
    }
  });
  it('이슈 제목 · 같은 경보 생략 · 실행 뒤 검사 인수', () => {
    expect(health).toContain('title="운영 경보 $date"');
    expect(health).toContain('--title "운영 경보 시험 $date"');
    expect(health).toContain('if [[ "$existing" == *"$log"* ]]; then');
    expect(health).toContain('args=(--mode after --run "$CONCLUSION")');
    // 이슈 본문 · 요약 = 표준 출력만, 이슈 찾기는 검색 색인 없이
    expect(health).toContain('> "$RUNNER_TEMP/health.log" 2> "$RUNNER_TEMP/health.err"');
    expect(health).not.toContain('--search');
  });
});

describe('push.yml · vercel.json', () => {
  it('push.yml은 daily 끝에 이어짐 그대로', () => {
    expect(push).toContain('  workflow_run:\n    workflows: [daily]\n    types: [completed]\n');
  });
  it('vercel.json crons 두 개(UTC 19시 · 23시) · regions 그대로', () => {
    expect(JSON.parse(read('vercel.json'))).toEqual({
      regions: ['icn1'],
      crons: [{ path: '/api/cron/daily', schedule: '0 19 * * *' }, { path: '/api/cron/health', schedule: '0 23 * * *' }],
    });
  });
});
