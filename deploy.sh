#!/bin/bash
set -e

# env 읽기
if [ -f .env ]; then
    source .env
fi

echo "${BLUE_PORT}"

IMAGE_TO_DEPLOY="${BACKEND_IMAGE_TAG}"

if [ -z "$IMAGE_TO_DEPLOY" ]; then
  echo "오류: 배포할 Docker 이미지 태그(BACKEND_IMAGE_TAG 환경 변수)가 지정되지 않았습니다."
  exit 1
fi

FULL_IMAGE_NAME="resd/backend:${IMAGE_TO_DEPLOY}"
echo "==== 배포 시작: ${IMAGE_TO_DEPLOY} ===="

echo "Docker Image pull... ${IMAGE_TO_DEPLOY}"
docker pull "${FULL_IMAGE_NAME}" || { echo "오류: 이미지 풀(${FULL_IMAGE_NAME}) 실패"; exit 1; }

echo "기존 컨테이너 중지..."
docker compose down

echo "New Container Running... ${IMAGE_TO_DEPLOY}"
docker compose up -d --force-recreate --pull always

echo "Docker Image prune"
docker image prune -f || true

exit 0