#!/bin/bash
set -e

# ========================================
# 安威远程监控系统 - 构建并推送镜像到 Harbor
# 用法: ./build-push.sh -v v1.0 [-a ACR] [-i ACR_INFRA] [-b BACKEND_DIR] [-f FRONTEND_DIR] [-j JDK_DIR]
# ========================================

VERSION=""
ACR="**:8800/anwei-remote-monitor"
ACR_INFRA="**:8800/library"
JDK=""
BACKEND_DIR=""
FRONTEND_DIR=""
BACKEND_BRANCH="master"
FRONTEND_BRANCH="master"
DEV_DIR=""
NPM_REGISTRY="https://registry.npmmirror.com/"

usage() {
    echo "用法: $0 -v VERSION [-a ACR] [-i ACR_INFRA] [-b BACKEND_DIR] [-f FRONTEND_DIR] [-j JDK_DIR] [-r NPM_REGISTRY] [--dev-dir DIR] [--backend-branch BRANCH] [--frontend-branch BRANCH]"
    echo ""
    echo "参数:"
    echo "  -v VERSION          镜像版本号，如 v1.0（必填）"
    echo "  -a ACR              Harbor仓库地址（默认: **:8800/anwei-remote-monitor）"
    echo "  -i ACR_INFRA        Harbor基础设施仓库地址（默认: **:8800/library）"
    echo "  -b BACKEND_DIR      后端项目目录（默认: 脚本上级目录）"
    echo "  -f FRONTEND_DIR     前端项目目录（默认: 自动检测同级目录）"
    echo "  -j JDK_DIR          JDK安装目录（默认: 自动检测）"
    echo "  -r NPM_REGISTRY     npm registry 地址（默认: https://registry.npmmirror.com/）"
    echo "  --dev-dir DIR       开发根目录（默认: 脚本上级目录）"
    echo "  --backend-branch    后端Git分支（默认: master）"
    echo "  --frontend-branch   前端Git分支（默认: master）"
    exit 1
}

# ============ 参数解析 ============
while [[ $# -gt 0 ]]; do
    case "$1" in
        -v) VERSION="$2"; shift 2 ;;
        -a) ACR="$2"; shift 2 ;;
        -i) ACR_INFRA="$2"; shift 2 ;;
        -b) BACKEND_DIR="$2"; shift 2 ;;
        -f) FRONTEND_DIR="$2"; shift 2 ;;
        -j) JDK="$2"; shift 2 ;;
        -r) NPM_REGISTRY="$2"; shift 2 ;;
        --dev-dir) DEV_DIR="$2"; shift 2 ;;
        --backend-branch) BACKEND_BRANCH="$2"; shift 2 ;;
        --frontend-branch) FRONTEND_BRANCH="$2"; shift 2 ;;
        -h|--help) usage ;;
        *) echo "未知参数: $1"; usage ;;
    esac
done

if [ -z "$VERSION" ]; then
    echo "ERROR: 必须指定版本号 -v"
    usage
fi

# ============ 动态路径检测 ============
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# DevDir
if [ -z "$DEV_DIR" ]; then
    DEV_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
fi

# BackendDir
if [ -z "$BACKEND_DIR" ]; then
    BACKEND_DIR="$DEV_DIR"
    echo "[Auto] BackendDir = DevDir: $BACKEND_DIR"
fi

