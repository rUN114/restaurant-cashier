# 安全维护（2026-10-23）

前端构建镜像固定为 Node 22.23.4；本机构建建议使用 .nvmrc 指定的 Node 24.22.1。
后端 Docker 启用 Oracle 针对 CVE-2026-91403 推荐的证书吊销检查。必须允许访问证书 CRL 地址，离线网络可能导致 TLS 连接失败；不要为恢复连接关闭检查。
重新部署时执行 docker compose build --pull，再重建容器；现有容器不会自动更新。本机直接运行 Java 时也需使用已修补的 JDK，或添加相同的两个 JVM 参数。

尚待确认：数据库服务端版本未核实，不能直接升级仅支持 MySQL 8.4+ 的 Connector/J 26.10.0。当前驱动 9.7.0 的升级仍待兼容性验证。
json-smart 2.6.0 仅属于测试依赖；现有 JSONPath 测试依赖它，暂未删除，未将其标记为已修复。

官方依据：
- https://nodejs.org/en/blog/vulnerability/october-2026-security-releases
- https://ops.java/emudhra/
