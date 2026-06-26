#!/bin/bash
# ============================================
# 自动部署脚本 — 构建 → 打包 → 上传 → 重启
# 用法: ./deploy.sh
# ============================================
set -e

SERVER="root@203.0.113.10"
SSH_KEY="$HOME/.ssh/id_ed25519"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_DIR/restaurant-cashier-system 4.0"
FRONTEND_DIR="$PROJECT_DIR/cashier-front2.0"
JAR_NAME="restaurant-cashier-system-0.0.1-SNAPSHOT.jar"
REMOTE_DIR="/root"
NGINX_DIR="/var/www/vue-app"

echo "============================================"
echo "  餐厅收银系统 — 自动部署"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================"

# --- 1. 构建后端 ---
echo ""
echo "[1/6] 构建后端 JAR..."
cd "$BACKEND_DIR"
mvn package -DskipTests -q
echo "  ✅ 后端构建完成"

# --- 2. 构建前端 ---
echo ""
echo "[2/6] 构建前端..."
cd "$FRONTEND_DIR"
npx vite build 2>&1 | grep "built"
echo "  ✅ 前端构建完成"

# --- 3. 打包 ---
echo ""
echo "[3/6] 打包 deploy.tar.gz..."
cd "$PROJECT_DIR"
rm -f deploy.tar.gz
cp "$BACKEND_DIR/target/$JAR_NAME" .
cp -r "$FRONTEND_DIR/dist" .
tar -czf deploy.tar.gz "$JAR_NAME" dist/
rm -f "$JAR_NAME"
rm -rf dist
echo "  ✅ deploy.tar.gz $(du -h deploy.tar.gz | cut -f1)"

# --- 4. 上传 ---
echo ""
echo "[4/6] 上传到服务器..."
scp -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new deploy.tar.gz "$SERVER:$REMOTE_DIR/"
echo "  ✅ 上传完成"

# --- 5. 服务器端部署 ---
echo ""
echo "[5/6] 停旧服务 + 部署新版本..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$SERVER" << 'ENDSSH'
set -e

# 停旧后端
OLD_PID=$(pgrep -f "restaurant-cashier" 2>/dev/null || true)
if [ -n "$OLD_PID" ]; then
    echo "  停止旧后端 PID: $OLD_PID"
    kill $OLD_PID 2>/dev/null || true
    sleep 2
fi

# 解压
cd /root
tar -xzf deploy.tar.gz

# 更新前端
rm -rf /var/www/vue-app/*
cp -r dist/* /var/www/vue-app/

# 清理解压残留
rm -rf dist restaurant-cashier-system-0.0.1-SNAPSHOT.jar

echo "  ✅ 文件部署完成"
ENDSSH

# --- 6. 启动后端 ---
echo ""
echo "[6/6] 启动后端..."
ssh -i "$SSH_KEY" -o StrictHostKeyChecking=accept-new "$SERVER" "
cd /root && nohup java -jar $JAR_NAME \
  --spring.profiles.active=prod \
  --spring.datasource.url='jdbc:mysql://localhost:3306/restaurant_chain?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&allowMultiQueries=true' \
  --spring.data.redis.host=localhost \
  > app.log 2>&1 &
sleep 8

# 健康检查
HEALTH=\$(curl -s http://localhost:8088/actuator/health | python3 -c 'import sys,json;print(json.load(sys.stdin).get(\"status\",\"DOWN\"))' 2>/dev/null || echo 'DOWN')
if [ \"\$HEALTH\" = \"UP\" ]; then
    echo '  ✅ 后端启动成功，健康检查 UP'
else
    echo '  ⚠️  健康检查 FAIL，请检查 /root/app.log'
    tail -20 /root/app.log | grep -i error || true
fi

# Nginx 重载
nginx -t > /dev/null 2>&1 && nginx -s reload
"

# --- 完成 ---
echo ""
echo "============================================"
echo "  🚀 部署完成!"
echo "  前端: http://203.0.113.10"
echo "  健康: http://203.0.113.10/actuator/health"
echo "============================================"
