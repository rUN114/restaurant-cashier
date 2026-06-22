# Bug 梳理报告：餐饮连锁收银管理系统

---

## 一、系统概览

| 维度 | 描述 |
|------|------|
| 核心功能 | POS 收银 → 订单结算 → 会员积分/折扣 → BOM 库存自动消耗 → 多维度统计 |
| 数据流 | 前端 Vue3 → Axios → JWT Filter → Controller → Service → MyBatis-Plus → MySQL |
| 关键状态 | 订单: in_progress → settled / cancelled；桌台: free ↔ occupied；库存: 扣减；会员积分: 增减 |
| 安全层 | Spring Security + JWT + @PreAuthorize + RBAC 五角色 + 分店停用拦截 |
| 缓存层 | Spring Cache + Redis（门店/角色/配置），多 TTL + CacheErrorHandler 降级 |

---

## 二、🔴 高危 Bug（应优先修复）

### Bug #1 — 会员列表 N+1 查询（2 处）

| 项 | 详情 |
|------|------|
| 文件/函数 | `MemberController.java` — `list()` 和 `page()` |
| 触发条件 | 会员表有 500 条记录时访问列表页 |
| 问题 | `for (Member m : list) { memberLevelService.getById(m.getLevelId()); }` — 每条会员记录单独查一次等级表。500 会员 = 501 次 SQL |
| 后果 | 列表页加载 3-5 秒，数据库连接池耗尽 |
| 优先级 | 🔴 高 |

### Bug #2 — 手动积分调整双重记账

| 项 | 详情 |
|------|------|
| 文件/函数 | `MemberController.java` — `adjustPoints()` + `MemberServiceImpl.addPoints()` / `deductPoints()` |
| 触发条件 | 管理员给会员手动加 100 积分 |
| 问题 | `adjustPoints()` 先调用 `memberService.addPoints(dto.getMemberId(), change)`，该方法内部调用 `pointLogService.log()` 记一条。然后 `adjustPoints()` 又自己调用 `pointLogService.log()` 再记一条。同一笔操作生成了两条积分流水记录。 |
| 后果 | 积分流水表出现重复记录，对账混乱 |
| 优先级 | 🔴 高 |

### Bug #3 — 订单重复结算缺少业务层防护

| 项 | 详情 |
|------|------|
| 文件/函数 | `OrderServiceImpl.java` — `settleOrder()` |
| 触发条件 | 同一订单被绕过 Controller 幂等检查调用（定时任务、内部调用） |
| 问题 | `settleOrder` 不检查 `order.getStatus()` 是否已是 "settled"。幂等全靠 Controller 层的 `requestId` Redis SETNX，但 Service 方法本身无状态校验 |
| 后果 | 库存重复扣减、会员重复积分。SETNX 防了网络重放但没防其他调用路径 |
| 优先级 | 🔴 高 |

### Bug #4 — 手动积分调整的竞态条件

| 项 | 详情 |
|------|------|
| 文件/函数 | `MemberController.java` — `adjustPoints()` |
| 触发条件 | 两个管理员同时对同一会员手动调整积分 |
| 问题 | `findByIdForUpdate(memberId)` 获取行锁 → 计算 `after = before + change` → 然后分别调用 `addPoints()`/`deductPoints()`，这两个方法内部再次 SELECT FOR UPDATE + 再次计算。两次加锁之间积分可能已被另一事务修改。`after < 0` 的校验基于第一次读取的值，第二次操作时积分已经变了 |
| 后果 | 积分扣成负数（校验被绕过）；积分变动日志中的 before/after 值与实际不符 |
| 优先级 | 🔴 高 |

### Bug #5 — 采购入库的库存竞态

| 项 | 详情 |
|------|------|
| 文件/函数 | `PurchaseServiceImpl.java` — `save()` |
| 触发条件 | 两批采购同时审批入库，采购的是同一原料 |
| 问题 | `lambdaQuery().eq(...).one()` 无锁读取库存 → `currentStock.add(quantity)` → `updateById(inv)`。两个事务同时读到 stock=10，各自加 5，最终 stock=15 而不是 20（丢失更新） |
| 后果 | 库存数据不准，影响后续 BOM 消耗扣减校验 |
| 优先级 | 🔴 高 |

