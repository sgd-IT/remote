#!/bin/bash
# ========================================
# 安威远程监控系统 - 启动/停止服务
# 用法: ./start-services.sh [start|stop|restart|status|logs]
# ========================================

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

case "${1:-start}" in
    start)
        echo "Starting all services..."
        # 预创建日志目录，避免 Docker bind mount 未自动创建
        mkdir -p "$SCRIPT_DIR/data/logs/server"
        docker-compose up -d
        echo ""
        echo "Service status:"
        docker-compose ps
        ;;
    stop)
        echo "Stopping all services..."
        docker-compose down
        ;;
    restart)
        echo "Restarting all services..."
        docker-compose restart
        ;;
    status)
        docker-compose ps
        ;;
    logs)
        docker-  logs -f --tail=100
        ;;
    *)
        echo "用法: $0 [start|stop|restart|status|logs]"
        echo ""
        echo "  start    启动所有服务（默认）"
        echo "  stop     停止所有服务"
        echo "  restart  重启所有服务"
        echo "  status   查看服务状态"
        echo "  logs     查看实时日志"
        exit 1
        ;;
esac
