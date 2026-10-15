#!/data/data/com.termux/files/usr/bin/bash
# Termux에 설치된 proot Ubuntu + Linux Node로 ex09 명령을 실행한다.
# 사용: bash scripts/ex09-ubuntu.sh npm test (인수가 없으면 Ubuntu 셸)
set -eu
repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
if ! command -v proot-distro >/dev/null 2>&1; then
  echo "Termux의 proot-distro와 Ubuntu가 필요합니다. HANDOFF.md를 참고하세요." >&2
  exit 1
fi
if [ "$#" -eq 0 ]; then
  set -- /bin/bash
fi
exec proot-distro login ubuntu \
  --bind "$repo_dir:/workspace/capstone" \
  --work-dir /workspace/capstone/ex09 \
  -- /usr/bin/env \
  PATH=/opt/node-v24.18.0-linux-arm64/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
  "$@"
