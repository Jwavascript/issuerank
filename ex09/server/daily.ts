// npm run daily [-- --pages N] [--search N] [--max N]: 하루 실행 collect → group → summarize를 한 명령으로(US-01, D5)
// 각 단계는 자식 프로세스 tsx server/{단계}.ts로 돌고 출력 · 종료 코드는 그 명령 그대로. 순서 · 멈춤 · 종료 코드는 steps.ts
// 종료 코드: 실행한 단계 코드의 최댓값(0 모두 정상 · 1 일부 실패 · 2 멈춤)
import { spawn } from 'node:child_process';
import { hide } from './hide';
import { runDaily, type Step } from './steps';

// 자식은 이 프로세스의 env(.env를 읽은 값 포함)를 그대로 받음. 못 띄우면 던짐(steps.ts가 `실행 오류: {단계}`)
function runStep(step: Step, args: string[]): Promise<number> {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, ['--import', 'tsx', `server/${step}.ts`, ...args], { stdio: 'inherit' });
    child.on('error', () => reject(new Error('spawn')));
    child.on('close', code => resolve(code ?? 2));
  });
}

runDaily(process.argv.slice(2), runStep, line => console.log(hide(line)))
  .then(code => { process.exitCode = code; });
