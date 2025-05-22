#!/bin/bash
set -e

# env 읽기
if [ -f .env ]; then
  set -a
    source .env
fi

IMAGE_TO_DEPLOY="${BACKEND_IMAGE_TAG}"
BACKEND_CONTAINER_NAME="backend"

if [ -z "$IMAGE_TO_DEPLOY" ]; then
  echo "오류: 배포할 Docker 이미지 태그(BACKEND_IMAGE_TAG 환경 변수)가 지정되지 않았습니다."
  exit 1
fi

# 이미지 전체 이름
FULL_IMAGE_NAME="resd/backend:${IMAGE_TO_DEPLOY}"
echo "==== 배포 시작: ${IMAGE_TO_DEPLOY} ===="

echo "Database Start"
docker compose up -d mongodb mysql --wait || { echo "오류: DB 컨테이너 시작 실패"; exit 1; }

echo "Docker Image pull ${IMAGE_TO_DEPLOY}"
docker pull "${FULL_IMAGE_NAME}" || { echo "오류: 이미지 풀(${FULL_IMAGE_NAME}) 실패"; exit 1; }

echo "Stop Container"
if [ "$(docker ps -q -f name=^/backend)" ]; then
  docker compose stop ${BACKEND_CONTAINER_NAME} || echo "${BACKEND_CONTAINER_NAME} 컨테이너 중지 시도 중 오류 발생"
fi

echo "Remove Container"
EXISTING_CONTAINER_ID=$(docker ps -aq -f name=^/${BACKEND_CONTAINER_NAME}$)
if [ -n "${EXISTING_CONTAINER_ID}" ]; then
  docker compose rm -f ${BACKEND_CONTAINER_NAME}
else
  echo "${BACKEND_CONTAINER_NAME} 이름의 컨테이너를 찾을 수 없어 삭제하지 않습니다."
fi

echo "New Container Start ${IMAGE_TO_DEPLOY}"
docker compose up -d --no-deps --force-recreate ${BACKEND_CONTAINER_NAME} --wait

echo "Clean Docker Image"
docker images resd/backend --format "{{.Repository}}:{{.Tag}}" | sort -r | tail -n +6 | xargs -r docker rmi
docker image prune -f || true