---

## 三、🟡 中危 Bug

### Bug #6 — POS 前端结账金额为 0 的静默绕过

| 项 | 详情 |
|------|------|
| 文件/函数 | `PosView.vue` — `handleSettle()` 第 641-648 行 |
| 触发条件 | 会员结算信息加载失败时 `memberCheckout.value = null` |
| 问题 | 最终实付 `finalPay` 退化为 `Number(finalAmount.value)` → 如果 `finalAmount` 也是空的且 `discountAmount` 为 0 → `isNaN(f) ? 0` → 结账金额为 0 |
| 后果 | 0 元订单被提交到后端，虽然 `settleOrder` 不会因为金额 0 报错，但会产生一条金额为 0 的已结算订单 |
| 优先级 | 🟡 中 |

### Bug #7 — SecurityUtils.getCurrentStoreId() 硬编码返回 null

| 项 | 详情 |
|------|------|
| 文件/函数 | `SecurityUtils.java` — `getCurrentStoreId()` |
| 触发条件 | 任何调用此方法获取门店 ID 的代码 |
| 问题 | 方法体就是 `return null;`。但 JWT token 中明明存了 `storeId`，Filter 也解析了 |
| 后果 | 依赖此方法的代码拿到 null，可能绕过门店级数据隔离 |
| 优先级 | 🟡 中 |

### Bug #8 — 退单后前端乐观更新桌台状态

| 项 | 详情 |
|------|------|
| 文件/函数 | `PosView.vue` — `handleCancel()` 第 709-711 行 |
| 触发条件 | 退单请求成功返回后，后端实际未更新桌台（异常被吞） |
| 问题 | `t.status = 'free'` 直接在前端改状态。如果后端退单成功但桌台释放失败，前端显示空闲但后端仍为占用 |
| 后果 | UI 与实际状态不一致，服务员对已被占用的桌台开单 |
| 优先级 | 🟡 中 |

### Bug #9 — `request.js` 自动附加 store_id 到所有请求

| 项 | 详情 |
|------|------|
| 文件/函数 | `request.js` — 请求拦截器 第 22 行 |
| 触发条件 | 任何有 `cashier-store-id` cookie 的请求 |
| 问题 | `config.params.store_id = storeId` 给所有请求都加了参数。虽然 Spring 忽略未知参数，但会污染日志、可能命中某些校验、影响 Redis 缓存 key 生成 |
| 后果 | 调试困难；如果将来有人加了 `@RequestParam(required=true) storeId`，接口突然报 400 |
| 优先级 | 🟡 中 |

### Bug #10 — 后端 `/api/members/search` 无频率限制

| 项 | 详情 |
|------|------|
| 文件/函数 | `MemberController.java` — `searchByPhone()` |
| 触发条件 | 攻击者遍历手机号 |
| 问题 | 公开接口（无 @PreAuthorize）、无验证码、无频率限制。返回不同信息区分"未找到"与"已禁用"，可被用于枚举 |
| 后果 | 会员手机号泄露风险 |
| 优先级 | 🟡 中 |

---

## 四、🟢 低危 Bug

### Bug #11 — JWT 默认密钥硬编码在源码中

| 项 | 详情 |
|------|------|
| 文件 | `JwtUtil.java:18` |
| 问题 | `@Value("${jwt.secret:${JWT_SECRET}}")` — 不设环境变量则使用源码中的已知字符串 |
| 后果 | 攻击者拿到源码即可伪造 JWT token |
| 优先级 | 🟢 低（部署时通常会覆盖） |

### Bug #12 — Result.java 无参构造器为 private

