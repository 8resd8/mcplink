#!/bin/bash
set -e

IMAGE_TO_DEPLOY="$1"

if [ -z "$IMAGE_TO_DEPLOY" ]; then
  echo "오류: 배포할 Docker 이미지 이름이 지정되지 않았습니다."
  exit 1
fi

echo "==== 배포 시작: ${IMAGE_TO_DEPLOY} ===="

echo "Docker Image pull... ${IMAGE_TO_DEPLOY}"
docker pull "${IMAGE_TO_DEPLOY}" || { echo "오류: 이미지 풀(pull) 실패"; exit 1; }


echo "기존 컨테이너 중지..."
docker compose down

echo "New Container Running... ${IMAGE_TO_DEPLOY}"
docker compose up -d --force-recreate --pull always

echo "Docker Image prune"
docker image prune -f || true

exit 0