#!/usr/bin/env bash
#
# Deploy service theo đúng thứ tự phụ thuộc, đợi Ready mới sang service tiếp
# theo — tránh CPU/memory storm khi nhiều JVM cold-start cùng lúc trên máy
# tài nguyên hạn chế (kind local). Xem specs/service-dependencies.md cho lý
# do thứ tự này.
#
# Dùng: scripts/deploy-sequential.sh [timeout]
#   timeout   timeout rollout mỗi service, mặc định 120s

set -euo pipefail

NAMESPACE="social"
TIMEOUT="${1:-120s}"

ORDER=(
  auth-service
  post-api
  user-api
  notification-api
  websocket-service
  interaction-service
  user-consumer
  post-consumer
  notification-consumer
)

FAILED=()

for name in "${ORDER[@]}"; do
  echo "=== Deploying $name ==="
  kubectl apply -f "k8s/services/${name}.yaml"
  if kubectl rollout status "deployment/${name}" -n "$NAMESPACE" --timeout="$TIMEOUT"; then
    echo "✅ $name Ready"
  else
    echo "⚠️  $name chưa Ready sau ${TIMEOUT} — tiếp tục sang service kế tiếp"
    FAILED+=("$name")
  fi
  echo
done

echo "================================================"
if [ ${#FAILED[@]} -eq 0 ]; then
  echo "✅ Tất cả 9 service đã Ready."
else
  echo "⚠️  ${#FAILED[@]} service chưa Ready: ${FAILED[*]}"
  echo "   Kiểm tra: kubectl logs -n $NAMESPACE deployment/<tên> --tail=50"
fi
