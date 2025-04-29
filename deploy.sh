#!/bin/bash
set -e

IMAGE_TO_DEPLOY="$1"

if [ -z "$IMAGE_TO_DEPLOY" ]; then
  echo "오류: 배포할 Docker 이미지 이름이 지정되지 않았습니다."
  echo "사용법: ./deploy.sh <이미지_전체_이름>"
  exit 1
fi

echo "==== 배포 시작: ${IMAGE_TO_DEPLOY} ===="


echo "Docker Image pull... ${IMAGE_TO_DEPLOY}"
docker pull "${IMAGE_TO_DEPLOY}" || { echo "오류: 이미지 풀(pull) 실패"; exit 1; }

echo "기존 컨테이너 중지 및 삭제..."
docker compose down

echo "New Container Running... ${IMAGE_TO_DEPLOY}"
DEPLOY_IMAGE_NAME="${IMAGE_TO_DEPLOY}" docker compose up -d

echo "==== 배포 완료! ===="

exit 0