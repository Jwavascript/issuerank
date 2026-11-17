import type { Fetch } from './newsdata';

// 무료 플랜 15분30회. 첫 호출도 기다려 collect→group 경계를 보호한다.
export function newsFetch(send: Fetch): Fetch {
  return async (url, init) => {
    await new Promise(resolve => setTimeout(resolve, 31_000));
    return send(url, init);
  };
}
