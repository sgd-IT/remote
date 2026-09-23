#!/bin/bash
set -e

# ========================================
# 安威远程监控系统 - 从 Harbor 拉取镜像
# ========================================

# 读取 .env 配置
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
if [ -f "$ENV_FILE" ]; then
    eval "$(sed 's/\r$//' "$ENV_FILE" | grep -E '^[A-Z_]+=.+')"
fi

INFRA_ACR="${ACR_INFRA:-**:8800/library}"
APP_ACR="${ACR_REGISTRY:-**:8800/anwei-remote-monitor}"
SERVER_VER="${SERVER_VERSION:-v1.0}"
FRONTEND_VER="${FRONTEND_VERSION:-v1.0}"

echo ""
echo "========================================"
echo "  请选择拉取目标"
echo "========================================"
echo "  1. 全部拉取  （基础设施 + 应用镜像）"
echo "  2. 只拉取应用镜像 （server + frontend）"
echo "  3. 只拉取基础设施镜像 （tdengine）"
echo "  4. 只拉取 anwei-remote-monitor"
echo "  5. 只拉取 nginx-frontend"
echo "  6. 只拉取 TDengine"
echo "========================================"
read -p "请输入序号 [1-6]: " choice
choice=$(echo "$choice" | tr -d '\r')

case "$choice" in
  1) need_infra=1; need_server=1; need_frontend=1 ;;
  2) need_infra=0; need_server=1; need_frontend=1 ;;
  3) need_infra=1; need_server=0; need_frontend=0 ;;
  4) need_infra=0; need_server=1; need_frontend=0 ;;
  5) need_infra=0; need_server=0; need_frontend=1 ;;
  6) need_infra=0; need_tdengine=1; need_server=0; need_frontend=0 ;;
  *) echo "ERROR: 无效选择 '$choice'"; exit 1 ;;
esac

# 如果选了3，需要拉取基础设施
if [ "$choice" = "3" ]; then
    need_tdengine=1
fi
if [ "$choice" = "1" ]; then
    need_tdengine=1
fi

echo ""
echo "========================================"
echo "  Docker login to Harbor"
echo "========================================"
HARBOR_HOST=$(echo "$INFRA_ACR" | cut -d/ -f1)
docker login "$HARBOR_HOST" -u admin -p AWHarbor

if [ "${need_infra:-0}" -eq 1 ] || [ "${need_tdengine:-0}" -eq 1 ]; then
    echo ""
    echo "========================================"
    echo "  Pull TDengine image"
    echo "========================================"
    docker pull "$INFRA_ACR/tdengine:3.0.7.1"
fi

if [ "$need_server" -eq 1 ]; then
    echo "Pulling $APP_ACR/anwei-remote-monitor:$SERVER_VER ..."
    docker pull "$APP_ACR/anwei-remote-monitor:$SERVER_VER"
fi

if [ "$need_frontend" -eq 1 ]; then
    echo "Pulling $APP_ACR/nginx-frontend:$FRONTEND_VER ..."
    docker pull "$APP_ACR/nginx-frontend:$FRONTEND_VER"
fi

echo ""
echo "========================================"
echo "  本地镜像列表"
echo "========================================"
docker images | grep -E "anwei-remote-monitor|nginx-frontend|tdengine"

echo ""
if [ "${need_tdengine:-0}" -eq 1 ] || [ "${need_infra:-0}" -eq 1 ]; then echo "  TDengine 3.0.7.1"; fi
if [ "$need_server" -eq 1 ];  then echo "  anwei-remote-monitor:$SERVER_VER"; fi
if [ "$need_frontend" -eq 1 ]; then echo "  nginx-frontend:$FRONTEND_VER"; fi
echo ""
echo "Done. Run 'docker-compose up -d' to start services."
