# 餐饮连锁收银管理系统

同一仓库维护 Vue 前端与 Spring Boot 后端，前后端分别启动，通过 HTTP 接口通信。

## 目录

- `cashier-front2.0/`：Vue 3、Vite、Element Plus 前端。
- `restaurant-cashier-system 4.0/`：Java 21、Spring Boot 后端。
- `restaurant_chain.sql`：餐饮数据库结构与初始化数据。
- `member_system.sql`：会员模块数据库脚本。
- `seckill_migration.sql`：秒杀功能数据库迁移。
- `deploy.sh`、`docker-compose.yml`：部署相关配置。

## 本地开发

准备 Java 21、Maven、Node.js 22.12 或更高版本，以及项目使用的 MySQL、Redis 服务。先检查数据库脚本与现有数据库结构，使用独立的开发数据库导入所需脚本。

后端使用以下环境变量配置本地环境：

- `DB_USERNAME`：数据库用户名，默认 `root`。
- `DB_PASSWORD`：数据库密码，必须自行设置。
- `JWT_SECRET`：JWT 签名密钥，至少 32 个字符，建议使用随机值。
- `DEEPSEEK_API_KEY`：使用 DeepSeek AI 功能时设置自己的密钥。

后端启动：

```powershell
cd "restaurant-cashier-system 4.0"
mvn spring-boot:run
```

前端启动：

```powershell
cd cashier-front2.0
npm ci
npm run dev
```

前端默认端口为 `5173`，后端默认端口为 `8088`。其余配置参见后端 `src/main/resources/` 与前端 `vite.config.js`。

## 部署配置

仓库中不保存可用的个人服务器凭证。`deploy.sh` 中的 `203.0.113.10` 是示例地址，部署前需要改成自己的服务器，并检查远程目录、SSH 密钥路径、数据库及环境变量。

不要提交真实的 `application-secret.yml`、个人文档、运行日志或包含私有配置的部署包。

## 许可证

本项目代码使用 [MIT 许可证](LICENSE)。第三方依赖及素材仍适用各自的许可证。