# JDK
if [ -z "$JDK" ]; then
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        JDK="$JAVA_HOME"
        echo "[Auto] JDK from JAVA_HOME: $JDK"
    elif command -v java &>/dev/null; then
        JAVA_PATH="$(which java)"
        JDK="$(dirname "$(dirname "$JAVA_PATH")")"
        echo "[Auto] JDK from PATH: $JDK"
    else
        # 搜索常见 Linux / WSL JDK 路径
        SEARCH_PATHS=(
            "/usr/lib/jvm" "/opt" "/usr/local/java"
            # WSL 下 Windows 侧 JDK 路径
            "/mnt/c/Program Files/Eclipse Adoptium"
            "/mnt/c/Program Files/Java"
            "/mnt/d/Java"
        )
        FOUND=""
        for SP in "${SEARCH_PATHS[@]}"; do
            if [ -d "$SP" ]; then
                # 优先找 JDK 21
                FOUND=$(find "$SP" -maxdepth 2 \( -name "java" -o -name "java.exe" \) -path "*/bin/*" 2>/dev/null \
                    | grep -i "21" | head -1)
                # 没找到 21 则找任意版本
                if [ -z "$FOUND" ]; then
                    FOUND=$(find "$SP" -maxdepth 2 \( -name "java" -o -name "java.exe" \) -path "*/bin/*" 2>/dev/null | head -1)
                fi
                if [ -n "$FOUND" ]; then
                    JDK="$(dirname "$(dirname "$FOUND")")"
                    break
                fi
            fi
        done
        if [ -n "$JDK" ]; then
            echo "[Auto] JDK from search: $JDK"
        else
            echo "ERROR: 无法自动检测 JDK，请通过 -j 参数指定路径"
            echo "  示例: $0 -v v1.0 -j '/mnt/c/Program Files/Eclipse Adoptium/jdk-21.0.3.9-hotspot'"
            echo "        $0 -v v1.0 -j /usr/lib/jvm/java-21-temurin"
            exit 1
        fi
    fi
fi


# FrontendDir
if [ -z "$FRONTEND_DIR" ]; then
    FRONTEND_NAME="anwei-fire-remote-monitor-vue"
    DEV_PARENT="$(dirname "$DEV_DIR")"
    CANDIDATE="${DEV_PARENT}/${FRONTEND_NAME}"
    if [ -d "$CANDIDATE" ]; then
        FRONTEND_DIR="$CANDIDATE"
        echo "[Auto] FrontendDir from sibling: $FRONTEND_DIR"
    else
        echo "[Auto] FrontendDir not detected, will prompt later"
    fi
fi

echo ""
echo "--- 检测到的路径 ---"
echo "  DevDir:      $DEV_DIR"
echo "  JDK:         $JDK"
echo "  BackendDir:  $BACKEND_DIR"
echo "  FrontendDir: $FRONTEND_DIR"
echo ""

# ============ 构建目标选择 ============
echo ""
echo "========================================"
echo "  请选择构建目标"
echo "========================================"
echo "  1. 全部构建  （前端 + 后端）"
echo "  2. 只构建后端 （anwei-remote-monitor）"
echo "  3. 只构建前端 （nginx-frontend）"
echo "========================================"
read -p "请输入序号 [1-3]: " choice

case "$choice" in
    1) need_backend=1; need_frontend=1 ;;
    2) need_backend=1; need_frontend=0 ;;
    3) need_backend=0; need_frontend=1 ;;
    *) echo "ERROR: 无效选择 '$choice'，请输入 1-3"; exit 1 ;;
esac

echo ""
if [ "$need_backend" -eq 1 ];  then echo "  [√] 后端构建"; fi
if [ "$need_frontend" -eq 1 ]; then echo "  [√] 前端构建"; fi
echo ""

step() {
    echo ""
    echo "========================================"
    echo "  $1"
    echo "========================================"
}

check() {
    if [ $? -ne 0 ]; then
        echo "ERROR: $1"
        exit 1
    fi
}

# ============ 环境设置 ============
step "Environment Setup"
export JAVA_HOME="$JDK"
export PATH="$JDK/bin:$PATH"
export DOCKER_BUILDKIT=0
echo "JDK: $JDK"
echo "ACR: $ACR"
echo "Version: $VERSION"
if [ "$need_backend" -eq 1 ];  then echo "Build target: Backend"; fi
if [ "$need_frontend" -eq 1 ]; then echo "Build target: Frontend"; fi