| 项 | 详情 |
|------|------|
| 文件 | `Result.java:13` |
| 问题 | `@NoArgsConstructor(access = AccessLevel.PRIVATE)` — Jackson 反序列化通常需要 public 无参构造器。Jackson 可通过反射绕过，但部分序列化场景可能失败 |
| 后果 | 某些反序列化场景（如 Redis 缓存反序列化、测试中 MockMvc）抛异常 |
| 优先级 | 🟢 低 |

### Bug #13 — 菜品图片上传无文件类型校验

| 项 | 详情 |
|------|------|
| 文件 | `DishController.java`（upload-image 方法） |
| 问题 | 接收 `MultipartFile` 直接写入磁盘，不校验扩展名、MIME 类型、文件大小 |
| 后果 | 可上传任意文件（含 webshell），导致 RCE 或存储耗尽 |
| 优先级 | 🟢 低（内网部署环境中风险较低） |

### Bug #14 — 前端多处 `.catch(() => {})` 吞异常

| 项 | 详情 |
|------|------|
| 文件 | `PosView.vue` 等多处 |
| 问题 | `.catch(() => {})` 或 `.catch(e => { /* ignore */ })` 完全丢弃异常 |
| 后果 | 用户操作失败后无任何提示，以为操作成功了 |
| 优先级 | 🟢 低 |

### Bug #15 — JwtAuthenticationFilter 异常分支静默吞异常

| 项 | 详情 |
|------|------|
| 文件 | `JwtAuthenticationFilter.java:85-87` |
| 问题 | `catch (Exception e) { SecurityContextHolder.clearContext(); }` — 吞掉所有异常且无日志。如果 JWT 解析失败（过期、篡改），静默清除上下文 |
| 后果 | JWT 过期返回 403 而非 401，语义不准 |
| 优先级 | 🟢 低 |

---

## 五、前后端契约检查

| 接口 | 问题 | 风险 |
|------|------|------|
| `POST /api/orders` createOrder | `createdBy` 从 token 解析，但 `settledBy` 在 settle 时从前端传 `cashierId`。如果前端传了错误的 cashierId 怎么办？后端无校验 | 中 |
| `PUT /api/orders/settle/{id}` | 接收 `requestId` 为可选参数。不给 requestId 则幂等检查被跳过 | 中 |
| `PUT /api/users/{id}` | `UserDTO` 的 `password` 字段如果为空，会不会把用户密码清空？ | 中 |
| `POST /api/members` | `MemberDTO` 缺少 phone 非空校验 → 可能创建无手机号的会员 | 低 |

---

## 六、盲区声明

以下是我无法确认的风险，因为缺少运行时环境或第三方库内部行为信息：

| # | 盲区 | 原因 |
|---|------|------|
| 1 | MyBatis-Plus 软删除 + @TableLogic 的边界行为 | `User` 的 `@TableLogic` 和手动 `setDeletedAt()` 是否冲突？`UserServiceImpl.softDeleteUser()` 同时用了注解和手动设值 |
| 2 | Spring Cache + Redis 序列化的跨版本兼容 | `RedisConfig` 使用 `GenericJackson2JsonRedisSerializer` + `ObjectMapper.DefaultTyping.NON_FINAL`，类结构变更后旧缓存反序列化可能抛 ClassCastException（已知风险，`v2:` 前缀已缓解） |
| 3 | tianai-captcha 内部的 Redis 依赖 | 验证码组件自带 Redis 集成，如果 Redis 不可用，验证码生成是降级还是崩溃？未测试 |
| 4 | embedded-redis 在 Windows 上的行为 | 嵌入式 Redis 在 Windows 上可能需要 WSL 或特殊配置 |
| 5 | MySQL 8.0 `settled_at` 索引使用情况 | `DATE(settled_at)=CURDATE()` 和 `YEAR()/MONTH()` 是否能用到索引取决于 MySQL 版本和索引类型 |
| 6 | 大事务超时 | `consumeOrderMaterials` 对大量库存行 FOR UPDATE，锁等待可能导致事务超时或死锁 |
| 7 | Vue 3 `crypto.randomUUID()` 兼容性 | 仅在现代浏览器（Chrome 92+）可用，老旧平板/收银机可能不支持 |
