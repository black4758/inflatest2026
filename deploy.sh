#!/bin/bash
set -e

echo "=================================================="
echo "🚀 [Blue/Green Deploy] Zero-Downtime Deployment"
echo "=================================================="

# ----------------------------------------------------
# 1. 서버 Swap 메모리 점검 및 자동 할당 (2GB)
# ----------------------------------------------------
SWAP_TOTAL=$(free -m | awk '/^Swap:/ {print $2}')
if [ "$SWAP_TOTAL" -eq 0 ]; then
  echo "💾 Swap 메모리가 설정되어 있지 않아 2GB 가상 메모리를 생성합니다..."
  sudo fallocate -l 2G /swapfile || sudo dd if=/dev/zero of=/swapfile bs=1M count=2048
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  if ! grep -q '/swapfile' /etc/fstab; then
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
  fi
  echo "✅ 2GB Swap 메모리 할당 완료!"
else
  echo "💾 Swap 메모리 정상 작동 중: ${SWAP_TOTAL}MB"
fi

# ----------------------------------------------------
# 2. Docker 소켓 권한 확인
# ----------------------------------------------------
sudo chmod 666 /var/run/docker.sock || true

# ----------------------------------------------------
# 3. 최신 코드 및 GHCR 이미지 Pull
# ----------------------------------------------------
echo "📥 Pulling latest images from GHCR..."
docker compose pull || sudo docker compose pull

# MySQL, Frontend, Gateway가 아직 안 떠있다면 먼저 기동
docker compose up -d mysql frontend gateway || sudo docker compose up -d mysql frontend gateway

# ----------------------------------------------------
# 4. 현재 활성화된 서비스 확인 및 타겟 결정
# ----------------------------------------------------
UPSTREAM_CONF="./nginx/backend-upstream.conf"
if [ ! -f "$UPSTREAM_CONF" ] || grep -q "backend-green" "$UPSTREAM_CONF"; then
  TARGET_SERVICE="backend-blue"
  OLD_SERVICE="backend-green"
else
  TARGET_SERVICE="backend-green"
  OLD_SERVICE="backend-blue"
fi

echo "🎯 현재 배포 대상 (새 버전): $TARGET_SERVICE"
echo "⏸️ 기존 서비스 중인 대상: $OLD_SERVICE"

# ----------------------------------------------------
# 5. 새 버전 컨테이너 기동
# ----------------------------------------------------
echo "🐳 Starting $TARGET_SERVICE container..."
docker compose up -d $TARGET_SERVICE || sudo docker compose up -d $TARGET_SERVICE

# ----------------------------------------------------
# 6. Actuator 헬스체크 대기 (새 컨테이너가 UP 될 때까지)
# ----------------------------------------------------
echo "⏳ Waiting for $TARGET_SERVICE (/actuator/health) to be UP..."
MAX_RETRIES=25
RETRY_COUNT=0
HEALTHY=false

until [ $RETRY_COUNT -ge $MAX_RETRIES ]
do
  STATUS=$(docker exec infra-$TARGET_SERVICE curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -o '"status":"UP"' || true)
  if [ "$STATUS" == '"status":"UP"' ]; then
    HEALTHY=true
    echo "💚 $TARGET_SERVICE is healthy and ready to receive traffic!"
    break
  fi
  RETRY_COUNT=$((RETRY_COUNT+1))
  echo "Still checking $TARGET_SERVICE health ($RETRY_COUNT/$MAX_RETRIES)..."
  sleep 3
done

if [ "$HEALTHY" != "true" ]; then
  echo "❌ [배포 실패] $TARGET_SERVICE 헬스체크가 실패했습니다!"
  echo "기존 서비스($OLD_SERVICE)를 유지하고 새 컨테이너를 중지합니다."
  docker compose stop $TARGET_SERVICE || true
  exit 1
fi

# ----------------------------------------------------
# 7. Nginx 트래픽 스위칭 (0.01초 무중단 전환)
# ----------------------------------------------------
echo "🔄 Switching Nginx upstream to $TARGET_SERVICE..."
echo "server $TARGET_SERVICE:8080;" > "$UPSTREAM_CONF"
docker exec infra-gateway nginx -s reload || sudo docker exec infra-gateway nginx -s reload
echo "✨ Traffic successfully routed to $TARGET_SERVICE!"

# ----------------------------------------------------
# 8. 기존 이전 버전 컨테이너 안전 종료
# ----------------------------------------------------
echo "🛑 Stopping and removing previous container: $OLD_SERVICE..."
docker compose stop $OLD_SERVICE || true
docker compose rm -f $OLD_SERVICE || true

# ----------------------------------------------------
# 9. 찌꺼기 이미지 정리
# ----------------------------------------------------
echo "🧹 Cleaning up unused Docker images..."
docker image prune -f || true

echo "=================================================="
echo "🎉 [Success] Blue/Green Zero-Downtime Deployment Finished!"
echo "=================================================="
