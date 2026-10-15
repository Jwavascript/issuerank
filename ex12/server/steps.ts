// 하루 실행(npm run daily)의 순서 · 멈춤 · 종료 코드 규칙. 단계 실행 함수를 받아 쓰고 process.env · DB · 네트워크는 읽지 않음
// 순서 collect → group → summarize. 어떤 단계가 2(멈춤)이면 남은 단계는 건너뜀. 0 · 1이면 다음 단계 진행(AC-01-2)
export type Step = 'collect' | 'group' | 'summarize';
export const STEPS: Step[] = ['collect', 'group', 'summarize'];

// 단계마다 넘기는 옵션 하나(값 검사는 각 명령이 기존 문구로)
const OPTION_OF: Record<Step, string> = { collect: '--pages', group: '--search', summarize: '--max' };

export type RunStep = (step: Step, args: string[]) => Promise<number>;

// --pages · --search · --max(각각 값 하나)만 받아 단계별 인수로 나눔. 다른 인수가 있으면 null
export function splitArgs(args: string[]): Record<Step, string[]> | null {
  const out: Record<Step, string[]> = { collect: [], group: [], summarize: [] };
  for (let i = 0; i < args.length; i += 2) {
    const step = STEPS.find(s => OPTION_OF[s] === args[i]);
    if (!step) return null;
    out[step] = args.slice(i, i + 2);
  }
  return out;
}

// 단계를 차례로 실행하고 종료 코드(실행한 단계 코드의 최댓값)를 돌려줌. 출력은 log로만
export async function runDaily(args: string[], run: RunStep, log: (line: string) => void): Promise<number> {
  const split = splitArgs(args);
  if (!split) {
    log('--pages · --search · --max만 받습니다');
    return 2;
  }
  const codes: Partial<Record<Step, number>> = {};
  for (const step of STEPS) {
    log(`== ${step} ==`);
    let code: number;
    try {
      code = await run(step, split[step]);
    } catch {
      // 오류 객체는 찍지 않음
      log(`실행 오류: ${step}`);
      code = 2;
    }
    codes[step] = code;
    if (code >= 2) break;
  }
  log(`하루 실행 끝 · ${STEPS.map(s => `${s} ${codes[s] ?? '건너뜀'}`).join(' · ')}`);
  return Math.max(...Object.values(codes));
}
