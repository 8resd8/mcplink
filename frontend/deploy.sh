#!/bin/bash
set -e

# 웹사이트 파일이 복사될 EC2 경로
REMOTE_TARGET_DIR="/var/www/scp-link"
# 파일 권한 설정 (Nginx 사용자: www-data)
NGINX_USER="www-data"

echo "Setting permissions for Nginx..."
# /var/www/scp-link 디렉토리의 소유권 및 권한 설정
sudo mkdir -p ${REMOTE_TARGET_DIR}
sudo chown -R ${NGINX_USER}:${NGINX_USER} ${REMOTE_TARGET_DIR}
sudo chmod -R 755 ${REMOTE_TARGET_DIR}

echo "Reloading Nginx configuration..."
sudo systemctl reload nginx

echo "Deployment tasks finished!"

exit 0