# ============ Docker登录 ============
step "Docker login to Harbor"
HARBOR_HOST="$(echo "$ACR" | cut -d'/' -f1)"
docker login "$HARBOR_HOST" -u admin -p AWHarbor
check "docker login failed"

# ============ 后端构建 ============
if [ "$need_backend" -eq 1 ]; then
    step "Check base image"
    BASE_IMG=$(docker images -q eclipse-temurin:21-jre)
    if [ -z "$BASE_IMG" ]; then
        echo "Base image not found locally, trying Harbor..."
        HARBOR_BASE="$ACR_INFRA/eclipse-temurin:21-jre"
        docker pull "$HARBOR_BASE"
        if [ $? -eq 0 ]; then
            docker tag "$HARBOR_BASE" eclipse-temurin:21-jre
            echo "Base image pulled from Harbor and tagged"
        else
            echo "Harbor pull failed, trying Docker Hub mirror..."
            MIRROR_BASE="hub.rat.dev/library/eclipse-temurin:21-jre"
            docker pull "$MIRROR_BASE"
            check "Pull base image failed (both Harbor and mirror)"
            docker tag "$MIRROR_BASE" eclipse-temurin:21-jre
            echo "Base image pulled from mirror and tagged"
        fi
    else
        echo "Base image already exists, skip"
    fi
fi

if [ "$need_backend" -eq 1 ]; then
    step "Check TDengine image"
    TD_IMG=$(docker images -q "$ACR_INFRA/tdengine:3.0.7.1")
    if [ -z "$TD_IMG" ]; then
        echo "TDengine image not found locally, pulling from Docker Hub mirror..."
        TD_MIRROR="hub.rat.dev/tdengine/tdengine:3.0.7.1"
        docker pull "$TD_MIRROR"
        if [ $? -eq 0 ]; then
            docker tag "$TD_MIRROR" "$ACR_INFRA/tdengine:3.0.7.1"
            docker push "$ACR_INFRA/tdengine:3.0.7.1"
            echo "TDengine image pushed to Harbor"
        else
            echo "WARNING: Failed to pull TDengine image, please push manually"
        fi
    else
        echo "TDengine image already exists, skip"
    fi
fi

if [ "$need_backend" -eq 1 ]; then
    step "Check Dockerfile"
    DOCKERFILE="$BACKEND_DIR/server/Dockerfile"
    if [ ! -f "$DOCKERFILE" ]; then
        echo "ERROR: $DOCKERFILE not found"
        exit 1
    fi

    step "Update backend code"
    cd "$BACKEND_DIR"
    git checkout "$BACKEND_BRANCH"
    check "git checkout backend $BACKEND_BRANCH failed"
    git stash push -m "auto-stash before build-push" 2>/dev/null || true
    git pull origin "$BACKEND_BRANCH"
    check "git pull backend failed"
    git stash pop 2>/dev/null || true

    step "Maven package"
    cd "$BACKEND_DIR"
    mvn clean package -pl server -am -DskipTests
    check "mvn package server failed"

    step "Build and push anwei-remote-monitor"
    docker build -t "$ACR/anwei-remote-monitor:${VERSION}" "$BACKEND_DIR/server"
    check "docker build anwei-remote-monitor failed"
    docker push "$ACR/anwei-remote-monitor:${VERSION}"
    check "docker push anwei-remote-monitor failed"
fi

