#!/usr/bin/env bash
#
# Xoá sạch kind cluster "social". Xem docs/k8s-getting-started.md mục
# "Shutdown an toàn" cho các mức độ khác + lý do đây là mặc định hợp lý.
#
# Dùng: scripts/shutdown-k8s.sh [-y|--yes]
#   -y, --yes   bỏ qua xác nhận, chạy thẳng (dùng khi gọi từ script khác)

set -euo pipefail

CLUSTER="social"
NAMESPACE="social"
SKIP_CONFIRM=false

for arg in "$@"; do
  case "$arg" in
    -y|--yes) SKIP_CONFIRM=true ;;
    *)
      echo "Không nhận diện được tham số: $arg" >&2
      echo "Dùng: $0 [-y|--yes]" >&2
      exit 1
      ;;
  esac
done

if ! command -v kind >/dev/null 2>&1; then
  echo "Không tìm thấy lệnh 'kind' — không có gì để dọn (chưa cài, hoặc chưa từng tạo cluster)." >&2
  exit 0
fi

if ! kind get clusters 2>/dev/null | grep -qx "$CLUSTER"; then
  echo "Cluster '$CLUSTER' không tồn tại — có thể đã xoá rồi, không có gì để làm."
  exit 0
fi

# Dọn port-forward chạy nền của project trước khi xoá, tránh orphan process.
PF_PIDS=$(pgrep -f "kubectl[^|]*port-forward[^|]*-n ${NAMESPACE}" 2>/dev/null || true)
if [ -n "$PF_PIDS" ]; then
  echo "Đang dừng các kubectl port-forward chạy nền cho namespace '$NAMESPACE':"
  while IFS= read -r pid; do
    [ -z "$pid" ] && continue
    ps -p "$pid" -o command= 2>/dev/null | sed 's/^/  - /' || true
    kill "$pid" 2>/dev/null || true
  done <<< "$PF_PIDS"
fi

echo
echo "Sắp xoá hoàn toàn kind cluster '$CLUSTER' (kể cả Traefik, mọi image đã"
echo "kind-load vào node). Không có data nào bị mất — repo không dùng PVC ở"
echo "đâu cả. Image trên Docker local (docker images) KHÔNG bị xoá."
echo

if [ "$SKIP_CONFIRM" = false ]; then
  if [ ! -t 0 ]; then
    echo "Không chạy trong terminal tương tác — cần cờ -y/--yes để xác nhận, không tự đoán ý bạn." >&2
    exit 1
  fi
  read -r -p "Xoá cluster '$CLUSTER' ngay bây giờ? [y/N] " confirm
  case "$confirm" in
    y|Y|yes|YES) ;;
    *) echo "Huỷ — không xoá gì."; exit 0 ;;
  esac
fi

kind delete cluster --name "$CLUSTER"

echo
echo "✅ Đã xoá cluster '$CLUSTER'."
echo "   Dựng lại từ đầu: make cluster-create (xem docs/k8s-getting-started.md)"
echo "   Dọn thêm image Docker local nếu cần: docker image prune"