# ============ 前端构建 ============
if [ "$need_frontend" -eq 1 ]; then
    if [ -z "$FRONTEND_DIR" ]; then
        echo "ERROR: 未指定前端项目目录，请通过 -f 参数指定"
        echo "  示例: $0 -v v1.0 -f /path/to/anwei-fire-remote-monitor-vue"
        exit 1
    fi

    # 前置检查：Node.js 版本（Vite 5.x 要求 Node >= 18）
    if ! command -v node &>/dev/null; then
        echo "ERROR: 未检测到 Node.js，请先安装 Node.js 18+"
        echo "  推荐方式: nvm install 18 && nvm use 18"
        exit 1
    fi
    NODE_VER=$(node -v | sed 's/^v//')
    NODE_MAJOR=$(echo "$NODE_VER" | cut -d. -f1)
    echo "Node.js version: $NODE_VER"
    if [ "$NODE_MAJOR" -lt 18 ]; then
        echo "ERROR: Node.js 版本过低 (当前: v${NODE_VER})，Vite 5.x 要求 Node >= 18"
        echo "  升级方式:"
        echo "    nvm install 18 && nvm use 18"
        echo "    或: curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash - && sudo apt-get install -y nodejs"
        exit 1
    fi

    step "Update frontend code"
    cd "$FRONTEND_DIR"
    git checkout "$FRONTEND_BRANCH"
    check "git checkout frontend $FRONTEND_BRANCH failed"
    git stash push -m "auto-stash before build-push" 2>/dev/null || true
    git pull origin "$FRONTEND_BRANCH"
    check "git pull frontend failed"
    git stash pop 2>/dev/null || true

    step "Build and push nginx-frontend"
    DIST_DIR="$FRONTEND_DIR/dist-prod"

    # 0. 检查是否需要重新构建
    cd "$FRONTEND_DIR"
    SKIP_BUILD=false
    if [ -d "$DIST_DIR" ]; then
        echo "dist-prod directory already exists."
        read -p "Rebuild frontend? (y/N): " REBUILD
        if [[ "$REBUILD" != "y" && "$REBUILD" != "Y" && "$REBUILD" != "yes" && "$REBUILD" != "YES" ]]; then
            SKIP_BUILD=true
            echo "Skipping npm build, using existing dist-prod"
        fi
    fi
    if [ "$SKIP_BUILD" = false ]; then
        if [ -d "$DIST_DIR" ]; then
            echo "Cleaning old dist..."
            rm -rf "$DIST_DIR"
        fi
        npm cache clean --force
        npm install --ignore-scripts --legacy-peer-deps --registry "$NPM_REGISTRY" --fetch-retries 5
        check "npm install failed"
        npm run build:prod
        check "npm run build failed"
    fi

    if [ ! -d "$DIST_DIR" ]; then
        echo "ERROR: dist directory not found: $DIST_DIR"
        exit 1
    fi
    echo "dist build completed"

    # 1. 启动临时 nginx 容器
    TEMP_NAME="nginx-build-temp"
    docker rm -f "$TEMP_NAME" 2>/dev/null || true
    # 确保 nginx:1.31.4-alpine 本地可用
    NGINX_LOCAL=$(docker images -q "nginx:1.31.4-alpine")
    if [ -z "$NGINX_LOCAL" ]; then
        echo "Pulling nginx:1.31.4-alpine from mirror..."
        MIRRORS=("hub.rat.dev/library/nginx:1.31.4-alpine" "docker.m.daocloud.io/library/nginx:1.31.4-alpine" "dockerpull.org/library/nginx:1.31.4-alpine")
        PULLED=false
        for M in "${MIRRORS[@]}"; do
            echo "Trying $M ..."
            docker pull "$M"
            if [ $? -eq 0 ]; then
                docker tag "$M" nginx:1.31.4-alpine
                PULLED=true
                echo "nginx:1.31.4-alpine pulled from $M"
                break
            fi
        done
        if [ "$PULLED" = false ]; then
            echo "ERROR: Failed to pull nginx:1.31.4-alpine from all mirrors"
            exit 1
        fi
    fi
    docker run -d --name "$TEMP_NAME" nginx:1.31.4-alpine
    check "docker run nginx-temp failed"
    docker exec "$TEMP_NAME" sh -c "rm -rf /usr/share/nginx/html/*"

    # 2. 通过 tar pipe 传输文件（WSL + Docker Desktop 环境下 docker cp 可能损坏文件）
    echo "Transferring dist files via tar..."
    TAR_FILE="${DIST_DIR}/../dist-build.tar"
    tar -cf "$TAR_FILE" -C "$DIST_DIR" .
    check "tar create failed"
    docker cp "$TAR_FILE" "${TEMP_NAME}:/tmp/dist.tar"
    docker exec "$TEMP_NAME" sh -c "tar -xf /tmp/dist.tar -C /usr/share/nginx/html/ && rm /tmp/dist.tar"
    check "tar extract failed"
    rm -f "$TAR_FILE"
    echo "Dist files transferred successfully"

    # 2b. 通过 tar 传输 nginx.conf（docker cp 在 Docker Desktop 环境下可能损坏文件）
    NGINX_CONF="${SCRIPT_DIR}/config/frontend/nginx.conf"
    if [ -f "$NGINX_CONF" ]; then
        CONF_DIR="$(dirname "$NGINX_CONF")"
        CONF_NAME="$(basename "$NGINX_CONF")"
        CONF_TAR="${SCRIPT_DIR}/nginx-conf.tar"
        tar -cf "$CONF_TAR" -C "$CONF_DIR" "$CONF_NAME"
        docker cp "$CONF_TAR" "${TEMP_NAME}:/tmp/conf.tar"
        docker exec "$TEMP_NAME" sh -c "cd /etc/nginx/conf.d && tar -xf /tmp/conf.tar && mv $CONF_NAME default.conf && rm /tmp/conf.tar"
        check "nginx.conf transfer failed"
        rm -f "$CONF_TAR"
        echo "nginx.conf transferred successfully"
    else
        echo "WARNING: nginx.conf not found at $NGINX_CONF"
    fi

    # 3. Commit 为镜像
    IMAGE_TAG="$ACR/nginx-frontend:${VERSION}"
    docker commit "$TEMP_NAME" "$IMAGE_TAG"
    check "docker commit nginx-frontend failed"

    # 4. 验证 index.html
    CHECK=$(docker run --rm "$IMAGE_TAG" sh -c "head -c 20 /usr/share/nginx/html/index.html" 2>/dev/null)
    if [[ ! "$CHECK" =~ (doctype|html|\<) ]]; then
        echo "ERROR: index.html appears corrupted!"
        docker rm -f "$TEMP_NAME" 2>/dev/null || true
        exit 1
    fi
    echo "index.html verified OK"

    # 4b. 验证 nginx.conf
    CONF_CHECK=$(docker run --rm "$IMAGE_TAG" sh -c "head -c 10 /etc/nginx/conf.d/default.conf" 2>/dev/null)
    if [[ ! "$CONF_CHECK" =~ (server|listen|location) ]]; then
        echo "ERROR: nginx.conf appears corrupted!"
        docker rm -f "$TEMP_NAME" 2>/dev/null || true
        exit 1
    fi
    echo "nginx.conf verified OK"

    # 5. 清理临时容器
    docker rm -f "$TEMP_NAME" 2>/dev/null || true

    # 6. 推送镜像
    docker push "$IMAGE_TAG"
    check "docker push nginx-frontend failed"
fi

# ============ 验证 ============
step "Verify registry images"
curl -s -u "admin:AWHarbor" "http://$HARBOR_HOST/api/v2.0/repositories" | python3 -m json.tool 2>/dev/null || \
curl -s -u "admin:AWHarbor" "http://$HARBOR_HOST/api/v2.0/repositories"

step "ALL DONE"
if [ "$need_backend" -eq 1 ];  then echo "  anwei-remote-monitor:     $ACR/anwei-remote-monitor:$VERSION"; fi
if [ "$need_frontend" -eq 1 ]; then echo "  nginx-frontend:   $ACR/nginx-frontend:$VERSION"; fi
echo ""
